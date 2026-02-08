package com.simra.konsumgandalf.common.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Entity representing a safety metrics run like safety metrics profile analysis
 */
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
public class MethodRun {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private String name;

	@CreatedDate
	private Instant createdDate;

	private Long duration;

	public MethodRun() {
	}

	public MethodRun(String name, Long duration) {
		this.name = name;
		this.duration = duration;
	}
}
