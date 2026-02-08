package com.simra.konsumgandalf.osmPlanet.classes.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimraRegionMapper {

	public final Map<String, List<String>> map = new HashMap<>();

	public SimraRegionMapper() {
		map.put("Berlin-Potsdam", List.of("Berlin", "Potsdam", "Havelland", "Potsdam-Mittelmark", "Teltow-Fläming",
				"Barnim", "Oberhavel", "Märkisch-Oderland", "Oder-Spree", "Dahme-Spreewald"));
		map.put("London", List.of("London"));
		map.put("Bern", List.of("Bern"));
		map.put("Pforzheim-Enzkreis", List.of("Pforzheim", "Enzkreis"));
		map.put("Augsburg", List.of("Augsburg"));
		map.put("Ruhr-Region",
				List.of("Ruhrgebiet", "Essen", "Dortmund", "Bochum", "Duisburg", "Gelsenkirchen", "Oberhausen",
						"Bottrop", "Mülheim an der Ruhr", "Recklinghausen", "Herne", "Hagen", "Witten", "Iserlohn",
						"Castrop-Rauxel", "Lünen", "Unna", "Schwerte", "Wetter", "Herdecke", "Hattingen",
						"Sprockhövel"));
		map.put("Stuttgart", List.of("Stuttgart", "Ludwigsburg", "Rems-Murr-Kreis", "Esslingen", "Böblingen"));
		map.put("Leipzig", List.of("Leipzig", "Landkreis Leipzig", "Nordsachsen"));
		map.put("Wuppertal-Solingen-Remscheid", List.of("Wuppertal", "Solingen", "Remscheid"));
		map.put("Düsseldorf", List.of("Düsseldorf"));
		map.put("Eichwalde-Zeuthen-Schulzendorf", List.of("Eichwalde", "Zeuthen", "Schulzendorf"));
		map.put("Hannover", List.of("Region Hannover"));
		map.put("Bielefeld", List.of("Bielefeld"));
		map.put("München", List.of("München", "Landkreis München", "Starnberg", "Fürstenfeldbruck", "Dachau"));
		map.put("ZES-Experimental", List.of(""));
		map.put("Konstanz", List.of("Konstanz"));
		map.put("Weimar", List.of("Weimar"));
		map.put("Karlsruhe", List.of("Karlsruhe", "Landkreis Karlsruhe"));
	}

	public List<String> getRegionsForSimraRegion(String simraRegion) {
		return map.getOrDefault(simraRegion, List.of());
	}

}
