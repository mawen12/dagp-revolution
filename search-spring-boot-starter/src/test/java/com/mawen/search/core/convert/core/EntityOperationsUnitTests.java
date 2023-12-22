package com.mawen.search.core.convert.core;

import java.util.Arrays;
import java.util.HashSet;

import com.mawen.search.core.EntityOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Routing;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.routing.DefaultRoutingResolver;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

class EntityOperationsUnitTests {

	@Nullable
	private static ConversionService conversionService;
	@Nullable private static EntityOperations entityOperations;
	@Nullable private static SimpleElasticsearchMappingContext mappingContext;

	@BeforeAll
	static void setUpAll() {
		mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(new HashSet<>(Arrays.asList(EntityWithRouting.class)));
		mappingContext.afterPropertiesSet();
		entityOperations = new EntityOperations(mappingContext);

		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext,
				new GenericConversionService());
		converter.afterPropertiesSet();

		conversionService = converter.getConversionService();
	}

	@Test // #1218
	@DisplayName("should return routing from DefaultRoutingAccessor")
	void shouldReturnRoutingFromDefaultRoutingAccessor() {

		EntityWithRouting entity = new EntityWithRouting();
		entity.setId("42");
		entity.setRouting("theRoute");
		EntityOperations.AdaptableEntity<EntityWithRouting> adaptableEntity = entityOperations.forEntity(entity,
				conversionService, new DefaultRoutingResolver(mappingContext));

		String routing = adaptableEntity.getRouting();

		assertThat(routing).isEqualTo("theRoute");
	}


	@Document(indexName = "entity-operations-test")
	@Routing("routing")
	static class EntityWithRouting {
		@Nullable
		@Id
		private String id;
		@Nullable private String routing;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getRouting() {
			return routing;
		}

		public void setRouting(@Nullable String routing) {
			this.routing = routing;
		}
	}
}