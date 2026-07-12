WITH cte AS (
    SELECT MIN(ctid) AS keep_ctid, osm_id
    FROM planet_osm_line
    GROUP BY osm_id
    HAVING COUNT(*) > 1
)
DELETE FROM planet_osm_line
WHERE osm_id IN (SELECT osm_id FROM cte)
  AND ctid NOT IN (SELECT keep_ctid FROM cte);

DO
'
DECLARE
BEGIN
    -- Check if the primary key constraint already exists
    IF NOT EXISTS (
        SELECT 1
                FROM information_schema.table_constraints tc
                JOIN information_schema.key_column_usage kcu
                ON tc.constraint_name = kcu.constraint_name
                WHERE tc.table_name = ''planet_osm_line''
                AND tc.constraint_type = ''PRIMARY KEY''
                AND kcu.column_name = ''osm_id''
    ) THEN
        ALTER TABLE planet_osm_line ADD UNIQUE (osm_id);
        ALTER TABLE planet_osm_line ADD PRIMARY KEY (osm_id);
    END IF;
END;
'  LANGUAGE PLPGSQL;

CREATE OR REPLACE FUNCTION calculate_geometry_and_clear_coordinates()
RETURNS TRIGGER AS '
BEGIN
    -- Convert the coordinates JSON into a POINT geometry
    WITH points AS (
        SELECT ST_SetSRID(ST_MakePoint(coord.lng, coord.lat), 4326) AS geom
        FROM json_populate_recordset(NULL::record, NEW.coordinates::json) AS coord(lng double precision, lat double precision)
    )
    -- Create the LINESTRING geometry from the points
    UPDATE ride_entity
    SET way = (SELECT ST_MakeLine(geom) FROM points),
        coordinates = NULL  -- Clear the coordinates field
    WHERE id = NEW.id;

    -- Return the NEW record
    RETURN NEW;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_calculate_geometry_and_clear_coordinates
AFTER INSERT ON ride_entity
FOR EACH ROW
EXECUTE FUNCTION calculate_geometry_and_clear_coordinates();

-- Auto calculate the way of all ride incidents
CREATE OR REPLACE FUNCTION calculate_way_of_ride_incident()
RETURNS TRIGGER AS '
BEGIN
    NEW.way = ST_MakePoint(NEW.lng, NEW.lat);
    RETURN NEW;
END;
' LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER trigger_calculate_way_of_ride_incident
BEFORE INSERT OR UPDATE ON ride_incident
FOR EACH ROW
EXECUTE FUNCTION calculate_way_of_ride_incident();

-- Calculate the length of a geometry in kilometers which is not possible with non native queries
CREATE OR REPLACE FUNCTION st_length_m(geom geometry)
RETURNS double precision AS '
BEGIN
    RETURN ST_Length(geom::geography);
END;
' LANGUAGE plpgsql IMMUTABLE;

--- Make ltree function available
CREATE OR REPLACE FUNCTION str_to_ltree_is_ancestor(child_path ltree, parent_path text)
    RETURNS boolean AS '
BEGIN
    RETURN child_path::ltree <@ parent_path::ltree;
END;
' LANGUAGE plpgsql IMMUTABLE STRICT;

--- Set Indexes for the analyticsServices
CREATE INDEX IF NOT EXISTS idx_region_way_gist ON region USING GIST (way);
CREATE INDEX IF NOT EXISTS idx_simra_region_way_gist ON simra_region USING GIST (way);
CREATE INDEX IF NOT EXISTS idx_ride_entity_way_gist ON ride_entity USING GIST (way);

--- Set indexes for intersection
CREATE INDEX IF NOT EXISTS traffic_signal_geom25833_idx ON traffic_signal USING GIST (geom25833);
CREATE INDEX IF NOT EXISTS traffic_signal_cluster_geom_3857_idx ON traffic_signal_cluster USING GIST (geom3857);
CREATE INDEX IF NOT EXISTS region_geom_3857_idx ON region USING GIST (geom3857);
CREATE INDEX IF NOT EXISTS idx_region_ltree_path ON region USING gist (ltree_path);

CREATE INDEX IF NOT EXISTS idx_intersection__matched_points_m ON intersection__matched_points (matched_point_id);
CREATE INDEX IF NOT EXISTS idx_intersection__matched_points_i ON intersection__matched_points (intersection_id);

CREATE INDEX IF NOT EXISTS base_time ON intersection_base (week_day, traffic_time, year);
CREATE INDEX IF NOT EXISTS base_ride ON intersection_base (ride_id);
CREATE INDEX IF NOT EXISTS matched_point_ride ON matched_point(ride_id);
CREATE INDEX IF NOT EXISTS ride_point_ride ON ride_point(ride_id);

CREATE INDEX IF NOT EXISTS node_traffic_signal_cluster_id ON intersection_node (traffic_signal_cluster_id);
CREATE INDEX IF NOT EXISTS node_valhalla ON intersection_node (start_valhalla_edge_id, end_valhalla_edge_id);

