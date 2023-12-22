/*
 * Copyright 2023 the original author or authors.
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
package com.mawen.search.core.query;

import com.mawen.search.client.query.NativeQuery;
import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.SearchHits;
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
public abstract class NativeQueryIntegrationTests {
	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
	}

	@Test // #2391
	@DisplayName("should be able to use CriteriaQuery in a NativeQuery")
	void shouldBeAbleToUseCriteriaQueryInANativeQuery() {

		SampleEntity entity = new SampleEntity();
		entity.setId("7");
		entity.setText("seven");
		operations.save(entity);
		entity = new SampleEntity();
		entity.setId("42");
		entity.setText("criteria");
		operations.save(entity);

		CriteriaQuery criteriaQuery = CriteriaQuery.builder(Criteria.where("text").is("criteria")).build();
		NativeQuery nativeQuery = NativeQuery.builder().withQuery(criteriaQuery).build();

		SearchHits<SampleEntity> searchHits = operations.search(nativeQuery, SampleEntity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo(entity.getId());
	}

	@Test // #2391
	@DisplayName("should be able to use StringQuery in a NativeQuery")
	void shouldBeAbleToUseStringQueryInANativeQuery() {

		SampleEntity entity = new SampleEntity();
		entity.setId("7");
		entity.setText("seven");
		operations.save(entity);
		entity = new SampleEntity();
		entity.setId("42");
		entity.setText("string");
		operations.save(entity);

		StringQuery stringQuery = StringQuery.builder("{\n" +
													  "    \"bool\": {\n" +
													  "      \"must\": [\n" +
													  "        {\n" +
													  "          \"match\": {\n" +
													  "            \"text\": \"string\"\n" +
													  "          }\n" +
													  "        }\n" +
													  "      ]\n" +
													  "    }\n" +
													  "  }\n").build();
		NativeQuery nativeQuery = NativeQuery.builder().withQuery(stringQuery).build();

		SearchHits<SampleEntity> searchHits = operations.search(nativeQuery, SampleEntity.class);

		assertThat(searchHits.getTotalHits()).isEqualTo(1);
		assertThat(searchHits.getSearchHit(0).getId()).isEqualTo(entity.getId());
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class SampleEntity {

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}

		@Nullable
		@Id private String id;

		@Field(type = FieldType.Text) private String text;
	}
}
