package com.simra.konsumgandalf.valhalla.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.models.classes.MatchInformationDate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service is dedicated to deal with the endpoint /trace_attributes, which is used to
 * match street segments
 */
@Service
public class ValhallaMapMatchingService extends ValhallaService {

	private static final Map<String, Object> BASE_PAYLOAD = Map.of(
        "costing", "bicycle",
        "shape_match", "map_snap",
        "snap_prevention", List.of("motorway", "trunk"),
        "trace_options", Map.of(
            "turn_penalty_factor", 300,
            "search_radius", 15,
            "breakage_distance", 50,
            "interpolation_distance", 10
        )
    );

	public ValhallaMapMatchingService(@Value("${VALHALLA_BACKEND_URL}") String valhallaBackendUrl) {
		super(valhallaBackendUrl, 1024);
	}

	public Map<String, Object> getTraceAttributes(List<MatchInformationDate> coordinates) {
		List<List<MatchInformationDate>> partitions = partitionList(coordinates, DEFAULT_PARTITION_SIZE);
		List<Map<String, Object>> results = new ArrayList<>();

		for (List<MatchInformationDate> chunk : partitions) {
			results.addAll(fetchWithRetry(chunk));
		}
		return combineChunks(results);
	}

	private List<Map<String, Object>> fetchWithRetry(List<MatchInformationDate> chunk) {
        List<Map<String, Object>> results = new ArrayList<>();
		try {
            results.add(doRequest(chunk));
			return results;
		}
		catch (WebClientResponseException ex) {
			if (chunk.size() > 4) {
				List<List<MatchInformationDate>> halves = partitionList(chunk, chunk.size() / 2);
				List<List<Map<String, Object>>> subResults = halves.stream().map(this::fetchWithRetry).toList();
                for (List<Map<String, Object>> subResult : subResults) {
                    results.addAll(subResult);
                }
				return results;
			}
            results.add(getErrorMap("Failed Request, but too few to chunk.", chunk.size()));
			return results;
		}
	}

	private Map<String, Object> getErrorMap(String error, int numberOfUnmatchedPoints) {
		Map<String, Object> response = new HashMap<>();
		response.put("error", error);
		List<Object> unmatchedPoints = new ArrayList<>();
		for (int i = 0; i < numberOfUnmatchedPoints; i++) {
			Map<String, Object> unmatched = new HashMap<>();
			unmatched.put("error", error);
			unmatchedPoints.add(unmatched);
		}
		response.put("matched_points", unmatchedPoints);
		return response;
	}

	private Map<String, Object> doRequest(List<MatchInformationDate> coordinates) {
		Map<String, Object> payload = new HashMap<>(BASE_PAYLOAD);
		payload.put("shape", coordinates);
		payload.put("begin_time", coordinates.get(0).getValhallaTimestamp());
		payload.put("use_timestamps", true);

		try {
			ResponseEntity<String> entity = webClient.post()
				.uri("/trace_attributes")
				.bodyValue(payload)
				.retrieve()
				.toEntity(String.class)
				.block();
			if (entity == null) {
				logger.error("Error while fetching trace attributes, no response (entity is null)");
				return getErrorMap("No response (entity is null)", coordinates.size());
			}
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(entity.getBody(), new TypeReference<>() {
			});
		}
		catch (WebClientResponseException ex) {
			String body = ex.getResponseBodyAsString();
			if (isNotFoundStreetSegmentError(ex, body)) {
				throw ex;
			}
			logger.error("Unexpected WebclientError {}", body);
			return getErrorMap("Unexpected WebclientError", coordinates.size());
		}
		catch (Exception e) {
			logger.error("Unexpected Exception while fetching trace data.", e);
			return getErrorMap("Unexpected Exception while fetching trace data.", coordinates.size());
		}
	}

	private <T> List<List<T>> partitionList(List<T> list, int size) {
		List<List<T>> parts = new ArrayList<>();
		for (int i = 0; i < list.size(); i += size) {
			parts.add(list.subList(i, Math.min(i + size, list.size())));
		}
		return parts;
	}

    /**
     * This function stitches together individual chunks.
     * Each attribute with global scope in edges and matched_points are only for a specific chunk,
     * unless it is specifically fixed in here.
     */
	private Map<String, Object> combineChunks(List<Map<String, Object>> chunkedResponses) {
		Map<String, Object> combined = new HashMap<>();

        int chunkId = 0;
        int edgeId = 0;
		List<Object> allEdges = new ArrayList<>();
        List<Object> allMatchedPoints = new ArrayList<>();
		for (Map<String, Object> resp : chunkedResponses) {
			Object edgesObject = resp.get("edges");
            Object matchedPointsObject = resp.get("matched_points");
            int edgeListSize = 0;
            if (edgesObject instanceof List<?>) {
                ArrayList<HashMap<String, Object>> edgesList = (ArrayList<HashMap<String, Object>>) edgesObject;
                for (HashMap<String, Object> edge : edgesList) {
                    edge.put("chunk_id", chunkId);
                }
                allEdges.addAll((List<?>) edgesObject);
                edgeListSize = edgesList.size();
            }
            if (matchedPointsObject instanceof List<?>) {
                ArrayList<HashMap<String, Object>> matchedPointsList = (ArrayList<HashMap<String, Object>>) matchedPointsObject;
                for (HashMap<String, Object> point : matchedPointsList) {
                    point.put("chunk_id", chunkId);
                    Number edgeIndexNumber = (Number) point.get("edge_index");
                    if (edgeIndexNumber != null) {
                        int edgeIndex = edgeIndexNumber.intValue();
                        if (edgeIndex >= 0 && edgeIndex < edgeId + edgeListSize) {
                            point.put("edge_index", edgeId + edgeIndex);
                        } else {
                            point.remove("edge_index");
                        }
                    }
                }
                allMatchedPoints.addAll((List<?>) matchedPointsObject);
            }
			edgeId += edgeListSize;
            chunkId++;
		}
		combined.put("edges", allEdges);
		combined.put("matched_points", allMatchedPoints);

		return combined;
	}

	private boolean isNotFoundStreetSegmentError(WebClientResponseException ex, String body) {
		return ex.getStatusCode() == HttpStatus.BAD_REQUEST
				&& (body.contains("failed to snap the shape points to the correct shape")
						|| body.contains("Insufficient shape provided"));
	}

}