CREATE INDEX IF NOT EXISTS edge_osm_id ON intersection_edge (prev_osm_id, osm_id, next_osm_id);
CREATE INDEX IF NOT EXISTS edge_valhalla ON
    intersection_edge (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id);


CREATE INDEX IF NOT EXISTS traffic_signal_geom4326_idx ON traffic_signal USING GIST (geom);

--- Custom functions
CREATE OR REPLACE FUNCTION find_names_with_prefix(_prefix TEXT)
RETURNS TABLE(name VARCHAR)
LANGUAGE plpgsql
AS '
BEGIN
  SET LOCAL enable_seqscan = OFF;

  RETURN QUERY
    SELECT DISTINCT p.name
    FROM planet_osm_line p
    WHERE LOWER(p.name) LIKE LOWER(_prefix || ''%'')
        AND EXISTS (
            SELECT 1
            FROM safety_metrics__planet_osm_line s
            WHERE s.osm_id = p.osm_id
        )
    ORDER BY p.name
    LIMIT 10;
END;
';

CREATE OR REPLACE FUNCTION find_osm_ids_with_prefix(_prefix TEXT)
RETURNS TABLE(osm_id TEXT)
LANGUAGE plpgsql
AS '
BEGIN
  SET LOCAL enable_seqscan = OFF;

  RETURN QUERY
    SELECT DISTINCT text(p.osm_id)
    FROM planet_osm_line p
    WHERE text(p.osm_id) LIKE _prefix || ''%''
      AND EXISTS (
            SELECT 1
            FROM safety_metrics__planet_osm_line s
            WHERE s.osm_id = p.osm_id
        )
    ORDER BY text(p.osm_id)
    LIMIT 10;
END;
';

CREATE OR REPLACE FUNCTION calculate_dangerous_score(
    number_of_rides BIGINT,
    number_of_incidents BIGINT,
    number_of_scary_incidents BIGINT
)
    RETURNS FLOAT
    LANGUAGE SQL
    IMMUTABLE
AS '
SELECT
    (4.4 * number_of_scary_incidents + (number_of_incidents - number_of_scary_incidents))::float
    / NULLIF(number_of_rides, 0);
';

CREATE OR REPLACE FUNCTION get_color_for_score(score FLOAT)
    RETURNS TEXT
    LANGUAGE SQL
    IMMUTABLE
AS '
SELECT CASE
    WHEN score >= 0.5  THEN ''#EF4444''  -- RED_500
    WHEN score >= 0.25 THEN ''#F97316''  -- ORANGE_500
    WHEN score >= 0.1  THEN ''#F59E0B''  -- AMBER_500
    WHEN score >= 0.04 THEN ''#84CC16''  -- LIME_500
    WHEN score >= 0.0  THEN ''#22C55E''  -- GREEN_500
    ELSE ''#E5E5E5''                     -- NEUTRAL_200
END;
';






--- MATERIALIZED VIEWs


