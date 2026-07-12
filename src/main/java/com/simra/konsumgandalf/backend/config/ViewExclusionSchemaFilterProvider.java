package com.simra.konsumgandalf.backend.config;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.boot.model.relational.Sequence;
import org.hibernate.mapping.Table;
import org.hibernate.tool.schema.spi.SchemaFilter;
import org.hibernate.tool.schema.spi.SchemaFilterProvider;

public class ViewExclusionSchemaFilterProvider implements SchemaFilterProvider {

	@Override
	public SchemaFilter getCreateFilter() {
		return ViewFilter.INSTANCE;
	}

	@Override
	public SchemaFilter getDropFilter() {
		return ViewFilter.INSTANCE;
	}

	@Override
	public SchemaFilter getMigrateFilter() {
		return ViewFilter.INSTANCE;
	}

	@Override
	public SchemaFilter getValidateFilter() {
		return ViewFilter.INSTANCE;
	}

	@Override
	public SchemaFilter getTruncatorFilter() {
		return ViewFilter.INSTANCE;
	}

	private static class ViewFilter implements SchemaFilter {

		private static final ViewFilter INSTANCE = new ViewFilter();

		@Override
		public boolean includeNamespace(Namespace namespace) {
			return true;
		}

		@Override
		public boolean includeSequence(Sequence sequence) {
			return true;
		}

		@Override
		public boolean includeTable(Table table) {
			// This file is only used to skip DDL execution for materialized views
			return !table.getName().equalsIgnoreCase("intersection_edge_metrics")
					&& !table.getName().equalsIgnoreCase("intersection_node_metrics")
					&& !table.getName().equalsIgnoreCase("safety_metrics__simra_region")
					&& !table.getName().equalsIgnoreCase("safety_metrics__region")
					&& !table.getName().equalsIgnoreCase("safety_metrics__planet_osm_line")
					&& !table.getName().equalsIgnoreCase("intersection_ride_region_metrics")
					&& !table.getName().equalsIgnoreCase("intersection_region_metrics");
		}

	}

}
