package com.cityroulette.domain.trip.repository;

import com.cityroulette.domain.member.repository.MemberRepository;
import com.cityroulette.domain.member.entity.Member;
import com.cityroulette.domain.trip.entity.TripRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class TripRecordRepositoryTests {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TripRecordRepository tripRecordRepository;

    @Test
    void returnsOnlyRecordsOwnedByRequestedMember() {
        Member memberA = memberRepository.saveAndFlush(new Member(
                "tripmembera", "encoded-password",
                "trip-a@example.com", "회원A"));
        Member memberB = memberRepository.saveAndFlush(new Member(
                "tripmemberb", "encoded-password",
                "trip-b@example.com", "회원B"));

        TripRecord recordA = tripRecordRepository.saveAndFlush(record(memberA, "A의 여행"));
        TripRecord recordB = tripRecordRepository.saveAndFlush(record(memberB, "B의 여행"));

        assertThat(tripRecordRepository.findByMemberOrderByCreatedAtDesc(memberA))
                .extracting(TripRecord::getTitle)
                .containsExactly("A의 여행");
        assertThat(tripRecordRepository.findByIdAndMember(recordA.getId(), memberA)).isPresent();
        assertThat(tripRecordRepository.findByIdAndMember(recordB.getId(), memberA)).isEmpty();
    }

    private TripRecord record(Member member, String title) {
        return new TripRecord(member, title, "감천문화마을", "부산광역시 사하구",
                "기록", LocalDate.of(2026, 10, 26), null, null, null, null);
    }
}
