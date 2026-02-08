package com.simra.konsumgandalf.valhalla.services;

import com.google.common.collect.Lists;
import com.simra.konsumgandalf.common.logging.LogExecutionTimeSubTask;
import com.simra.konsumgandalf.common.models.classes.MatchInformation;
import com.simra.konsumgandalf.valhalla.models.ValhallaEdge;
import com.simra.konsumgandalf.valhalla.models.ValhallaTraceAttributesResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This service is dedicated to deal with the endpoint /trace_attributes, which is used to
 * match street segments
 */
@Service
public class ValhallaTraceAttributesService extends ValhallaService {

	private static final Map<String, Object> BASE_PAYLOAD = Map.of("costing", "bicycle", "shape_match", "map_snap",
			"snap_prevention", List.of("motorway", "trunk"), "filters",
			Map.of("attributes", List.of("edge.way_id"), "action", "include"));

	private final int TURN_PENALTY_FACTOR;

	public ValhallaTraceAttributesService(@Value("${VALHALLA_BACKEND_URL}") String osmrBackendUrl,
			@Value("${VALHALLA_TURN_PENALTY_FACTOR}") int turnPenaltyFactor) {
		super(osmrBackendUrl + "/trace_attributes", 1024, 10 * 1024 * 1024);
		TURN_PENALTY_FACTOR = turnPenaltyFactor;
	}

    @LogExecutionTimeSubTask
	public List<Long> calculateStreetSegmentIdsOfRoute(List<MatchInformation> coordinates) {
		List<List<MatchInformation>> partitions = Lists.partition(coordinates, DEFAULT_PARTITION_SIZE);
        List<Long> results = new ArrayList<>();

        for (List<MatchInformation> chunk : partitions) {
            results.addAll(fetchWithRetry(chunk));
        }
        return results;
	}

	public List<Long> fetchWithRetry(List<MatchInformation> chunk) {
        List<Long> results = new ArrayList<>();
        try {
            if (chunk.size() >= 4) {
                results.addAll(fetchIdsFromChunk(chunk));
            }
            return results;
        } catch (WebClientResponseException ex) {
            if (isInsufficientShapeError(ex)) {
                logger.warn("Insufficient shape, chunk size: {}", chunk.size());
                return results;
            }
            if (!isNotFoundStreetSegmentError(ex)) {
                logger.error("Unexpected error: {} - HTTP Status: {}", ex.getResponseBodyAsString(), ex.getStatusCode());
                return results;
            }
            if (chunk.size() >= 32) {
                List<List<MatchInformation>> subPartitions = Lists.partition(chunk, chunk.size() / 2);
                List<List<Long>> subResults = subPartitions.stream().map(this::fetchWithRetry).toList();
                for (List<Long> subResult : subResults) {
                    results.addAll(subResult);
                }
                return results;
            }
            return results;
        }
	}

	public List<Long> fetchIdsFromChunk(List<MatchInformation> coordinates) {
		Map<String, Object> payload = new HashMap<>(BASE_PAYLOAD);
		payload.put("shape", coordinates);
		payload.put("begin_time", coordinates.getFirst().getTimestamp());
		payload.put("use_timestamps", true);
		payload.put("trace_options", Map.of("turn_penalty_factor", TURN_PENALTY_FACTOR));

		return webClient.post()
			.bodyValue(payload)
			.retrieve()
			.bodyToMono(ValhallaTraceAttributesResponse.class)
			.flatMapMany(response -> Flux.fromIterable(response.getEdges()))
			.map(ValhallaEdge::getId)
			.distinct()
			.collectList()
            .block();
	}
}
