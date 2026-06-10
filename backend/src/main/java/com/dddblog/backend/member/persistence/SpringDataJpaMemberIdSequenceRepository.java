package com.dddblog.backend.member.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface SpringDataJpaMemberIdSequenceRepository extends JpaRepository<JpaMemberIdSequenceEntity, String> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select s from JpaMemberIdSequenceEntity s where s.name = :name")
	Optional<JpaMemberIdSequenceEntity> findByNameWithLock(@Param("name") String name);
}