CREATE MATERIALIZED VIEW IF NOT EXISTS safety_metrics__planet_osm_line AS
WITH rides AS (
    SELECT l.planet_osm_lines_osm_id AS osm_id, r.week_day, r.traffic_time, r.year, COUNT(r.id) AS number_of_rides
    FROM ride_entity__planet_osm_line l
         JOIN ride_entity r ON l.ride_entities_id = r.id
    GROUP BY l.planet_osm_lines_osm_id, r.week_day, r.traffic_time, r.year
),
incidents AS (
    SELECT planet_osm_line_osm_id AS osm_id, week_day, traffic_time, year,
        SUM(CASE WHEN scary = true THEN 1 ELSE 0 END)  AS number_of_scary_incidents,
        SUM(CASE WHEN incident_type = 'PULLING_IN_OUT' THEN 1 ELSE 0 END)  AS number_of_pull_in_outs,
        SUM(CASE WHEN incident_type = 'CLOSE_PASS' THEN 1 ELSE 0 END)  AS number_of_close_passes,
        SUM(CASE WHEN incident_type = 'NEAR_LEFT_RIGHT_HOOK' THEN 1 ELSE 0 END)  AS number_of_near_left_right_hooks,
        SUM(CASE WHEN incident_type = 'HEAD_ON_APPROACH' THEN 1 ELSE 0 END)  AS number_of_head_on_approaches,
        SUM(CASE WHEN incident_type = 'TAILGATING' THEN 1 ELSE 0 END)  AS number_of_tailgating,
        SUM(CASE WHEN incident_type = 'NEAR_DOORING' THEN 1 ELSE 0 END)  AS number_of_near_doorings,
        SUM(CASE WHEN incident_type = 'DODGING_OBSTACLE' THEN 1 ELSE 0 END)  AS number_of_obstacle_dodges,
        COUNT(*) AS number_of_incidents
    FROM ride_incident
    GROUP BY planet_osm_line_osm_id, week_day, traffic_time, year
),
combined AS (
    SELECT
        r.osm_id,
        r.week_day,
        r.traffic_time,
        r.year,
        r.number_of_rides,
        coalesce(i.number_of_incidents, 0)       AS number_of_incidents,
        coalesce(i.number_of_scary_incidents, 0) AS number_of_scary_incidents,
        coalesce(i.number_of_pull_in_outs, 0) AS number_of_pull_in_outs,
        coalesce(i.number_of_close_passes, 0) AS number_of_close_passes,
        coalesce(i.number_of_near_left_right_hooks, 0) AS number_of_near_left_right_hooks,
        coalesce(i.number_of_head_on_approaches, 0) AS number_of_head_on_approaches,
        coalesce(i.number_of_tailgating, 0) AS number_of_tailgating,
        coalesce(i.number_of_near_doorings, 0) AS number_of_near_doorings,
        coalesce(i.number_of_obstacle_dodges, 0) AS number_of_obstacle_dodges
    FROM rides r
    LEFT JOIN incidents i ON
        r.osm_id = i.osm_id AND
        r.week_day = i.week_day AND
        r.traffic_time = i.traffic_time AND
        r.year = i.year
),
aggregated AS (
    SELECT
        osm_id,

        COALESCE(week_day, 'ALL_WEEK')      AS week_day,     -- aggregation name for week
        COALESCE(traffic_time, 'ALL_DAY')   AS traffic_time, -- aggregation name for traffic time
        COALESCE(year, 2000)                AS year,         -- aggregation number for years

        SUM(number_of_rides)::bigint AS number_of_rides,
        SUM(number_of_incidents)::bigint AS number_of_incidents,
        SUM(number_of_scary_incidents)::bigint AS number_of_scary_incidents,
        SUM(number_of_pull_in_outs) AS number_of_pull_in_outs,
        SUM(number_of_close_passes) AS number_of_close_passes,
        SUM(number_of_near_left_right_hooks) AS number_of_near_left_right_hooks,
        SUM(number_of_head_on_approaches) AS number_of_head_on_approaches,
        SUM(number_of_tailgating) AS number_of_tailgating,
        SUM(number_of_near_doorings) AS number_of_near_doorings,
        SUM(number_of_obstacle_dodges) AS number_of_obstacle_dodges
    FROM combined
    GROUP BY GROUPING SETS (
    (osm_id),
    (osm_id, year),
    (osm_id, traffic_time),
    (osm_id, traffic_time, year),
    (osm_id, week_day),
    (osm_id, week_day, year),
    (osm_id, week_day, traffic_time),
    (osm_id, week_day, traffic_time, year)
    )
)
SELECT
    osm_id,
    week_day,
    traffic_time,
    year,
    number_of_rides,
    number_of_incidents,
    calculate_dangerous_score(number_of_rides, number_of_incidents, number_of_scary_incidents) AS dangerous_score,
    get_color_for_score(calculate_dangerous_score(number_of_rides, number_of_incidents, number_of_scary_incidents)) AS dangerous_color,
    number_of_scary_incidents,
    number_of_pull_in_outs,
    number_of_close_passes,
    number_of_near_left_right_hooks,
    number_of_head_on_approaches,
    number_of_tailgating,
    number_of_near_doorings,
    number_of_obstacle_dodges
FROM aggregated
;
CREATE UNIQUE INDEX IF NOT EXISTS safety_metrics__planet_osm_line_pk
    ON safety_metrics__planet_osm_line (osm_id, week_day, traffic_time, year);
CREATE INDEX IF NOT EXISTS safety_metrics__planet_osm_line_dangerous
    ON safety_metrics__planet_osm_line (dangerous_score);
CREATE INDEX IF NOT EXISTS safety_metrics__planet_osm_line_osm_id
    ON safety_metrics__planet_osm_line (osm_id);


