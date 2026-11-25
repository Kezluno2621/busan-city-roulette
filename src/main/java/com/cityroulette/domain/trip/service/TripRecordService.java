package com.cityroulette.domain.trip.service;

import com.cityroulette.domain.member.entity.Member;
import com.cityroulette.domain.trip.entity.TripRecord;
import com.cityroulette.domain.trip.dto.StoredFileInfo;
import com.cityroulette.domain.trip.dto.TripAttachment;
import com.cityroulette.domain.trip.dto.TripRecordRequest;
import com.cityroulette.domain.member.repository.MemberRepository;
import com.cityroulette.domain.trip.repository.TripRecordRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class TripRecordService {

    private final TripRecordRepository tripRecordRepository;
    private final MemberRepository memberRepository;
    private final FileStorageService fileStorageService;

    public TripRecordService(
            TripRecordRepository tripRecordRepository,
            MemberRepository memberRepository,
            FileStorageService fileStorageService) {
        this.tripRecordRepository = tripRecordRepository;
        this.memberRepository = memberRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public TripRecord create(String username, TripRecordRequest request) {
        Member member = requireMember(username);
        Optional<StoredFileInfo> storedFile = fileStorageService.store(request.getImage());
        StoredFileInfo fileInfo = storedFile.orElse(null);

        TripRecord record = new TripRecord(
                member,
                request.getTitle().trim(),
                request.getPlaceName().trim(),
                trimToNull(request.getPlaceAddress()),
                trimToNull(request.getMemo()),
                request.getVisitDate(),
                fileInfo == null ? null : fileInfo.originalFileName(),
                fileInfo == null ? null : fileInfo.storedFileName(),
                fileInfo == null ? null : fileInfo.contentType(),
                fileInfo == null ? null : fileInfo.size());

        try {
            return tripRecordRepository.saveAndFlush(record);
        } catch (DataAccessException exception) {
            storedFile.ifPresent(info -> deleteAfterFailedSave(info.storedFileName()));
            throw new TripRecordSaveException("여행 기록을 저장하지 못했습니다.", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<TripRecord> findAll(String username) {
        return tripRecordRepository.findByMemberOrderByCreatedAtDesc(requireMember(username));
    }

    @Transactional(readOnly = true)
    public TripRecord findOne(String username, Long id) {
        return tripRecordRepository.findByIdAndMember(id, requireMember(username))
                .orElseThrow(TripRecordNotFoundException::new);
    }

    @Transactional(readOnly = true)
    public TripAttachment loadAttachment(String username, Long id) {
        TripRecord record = findOne(username, id);
        if (!record.hasAttachment()) {
            throw new TripRecordNotFoundException();
        }
        return new TripAttachment(
                fileStorageService.load(record.getStoredFileName()),
                record.getOriginalFileName(),
                record.getFileContentType(),
                record.getFileSize() == null ? 0L : record.getFileSize());
    }

    private Member requireMember(String username) {
        return memberRepository.findByUsername(username)
                .orElseThrow(TripRecordNotFoundException::new);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private void deleteAfterFailedSave(String storedFileName) {
        try {
            fileStorageService.delete(storedFileName);
        } catch (FileStorageException ignored) {
            // DB 저장 예외가 우선이며, 정리 실패는 원인으로 덮어쓰지 않습니다.
        }
    }
}
