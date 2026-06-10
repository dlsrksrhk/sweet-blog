package com.dddblog.backend.member.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "member_id_sequences")
class JpaMemberIdSequenceEntity {

	@Id
	@Column(length = 50)
	private String name;

	@Column(name = "next_value", nullable = false)
	private Long nextValue;

	protected JpaMemberIdSequenceEntity() {
	}

	JpaMemberIdSequenceEntity(String name, Long nextValue) {
		this.name = name;
		this.nextValue = nextValue;
	}

	String name() {
		return name;
	}

	Long nextValue() {
		return nextValue;
	}

	void increase() {
		nextValue++;
	}
}
