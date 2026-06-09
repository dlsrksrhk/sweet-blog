package com.dddblog.backend.member.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataJpaMemberRepository extends JpaRepository<JpaMemberEntity, Long> {

	boolean existsByLoginId(String loginId);

	boolean existsByNickname(String nickname);
}
