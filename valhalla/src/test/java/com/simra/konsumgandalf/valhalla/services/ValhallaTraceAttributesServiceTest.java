package com.simra.konsumgandalf.valhalla.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;

@ExtendWith(MockitoExtension.class)
public class ValhallaTraceAttributesServiceTest {

	ValhallaTraceAttributesService serviceSpy;

	@InjectMocks
	private ValhallaTraceAttributesService service = new ValhallaTraceAttributesService("http://localhost:8080", 10000);

	@BeforeEach
	public void setUp() {
		serviceSpy = spy(service);
	}

	@Nested
	class TestFetchStepsFromChunk {

		private MockWebServer mockWebServer;

		private ValhallaTraceAttributesService service;

		private final ObjectMapper objectMapper = new ObjectMapper();

		@BeforeEach
		public void setUp() throws IOException {
			mockWebServer = new MockWebServer();
			mockWebServer.start();
			String baseUrl = mockWebServer.url("/").toString();
			service = new ValhallaTraceAttributesService(baseUrl, 100000);
		}

		@AfterEach
		public void tearDown() throws IOException {
			mockWebServer.shutdown();
		}

		@Test
		public void testFetchStepsFromChunk_ChunkSizing() throws InterruptedException, JsonProcessingException {
			ArrayList<MatchInformation> chunk = new ArrayList<>();
			chunk.add(new MatchInformation(52.520007, 13.404954, 1693842834));
			chunk.add(new MatchInformation(42.520007, 23.404954, 1693842835));

			String jsonResponse = "{ \"edges\": [ { \"way_id\": 1 }, { \"way_id\": 2 } ] }";
			mockWebServer
				.enqueue(new MockResponse().setBody(jsonResponse).addHeader("Content-Type", "application/json"));

			List<Long> ids = service.fetchIdsFromChunk(chunk);

			RecordedRequest request = mockWebServer.takeRequest();
			assertEquals("/trace_attributes", request.getPath());
			assertEquals("POST", request.getMethod());

			JsonNode expectedNode = objectMapper.readTree(
					"{\"shape_match\":\"map_snap\",\"shape\":[{\"lat\":13.404954,\"lon\":52.520007,\"time\":1693842834},{\"lat\":23.404954,\"lon\":42.520007,\"time\":1693842835}],\"costing\":\"bicycle\",\"begin_time\":1693842834,\"filters\":{\"action\":\"include\",\"attributes\":[\"edge.way_id\"]},\"trace_options\":{\"turn_penalty_factor\":100000},\"use_timestamps\":true,\"snap_prevention\":[\"motorway\",\"trunk\"]}");
			JsonNode actualNode = objectMapper.readTree(request.getBody().readUtf8());
			assertEquals(expectedNode, actualNode);
			assertEquals(List.of(1L, 2L), ids);
		}

	}

	@Nested
	class TestIsNotFoundStreetSegmentError {

		@Test
		void isTrue_NoSuitableEdges() {
			WebClientResponseException ex = WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(),
					"Bad Request", null, "No suitable edges near location".getBytes(), null);

			boolean result = service.isNotFoundStreetSegmentError(ex);
			assertTrue(result);
		}

		@Test
		void isTrue_MapMatchAlgorithm() {
			WebClientResponseException ex = WebClientResponseException.create(HttpStatus.BAD_REQUEST.value(),
					"Bad Request", null,
					"Map Match algorithm failed to find path: map_snap algorithm failed to snap the shape points to the correct shape."
						.getBytes(),
					null);

			boolean result = service.isNotFoundStreetSegmentError(ex);
			assertTrue(result);
		}

	}

}
