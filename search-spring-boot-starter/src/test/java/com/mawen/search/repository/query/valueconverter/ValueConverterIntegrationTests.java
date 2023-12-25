/*
 * Copyright 2022-2023 the original author or authors.
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
package com.mawen.search.repository.query.valueconverter;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.annotation.Query;
import com.mawen.search.core.annotation.ValueConverter;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.mapping.PropertyValueConverter;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.repository.ElasticsearchRepository;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;
import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
abstract class ValueConverterIntegrationTests {

	@Autowired private EntityRepository repository;
	@Autowired
	ElasticsearchOperations operations;
	@Autowired
	IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
	}

	@Test // #2338
	@DisplayName("should apply ValueConverter")
	void shouldApplyValueConverter() {

		Entity entity = new Entity();
		entity.setId("42");
		entity.setText("answer");
		operations.save(entity);

		SearchHits<Entity> searchHits = repository.queryByText("text-answer");
		assertThat(searchHits.getTotalHits()).isEqualTo(1);

		searchHits = repository.findByText("answer");
		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	interface EntityRepository extends ElasticsearchRepository<Entity, String> {
		SearchHits<Entity> findByText(String text);

		@Query("{ \"term\": { \"text\": \"?0\" } }")
		SearchHits<Entity> queryByText(String text);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Id
		@Nullable private String id;

		@Field(type = FieldType.Keyword)
		@ValueConverter(TextConverter.class)
		@Nullable private String text;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getText() {
			return text;
		}

		public void setText(@Nullable String text) {
			this.text = text;
		}
	}

	static class TextConverter implements PropertyValueConverter {

		public static final String PREFIX = "text-";

		@Override
		public Object write(Object value) {
			return PREFIX + value.toString();
		}

		@Override
		public Object read(Object value) {

			String valueString = value.toString();

			if (valueString.startsWith(PREFIX)) {
				return valueString.substring(PREFIX.length());
			} else {
				return value;
			}
		}
	}
}
