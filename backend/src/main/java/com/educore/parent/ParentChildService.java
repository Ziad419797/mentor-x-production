package com.educore.parent;

import com.educore.assignment.StudentAssignmentAttempt;
import com.educore.assignment.StudentAssignmentAttemptRepository;
import com.educore.exception.ResourceNotFoundException;
import com.educore.parent.dto.ChildAssignmentResultDto;
import com.educore.parent.dto.ChildQuizResultDto;
import com.educore.quiz.StudentQuizAttempt;
import com.educore.quiz.StudentQuizAttemptRepository;
import com.educore.question.Question;
import com.educore.question.QuestionRepository;
import com.educore.student.Student;
import com.educore.student.StudentRepository;
import com.educore.wallet.WalletService;
import com.educore.wallet.dto.WalletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentChildService {

    private final ParentRepository                   parentRepository;
    private final StudentRepository                  studentRepository;
    private final StudentQuizAttemptRepository       quizAttemptRepository;
    private final StudentAssignmentAttemptRepository assignmentAttemptRepository;
    private final QuestionRepository                 questionRepository;
    private final WalletService                      walletService;

    // ─────────────────────────────────────────────────────────────
    // Guard: verify the child belongs to this parent
    // ─────────────────────────────────────────────────────────────

    private Student verifyOwnership(Long parentId, Long studentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("ولي الأمر غير موجود"));

        return parent.getStudents().stream()
                .filter(s -> s.getId().equals(studentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("هذا الطالب غير مرتبط بحساب ولي الأمر"));
    }

    // ─────────────────────────────────────────────────────────────
    // Quiz Results
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ChildQuizResultDto> getQuizResults(Long parentId, Long studentId, Pageable pageable) {
        verifyOwnership(parentId, studentId);

        Page<StudentQuizAttempt> attempts = quizAttemptRepository
                .findByStudentIdAndSubmitted(studentId, true, pageable);

        List<ChildQuizResultDto> dtos = attempts.getContent().stream().map(a -> {
            // حساب إجمالي درجات الكويز من أسئلته
            int totalMarks = questionRepository.findByQuizId(a.getQuiz().getId(), Pageable.unpaged())
                    .stream().mapToInt(q -> q.getMark() != null ? q.getMark() : 0).sum();

            return ChildQuizResultDto.builder()
                    .attemptId(a.getId())
                    .quizId(a.getQuiz().getId())
                    .quizTitle(a.getQuiz().getTitle())
                    .score(a.getScore())
                    .totalMarks(totalMarks)
                    .passed(a.getPassed())
                    .correctAnswers(a.getCorrectAnswers())
                    .attemptNumber(a.getAttemptNumber())
                    .submittedAt(a.getSubmittedAt())
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, attempts.getTotalElements());
    }

    // ─────────────────────────────────────────────────────────────
    // Assignment Results
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<ChildAssignmentResultDto> getAssignmentResults(Long parentId, Long studentId, Pageable pageable) {
        verifyOwnership(parentId, studentId);

        Page<StudentAssignmentAttempt> attempts = assignmentAttemptRepository
                .findByStudentIdAndSubmitted(studentId, true, pageable);

        List<ChildAssignmentResultDto> dtos = attempts.getContent().stream().map(a ->
                ChildAssignmentResultDto.builder()
                        .attemptId(a.getId())
                        .assignmentId(a.getAssignment().getId())
                        .assignmentTitle(a.getAssignment().getTitle())
                        .score(a.getScore())
                        .submitted(a.getSubmitted())
                        .submittedAt(a.getSubmittedAt())
                        .build()
        ).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, attempts.getTotalElements());
    }

    // ─────────────────────────────────────────────────────────────
    // Wallet
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long parentId, Long studentId) {
        verifyOwnership(parentId, studentId);
        return walletService.getWallet(studentId);
    }
}
