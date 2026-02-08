package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.models.classes.RideLocation;
import com.simra.konsumgandalf.common.models.entities.RideEntity;
import com.simra.konsumgandalf.common.models.entities.RideIncident;
import com.simra.konsumgandalf.common.models.enums.IncidentType;
import com.simra.konsumgandalf.common.models.enums.ParticipantType;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.utils.services.CsvUtilService;
import com.simra.konsumgandalf.common.utils.services.FileReaderService;
import com.simra.konsumgandalf.rides.repositories.RideEntityRepository;
import com.simra.konsumgandalf.valhalla.services.ValhallaTraceAttributesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.simra.konsumgandalf.common.constants.AppDates.FALLBACK_DATE;
import static com.simra.konsumgandalf.common.constants.AppDates.START_OF_RECORDING;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RideEntityServiceTest {

	@Mock
	private RideEntityRepository rideEntityRepository;

	@Mock
	private PlanetOsmLineRepository planetOsmLineRepository;

	@Mock
	private ValhallaTraceAttributesService valhallaTraceAttributesService;

	@Mock
	private FileReaderService fileReaderService;

	@Mock
	private CsvUtilService csvUtilService;

	@InjectMocks
	private RideEntityService rideEntityService = new RideEntityService("./");

	RideEntityService rideEntityServiceSpy;

	@BeforeEach
	public void setUp() {
		rideEntityServiceSpy = spy(rideEntityService);
	}

	@Nested
	class EnrichRideEntityWithCsv {

		RideEntity mockRideEntity;

		@Test
		public void testEnrichRideEntityWithCsv_ValidFile() throws Exception {

			when(fileReaderService.readFileFromPath("valid.csv"))
				.thenReturn("bike,incident\n1,3" + "\n====\nlat,lng\n1.0,4.0");

			RideIncident mockRideIncident = new RideIncident();
			mockRideIncident.setLat(1.0);
			mockRideIncident.setLng(4.0);
			mockRideIncident.setI1(1);
			mockRideIncident.setTs(START_OF_RECORDING.getTime());
			mockRideIncident.setIncidentType(IncidentType.CLOSE_PASS);
			mockRideIncident.setParticipantsInvolved(new ArrayList<>());
			when(csvUtilService.parseCsvToModel("bike,incident\n1,3", RideIncident.class))
				.thenReturn(Collections.singletonList(mockRideIncident));

			RideLocation mockRideLocationValid = new RideLocation();
			mockRideLocationValid.setLat(1.0);
			mockRideLocationValid.setLng(4.0);
			mockRideLocationValid.setTimeStamp(START_OF_RECORDING.getTime());
			RideLocation mockRideLocationValid2 = new RideLocation();
			mockRideLocationValid2.setLat(1.2);
			mockRideLocationValid2.setLng(4.1);
			mockRideLocationValid2.setTimeStamp(START_OF_RECORDING.getTime());
			RideLocation mockRideLocationValid3 = new RideLocation();
			mockRideLocationValid3.setLat(1.4);
			mockRideLocationValid3.setLng(4.3);
			mockRideLocationValid3.setTimeStamp(START_OF_RECORDING.getTime());
			RideLocation mockInvalidRideLocation = new RideLocation();
			mockInvalidRideLocation.setLat(1.0);
			mockInvalidRideLocation.setLng(4.0);
			when(csvUtilService.parseCsvToModel("lat,lng\n1.0,4.0", RideLocation.class)).thenReturn(List
				.of(mockRideLocationValid, mockRideLocationValid2, mockRideLocationValid3, mockInvalidRideLocation));


			RideEntity result = rideEntityService.enrichRideEntityWithCsv("valid.csv");
            result.setRideIncidents(Collections.singletonList(mockRideIncident));
            result.setRideLocations(List.of(mockRideLocationValid));

			InOrder inOrder = inOrder(csvUtilService);
			inOrder.verify(csvUtilService).parseCsvToModel("lat,lng\n1.0,4.0", RideLocation.class);
			inOrder.verify(csvUtilService).parseCsvToModel("bike,incident\n1,3", RideIncident.class);

			RideIncident resultRideIncident = result.getRideIncidents().get(0);
			assertEquals(Collections.singletonList(ParticipantType.BUS_COACH),
					resultRideIncident.getParticipantsInvolved());
			assertEquals(FALLBACK_DATE, resultRideIncident.getTimeStamp());

			assertEquals(START_OF_RECORDING, result.getRideStart());
			assertEquals(START_OF_RECORDING, result.getRideEnd());
			assertEquals(List.of(mockRideLocationValid, mockRideLocationValid2, mockRideLocationValid3),
					result.getRideLocations());
		}

		@Nested
		class ValidateRideLocation {

			RideLocation mockRideLocation;

			@BeforeEach
			public void setUp() {
				mockRideLocation = new RideLocation();
				mockRideLocation.setLat(1.0);
				mockRideLocation.setLng(2.0);
			}

			@Test
			public void testValidateRideLocation_Valid() {
				mockRideLocation.setTimeStamp(1000);

				boolean result = rideEntityService.validateRideLocation(mockRideLocation);
				assertTrue(result);
			}

			@Test
			public void testValidateRideLocation_Invalid() {
				boolean result = rideEntityService.validateRideLocation(mockRideLocation);

				assertFalse(result);
			}

		}

		@Nested
		class ValidateRideIncident {

			RideIncident mockRideIncident;

			@BeforeEach
			public void setUp() {
				mockRideIncident = new RideIncident();
				mockRideIncident.setLat(1.0);
			}

			@Test
			public void testValidateRideIncident_Valid() {
				mockRideIncident.setLng(1.2);

				boolean result = rideEntityService.validateRideIncident(mockRideIncident);
				assertTrue(result);
			}

			@Test
			public void testValidateRideIncident_Invalid() {
				boolean result = rideEntityService.validateRideIncident(mockRideIncident);

				assertFalse(result);
			}

		}

		@Test
		public void testEnrichRideEntityWithCsv_InvalidFile() {
			when(fileReaderService.readFileFromPath("invalid.csv")).thenReturn("manual1,manual2\nvalue1,value2");

			RideEntity result = rideEntityService.enrichRideEntityWithCsv("invalid.csv");

			assertNull(result);
			verify(csvUtilService, times(0)).parseCsvToModel(any(String.class), eq(RideIncident.class));
		}

	}

	@Nested
	class GenerateNewRideEntity {

		@Test
		public void testGenerateNewRideEntity_Valid() throws Exception {
			RideEntity mockRideEntity = new RideEntity("valid.csv");
			RideLocation mockRideLocation1 = new RideLocation();
			mockRideLocation1.setLat(1.0);
			mockRideLocation1.setLng(2.0);
			List<RideLocation> mockRideLocationList = Collections.singletonList(mockRideLocation1);
			mockRideEntity.setRideLocations(mockRideLocationList);

			doReturn(mockRideEntity).when(rideEntityServiceSpy).enrichRideEntityWithCsv("valid.csv");
			doReturn("[]").when(rideEntityServiceSpy).generateCoordinateString(mockRideLocationList);
			doReturn(mockRideEntity).when(rideEntityServiceSpy).linkToPlanetOsmLine(any(RideEntity.class));

			when(rideEntityRepository.save(any(RideEntity.class))).thenAnswer(i -> i.getArgument(0));

			RideEntity result = rideEntityServiceSpy.generateNewRideEntity("valid.csv");

			assertEquals(mockRideEntity, result);

			verify(rideEntityServiceSpy, times(1)).enrichRideEntityWithCsv("valid.csv");
			verify(rideEntityServiceSpy, times(1)).generateCoordinateString(mockRideLocationList);
			verify(rideEntityServiceSpy, times(1)).linkToPlanetOsmLine(any(RideEntity.class));
			verify(rideEntityRepository, times(1)).save(mockRideEntity);
		}

		@Test
		public void testGenerateNewRideEntity_InvalidCsvFile() {
			RideEntity mockRideEntity = new RideEntity("invalid.csv");
			doThrow(new IllegalArgumentException()).when(rideEntityServiceSpy).enrichRideEntityWithCsv("valid.csv");

			assertThrows(RuntimeException.class, () -> {
				rideEntityServiceSpy.generateNewRideEntity(mockRideEntity.getPath());
			});
		}

		@Test
		public void testGenerateNewRideEntity_JsonProcessingException() throws Exception {
			RideEntity mockRideEntity = new RideEntity("valid.csv");
			doReturn(mockRideEntity).when(rideEntityServiceSpy).enrichRideEntityWithCsv("valid.csv");
			doThrow(new IllegalArgumentException()).when(rideEntityServiceSpy).generateCoordinateString(anyList());

			assertThrows(RuntimeException.class, () -> {
				rideEntityServiceSpy.generateNewRideEntity(mockRideEntity.getPath());
			});
		}

	}

	@Nested
	class CreateGeometryFromRideLocations {

		@Test
		public void testGenerateCoordinateString() throws Exception {
			List<RideLocation> rideLocationList = new ArrayList<>();

			RideLocation mockRideLocation1 = new RideLocation();
			mockRideLocation1.setLat(1.0);
			mockRideLocation1.setLng(2.0);
			rideLocationList.add(mockRideLocation1);

			RideLocation mockRideLocation2 = new RideLocation();
			mockRideLocation2.setLat(3.0);
			mockRideLocation2.setLng(4.0);
			rideLocationList.add(mockRideLocation2);

			String expectedString = "[{\"lng\":2.0,\"lat\":1.0},{\"lng\":4.0,\"lat\":3.0}]";

			String result = rideEntityService.generateCoordinateString(rideLocationList);

			assertEquals(expectedString, result);
		}

	}

	@Nested
	class GetTimeStampFromRideIncident {

		private RideIncident mockRideIncident;

		private List<RideLocation> mockRideLocationList;

		private long[] mockTimestamps;

		private String mockRidePath;

		private long START_OF_RECORDING_TIMESTAMP;

		@BeforeEach
		public void setUp() {
			mockRideIncident = new RideIncident();
			mockRideLocationList = new ArrayList<>();
			mockTimestamps = new long[] {};
			mockRidePath = "valid.csv";
			START_OF_RECORDING_TIMESTAMP = START_OF_RECORDING.getTime();
		}

		Date getExpectedDate(long timestamp) {
			return new Date(START_OF_RECORDING_TIMESTAMP + timestamp);
		}

		@Test
		public void testGetTimeStampFromRideIncident_ValidTs() {
			mockRideIncident.setTs(START_OF_RECORDING_TIMESTAMP + 1000);

			Date result = rideEntityService.getTimeStampFromRideIncident(mockRideIncident, mockRideLocationList,
					mockTimestamps, mockRidePath);

			assertEquals(getExpectedDate(1000), result);
		}

		@Test
		public void testGetTimeStampFromRideIncident_LocationTimeStamp() {
			mockRideIncident.setLat(1.0);
			mockRideIncident.setLng(2.0);

			RideLocation mockRideLocation = new RideLocation();
			mockRideLocation.setLat(1.0);
			mockRideLocation.setLng(2.0);
			mockRideLocation.setTimeStamp(START_OF_RECORDING_TIMESTAMP + 2000);
			mockRideLocationList.add(mockRideLocation);

			Date result = rideEntityService.getTimeStampFromRideIncident(mockRideIncident, mockRideLocationList,
					mockTimestamps, mockRidePath);

			assertEquals(getExpectedDate(2000), result);
		}

		@Test
		public void testGetTimeStampFromRideIncident_RidePath() {
			mockTimestamps = new long[] { START_OF_RECORDING_TIMESTAMP + 1000, START_OF_RECORDING_TIMESTAMP + 10000 };

			Date result = rideEntityService.getTimeStampFromRideIncident(mockRideIncident, mockRideLocationList,
					mockTimestamps, mockRidePath);

			assertEquals(getExpectedDate(5500), result);
		}

		@Test
		public void testGetTimeStampFromRideIncident_LastModified() {
			when(fileReaderService.getFileLastModified(mockRidePath)).thenReturn(getExpectedDate(5000));

			Date result = rideEntityService.getTimeStampFromRideIncident(mockRideIncident, mockRideLocationList,
					mockTimestamps, mockRidePath);

			assertEquals(getExpectedDate(5000), result);
		}

		@Test
		public void testGetTimeStampFromRideIncident_Fallback() {
			Date result = rideEntityService.getTimeStampFromRideIncident(mockRideIncident, mockRideLocationList,
					mockTimestamps, mockRidePath);

			assertEquals(FALLBACK_DATE, result);
		}

	}

}
