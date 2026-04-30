package com.sms.service;

import com.sms.dto.*;
import com.sms.entity.*;
import com.sms.repository.*;
import com.sms.security.UserDetailsImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResultService {

    @Autowired
    private ResultRepository resultRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser() {

        var auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl ud) {
            return userRepository.findById(ud.getId()).orElse(null);
        }

        return null;
    }

    private String computeGrade(double percentage) {

        if (percentage >= 90) return "O";
        if (percentage >= 80) return "A+";
        if (percentage >= 70) return "A";
        if (percentage >= 60) return "B+";
        if (percentage >= 50) return "B";
        if (percentage >= 40) return "C";

        return "F";
    }

    public ResultResponse add(ResultRequest req) {

        Student student = studentRepository
                .findById(req.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));

        double pct = (req.getMarksObtained() / req.getMaxMarks()) * 100;

        Result result = new Result();
        result.setStudent(student);
        result.setSubject(req.getSubject());
        result.setSemester(req.getSemester());
        result.setExamType(req.getExamType());
        result.setMarksObtained(req.getMarksObtained());
        result.setMaxMarks(req.getMaxMarks());
        result.setGrade(computeGrade(pct));
        result.setEnteredBy(getCurrentUser());
        result.setRemarks(req.getRemarks());

        return ResultResponse.from(resultRepository.save(result));
    }

    public List<ResultResponse> getByStudent(Long studentId) {

        return resultRepository
                .findByStudentId(studentId)
                .stream()
                .map(ResultResponse::from)
                .collect(Collectors.toList());
    }

    public List<ResultResponse> getBySemester(Long studentId, Integer semester) {

        return resultRepository
                .findByStudentIdAndSemester(studentId, semester)
                .stream()
                .map(ResultResponse::from)
                .collect(Collectors.toList());
    }

    public ResultResponse update(Long id, ResultRequest req) {

        Result result = resultRepository
                .findById(id)
                .orElseThrow(() -> new RuntimeException("Result not found: " + id));

        result.setMarksObtained(req.getMarksObtained());
        result.setMaxMarks(req.getMaxMarks());
        result.setRemarks(req.getRemarks());

        double pct = (req.getMarksObtained() / req.getMaxMarks()) * 100;

        result.setGrade(computeGrade(pct));

        return ResultResponse.from(resultRepository.save(result));
    }

    public Map<String, Object> getReport(Long studentId) {

        List<Result> results = resultRepository.findByStudentId(studentId);

        Map<String, Object> report = new HashMap<>();

        if (results.isEmpty()) {

            report.put("message", "No results found");
            return report;
        }

        double avg = results.stream()
                .mapToDouble(result ->
                        (result.getMarksObtained() / result.getMaxMarks()) * 100
                )
                .average()
                .orElse(0);

        Map<String, List<ResultResponse>> bySemester = new HashMap<>();

        for (Result r : results) {

            String semesterKey = "Semester " + r.getSemester();

            bySemester
                    .computeIfAbsent(semesterKey, k -> new ArrayList<>())
                    .add(ResultResponse.from(r));
        }

        String overallGrade = computeGrade(avg);

        report.put("totalExams", results.size());
        report.put("averagePercentage", Math.round(avg * 10.0) / 10.0);
        report.put("overallGrade", overallGrade);
        report.put("semesterWise", bySemester);
        report.put(
                "results",
                results.stream()
                        .map(ResultResponse::from)
                        .collect(Collectors.toList())
        );

        return report;
    }

    public Student getStudentByRollNumber(String rollNumber) {

        return studentRepository
                .findByRollNumber(rollNumber)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }
}
