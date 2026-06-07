package com.dddblog.backend.blog.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJpaTagRepository extends JpaRepository<JpaTagEntity, Long> {

	Optional<JpaTagEntity> findByName(String name);
}
