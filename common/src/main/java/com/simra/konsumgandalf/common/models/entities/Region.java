package com.simra.konsumgandalf.common.models.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.simra.konsumgandalf.common.models.dtos.RegionAggregate;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SqlResultSetMapping;

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
    JOIN region ON region.name = e.region_id
    WHERE (:region IS NULL OR :region = name)
    """,
        resultSetMapping = "RegionAggregateMapping"
)
@Entity
public class Region implements FeatureMappable {

	@Id
	private String name;

	@Column
	private Long id;

	@Column
	private int adminLevel;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "region", fetch = FetchType.LAZY)
	@JsonIgnore
	private List<SafetyMetricsRegion> safetyMetricsRegions;

	@ManyToMany(cascade = CascadeType.ALL)
	@JsonIgnore
	private List<SimraRegion> simraRegions;

	@Column()
	private Geometry way;

	public Region() {
	}

	public Region(String name) {
		this.name = name;
	}

	public Region(String name, Long id, int adminLevel) {
		this.name = name;
		this.id = id;
		this.adminLevel = adminLevel;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<SafetyMetricsRegion> getSafetyMetricsRegions() {
		return safetyMetricsRegions;
	}

	public void setSafetyMetricsRegions(List<SafetyMetricsRegion> safetyMetricsCities) {
		this.safetyMetricsRegions = safetyMetricsCities;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Region region = (Region) o;
		return Objects.equals(name, region.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public List<SimraRegion> getSimraRegions() {
		return simraRegions;
	}

	public void setSimraRegions(List<SimraRegion> simraRegions) {
		this.simraRegions = this.simraRegions;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long osmId) {
		this.id = osmId;
	}

	public Geometry getWay() {
		return way;
	}

	public void setWay(Geometry way) {
		this.way = way;
	}

	public int getAdminLevel() {
		return adminLevel;
	}

	public void setAdminLevel(int level) {
		this.adminLevel = level;
	}

}
