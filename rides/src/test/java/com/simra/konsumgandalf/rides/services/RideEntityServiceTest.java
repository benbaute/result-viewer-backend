package com.simra.konsumgandalf.rides.services;

import com.simra.konsumgandalf.common.models.classes.RideLocation;
import com.simra.konsumgandalf.common.models.entities.RideEntity;
import com.simra.konsumgandalf.common.models.entities.RideIncident;
import com.simra.konsumgandalf.common.repositories.PlanetOsmLineRepository;
import com.simra.konsumgandalf.common.utils.services.CsvUtilService;
import com.simra.konsumgandalf.common.utils.services.FileReaderService;
import com.simra.konsumgandalf.rides.repositories.RideEntityRepository;
import com.simra.konsumgandalf.valhalla.services.ValhallaTraceAttributesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static com.simra.konsumgandalf.common.constants.AppDates.START_OF_RECORDING;
import static org.mockito.Mockito.spy;

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



		@Nested
		class ValidateRideLocation {

			RideLocation mockRideLocation;

			@BeforeEach
			public void setUp() {
				mockRideLocation = new RideLocation();
				mockRideLocation.setLat(1.0);
				mockRideLocation.setLng(2.0);
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



		}



	}

	@Nested
	class GenerateNewRideEntity {



	}

	@Nested
	class CreateGeometryFromRideLocations {



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
	}

}
