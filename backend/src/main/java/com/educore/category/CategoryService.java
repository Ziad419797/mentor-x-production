package com.educore.category;

import com.educore.common.CacheNames;
import com.educore.common.SortFields;
import com.educore.common.SortValidator;
import com.educore.dtocourse.mapper.CategoryMapper;
import com.educore.dtocourse.request.CategoryCreateRequest;
import com.educore.dtocourse.request.CategoryUpdateRequest;
import com.educore.dtocourse.response.CategoryResponse;
import com.educore.exception.ResourceNotFoundException;
import com.educore.level.LevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@CacheConfig(cacheNames = CacheNames.CATEGORIES)
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final LevelRepository levelRepository;
    private final CategoryMapper categoryMapper;
    private final SortValidator sortValidator;
    private final com.educore.course.CourseRepository courseRepository;

    @Caching(evict = {
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_PAGES, allEntries = true)
    })
    public CategoryResponse createCategory(CategoryCreateRequest request) {

        log.info("Creating category '{}' for level {}", request.getName(), request.getLevelId());

        var level = levelRepository.findById(request.getLevelId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Level not found with id " + request.getLevelId()));

        var category = categoryMapper.toEntity(request);
        // ✅ Ensure active = true for new categories (Lombok @Builder defaults primitive boolean to false)
        category.setActive(true);
        category.setLevel(level);

        categoryRepository.save(category);

        return categoryMapper.toResponse(category);
    }
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CATEGORIES, key = "#id"),
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_PAGES, allEntries = true)
    })
    public CategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {

        log.info("Updating category id {}", id);

        var category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with id " + id));

        categoryMapper.updateEntityFromRequest(request, category);

        return categoryMapper.toResponse(category);
    }
    @Caching(evict = {
            @CacheEvict(value = CacheNames.CATEGORIES, key = "#id"),
            @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_BY_CATEGORY, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES_PAGES, allEntries = true),
            @CacheEvict(value = CacheNames.COURSES, allEntries = true)
    })
    public void deleteCategory(Long id) {

        log.info("Deleting category id {}", id);

        var category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with id " + id));

        // فك ربط الكورسات من جدول course_category — بدون حذف الكورسات نفسها
        courseRepository.unlinkAllCoursesFromCategory(id);

        categoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATEGORIES, key = "#id")
    public CategoryResponse getCategoryById(Long id) {

        var category = categoryRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found with id " + id));

        return categoryMapper.toResponse(category);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheNames.CATEGORIES_PAGES, key = "'level-' + #levelId + '-' + #pageable.pageNumber")
    public Page<CategoryResponse> getCategoriesByLevel(Long levelId, Pageable pageable) {

        if (!levelRepository.existsById(levelId)) {
            throw new ResourceNotFoundException("Level not found with id " + levelId);
        }
        sortValidator.validate(pageable, SortFields.CATEGORY);

        return categoryRepository.findByLevelId(levelId, pageable)
                .map(categoryMapper::toResponse);
    }

    /** لوحة التحكم: جلب كل كاتيجوريز المستوى شاملاً غير النشطة */
    @Transactional(readOnly = true)
    public Page<CategoryResponse> getCategoriesByLevelForAdmin(Long levelId, Pageable pageable) {
        if (!levelRepository.existsById(levelId)) {
            throw new ResourceNotFoundException("Level not found with id " + levelId);
        }
        return categoryRepository.findAllByLevelIdIncludingInactive(levelId, pageable)
                .map(categoryMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponse> getAllCategories(Pageable pageable) {
        return categoryRepository.findAllIncludingInactive(pageable)
                .map(categoryMapper::toResponse);
    }

    // ================= active =================

    @Transactional
    public CategoryResponse toggleCategoryActive(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setActive(!category.isActive());
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    @Transactional
    @CacheEvict(value = CacheNames.CATEGORIES_PAGES, allEntries = true)
    public void reorderCategories(java.util.List<java.util.Map<String, Object>> orders) {
        for (java.util.Map<String, Object> item : orders) {
            Long catId = Long.valueOf(item.get("id").toString());
            Integer sortOrder = Integer.valueOf(item.get("sortOrder").toString());
            categoryRepository.findById(catId).ifPresent(cat -> {
                cat.setSortOrder(sortOrder);
                categoryRepository.save(cat);
            });
        }
    }
}
