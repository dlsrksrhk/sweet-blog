package com.dddblog.backend.blog.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.dddblog.backend.blog.domain.PostStatus;

interface SpringDataJpaPostRepository extends JpaRepository<JpaPostEntity, Long> {

	Optional<JpaPostEntity> findByIdAndStatus(Long id, PostStatus status);

	Page<JpaPostEntity> findByStatus(PostStatus status, Pageable pageable);
}
