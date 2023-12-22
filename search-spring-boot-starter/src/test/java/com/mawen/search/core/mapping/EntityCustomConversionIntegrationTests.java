/*
 * Copyright 2021-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mawen.search.core.mapping;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.query.Query;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

/**
 * Test that a whole entity can be converted using custom conversions
 *
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class EntityCustomConversionIntegrationTests {

	@Configuration
	@EnableElasticsearchRepositories(basePackages = { "org.springframework.data.elasticsearch.core.mapping" },
			considerNestedRepositories = true)
	static class Config {}

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
	}

	@Test // #1667
	@DisplayName("should use CustomConversions on entity")
	void shouldUseCustomConversionsOnEntity() {

		Entity entity = new Entity();
		entity.setValue("hello"); //

		Document document = Document
				.create();
		operations.getElasticsearchConverter().write(entity, document);

		assertThat(document.getString("the_value")).isEqualTo("hello");
		assertThat(document.getString("the_lon")).isEqualTo("8.0");
		assertThat(document.getString("the_lat")).isEqualTo("42.7");
	}

	@Test // #1667
	@DisplayName("should store and load entity from Elasticsearch")
	void shouldStoreAndLoadEntityFromElasticsearch() {

		Entity entity = new Entity();
		entity.setValue("hello"); //

		Entity savedEntity = operations.save(entity);

		SearchHits<Entity> searchHits = operations.search(Query.findAll(), Entity.class);
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		Entity foundEntity = searchHits.getSearchHit(0).getContent();
		assertThat(foundEntity).isEqualTo(entity);
	}

	@com.mawen.search.core.annotation.Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Nullable private String value;

		@Nullable
		public String getValue() {
			return value;
		}

		public void setValue(@Nullable String value) {
			this.value = value;
		}


		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Entity))
				return false;

			return Objects.equals(value, ((Entity) o).value);
		}

		@Override
		public int hashCode() {
			return value != null ? value.hashCode() : 0;
		}
	}

	@WritingConverter
	static class EntityToMapConverter implements Converter<Entity, Map<String, Object>> {
		@Override
		public Map<String, Object> convert(Entity source) {
			LinkedHashMap<String, Object> target = new LinkedHashMap<>();
			target.put("the_value", source.getValue());
			return target;
		}
	}

	@ReadingConverter
	static class MapToEntityConverter implements Converter<Map<String, Object>, Entity> {

		@Override
		public Entity convert(Map<String, Object> source) {
			Entity entity = new Entity();
			entity.setValue((String) source.get("the_value"));
			return entity;
		}
	}
}
