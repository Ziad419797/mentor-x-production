package com.educore.level;
import com.educore.category.CategoryRepository;
import com.educore.course.CourseRepository;
import com.educore.dtocourse.request.LevelCreateRequest;
import com.educore.dtocourse.request.LevelUpdateRequest;
import com.educore.dtocourse.response.LevelResponse;
import com.educore.dtocourse.response.LevelStatsResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.dtocourse.mapper.LevelMapper;
import com.educore.unit.SessionRepository;
import com.educore.lessonmaterial.LessonMaterialRepository;
import com.educore.quiz.QuizRepository;
import com.educore.question.QuestionRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LevelService {

    private final LevelRepository    levelRepository;
    private final LevelMapper        levelMapper;
    private final CategoryRepository categoryRepository;
    private final CourseRepository   courseRepository;
    private final SessionRepository       sessionRepository;
    private final LessonMaterialRepository materialRepository;
    private final QuizRepository          quizRepository;
    private final QuestionRepository      questionRepository;

    @CacheEvict(value = "levels", allEntries = true)
    @Transactional
    public LevelResponse createLevel(LevelCreateRequest request) {
        log.info("Creating level with name {}", request.getName());
        Level level = levelMapper.toEntity(request);
        levelRepository.save(level);
        return levelMapper.toResponse(level);
    }

    @Transactional
    @CacheEvict(value = "levels", allEntries = true)
    public LevelResponse updateLevel(Long id, LevelUpdateRequest request) {
        log.info("Updating level id {}", id);
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with id " + id));
        level.setName(request.getName());
        levelRepository.save(level);
        return levelMapper.toResponse(level);
    }

    @Transactional
    @CacheEvict(value = "levels", allEntries = true)
    public void deleteLevel(Long id) {
        log.info("Deleting level id {}", id);
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with id " + id));
        if (!level.getCategories().isEmpty()) {
            throw new IllegalStateException("Cannot delete level with existing categories");
        }
        levelRepository.delete(level);
    }

    @Transactional(readOnly = true)
    public LevelResponse getLevelById(Long id) {
        log.info("Fetching level id {}", id);
        Level level = levelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with id " + id));
        return levelMapper.toResponse(level);
    }

    @Cacheable(value = "levels", key = "'all'")
    @Transactional(readOnly = true)
    public List<LevelResponse> getAllLevels() {
        log.info("Fetching all levels");
        return levelRepository.findAll().stream()
                .map(levelMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LevelStatsResponse getLevelStats(Long levelId) {
        log.info("Fetching stats for level {}", levelId);
        Level level = levelRepository.findById(levelId)
                .orElseThrow(() -> new ResourceNotFoundException("Level not found with id " + levelId));
        return LevelStatsResponse.builder()
                .levelId(levelId)
                .levelName(level.getName())
                .categoriesCount(categoryRepository.countByLevelId(levelId))
                .coursesCount(courseRepository.countByLevelId(levelId))
                .sessionsCount(sessionRepository.countByLevelId(levelId))
                .studentsCount(0L)
                .videosCount(materialRepository.countVideosByLevelId(levelId))
                .quizzesCount(quizRepository.countByLevelId(levelId))
                .questionsCount(questionRepository.countByLevelId(levelId))
                .build();
    }
}