CREATE MATERIALIZED VIEW IF NOT EXISTS safety_metrics__region AS
WITH base_number_of_rides_and_length AS (
    SELECT region.id,
           r.traffic_time,
           r.week_day,
           r.year as year,
           COUNT(*) as totalRides,
           --  st_intersection is expensive and slows down each REFRESH
           -- TODO: Remove all geometry operations from materialized views
           SUM(ST_Length_M(ST_Intersection(r.way, region.way))) as totalDistance
    FROM region
             JOIN ride_entity r ON st_intersects(region.way, r.way)
    GROUP BY region.id, r.traffic_time, r.week_day, r.year
),
     number_of_rides_and_length AS (
         SELECT
             id,
             COALESCE(week_day, 'ALL_WEEK')      AS week_day,     -- aggregation name for week
             COALESCE(traffic_time, 'ALL_DAY')   AS traffic_time, -- aggregation name for traffic time
             COALESCE(year, 2000)                AS year,         -- aggregation number for years
             SUM(totalRides) AS number_of_rides,
             SUM(totalDistance) AS total_distance
         FROM base_number_of_rides_and_length
         -- The grouping sets MUST be the same as in safety_metrics__planet_osm_line or else aggregation is lost
         GROUP BY GROUPING SETS (
             (id),
             (id, year),
             (id, traffic_time),
             (id, traffic_time, year),
             (id, week_day),
             (id, week_day, year),
             (id, week_day, traffic_time),
             (id, week_day, traffic_time, year)
         )
     ),
     incidents AS (
         SELECT
             r.id, s.traffic_time, s.week_day, s.year,
             SUM(number_of_rides)::bigint AS number_of_rides,
             SUM(number_of_incidents)::bigint AS number_of_incidents,
             SUM(number_of_scary_incidents)::bigint AS number_of_scary_incidents,
             SUM(number_of_pull_in_outs) AS number_of_pull_in_outs,
             SUM(number_of_close_passes) AS number_of_close_passes,
             SUM(number_of_near_left_right_hooks) AS number_of_near_left_right_hooks,
             SUM(number_of_head_on_approaches) AS number_of_head_on_approaches,
             SUM(number_of_tailgating) AS number_of_tailgating,
             SUM(number_of_near_doorings) AS number_of_near_doorings,
             SUM(number_of_obstacle_dodges) AS number_of_obstacle_dodges
         FROM safety_metrics__planet_osm_line s
                  JOIN planet_osm_line l ON l.osm_id = s.osm_id
                  JOIN region r
                       ON l.way && r.geom3857
                           AND ST_Contains(r.geom3857, l.way)
         GROUP BY r.id, s.traffic_time, s.week_day, s.year
     ),
     combined AS (
         SELECT
             n.id,
             n.week_day,
             n.traffic_time,
             n.year,
             n.number_of_rides::bigint ,
             n.total_distance,
             coalesce(i.number_of_incidents, 0)::bigint  AS number_of_incidents,
             coalesce(i.number_of_scary_incidents, 0)::bigint AS number_of_scary_incidents,
             coalesce(i.number_of_pull_in_outs, 0) AS number_of_pull_in_outs,
             coalesce(i.number_of_close_passes, 0) AS number_of_close_passes,
             coalesce(i.number_of_near_left_right_hooks, 0) AS number_of_near_left_right_hooks,
             coalesce(i.number_of_head_on_approaches, 0) AS number_of_head_on_approaches,
             coalesce(i.number_of_tailgating, 0) AS number_of_tailgating,
             coalesce(i.number_of_near_doorings, 0) AS number_of_near_doorings,
             coalesce(i.number_of_obstacle_dodges, 0) AS number_of_obstacle_dodges
         FROM number_of_rides_and_length n
                  LEFT JOIN incidents i ON
             n.id = i.id AND
             n.week_day = i.week_day AND
             n.traffic_time = i.traffic_time AND
             n.year = i.year
     )
SELECT
    combined.id,
    region.name,
    week_day,
    traffic_time,
    year,
    number_of_rides,
    total_distance,
    number_of_incidents,
    calculate_dangerous_score(number_of_rides, number_of_incidents, number_of_scary_incidents) AS dangerous_score,
    get_color_for_score(calculate_dangerous_score(number_of_rides, number_of_incidents, number_of_scary_incidents)) AS dangerous_color,
    number_of_scary_incidents,
    number_of_pull_in_outs,
    number_of_close_passes,
    number_of_near_left_right_hooks,
    number_of_head_on_approaches,
    number_of_tailgating,
    number_of_near_doorings,
    number_of_obstacle_dodges
FROM combined
         JOIN region ON region.id = combined.id
;
CREATE UNIQUE INDEX IF NOT EXISTS safety_metrics__region_pk
    ON safety_metrics__region (id, week_day, traffic_time, year);
CREATE INDEX IF NOT EXISTS safety_metrics__region_dangerous
    ON safety_metrics__region(dangerous_score);
CREATE INDEX IF NOT EXISTS safety_metrics__region_name
    ON safety_metrics__region (name);


CREATE MATERIALIZED VIEW IF NOT EXISTS safety_metrics__simra_region AS
WITH region_safety AS (
    SELECT
        rr.simra_region_name AS name,
        m.traffic_time,
        m.week_day,
        m.year,
        SUM(total_distance)                  AS total_distance,
        SUM(number_of_rides)::bigint         AS number_of_rides,
        SUM(number_of_incidents)::bigint     AS number_of_incidents,
        SUM(number_of_scary_incidents)::bigint         AS number_of_scary_incidents,
        SUM(number_of_pull_in_outs)          AS number_of_pull_in_outs,
        SUM(number_of_close_passes)          AS number_of_close_passes,
        SUM(number_of_near_left_right_hooks) AS number_of_near_left_right_hooks,
        SUM(number_of_head_on_approaches)    AS number_of_head_on_approaches,
        SUM(number_of_tailgating)           AS number_of_tailgating,
        SUM(number_of_near_doorings)         AS number_of_near_doorings,
        SUM(number_of_obstacle_dodges)       AS number_of_obstacle_dodges
    FROM safety_metrics__region m
             JOIN simra_region__region rr
                  ON rr.region_id = m.id
    GROUP BY rr.simra_region_name, m.traffic_time, m.week_day, m.year
)
SELECT
    name,
    week_day,
    traffic_time,
    year,
    number_of_rides,
    total_distance,
    number_of_incidents,
    calculate_dangerous_score(number_of_rides, number_of_incidents, number_of_scary_incidents) AS dangerous_score,
    get_color_for_score(calculate_dangerous_score(number_of_rides, number_of_incidents, number_of_scary_incidents)) AS dangerous_color,
    number_of_scary_incidents,
    number_of_pull_in_outs,
    number_of_close_passes,
    number_of_near_left_right_hooks,
    number_of_head_on_approaches,
    number_of_tailgating,
    number_of_near_doorings,
    number_of_obstacle_dodges
