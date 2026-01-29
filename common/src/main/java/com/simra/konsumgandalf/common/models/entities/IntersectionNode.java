package com.simra.konsumgandalf.common.models.entities;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.locationtech.jts.geom.LineString;

import com.simra.konsumgandalf.common.models.dtos.IntersectionNodeAggregate;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;

import jakarta.persistence.ColumnResult;
import jakarta.persistence.ConstructorResult;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.SqlResultSetMapping;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Entity
@SqlResultSetMapping(
    name = "IntersectionNodeAggregateMapping",
    classes = @ConstructorResult(
        targetClass = IntersectionNodeAggregate.class,
        columns = {
            @ColumnResult(name = "geom", type = LineString.class),
            @ColumnResult(name = "example_id", type = Long.class),
            @ColumnResult(name = "start_osm_id", type = Long.class),
            @ColumnResult(name = "end_osm_id", type = Long.class),
            @ColumnResult(name = "start_name", type = String.class),
            @ColumnResult(name = "end_name", type = String.class),
            @ColumnResult(name = "street_names", type = String.class),
            @ColumnResult(name = "count", type = Long.class),
            @ColumnResult(name = "avg_length", type = Double.class),
            @ColumnResult(name = "avg_duration", type = Double.class),
            @ColumnResult(name = "max_duration", type = Double.class),
            @ColumnResult(name = "median_duration", type = Double.class),
            @ColumnResult(name = "avg_speed", type = Double.class),
            @ColumnResult(name = "median_speed", type = Double.class),
            @ColumnResult(name = "avg_waiting_time", type = Double.class),
            @ColumnResult(name = "max_waiting_time", type = Double.class),
            @ColumnResult(name = "median_waiting_time", type = Double.class),
            @ColumnResult(name = "traffic_signal_cluster_id", type = Long.class)
        }
    )
)
@NamedNativeQuery(
    name = "IntersectionNode.aggregateNodes",
    query = """
    SELECT
        d.geom AS geom,
        g.example_id AS example_id,
        g.start_osm_id,
        g.end_osm_id,
        sl.name AS start_name,
        el.name AS end_name,
        d.street_names,
        g.count,
        g.avg_length,
        g.avg_duration,
        g.max_duration,
        g.median_duration,
        g.avg_speed,
        g.median_speed,
        g.avg_waiting_time,
        g.max_waiting_time,
        g.median_waiting_time,
        d.traffic_signal_cluster_id
    FROM (
        SELECT
            start_osm_id,
            end_osm_id,
            COUNT(*) AS count,
            AVG(length) AS avg_length,
            AVG(duration) AS avg_duration,
            MAX(duration) AS max_duration,
            AVG(speed) AS avg_speed,
            MIN(node.id) AS example_id,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY speed DESC) AS median_speed,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY duration DESC) AS median_duration,
            AVG(waiting_time) AS avg_waiting_time,
            MAX(waiting_time) AS max_waiting_time,
            percentile_cont(0.5) WITHIN GROUP (ORDER BY waiting_time DESC) AS median_waiting_time
        FROM intersection_node node
        WHERE (:trafficSignalClusterId IS NULL OR :trafficSignalClusterId = traffic_signal_cluster_id)
        AND (
                :region IS NULL
                OR EXISTS (
                    SELECT 1
                    FROM intersection_node__region nr
                    JOIN region r ON r.name = nr.region_id
                    WHERE nr.node_id = node.id
                    AND r.name = :region
                )
            )
        GROUP BY start_osm_id, end_osm_id
    ) g
    JOIN intersection_node d ON d.id = g.example_id
    LEFT JOIN planet_osm_line sl ON sl.osm_id = d.start_osm_id
    LEFT JOIN planet_osm_line el ON el.osm_id = d.end_osm_id
    WHERE (:count IS NULL OR g.count >= :count)
    AND (:streetNames IS NULL OR d.street_names ILIKE CONCAT('%', :streetNames, '%'))
    """,
    resultSetMapping = "IntersectionNodeAggregateMapping"
)
public class IntersectionNode extends IntersectionBaseClass implements FeatureMappable {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "start_osm_id")
    private PlanetOsmLine startLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "end_osm_id")
    private PlanetOsmLine endLine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traffic_signal_cluster_id")
    private TrafficSignalCluster trafficSignalCluster;

    @ManyToMany
    @JoinTable(
            name = "intersection_node__region",
            joinColumns = @JoinColumn(name = "node_id"),
            inverseJoinColumns = @JoinColumn(name = "region_id")
    )
    private Set<Region> regions = new HashSet<>();

    private String streetNames;

	// --- Constructor ---
	public IntersectionNode() {
	}

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> properties = this.getBaseProperties();
        PlanetOsmLine startLine = this.getStartLine();
        PlanetOsmLine endLine = this.getEndLine();
        properties.put("start_osm_id", startLine != null ? startLine.getId() : null);
        properties.put("end_osm_id", endLine != null ? endLine.getId() : null);
        properties.put("start_name", startLine != null ? startLine.getName() : null);
        properties.put("end_name", endLine != null ? endLine.getName() : null);
        properties.put("street_names", this.getStreetNames());
        properties.put("traffic_signal_cluster_id", this.trafficSignalCluster.getId());
        return properties;
    }

    @Override
    public void setRegions(Set<Region>  regions) {
        this.regions = regions;
    }
}
