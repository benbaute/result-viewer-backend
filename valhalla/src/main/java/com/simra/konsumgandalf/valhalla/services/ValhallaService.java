package com.simra.konsumgandalf.valhalla.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

/**
 * This service interacts with the Valhalla routing engine for finding the matching route
 * for a ride.
 */
public abstract class ValhallaService {

	protected final WebClient webClient;

	protected final Logger logger;

	protected final int DEFAULT_PARTITION_SIZE;

	ValhallaService(String osmrEndpoint, int partitionSize, int maxInMemorySize) {
		ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
			.maxConnections(24)
			.disposeTimeout(Duration.ofMinutes(2))
			.build();

		webClient = WebClient.builder()
			.baseUrl(osmrEndpoint)
			.clientConnector(new ReactorClientHttpConnector(
					HttpClient.create(connectionProvider).responseTimeout(Duration.ofSeconds(60))))
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(maxInMemorySize))
			.build();

		DEFAULT_PARTITION_SIZE = partitionSize;

		logger = LoggerFactory.getLogger(this.getClass());
	}

	public ValhallaService(String osmrEndpoint, int partitionSize) {
		this(osmrEndpoint, partitionSize, 10 * 1024 * 1024);
	}

    private Integer getErrorCode(WebClientResponseException ex) {
        try {
            if (ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                return new ObjectMapper().readTree(ex.getResponseBodyAsString()).path("error_code").asInt();
            }
            return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean isNotFoundStreetSegmentError(WebClientResponseException ex) {
        Integer errorCode = getErrorCode(ex);
        if (errorCode == null) {
            return false;
        }
        return errorCode == 430 || errorCode == 441 || errorCode == 442 || errorCode == 443 || errorCode == 444;
    }

    protected boolean isInsufficientShapeError(WebClientResponseException ex) {
        Integer errorCode = getErrorCode(ex);
        if (errorCode == null) {
            return false;
        }
        return errorCode == 123;
    }
}