FROM region_safety
;
CREATE UNIQUE INDEX IF NOT EXISTS safety_metrics__simra_region_pk
    ON safety_metrics__simra_region (name, week_day, traffic_time, year);
CREATE INDEX IF NOT EXISTS safety_metrics__simra_region_dangerous
    ON safety_metrics__simra_region(dangerous_score);
CREATE INDEX IF NOT EXISTS safety_metrics__simra_region_id
    ON safety_metrics__simra_region (name);



CREATE MATERIALIZED VIEW IF NOT EXISTS intersection_edge_metrics AS
/* TODO: this does not work, it takes to much time to complete
WITH global_examples AS (
    SELECT
        valhalla_edge_id,
        prev_valhalla_edge_id,
        next_valhalla_edge_id,
        MIN(id) AS global_example_id
    FROM intersection_edge
    GROUP BY valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id
)
*/
SELECT
    row_number() OVER () AS id, -- primary key for hibernate
    agg.valhalla_edge_id,
    agg.prev_valhalla_edge_id,
    agg.next_valhalla_edge_id,

    agg.week_day,
    agg.traffic_time,
    agg.year,

    example_base.smallest_region AS smallest_region,
    example_base.geom AS geom,
    example_id,
    -- global_example_id,
    line.name AS name,

    example.osm_id,
    example.prev_osm_id,
    example.next_osm_id,

    agg.number_of_rides,
    agg.avg_length,
    agg.avg_speed,
    agg.avg_duration,
    agg.avg_waiting,
    agg.sum_waiting_time,
    agg.max_waiting_time,
    agg.stop_rate,
    agg.avg_waiting_when_stopped
FROM (
         SELECT
             valhalla_edge_id,
             prev_valhalla_edge_id,
             next_valhalla_edge_id,
             COALESCE(week_day, 'ALL_WEEK')      AS week_day,     -- aggregation name for week
             COALESCE(traffic_time, 'ALL_DAY')   AS traffic_time, -- aggregation name for traffic time
             COALESCE(year, 2000)                AS year,         -- aggregation number for years

             MIN(base.id) AS example_id,

             COUNT(*) AS number_of_rides,
             AVG(length) AS avg_length,
             AVG(speed) AS avg_speed,
             AVG(duration) AS avg_duration,
             AVG(waiting_time) AS avg_waiting,
             SUM(waiting_time) AS sum_waiting_time,
             MAX(waiting_time) AS max_waiting_time,

             100::float8 * COALESCE(SUM(CASE WHEN waiting_time > 3 THEN 1.0 END), 0) / COUNT(*) AS stop_rate,
             COALESCE(AVG(CASE WHEN waiting_time > 3 THEN waiting_time END), 0)::float8 AS avg_waiting_when_stopped

         FROM intersection_edge edge
         JOIN intersection_base base ON edge.id = base.id

         GROUP BY GROUPING SETS (
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id),
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id, year),
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id, traffic_time),
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id, traffic_time, year),
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id, week_day),
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id, week_day, year),
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id, week_day, traffic_time),
             (valhalla_edge_id, prev_valhalla_edge_id, next_valhalla_edge_id, week_day, traffic_time, year)
             )
     ) agg
/*
JOIN global_examples g ON
    g.valhalla_edge_id IS NOT DISTINCT FROM agg.valhalla_edge_id AND
    g.prev_valhalla_edge_id IS NOT DISTINCT FROM agg.prev_valhalla_edge_id AND
    g.next_valhalla_edge_id IS NOT DISTINCT FROM agg.next_valhalla_edge_id
*/
JOIN intersection_edge example ON example.id = agg.example_id
JOIN intersection_base example_base ON example.id = example_base.id
LEFT JOIN planet_osm_line line ON line.osm_id = example.osm_id
;
CREATE INDEX IF NOT EXISTS idx_intersection_edge_time ON intersection_edge_metrics(week_day, traffic_time, year);
CREATE INDEX IF NOT EXISTS idx_intersection_edge_metrics_geom4326 ON intersection_edge_metrics USING GIST (geom);


