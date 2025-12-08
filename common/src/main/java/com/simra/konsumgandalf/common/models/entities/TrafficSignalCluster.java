package com.simra.konsumgandalf.common.models.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Entity
public class TrafficSignalCluster {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point geom;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon polygon;

    @Column(columnDefinition = "bigint[]")
    private List<Long> originalSignalIds;

	public TrafficSignalCluster() {
	}
}
