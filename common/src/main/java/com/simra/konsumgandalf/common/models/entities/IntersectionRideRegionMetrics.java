package com.simra.konsumgandalf.common.models.entities;

import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import org.locationtech.jts.geom.Polygon;

import java.util.Map;

@Getter
@Entity
@org.hibernate.annotations.Immutable
@org.hibernate.annotations.Subselect("select * from intersection_ride_region_metrics")
public class IntersectionRideRegionMetrics extends TimeBaseClass implements FeatureMappable {
    @Id
    private Long id;

    @Column(name = "region_id")
    private Long regionId;

    @Column(name = "ride_id")
    private Long rideId;

    @Column(name="geom", columnDefinition = "geometry(Polygon,4326)")
    private Polygon geom;

    @Column(name = "name")
    private String name;

    @Column(name = "admin_level")
    private int adminLevel;

    @Column(name = "number_of_edges")
    private int numberOfEdges;

    @Column(name = "number_of_nodes")
    private int numberOfNodes;

    @Column(name = "node_median_waiting_time")
    private Double nodeMedianWaitingTime; // can be null if no node

    @Column(name = "length_km")
    private double length; // km

    @Column(name = "node_waiting_s_per_km")
    private double nodeWaitingSPerKm; // s/km

    @Column(name = "edge_waiting_s_per_km")
    private double edgeWaitingSPerKm; // s/km

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = this.getBaseProperties();
        properties.put("rideId", rideId);
        properties.put("regionId", regionId);
        properties.put("name", name);
        properties.put("adminLevel", adminLevel);
        properties.put("numberOfEdges", numberOfEdges);
        properties.put("numberOfNodes", numberOfNodes);
        properties.put("nodeMedianWaitingTime", nodeMedianWaitingTime);
        properties.put("length", length);
        properties.put("nodeWaitingSPerKm", nodeWaitingSPerKm);
        properties.put("edgeWaitingSPerKm", edgeWaitingSPerKm);
        return properties;
    }
}
