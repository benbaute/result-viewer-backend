package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

/**
 * Contains metadata for a ride.
 */
@Getter
@Setter
@Entity
public class Ride {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String path;

	@Transient
	private ArrayList<MatchInformation> coordinates;

	public Ride() {
	}

	public Ride(String path) {
		this.path = path;
	}

}