CREATE MATERIALIZED VIEW IF NOT EXISTS intersection_node_metrics AS
WITH global_examples AS (
    SELECT
        traffic_signal_cluster_id,
        start_valhalla_edge_id,
        end_valhalla_edge_id,
        MIN(id) AS global_example_id
    FROM intersection_node
    GROUP BY traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id
)
SELECT
    row_number() OVER () AS id, -- primary key for hibernate
    agg.traffic_signal_cluster_id,
    agg.start_valhalla_edge_id,
    agg.end_valhalla_edge_id,

    agg.week_day,
    agg.traffic_time,
    agg.year,

    example_base.smallest_region AS smallest_region,
    example_base.geom AS geom,
    example_id,
    global_example_id,
    sl.name AS start_name,
    el.name AS end_name,
    example.street_names,

    example.start_osm_id,
    example.end_osm_id,

    agg.number_of_rides,
    agg.avg_length,
    agg.avg_speed,
    agg.avg_duration,
    agg.avg_waiting,
    agg.sum_waiting_time,
    agg.max_waiting_time,
    agg.stop_rate,
    agg.avg_waiting_when_stopped
FROM (
         SELECT
             traffic_signal_cluster_id,
             start_valhalla_edge_id AS start_valhalla_edge_id, -- map null to -1 for quick integer grouping
             end_valhalla_edge_id AS end_valhalla_edge_id,
             COALESCE(week_day, 'ALL_WEEK')      AS week_day,     -- aggregation name for week
             COALESCE(traffic_time, 'ALL_DAY')   AS traffic_time, -- aggregation name for traffic time
             COALESCE(year, 2000)                AS year,         -- aggregation number for years

             MIN(base.id) AS example_id,

             COUNT(*) AS number_of_rides,
             AVG(length) AS avg_length,
             AVG(speed) AS avg_speed,
             AVG(duration) AS avg_duration,
             AVG(waiting_time) AS avg_waiting,
             SUM(waiting_time) AS sum_waiting_time,
             MAX(waiting_time) AS max_waiting_time,

             100::float8 * COALESCE(SUM(CASE WHEN waiting_time > 3 THEN 1.0 END), 0) / COUNT(*) AS stop_rate,
             COALESCE(AVG(CASE WHEN waiting_time > 3 THEN waiting_time END), 0)::float8 AS avg_waiting_when_stopped

         FROM intersection_node node
         JOIN intersection_base base ON node.id = base.id
         GROUP BY GROUPING SETS (
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id),
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id, year),
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id, traffic_time),
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id, traffic_time, year),
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id, week_day),
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id, week_day, year),
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id, week_day, traffic_time),
             (traffic_signal_cluster_id, start_valhalla_edge_id, end_valhalla_edge_id, week_day, traffic_time, year)
             )
     ) agg
JOIN global_examples g ON
    g.traffic_signal_cluster_id = agg.traffic_signal_cluster_id AND
    g.start_valhalla_edge_id IS NOT DISTINCT FROM agg.start_valhalla_edge_id AND
    g.end_valhalla_edge_id IS NOT DISTINCT FROM agg.end_valhalla_edge_id
JOIN intersection_node example ON example.id = agg.example_id
JOIN intersection_base example_base ON example.id = example_base.id
LEFT JOIN planet_osm_line sl ON sl.osm_id = example.start_osm_id
LEFT JOIN planet_osm_line el ON el.osm_id = example.end_osm_id
;
CREATE INDEX IF NOT EXISTS idx_intersection_node_metrics_traffic_signal ON intersection_node_metrics(traffic_signal_cluster_id);
CREATE INDEX IF NOT EXISTS idx_intersection_node_time ON intersection_node_metrics(week_day, traffic_time, year);
CREATE INDEX IF NOT EXISTS idx_intersection_node_metrics_geom4326 ON intersection_node_metrics USING GIST (geom);


