package com.cityroulette.service;

import com.cityroulette.domain.Member;
import com.cityroulette.domain.TripRecord;
import com.cityroulette.dto.trip.StoredFileInfo;
import com.cityroulette.dto.trip.TripAttachment;
import com.cityroulette.dto.trip.TripRecordRequest;
import com.cityroulette.repository.MemberRepository;
import com.cityroulette.repository.TripRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TripRecordServiceTests {

    private final TripRecordRepository tripRecordRepository = mock(TripRecordRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final TripRecordService service = new TripRecordService(
            tripRecordRepository, memberRepository, fileStorageService);

    private Member member;

    @BeforeEach
    void setUp() {
        member = new Member("tripuser", "encoded", "trip@example.com", "여행자");
        when(memberRepository.findByUsername("tripuser")).thenReturn(Optional.of(member));
        when(tripRecordRepository.saveAndFlush(any(TripRecord.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void savesMemberTripWithoutFile() {
        TripRecordRequest request = request();
        when(fileStorageService.store(null)).thenReturn(Optional.empty());

        TripRecord saved = service.create("tripuser", request);

        assertThat(saved.getMember()).isSameAs(member);
        assertThat(saved.getTitle()).isEqualTo("부산 여행");
        assertThat(saved.hasAttachment()).isFalse();
        verify(tripRecordRepository).saveAndFlush(saved);
    }

    @Test
    void savesMemberTripWithStoredFileMetadata() {
        TripRecordRequest request = request();
        MockMultipartFile image = new MockMultipartFile(
                "image", "busan.jpg", "image/jpeg", new byte[]{1, 2});
        request.setImage(image);
        when(fileStorageService.store(image)).thenReturn(Optional.of(new StoredFileInfo(
                "busan.jpg", "8c51da48-a45d-4fb2-a465-2a7a44946e5d.jpg", "image/jpeg", 2)));

        TripRecord saved = service.create("tripuser", request);

        assertThat(saved.hasAttachment()).isTrue();
        assertThat(saved.getOriginalFileName()).isEqualTo("busan.jpg");
        assertThat(saved.getStoredFileName()).isNotEqualTo(saved.getOriginalFileName());
    }

    @Test
    void returnsOnlyCurrentMembersRecords() {
        List<TripRecord> expected = List.of(record(member, "내 여행"));
        when(tripRecordRepository.findByMemberOrderByCreatedAtDesc(member)).thenReturn(expected);

        assertThat(service.findAll("tripuser")).isEqualTo(expected);
        verify(tripRecordRepository).findByMemberOrderByCreatedAtDesc(member);
    }

    @Test
    void loadsOwnedRecord() {
        TripRecord record = record(member, "내 여행");
        when(tripRecordRepository.findByIdAndMember(10L, member)).thenReturn(Optional.of(record));

        assertThat(service.findOne("tripuser", 10L)).isSameAs(record);
    }

    @Test
    void rejectsRecordNotOwnedByCurrentMember() {
        when(tripRecordRepository.findByIdAndMember(20L, member)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findOne("tripuser", 20L))
                .isInstanceOf(TripRecordNotFoundException.class);
    }

    @Test
    void loadsAttachmentForOwnedRecord() {
        TripRecord record = new TripRecord(member, "사진 여행", "부산역", null, null,
                LocalDate.now(), "busan.jpg", "8c51da48-a45d-4fb2-a465-2a7a44946e5d.jpg",
                "image/jpeg", 3L);
        ByteArrayResource resource = new ByteArrayResource(new byte[]{1, 2, 3});
        when(tripRecordRepository.findByIdAndMember(10L, member)).thenReturn(Optional.of(record));
        when(fileStorageService.load(record.getStoredFileName())).thenReturn(resource);

        TripAttachment attachment = service.loadAttachment("tripuser", 10L);

        assertThat(attachment.resource()).isSameAs(resource);
        assertThat(attachment.originalFileName()).isEqualTo("busan.jpg");
        assertThat(attachment.size()).isEqualTo(3L);
    }

    @Test
    void rejectsAnotherMembersAttachmentBeforeLoadingFile() {
        when(tripRecordRepository.findByIdAndMember(30L, member)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadAttachment("tripuser", 30L))
                .isInstanceOf(TripRecordNotFoundException.class);
        verify(fileStorageService, never()).load(any());
    }

    @Test
    void rejectsDownloadWhenRecordHasNoAttachment() {
        TripRecord record = record(member, "파일 없는 여행");
        when(tripRecordRepository.findByIdAndMember(40L, member)).thenReturn(Optional.of(record));

        assertThatThrownBy(() -> service.loadAttachment("tripuser", 40L))
                .isInstanceOf(TripRecordNotFoundException.class);
        verify(fileStorageService, never()).load(any());
    }

    @Test
    void deletesStoredFileWhenDatabaseSaveFails() {
        TripRecordRequest request = request();
        MockMultipartFile image = new MockMultipartFile(
                "image", "busan.png", "image/png", new byte[]{1});
        request.setImage(image);
        StoredFileInfo fileInfo = new StoredFileInfo(
                "busan.png", "8c51da48-a45d-4fb2-a465-2a7a44946e5d.png", "image/png", 1);
        when(fileStorageService.store(image)).thenReturn(Optional.of(fileInfo));
        when(tripRecordRepository.saveAndFlush(any(TripRecord.class)))
                .thenThrow(new DataIntegrityViolationException("save failed"));

        assertThatThrownBy(() -> service.create("tripuser", request))
                .isInstanceOf(TripRecordSaveException.class);
        verify(fileStorageService).delete(fileInfo.storedFileName());
    }

    private TripRecordRequest request() {
        TripRecordRequest request = new TripRecordRequest();
        request.setTitle(" 부산 여행 ");
        request.setPlaceName(" 감천문화마을 ");
        request.setPlaceAddress(" 부산광역시 사하구 ");
        request.setMemo(" 좋은 여행 ");
        request.setVisitDate(LocalDate.of(2026, 10, 26));
        return request;
    }

    private TripRecord record(Member owner, String title) {
        return new TripRecord(owner, title, "부산역", null, null,
                LocalDate.of(2026, 10, 26), null, null, null, null);
    }
}
