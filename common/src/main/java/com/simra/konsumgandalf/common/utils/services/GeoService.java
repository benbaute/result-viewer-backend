package com.simra.konsumgandalf.common.utils.services;

import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.common.models.interfaces.FeatureMappable;
import com.simra.konsumgandalf.common.models.interfaces.PropertiesMappable;
import org.geotools.referencing.GeodeticCalculator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class GeoService {
    private static Map<String, Object> createFeatureCollection(List<Map<String, Object>> features) {
        return Map.of(
            "type", "FeatureCollection",
            "features", features
        );
    }

    public static Map<String, Object> getFeatureCollection(List<? extends FeatureMappable> elements) {
        return createFeatureCollection(elements.stream().map(FeatureMappable::getFeatureMap).toList());
    }

    public static Map<String, Object> getFeatureCollection(Page<? extends FeatureMappable> page) {
        return Map.of(
                "metadata", Map.of(
                        "totalElements", page.getTotalElements(),
                        "totalPages", page.getTotalPages(),
                        "currentPage", page.getNumber()
                ),
                "geoData", createFeatureCollection(page.getContent().stream()
                        .map(FeatureMappable::getFeatureMap).toList())
        );
    }

    public static List<Map<String, Object>> getPropertiesCollection(List<? extends PropertiesMappable> elements) {
        return elements.stream().map(PropertiesMappable::getProperties).toList();
    }

    public double calculateAverage(List<Double> values) {
        double sum = 0.0;
        for (Double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    public double calculateStandardDeviation (List<Double> values) {
        double average = calculateAverage(values);
        List<Double> standardDeviations = new ArrayList<>();
        for (Double value : values) {
            standardDeviations.add(Math.pow(value - average, 2));
        }
        return Math.sqrt(calculateAverage(standardDeviations));
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
        return distance(first.getY(), first.getX(), second.getY(), second.getX());
    }

    public double distance(Point first, Point second) {
        return distance(first.getCoordinate(), second.getCoordinate());
    }

    public double distance(MatchInformation first, MatchInformation second) {
        return distance(first.getLat(), first.getLng(), second.getLat(), second.getLng());
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

    /**
     * Calculates the distance in meters between two points in EPSG:4326
     * @param lat1 - Latitude of first point
     * @param lon1 - Longitude of first point
     * @param lat2 - Latitude of second point
     * @param lon2 - Longitude of second point
     * @return - distance in meters
     */
	public double orthodromicDistance(double lat1, double lon1, double lat2, double lon2) {
        GeodeticCalculator geodeticCalculator = new GeodeticCalculator();
		geodeticCalculator.setStartingGeographicPoint(lon1, lat1);
		geodeticCalculator.setDestinationGeographicPoint(lon2, lat2);
		return geodeticCalculator.getOrthodromicDistance();
	}

	public List<Double> calculateSpeed(List<MatchInformation> coordinates) {
		List<Double> speeds = new ArrayList<>();
		for (int i = 0; i < coordinates.size() - 1; i++) {
			MatchInformation current = coordinates.get(i);
			MatchInformation next = coordinates.get(i + 1);
			if (next.getTimestamp() <= current.getTimestamp()) {
				throw new RuntimeException("Timestamps not strictly monotonic increasing.");
			}
            double timeDiff = (double) (next.getTimestamp() - current.getTimestamp());
			double distance = distance(next.getLat(), next.getLng(), current.getLat(), current.getLng());
			speeds.add(distance / timeDiff);
		}
        speeds.add(0.0);
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
