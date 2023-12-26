/*
 * Copyright 2016-2023 the original author or authors.
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
package com.mawen.search.immutable;

import java.util.Optional;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.repository.CrudRepository;
import org.springframework.lang.Nullable;
import static org.assertj.core.api.Assertions.*;

/**
 * @author Young Gu
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Peter-Josef Meisch
 */
@SpringIntegrationTest
public abstract class ImmutableRepositoryIntegrationTests {

	@Autowired ImmutableElasticsearchRepository repository;
	@Autowired
	ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	public void before() {
		indexNameProvider.increment();
	}

	@Test // DATAES-281
	public void shouldSaveAndFindImmutableDocument() {

		// when
		ImmutableEntity entity = repository.save(new ImmutableEntity(null, "test name"));
		assertThat(entity.getId()).isNotNull();

		// then
		Optional<ImmutableEntity> entityFromElasticSearch = repository.findById(entity.getId());

		assertThat(entityFromElasticSearch).isPresent();

		entityFromElasticSearch.ifPresent(immutableEntity -> {

			assertThat(immutableEntity.getName()).isEqualTo("test name");
			assertThat(immutableEntity.getId()).isEqualTo(entity.getId());
		});
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	static class ImmutableEntity {
		private final String id, name;

		public ImmutableEntity(String id, String name) {
			this.id = id;
			this.name = name;
		}

		public ImmutableEntity withId(@Nullable String id) {
			return new ImmutableEntity(id, this.name);
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}

	public interface ImmutableElasticsearchRepository extends CrudRepository<ImmutableEntity, String> {}

}
