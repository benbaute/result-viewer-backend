package com.simra.konsumgandalf.common.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.List;

@Getter
@Setter
@Entity
public class TrafficSignalCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point geom;

    @Column(columnDefinition = "bigint[]")
    private List<Long> originalSignalIds;

	public TrafficSignalCluster() {
	}
}
