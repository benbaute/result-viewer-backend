package com.simra.konsumgandalf.common.utils.services;

import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class GeoServiceTest {

	@InjectMocks
	private GeoService geoService;

	@Test
	public void testDistance() {
		double lat1 = 52.531073237769306;
		double lon1 = 13.434186838567257;

		double lat2 = 52.53081578761339;
		double lon2 = 13.434407953172922;

		double haversine = geoService.haversine(lat1, lon1, lat2, lon2);
		double orthodromic = geoService.orthodromicDistance(lat1, lon1, lat2, lon2);
		System.out.println("Haversine: " + haversine + " Orthodromic: " + orthodromic);
		assertTrue(haversine >= 20 && haversine <= 40);
		assertTrue(orthodromic >= 20 && orthodromic <= 40);
		assertTrue(Math.abs(haversine - orthodromic) < 1);
	}

	@Test
	public void testSpeeds() {
		MatchInformation m1 = new MatchInformation(52.531073237769306, 13.434186838567257, 1736444365504L / 1000);
		MatchInformation m2 = new MatchInformation(52.53081578761339, 13.434407953172922, 1736444370000L / 1000);
		MatchInformation m3 = new MatchInformation(52.53081578761339, 13.434407953172922, 1736444372000L / 1000);
		MatchInformation m4 = new MatchInformation(52.53081578761339, 13.434407953172922, 1736444375000L / 1000);
		MatchInformation m5 = new MatchInformation(52.53081578761339, 13.434407953172922, 1736444379000L / 1000);
		MatchInformation m6 = new MatchInformation(52.53081654198468, 13.43436973169446, 1736444385000L / 1000);

		List<MatchInformation> mList1 = List.of(m1, m2, m3, m4, m5, m6);
		/*
        List<Double> speeds = geoService.calculateSpeed(mList1);
		List<Double> averages = geoService.centeredMovingAverage(speeds, 1);
		System.out.println("Speeds: " + speeds);
		System.out.println("Averages: " + geoService.centeredMovingAverage(speeds, 0));
        System.out.println("Averages: " + geoService.centeredMovingAverage(speeds, 1));
		System.out.println("Averages: " + geoService.centeredMovingAverage(speeds, 3));
        System.out.println("Medians: " + geoService.centeredMovingMedian(speeds, 0));
        System.out.println("Medians: " + geoService.centeredMovingMedian(speeds, 1));
        System.out.println("Medians: " + geoService.centeredMovingMedian(speeds, 3));
        *
		 */
	}

    @Test
    public void testAverage() {
        List<Double> values = List.of(5.0, 5.0, 5.0, 5.0, 5.0, 10.0);
        //Double average = geoService.calculateAverage(values);
        //System.out.println(average);

        //Double std = geoService.calculateStandardDeviation(values);
        //System.out.println(std);
    }

}
