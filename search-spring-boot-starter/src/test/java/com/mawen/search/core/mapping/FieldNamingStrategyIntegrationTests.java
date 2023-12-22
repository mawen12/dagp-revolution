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

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.query.Query;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class FieldNamingStrategyIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
	}
	@Test // #1565
	@DisplayName("should use configured FieldNameStrategy")
	void shouldUseConfiguredFieldNameStrategy() {

		Entity entity = new Entity();
		entity.setId("42");
		entity.setSomeText("the text to be searched");
		operations.save(entity);

		// use a native query here to prevent automatic property name matching
		Query query = nativeMatchQuery("some_text", "searched");
		SearchHits<Entity> searchHits = operations.search(query, Entity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
	}

	protected abstract Query nativeMatchQuery(String fieldName, String value);

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class Entity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(type = FieldType.Text) private String someText;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getSomeText() {
			return someText;
		}

		public void setSomeText(@Nullable String someText) {
			this.someText = someText;
		}
	}
}
