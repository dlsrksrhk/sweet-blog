package com.dddblog.backend.blog.persistence;

import java.util.LinkedHashSet;
import java.util.Set;

import com.dddblog.backend.blog.domain.PostStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "posts")
class JpaPostEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "author_id", nullable = false)
	private Long authorId;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(name = "content_markdown", nullable = false, columnDefinition = "text")
	private String contentMarkdown;

	@Column(nullable = false, length = 300)
	private String summary;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostStatus status;

	@ManyToMany
	@JoinTable(
		name = "post_tags",
		joinColumns = @JoinColumn(name = "post_id"),
		inverseJoinColumns = @JoinColumn(name = "tag_id"),
		uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "tag_id"})
	)
	private Set<JpaTagEntity> tags = new LinkedHashSet<>();

	protected JpaPostEntity() {
	}

	JpaPostEntity(
		Long authorId,
		String title,
		String contentMarkdown,
		String summary,
		PostStatus status,
		Set<JpaTagEntity> tags
	) {
		this.authorId = authorId;
		this.title = title;
		this.contentMarkdown = contentMarkdown;
		this.summary = summary;
		this.status = status;
		this.tags = new LinkedHashSet<>(tags);
	}

	Long id() {
		return id;
	}

	Long authorId() {
		return authorId;
	}

	String title() {
		return title;
	}

	String contentMarkdown() {
		return contentMarkdown;
	}

	String summary() {
		return summary;
	}

	PostStatus status() {
		return status;
	}

	Set<JpaTagEntity> tags() {
		return Set.copyOf(tags);
	}
}
