package com.simra.konsumgandalf.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.simra.konsumgandalf.common.models.serializer.GeometrySerializer;
import org.locationtech.jts.geom.Geometry;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer addCustomGeometrySerializer() {
		return builder -> builder.serializerByType(Geometry.class, new GeometrySerializer());
	}

}
