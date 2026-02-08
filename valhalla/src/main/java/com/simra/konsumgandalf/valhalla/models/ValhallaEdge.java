package com.simra.konsumgandalf.valhalla.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValhallaEdge {

	@JsonProperty("way_id")
	Long id;
}
