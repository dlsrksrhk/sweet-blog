package com.dddblog.backend.blog.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tags")
class JpaTagEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30, unique = true)
	private String name;

	protected JpaTagEntity() {
	}

	JpaTagEntity(String name) {
		this.name = name;
	}

	Long id() {
		return id;
	}

	String name() {
		return name;
	}
}
