package com.sms.repository;

import com.sms.entity.Result;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ResultRepository extends JpaRepository<Result, Long> {

    List<Result> findByStudentId(Long studentId);

    List<Result> findByStudentIdAndSemester(Long studentId, Integer semester);

    List<Result> findByStudentIdAndSubject(Long studentId, String subject);

    @Query("SELECT AVG(r.marksObtained / r.maxMarks * 100) FROM Result r WHERE r.student.id = :studentId")
    Double findAveragePercentage(@Param("studentId") Long studentId);
}
