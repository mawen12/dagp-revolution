package com.mawen.search.core.convert.core.domain;

import java.util.Collections;
import java.util.List;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.builder.FetchSourceFilterBuilder;
import com.mawen.search.core.support.MultiGetItem;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;

import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
abstract class SourceFilterIntegrationTests {

	@Autowired
	private ElasticsearchOperations operations;

	@BeforeEach
	void setUp() {

		Entity entity = new Entity();
		entity.setId("42");
		entity.setField1("one");
		entity.setField2("two");
		entity.setField3("three");
		operations.save(entity);
	}

	@Test
	@DisplayName("should only return requested fields on multiget")
	void shouldOnlyReturnRequestedFieldsOnMultiGet() {

		Query query = Query.multiGetQuery(Collections.singleton("42"));
		query.addSourceFilter(new FetchSourceFilterBuilder().withIncludes("field2").build());

		List<MultiGetItem<Entity>> entities = operations.multiGet(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.get(0).getItem();
		assertThat(entity.getField1()).isNull();
		assertThat(entity.getField2()).isEqualTo("two");
		assertThat(entity.getField3()).isNull();
	}

	@Test
	@DisplayName("should not return excluded fields from SourceFilter on search")
	void shouldNotReturnExcludedFieldsFromSourceFilterOnSearch() {
		Query query = Query.findAll();
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] {};
			}

			@Override
			public String[] getExcludes() {
				return new String[] { "field2" };
			}
		});

		SearchHits<Entity> entities = operations.search(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.getSearchHit(0).getContent();
		assertThat(entity.getField1()).isNotNull();
		assertThat(entity.getField2()).isNull();
		assertThat(entity.getField3()).isNotNull();
	}

	@Test // #1659, #1678
	@DisplayName("should not return excluded fields from SourceFilter on multiget")
	void shouldNotReturnExcludedFieldsFromSourceFilterOnMultiGet() {

		Query query = Query.multiGetQuery(Collections.singleton("42"));
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] {};
			}

			@Override
			public String[] getExcludes() {
				return new String[] { "field2" };
			}
		});

		List<MultiGetItem<Entity>> entities = operations.multiGet(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.get(0).getItem();
		assertThat(entity.getField1()).isNotNull();
		assertThat(entity.getField2()).isNull();
		assertThat(entity.getField3()).isNotNull();
	}

	@Test // #1659
	@DisplayName("should only return included fields from SourceFilter on search")
	void shouldOnlyReturnIncludedFieldsFromSourceFilterOnSearch() {

		Query query = Query.findAll();
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] { "field2" };
			}

			@Override
			public String[] getExcludes() {
				return new String[] {};
			}
		});

		SearchHits<Entity> entities = operations.search(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.getSearchHit(0).getContent();
		assertThat(entity.getField1()).isNull();
		assertThat(entity.getField2()).isNotNull();
		assertThat(entity.getField3()).isNull();
	}

	@Test // #1659, #1678
	@DisplayName("should only return included fields from SourceFilter on multiget")
	void shouldOnlyReturnIncludedFieldsFromSourceFilterOnMultiGet() {

		Query query = Query.multiGetQuery(Collections.singleton("42"));
		query.addSourceFilter(new SourceFilter() {
			@Override
			public String[] getIncludes() {
				return new String[] { "field2" };
			}

			@Override
			public String[] getExcludes() {
				return new String[] {};
			}
		});

		List<MultiGetItem<Entity>> entities = operations.multiGet(query, Entity.class);

		assertThat(entities).hasSize(1);
		Entity entity = entities.get(0).getItem();
		assertThat(entity.getField1()).isNull();
		assertThat(entity.getField2()).isNotNull();
		assertThat(entity.getField3()).isNull();
	}

	@Document(indexName = "entity")
	@Data
	public static class Entity {

		@Id
		private String id;
		@com.mawen.search.core.annotation.Field(type = FieldType.Text)
		private String field1;
		@com.mawen.search.core.annotation.Field(type = FieldType.Text)
		private String field2;
		@com.mawen.search.core.annotation.Field(type = FieldType.Text)
		private String field3;

	}
}