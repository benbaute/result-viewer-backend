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

--- Set Indexes for the analyticsServices
CREATE INDEX IF NOT EXISTS idx_rel_planet_osm_id ON ride_entity__planet_osm_line (planet_osm_lines_osm_id);
CREATE INDEX IF NOT EXISTS idx_rel_ride_entity_id ON ride_entity__planet_osm_line (ride_entities_id);

CREATE INDEX IF NOT EXISTS idx_region_way_gist ON region USING GIST (way);
CREATE INDEX IF NOT EXISTS idx_simra_region_way_gist ON region USING GIST (way);
CREATE INDEX IF NOT EXISTS idx_ride_entity_way_gist ON simra_region USING GIST (way);

--- Set indexes for intersection
CREATE INDEX IF NOT EXISTS traffic_signal_geom25833_idx ON traffic_signal USING GIST (geom25833);
CREATE INDEX IF NOT EXISTS traffic_signal_cluster_geom_3857_idx ON traffic_signal_cluster USING GIST (geom3857);

---Set Index for filtering streets
CREATE INDEX IF NOT EXISTS idx_planetosmline_lower_name_prefix
ON planet_osm_line (lower(name))
WHERE last_modified IS NOT NULL AND last_analysed IS NOT NULL;

CREATE OR REPLACE FUNCTION find_names_with_prefix(_prefix TEXT)
RETURNS TABLE(name VARCHAR)
LANGUAGE plpgsql
AS '
BEGIN
  SET LOCAL enable_seqscan = OFF;

  RETURN QUERY
    SELECT DISTINCT p.name
    FROM planet_osm_line p
    WHERE p.last_modified IS NOT NULL
      AND p.last_analysed IS NOT NULL
      AND LOWER(p.name) LIKE LOWER(_prefix || ''%'')
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
    WHERE p.last_modified IS NOT NULL
      AND p.last_analysed IS NOT NULL
      AND text(p.osm_id) LIKE _prefix || ''%''
    ORDER BY text(p.osm_id)
    LIMIT 10;
END;
';
