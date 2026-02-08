package com.simra.konsumgandalf.common.models.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.simra.konsumgandalf.common.models.dtos.RegionAggregate;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents an administrative region like a state or a city.
 */

@SqlResultSetMapping(
        name = "RegionAggregateMapping",
        classes = @ConstructorResult(
                targetClass = RegionAggregate.class,
                columns = {
                        @ColumnResult(name = "geom", type = Polygon.class),
                        @ColumnResult(name = "name", type = String.class),
                        @ColumnResult(name = "admin_level", type = Integer.class),
                        @ColumnResult(name = "number_of_rides", type = Long.class),
                        @ColumnResult(name = "node_median_waiting_time", type = Double.class),
                        @ColumnResult(name = "length_km", type = Double.class),
                        @ColumnResult(name = "node_waiting_s_per_km", type = Double.class),
                        @ColumnResult(name = "node_median_waiting_s_per_km", type = Double.class),
                        @ColumnResult(name = "edge_waiting_s_per_km", type = Double.class),
                        @ColumnResult(name = "edge_median_waiting_s_per_km", type = Double.class)
                }
        )
)
@NamedNativeQuery(
        name = "Region.aggregateRegions",
        query = """
WITH edges AS (
    SELECT
        intersection_edge__region.region_id,
        SUM(length) / 1000  AS edge_length_km,
        SUM(waiting_time) AS edge_waiting_time,
        percentile_cont(0.5) WITHIN GROUP (ORDER BY waiting_time DESC) AS edge_median_waiting_time
    FROM intersection_edge edge
    JOIN intersection_edge__region ON edge.id = intersection_edge__region.edge_id
    GROUP BY intersection_edge__region.region_id
), nodes AS (
    SELECT
        COUNT(DISTINCT node.ride_id) AS number_of_rides,
        COUNT(*) AS node_count,
        intersection_node__region.region_id,
        SUM(length) / 1000 AS node_length_km,
        SUM(waiting_time) AS node_waiting_time,
        percentile_cont(0.5) WITHIN GROUP (ORDER BY waiting_time DESC) AS node_median_waiting_time
    FROM intersection_node node
    JOIN intersection_node__region ON node.id = intersection_node__region.node_id
    GROUP BY intersection_node__region.region_id
)
    SELECT
        way AS geom,
        name,
        admin_level,
        number_of_rides,
        node_median_waiting_time,
        edge_length_km + node_length_km AS length_km,
        node_waiting_time / (edge_length_km + node_length_km) AS node_waiting_s_per_km,
        (node_median_waiting_time * node_count) / (edge_length_km + node_length_km) AS node_median_waiting_s_per_km,
        edge_waiting_time / (edge_length_km + node_length_km) AS edge_waiting_s_per_km,
        (edge_median_waiting_time * node_count) / (edge_length_km + node_length_km) AS edge_median_waiting_s_per_km
    FROM edges e JOIN nodes n ON e.region_id = n.region_id
    JOIN region ON region.id = e.region_id
    WHERE (:region IS NULL OR :region = name)
    """,
        resultSetMapping = "RegionAggregateMapping"
)
@Getter
@Setter
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Region implements FeatureMappable {
    @Id
    private Long id;

	@Column
	private String name;

	@Column
	private int adminLevel;

    @Column(columnDefinition = "geometry(Polygon,4326)")
    private Polygon way;

    // For spatial joins with planet osm line
    @Column(columnDefinition = "geometry(Polygon,3857)")
    private Polygon geom3857;

	public Region() {
	}

    @Override
    public Geometry getGeom() {
        return way;
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = new HashMap<>();
        properties.put("name", this.getName());
        properties.put("adminLevel", this.getAdminLevel());
        return properties;
    }
}
