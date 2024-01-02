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
package com.mawen.search.core.paginating;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.query.Query;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
public abstract class SearchAfterIntegrationTests {

	@Autowired private ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {

		indexNameProvider.increment();
	}

	@Test // #1143
	@DisplayName("should read pages with search_after")
	void shouldReadPagesWithSearchAfter() {

		List<Entity> entities = IntStream.rangeClosed(1, 10).mapToObj(i -> new Entity((long) i, "message " + i))
				.collect(Collectors.toList());
		operations.save(entities);

		Query query = Query.findAll();
		query.setPageable(PageRequest.of(0, 3));
		query.addSort(Sort.by(Sort.Direction.ASC, "id"));

		List<Object> searchAfter = null;
		List<Entity> foundEntities = new ArrayList<>();

		int loop = 0;
		do {
			query.setSearchAfter(searchAfter);
			SearchHits<Entity> searchHits = operations.search(query, Entity.class);

			if (searchHits.getSearchHits().size() == 0) {
				break;
			}
			foundEntities.addAll(searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList()));
			searchAfter = searchHits.getSearchHit(searchHits.getSearchHits().size() - 1).getSortValues();

			if (++loop > 10) {
				fail("loop not terminating");
			}
		} while (true);

		assertThat(foundEntities).containsExactlyElementsOf(entities);
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	private static class Entity {
		@Nullable
		@Id private Long id;
		@Nullable
		@Field(type = FieldType.Text) private String message;

		public Entity(@Nullable Long id, @Nullable String message) {
			this.id = id;
			this.message = message;
		}

		@Nullable
		public Long getId() {
			return id;
		}

		public void setId(@Nullable Long id) {
			this.id = id;
		}

		@Nullable
		public String getMessage() {
			return message;
		}

		public void setMessage(@Nullable String message) {
			this.message = message;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof Entity))
				return false;

			Entity entity = (Entity) o;

			if (!Objects.equals(id, entity.id))
				return false;
			return Objects.equals(message, entity.message);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (message != null ? message.hashCode() : 0);
			return result;
		}
	}
}
