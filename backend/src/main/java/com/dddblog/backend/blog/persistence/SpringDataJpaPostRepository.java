package com.dddblog.backend.blog.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJpaPostRepository extends JpaRepository<JpaPostEntity, Long> {
}
