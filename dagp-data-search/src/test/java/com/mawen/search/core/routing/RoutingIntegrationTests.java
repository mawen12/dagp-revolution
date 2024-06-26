/*
 * Copyright2020-2021 the original author or authors.
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
package com.mawen.search.core.routing;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import com.mawen.search.core.AbstractElasticsearchTemplate;
import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Routing;
import com.mawen.search.core.domain.IdWithRouting;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.support.MultiGetItem;
import com.mawen.search.test.SpringIntegrationTest;
import com.mawen.search.utils.IndexNameProvider;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

@SuppressWarnings("ConstantConditions")
@SpringIntegrationTest
public abstract class RoutingIntegrationTests {

	private static final String ID_0 = "id0";
	private static final String ID_1 = "id1";
	private static final String ID_2 = "id2";
	private static final String ID_3 = "id3";

	@Autowired
	ElasticsearchOperations operations;
	@Autowired private IndexNameProvider indexNameProvider;

	@BeforeEach
	void setUp() {
		indexNameProvider.increment();
	}

	@Test // #1218
	@DisplayName("should store data with different routing and be able to get it")
	void shouldStoreDataWithDifferentRoutingAndBeAbleToGetIt() {

		RoutingEntity entity = new RoutingEntity(ID_1, ID_2);
		operations.save(entity);

		RoutingEntity savedEntity = operations.withRouting(RoutingResolver.just(ID_2)).get(entity.id, RoutingEntity.class);

		assertThat(savedEntity).isEqualTo(entity);
	}

	@Test // #1218
	@DisplayName("should store data with different routing and be able to delete it")
	void shouldStoreDataWithDifferentRoutingAndBeAbleToDeleteIt() {

		RoutingEntity entity = new RoutingEntity(ID_1, ID_2);
		operations.save(entity);

		String deletedId = operations.withRouting(RoutingResolver.just(ID_2)).delete(entity.id,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(deletedId).isEqualTo(entity.getId());
	}

	@Test // #1218
	@DisplayName("should store data with different routing and get the routing in the search result")

	void shouldStoreDataWithDifferentRoutingAndGetTheRoutingInTheSearchResult() {

		RoutingEntity entity = new RoutingEntity(ID_1, ID_2);
		operations.save(entity);

		SearchHits<RoutingEntity> searchHits = operations.search(Query.findAll(), RoutingEntity.class);

		assertThat(searchHits.getSearchHits()).hasSize(1);
		assertThat(searchHits.getSearchHit(0).getRouting()).isEqualTo(ID_2);
	}

	@Test // #1954
	@DisplayName("should use routing values in multiget")
	void shouldUseRoutingValuesInMultiget() {

		Consumer<String> save = (String id) -> operations.save(new RoutingEntity(id, id));
		save.accept(ID_1);
		save.accept(ID_2);
		save.accept(ID_3);

		Query query = Query.multiGetQueryWithRouting( //
				Arrays.asList( //
						new IdWithRouting(ID_1, ID_1), //
						new IdWithRouting(ID_2, ID_2), //
						new IdWithRouting(ID_3, ID_3) //
				) //
		); //

		// make sure that the correct routing values are used
		((BaseQuery) query).setRoute(ID_0);

		List<MultiGetItem<RoutingEntity>> multiGetItems = operations.multiGet(query, RoutingEntity.class);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(multiGetItems).hasSize(3);
		softly.assertThat(multiGetItems.get(0).hasItem()).isTrue();
		softly.assertThat(multiGetItems.get(1).hasItem()).isTrue();
		softly.assertThat(multiGetItems.get(2).hasItem()).isTrue();
		softly.assertAll();
	}

	@Test
	void shouldCreateACopyOfTheClientWithRefreshPolicy() {
		// given
		AbstractElasticsearchTemplate sourceTemplate = (AbstractElasticsearchTemplate) operations;
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		DefaultRoutingResolver defaultRoutingResolver = new DefaultRoutingResolver(mappingContext);

		// when
		ElasticsearchOperations operationsCopy = this.operations.withRouting(defaultRoutingResolver);
		AbstractElasticsearchTemplate copyTemplate = (AbstractElasticsearchTemplate) operationsCopy;

		// then
		assertThat(sourceTemplate.getRefreshPolicy()).isEqualTo(copyTemplate.getRefreshPolicy());
	}

	@Document(indexName = "#{@indexNameProvider.indexName()}")
	@Routing("routing")
	static class RoutingEntity {
		@Nullable
		@Id private String id;
		@Nullable private String routing;

		public RoutingEntity(@Nullable String id, @Nullable String routing) {
			this.id = id;
			this.routing = routing;
		}

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

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (!(o instanceof RoutingEntity))
				return false;

			RoutingEntity that = (RoutingEntity) o;
			if (!Objects.equals(id, that.id))
				return false;
			return Objects.equals(routing, that.routing);
		}

		@Override
		public int hashCode() {
			int result = id != null ? id.hashCode() : 0;
			result = 31 * result + (routing != null ? routing.hashCode() : 0);
			return result;
		}
	}
}
