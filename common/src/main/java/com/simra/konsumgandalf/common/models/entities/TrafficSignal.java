package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Point;

import java.util.Date;

@Getter
@Setter
@Entity
public class TrafficSignal {

	@Id
	private Long id;

    @Column(columnDefinition = "geometry(Point,4326)", nullable = false)
    private Point geom;

	public TrafficSignal() {
	}

    public TrafficSignal(long id, Point point) {
        this.id = id;
        this.geom = point;
    }
}