CREATE MATERIALIZED VIEW IF NOT EXISTS intersection_region_metrics_smallest_regions AS
WITH edges AS (
    SELECT
        base.smallest_region,
        COALESCE(week_day, 'ALL_WEEK')                                 AS week_day,     -- aggregation name for week
        COALESCE(traffic_time, 'ALL_DAY')                              AS traffic_time, -- aggregation name for traffic time
        COALESCE(year, 2000)                                           AS year,         -- aggregation number for years
        COUNT(*)                                                       AS number_of_edges,
        SUM(length) / 1000.0                                           AS edge_length_km,
        SUM(duration)                                                  AS edge_duration,
        SUM(waiting_time)                                                   AS edge_waiting_time,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN 1.0 END), 0)           AS edge_stop_count,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN waiting_time END), 0)  AS edge_stop_time
    FROM intersection_edge edge
    JOIN intersection_base base ON edge.id = base.id
    GROUP BY GROUPING SETS (
        (base.smallest_region),
        (base.smallest_region, year),
        (base.smallest_region, traffic_time),
        (base.smallest_region, traffic_time, year),
        (base.smallest_region, week_day),
        (base.smallest_region, week_day, year),
        (base.smallest_region, week_day, traffic_time),
        (base.smallest_region, week_day, traffic_time, year)
    )
), nodes AS (
    SELECT
        base.smallest_region,
        COALESCE(week_day, 'ALL_WEEK')                                 AS week_day,     -- aggregation name for week
        COALESCE(traffic_time, 'ALL_DAY')                              AS traffic_time, -- aggregation name for traffic time
        COALESCE(year, 2000)                                           AS year,         -- aggregation number for years
        COUNT(*)                                                       AS number_of_nodes,
        SUM(length) / 1000.0                                           AS node_length_km,
        SUM(duration)                                                  AS node_duration,
        SUM(waiting_time)                                              AS node_waiting_time,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN 1.0 END), 0)           AS node_stop_count,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN waiting_time END), 0)  AS node_stop_time
    FROM intersection_node node
    JOIN intersection_base base ON node.id = base.id
    GROUP BY GROUPING SETS (
        (base.smallest_region),
        (base.smallest_region, year),
        (base.smallest_region, traffic_time),
        (base.smallest_region, traffic_time, year),
        (base.smallest_region, week_day),
        (base.smallest_region, week_day, year),
        (base.smallest_region, week_day, traffic_time),
        (base.smallest_region, week_day, traffic_time, year)
    )
)
SELECT
    n.smallest_region,
    n.week_day,
    n.traffic_time,
    n.year,
    
    e.number_of_edges,
    e.edge_length_km,
    e.edge_duration,
    e.edge_waiting_time,
    e.edge_stop_count,
    e.edge_stop_time,

    n.number_of_nodes,
    n.node_length_km,
    n.node_duration,
    n.node_waiting_time,
    n.node_stop_count,
    n.node_stop_time

FROM nodes n
JOIN edges e ON n.smallest_region = e.smallest_region
AND n.week_day = e.week_day
AND n.traffic_time = e.traffic_time
AND n.year = e.year
;
CREATE INDEX IF NOT EXISTS idx_intersection_region_metrics_smallest_regions_id ON intersection_region_metrics_smallest_regions (smallest_region);
CREATE INDEX IF NOT EXISTS idx_intersection_region_metrics_smallest_regions_time ON intersection_region_metrics_smallest_regions (week_day, traffic_time, year);


CREATE MATERIALIZED VIEW IF NOT EXISTS intersection_region_ride_counts_metrics AS
SELECT
    rr.region_id,
    COALESCE(base.week_day, 'ALL_WEEK')     AS week_day,
    COALESCE(base.traffic_time, 'ALL_DAY')  AS traffic_time,
    COALESCE(base.year, 2000)               AS year,
    COUNT(DISTINCT rr.ride_id)              AS number_of_rides
FROM ride__region rr
JOIN intersection_base base ON rr.ride_id = base.ride_id
GROUP BY GROUPING SETS (
    (rr.region_id),
    (rr.region_id, year),
    (rr.region_id, traffic_time),
    (rr.region_id, traffic_time, year),
    (rr.region_id, week_day),
    (rr.region_id, week_day, year),
    (rr.region_id, week_day, traffic_time),
    (rr.region_id, week_day, traffic_time, year)
);
CREATE INDEX IF NOT EXISTS idx_reg_ride_counts_perf
    ON intersection_region_ride_counts_metrics (region_id, week_day, traffic_time, year);


CREATE MATERIALIZED VIEW IF NOT EXISTS intersection_region_metrics AS
SELECT row_number() OVER () AS id, c.week_day, c.traffic_time, c.year,
       r.id AS region_id, c.number_of_rides,
       number_of_edges, edge_length_km, edge_duration, edge_waiting_time, edge_stop_count, edge_stop_time,
       number_of_nodes, node_length_km, node_duration, node_waiting_time, node_stop_count, node_stop_time
FROM region r
JOIN (
    SELECT combined.id, week_day, traffic_time, year,
        SUM(number_of_edges) AS number_of_edges, SUM(edge_length_km) AS edge_length_km,
        SUM(edge_duration) AS edge_duration, SUM(edge_waiting_time) AS edge_waiting_time,
        SUM(edge_stop_count) AS edge_stop_count,  SUM(edge_stop_time) AS edge_stop_time,
        SUM(number_of_nodes) AS number_of_nodes, SUM(node_length_km) AS node_length_km,
        SUM(node_duration) AS node_duration, SUM(node_waiting_time) AS node_waiting_time,
        SUM(node_stop_count) AS node_stop_count,  SUM(node_stop_time) AS node_stop_time
    FROM region combined
    JOIN (
        SELECT *
        FROM intersection_region_metrics_smallest_regions i
        JOIN region r ON r.id = i.smallest_region
    ) small
    ON small.ltree_path <@ combined.ltree_path
    GROUP BY combined.id, week_day, traffic_time, year
) agg ON agg.id = r.id
JOIN intersection_region_ride_counts_metrics c
ON c.region_id = r.id
    AND c.year = agg.year
    AND c.week_day = agg.week_day
    AND c.traffic_time = agg.traffic_time;
