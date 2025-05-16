package com.WEB4_5_GPT_BE.unihub.domain.timetable.repository;

import com.WEB4_5_GPT_BE.unihub.domain.timetable.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    boolean existsByMemberIdAndYearAndSemester(Long id, Integer year, Integer semester);
    Optional<Timetable> findByMemberIdAndYearAndSemester(Long memberId, int year, int semester);

    @Query("SELECT t FROM Timetable t " +
            "LEFT JOIN FETCH t.items " +
            "WHERE t.id = :id")
    Optional<Timetable> findWithItemsById(Long id);
}
