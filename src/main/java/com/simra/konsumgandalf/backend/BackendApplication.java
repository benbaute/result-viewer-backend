package com.simra.konsumgandalf.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = { "com.simra.konsumgandalf.backend.config",
		"com.simra.konsumgandalf.common.utils.services", "com.simra.konsumgandalf.valhalla.services",
		"com.simra.konsumgandalf.osmPlanet.services", "com.simra.konsumgandalf.osmPlanet.controller",
		"com.simra.konsumgandalf.rides.services", "com.simra.konsumgandalf.common.logging",
		"com.simra.konsumgandalf.rides.controllers", "com.simra.konsumgandalf.common.services",
		"com.simra.konsumgandalf.common.controller", "com.simra.konsumgandalf.backend.services" })
@EnableJpaRepositories(
		basePackages = { "com.simra.konsumgandalf.common.repositories", "com.simra.konsumgandalf.rides.repositories",
				"com.simra.konsumgandalf.osmPlanet.repositories" })
@EntityScan(basePackages = { "com.simra.konsumgandalf.common.models.entities.osm",
		"com.simra.konsumgandalf.common.models.entities", "com.simra.konsumgandalf.rides.models.entities" })
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}

}