CREATE INDEX IF NOT EXISTS idx_intersection_region_metrics_id ON intersection_region_metrics (region_id);
CREATE INDEX IF NOT EXISTS idx_intersection_region_metrics_time ON intersection_region_metrics (week_day, traffic_time, year);


CREATE MATERIALIZED VIEW IF NOT EXISTS intersection_ride_region_metrics_smallest_regions AS
WITH edges AS (
    SELECT
        base.smallest_region,
        base.ride_id,
        MIN(base.id)                                                        AS first_id,
        COUNT(*)                                                            AS number_of_edges,
        SUM(length) / 1000.0                                                AS edge_length_km,
        SUM(duration)                                                       AS edge_duration,
        SUM(waiting_time)                                                   AS edge_waiting_time,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN 1.0 END), 0)           AS edge_stop_count,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN waiting_time END), 0)  AS edge_stop_time
    FROM intersection_edge edge
    JOIN intersection_base base ON edge.id = base.id
    GROUP BY base.smallest_region, base.ride_id
), nodes AS (
    SELECT
        base.smallest_region,
        base.ride_id,
        COUNT(*)                                                            AS number_of_nodes,
        SUM(length) / 1000                                                  AS node_length_km,
        SUM(duration)                                                       AS node_duration,
        SUM(waiting_time)                                                   AS node_waiting_time,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN 1.0 END), 0)           AS node_stop_count,
        COALESCE(SUM(CASE WHEN waiting_time > 3 THEN waiting_time END), 0)  AS node_stop_time
    FROM intersection_node node
    JOIN intersection_base base ON node.id = base.id
    GROUP BY base.smallest_region, base.ride_id
)
SELECT
    e.first_id,
    e.smallest_region,
    e.ride_id,
    b.start_time,
    b.week_day,
    b.traffic_time, -- TODO: Selects traffic time based on first id, other function may be better
    b.year,

    b.median_speed AS median_ride_speed,

    e.number_of_edges,
    e.edge_length_km,
    e.edge_duration,
    e.edge_waiting_time,
    e.edge_stop_count,
    e.edge_stop_time,

    COALESCE(n.number_of_nodes, 0)::float8                  AS number_of_nodes,
    COALESCE(n.node_length_km, 0)::float8                   AS node_length_km,
    COALESCE(n.node_duration, 0)::float8                    AS node_duration,
    COALESCE(n.node_waiting_time, 0)::float8                AS node_waiting_time,
    COALESCE(n.node_stop_count, 0)::float8                  AS node_stop_count,
    COALESCE(n.node_stop_time, 0)::float8                   AS node_stop_time

FROM region
JOIN edges e ON region.id = e.smallest_region  -- Full Join, only include rows with at least one edge
LEFT JOIN nodes n ON region.id = n.smallest_region AND n.ride_id = e.ride_id -- Nodes not required
JOIN intersection_base b ON b.id = e.first_id
;
CREATE UNIQUE INDEX IF NOT EXISTS intersection_ride_region_metrics_smallest_regions_pk
    ON intersection_ride_region_metrics_smallest_regions (smallest_region, ride_id);


CREATE MATERIALIZED VIEW IF NOT EXISTS intersection_ride_region_metrics AS
SELECT row_number() OVER () AS id, b.week_day, b.traffic_time, b.year,
       agg.id AS region_id, agg.ride_id, b.start_time, b.median_speed,
       number_of_edges, edge_length_km, edge_duration, edge_waiting_time, edge_stop_count, edge_stop_time,
       number_of_nodes, node_length_km, node_duration, node_waiting_time, node_stop_count, node_stop_time
FROM (
    SELECT combined.id, ride_id, MIN(small.first_id) AS first_id,
       SUM(number_of_edges) AS number_of_edges, SUM(edge_length_km) AS edge_length_km,
       SUM(edge_duration) AS edge_duration, SUM(edge_waiting_time) AS edge_waiting_time,
       SUM(edge_stop_count) AS edge_stop_count,  SUM(edge_stop_time) AS edge_stop_time,
       SUM(number_of_nodes) AS number_of_nodes, SUM(node_length_km) AS node_length_km,
       SUM(node_duration) AS node_duration, SUM(node_waiting_time) AS node_waiting_time,
       SUM(node_stop_count) AS node_stop_count,  SUM(node_stop_time) AS node_stop_time
    FROM region combined
    JOIN (
        SELECT *
        FROM intersection_ride_region_metrics_smallest_regions i
        JOIN region r ON r.id = i.smallest_region
    ) small
    ON small.ltree_path <@ combined.ltree_path
    GROUP BY combined.id, ride_id
) agg
JOIN intersection_base b ON b.id = first_id;
CREATE UNIQUE INDEX IF NOT EXISTS intersection_ride_region_metrics_pk
    ON intersection_ride_region_metrics (region_id, ride_id);
