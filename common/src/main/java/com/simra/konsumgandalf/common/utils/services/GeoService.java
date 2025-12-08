package com.simra.konsumgandalf.common.utils.services;

import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.classes.MatchInformationDate;

import com.simra.konsumgandalf.common.models.entities.PlanetOsmLine;
import org.locationtech.jts.geom.*;
import org.locationtech.proj4j.*;


import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class GeoService {
    private static final CRSFactory crsFactory = new CRSFactory();
    private static final CoordinateTransformFactory ctFactory = new CoordinateTransformFactory();
    private static final CoordinateReferenceSystem sourceCRS = crsFactory.createFromName("EPSG:3857");
    private static final CoordinateReferenceSystem targetCRS = crsFactory.createFromName("EPSG:4326");
    private static final CoordinateTransform transform = ctFactory.createTransform(sourceCRS, targetCRS);

    public static Geometry transformLine(Geometry geom) {
        GeometryFactory geometryFactory = geom.getFactory();

        Coordinate[] srcCoords = geom.getCoordinates();
        Coordinate[] destCoords = new Coordinate[srcCoords.length];

        ProjCoordinate src = new ProjCoordinate();
        ProjCoordinate dest = new ProjCoordinate();

        for (int i = 0; i < srcCoords.length; i++) {
            src.x = srcCoords[i].x;
            src.y = srcCoords[i].y;
            transform.transform(src, dest);
            destCoords[i] = new Coordinate(dest.x, dest.y);
        }

        return geometryFactory.createLineString(destCoords);
    }

    public List<Coordinate> getLineCoordinates(PlanetOsmLine line) {
        Geometry transformGeometry = transformLine(line.getWay());
        List<Coordinate> coordinates = List.of();
        if (transformGeometry instanceof LineString lineString) {
            coordinates = Arrays.asList(lineString.getCoordinates());
        }
        return coordinates;
    }

	public double haversine(double lat1, double lon1, double lat2, double lon2) {
		final double R = 6371000;
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		return R * c;
	}

	public double distance(double lat1, double lon1, double lat2, double lon2) {
		return orthodromicDistance(lat1, lon1, lat2, lon2);
	}

    public double distance(Coordinate first, Coordinate second) {
        return distance(first.getX(), first.getY(), second.getX(), second.getY());
    }

    public double getLength(List<Coordinate> coordinates) {
        double length = 0;
        for (int i = 1; i < coordinates.size(); i++) {
            length += distance(coordinates.get(i - 1), coordinates.get(i));
        }
        return length;
    }

    public boolean pointInPolygon(Point point, Polygon polygon) {
        return polygon.contains(point);
    }

	public double orthodromicDistance(double lat1, double lon1, double lat2, double lon2) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
		geodeticCalculator.setStartingGeographicPoint(lon1, lat1);
		geodeticCalculator.setDestinationGeographicPoint(lon2, lat2);
		return geodeticCalculator.getOrthodromicDistance();
	}

	public List<Double> calculateSpeed(List<MatchInformationDate> coordinates) {
		List<Double> speeds = new ArrayList<>();
		speeds.add(0.0);
		for (int i = 1; i < coordinates.size(); i++) {
			MatchInformationDate current = coordinates.get(i);
			MatchInformationDate previous = coordinates.get(i - 1);
			if (previous.getOriginalTimestamp().getTime() >= current.getOriginalTimestamp().getTime()) {
				throw new RuntimeException("Timestamps not strictly monotonic increasing.");
			}
            long timeDifferenceLong = (current.getOriginalTimestamp().getTime() -
                    previous.getOriginalTimestamp().getTime());
            double timeDifference = (double) (timeDifferenceLong) / 1000; // In seconds
			double distance = distance(current.getLat(), current.getLng(), previous.getLat(), previous.getLng());
			speeds.add(distance / timeDifference);
		}
		return speeds;
	}

	public List<Double> calculateAcceleration(List<MatchInformation> coordinates, List<Double> speeds) {
		List<Double> acc = new ArrayList<>();
		acc.add(0.0);
		for (int i = 1; i < coordinates.size(); i++) {
			MatchInformation current = coordinates.get(i);
			MatchInformation previous = coordinates.get(i - 1);
			acc.add((speeds.get(i) - speeds.get(i - 1)) / (current.getTimestamp() - previous.getTimestamp()));
		}
		return acc;
	}

	public List<Double> centeredMovingAverage(List<Double> values, int windowSize) {
		List<Double> averages = new ArrayList<>();
		for (int i = 0; i < values.size(); i++) {
			double numerator = values.get(i);
			int denominator = 1;
			for (int j = 1; j <= windowSize; j++) {
				if (i + j < values.size()) {
					numerator += values.get(i + j);
					denominator += 1;
				}
				if (i - j >= 0) {
					numerator += values.get(i - j);
					denominator += 1;
				}
			}
			averages.add(numerator / denominator);
		}
		return averages;
	}

    public List<Double> centeredMovingMedian(List<Double> values, int windowSize) {
        List<Double> medians = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            List<Double> valuesToSort = new ArrayList<>();
            valuesToSort.add(values.get(i));
            for (int j = 1; j <= windowSize; j++) {
                if (i + j < values.size()) {
                    valuesToSort.add(values.get(i + j));
                }
                if (i - j >= 0) {
                    valuesToSort.add(values.get(i - j));
                }
            }
            Collections.sort(valuesToSort);
            medians.add(valuesToSort.get(valuesToSort.size()/2));
        }
        return medians;
    }

}
