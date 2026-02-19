package com.vgc.service;

import com.vgc.dto.CategoryRequestDto;
import com.vgc.dto.CategoryRequestResponse;
import com.vgc.dto.CategoryResponse;
import com.vgc.entity.Category;
import com.vgc.entity.CategoryRequest;
import com.vgc.entity.User;
import com.vgc.repository.CategoryRepository;
import com.vgc.repository.CategoryRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryRequestRepository categoryRequestRepository;

    public CategoryService(CategoryRepository categoryRepository, CategoryRequestRepository categoryRequestRepository) {
        this.categoryRepository = categoryRepository;
        this.categoryRequestRepository = categoryRequestRepository;
    }

    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    public CategoryRequestResponse requestCategory(CategoryRequestDto dto, User user) {
        if (categoryRepository.existsByName(dto.getName())) {
            throw new RuntimeException("이미 존재하는 게시판입니다.");
        }

        CategoryRequest request = new CategoryRequest();
        request.setName(dto.getName());
        request.setRequester(user);

        CategoryRequest saved = categoryRequestRepository.save(request);
        return CategoryRequestResponse.from(saved);
    }

    public List<CategoryRequestResponse> getPendingRequests() {
        return categoryRequestRepository.findByStatus("PENDING").stream()
                .map(CategoryRequestResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryResponse approveRequest(Long id, String label, String color, boolean hasStatus) {
        CategoryRequest request = categoryRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("이미 처리된 요청입니다.");
        }

        request.setStatus("APPROVED");
        categoryRequestRepository.save(request);

        Category category = new Category(request.getName(), label, color, hasStatus);
        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    public CategoryResponse createCategory(String name, String label, String color, boolean hasStatus) {
        if (categoryRepository.existsByName(name)) {
            throw new RuntimeException("이미 존재하는 게시판입니다.");
        }
        Category category = new Category(name, label, color, hasStatus);
        Category saved = categoryRepository.save(category);
        return CategoryResponse.from(saved);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("게시판를 찾을 수 없습니다."));
        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryRequestResponse rejectRequest(Long id, String reason) {
        CategoryRequest request = categoryRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("요청을 찾을 수 없습니다."));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("이미 처리된 요청입니다.");
        }

        request.setStatus("REJECTED");
        request.setRejectionReason(reason);
        categoryRequestRepository.save(request);
        return CategoryRequestResponse.from(request);
    }
}
