package com.simra.konsumgandalf.valhalla.services;

import com.google.common.collect.Lists;
import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.valhalla.models.TraceResponse;
import com.simra.konsumgandalf.valhalla.models.ValhallaEdge;
import com.simra.konsumgandalf.valhalla.models.ValhallaMatchedPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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
            "search_radius", 25,
            "breakage_distance", 50,
            "interpolation_distance", 10
        )
    );

	public ValhallaMapMatchingService(@Value("${VALHALLA_BACKEND_URL}") String valhallaBackendUrl) {
		super(valhallaBackendUrl, 1024);
	}

    @LogExecutionTimeSubTask
	public TraceResponse getTraceAttributes(List<MatchInformation> coordinates) {
		List<List<MatchInformation>> partitions = Lists.partition(coordinates, DEFAULT_PARTITION_SIZE);
		List<TraceResponse> results = new ArrayList<>();

		for (List<MatchInformation> chunk : partitions) {
			results.addAll(fetchWithRetry(chunk));
		}
		return combineChunks(results);
	}

	private List<TraceResponse> fetchWithRetry(List<MatchInformation> chunk) {
        List<TraceResponse> results = new ArrayList<>();
		try {
            if (chunk.size() >= 4) {
                results.add(doRequest(chunk));
            }
			return results;
		}
		catch (WebClientResponseException ex) {
            if (isInsufficientShapeError(ex)) {
                logger.warn("Insufficient shape, chunk size: {}", chunk.size());
                return results;
            }
            if (!isNotFoundStreetSegmentError(ex)) {
                logger.error("Unexpected error: {} - HTTP Status: {}", ex.getResponseBodyAsString(), ex.getStatusCode());
                return results;
            }
			if (chunk.size() >= 32) {
				List<List<MatchInformation>> halves = Lists.partition(chunk, chunk.size() / 2);
				List<List<TraceResponse>> subResults = halves.stream().map(this::fetchWithRetry).toList();
                for (List<TraceResponse> subResult : subResults) {
                    results.addAll(subResult);
                }
			}
			return results;
		}
        catch (Exception ex) {
            logger.error("Unexpected exception: {}", ex.getMessage());
            return results;
        }
	}

	private TraceResponse doRequest(List<MatchInformation> coordinates) {
		Map<String, Object> payload = new HashMap<>(BASE_PAYLOAD);
		payload.put("shape", coordinates);
		payload.put("begin_time", coordinates.getFirst().getTimestamp());
		payload.put("use_timestamps", true);

        TraceResponse traceResponse = webClient.post()
            .uri("/trace_attributes")
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(TraceResponse.class)
            .block();

        if (traceResponse != null) {
            traceResponse.setPayloadCoordinates(coordinates);
        }
        return traceResponse;
	}

    /**
     * This function stitches together individual chunks.
     * Each attribute with global scope in edges and matched_points are only for a specific chunk,
     * unless it is specifically fixed in here.
     */
	private TraceResponse combineChunks(List<TraceResponse> chunkedResponses) {
		List<ValhallaEdge> edges = new ArrayList<>();
        List<ValhallaMatchedPoint> matchedPoints = new ArrayList<>();
        List<MatchInformation> coordinates = new ArrayList<>();

        int edgeOffset = 0;

        for (TraceResponse chunk : chunkedResponses) {
            edges.addAll(chunk.getEdges());
            for (ValhallaMatchedPoint p : chunk.getMatchedPoints()) {
                matchedPoints.add(new ValhallaMatchedPoint(p, edgeOffset));
            }
            coordinates.addAll(chunk.getPayloadCoordinates());
            edgeOffset += chunk.getEdges().size();
        }

		return new TraceResponse(edges, matchedPoints, coordinates);
	}
}