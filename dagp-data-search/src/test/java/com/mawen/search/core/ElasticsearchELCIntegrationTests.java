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
package com.mawen.search.core;

import java.util.ArrayList;
import java.util.List;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import com.mawen.search.CustomElasticsearchTemplateConfiguration;
import com.mawen.search.client.query.NativeQuery;
import com.mawen.search.client.query.Queries;
import com.mawen.search.client.query.builder.NativeQueryBuilder;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.builder.BaseQueryBuilder;
import com.mawen.search.test.ElasticsearchTemplateConfiguration;
import com.mawen.search.utils.IndexNameProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;

import static com.mawen.search.client.query.Queries.*;
import static com.mawen.search.utils.IndexBuilder.*;
import static org.assertj.core.api.Assertions.*;

@ContextConfiguration(classes = { ElasticsearchELCIntegrationTests.Config.class })
@DisplayName("Using Elasticsearch Client")
public class ElasticsearchELCIntegrationTests extends ElasticsearchIntegrationTests {

	@Configuration
	@Import({ CustomElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("integration");
		}
	}

	@Test // #2263
	public void shouldSortResultsBySortOptions() {

		List<IndexQuery> indexQueries = new ArrayList<>();

		indexQueries.add(buildIndex(SampleEntity.builder().id("1").message("ab xz").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("2").message("bc").build()));
		indexQueries.add(buildIndex(SampleEntity.builder().id("3").message("ac xz hi").build()));

		operations.bulkIndex(indexQueries, IndexCoordinates.of(indexNameProvider.indexName()));

		NativeQuery query = NativeQuery.builder().withSort(b -> b.field(fb -> fb.field("message.keyword").order(SortOrder.Asc)))
				.build();

		SearchHits<SampleEntity> searchHits = operations.search(query, SampleEntity.class,
				IndexCoordinates.of(indexNameProvider.indexName()));

		assertThat(searchHits.getSearchHits()) //
				.satisfiesExactly(e -> assertThat(e.getId()).isEqualTo("1"), e -> assertThat(e.getId()).isEqualTo("3"),
						e -> assertThat(e.getId()).isEqualTo("2"));
	}

	@Override
	protected Query queryWithIds(String... ids) {
		return Queries.queryWithIds(ids);
	}

	@Override
	protected Query getTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value)).build();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return Queries.getBuilderWithMatchAllQuery();
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithMatchQuery(String field, String value) {
		return NativeQuery.builder().withQuery(matchQueryAsQuery(field, value, null, null));
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value));
	}

	@Override
	protected BaseQueryBuilder<?, ?> getBuilderWithWildcardQuery(String field, String value) {
		return NativeQuery.builder().withQuery(wildcardQueryAsQuery(field, value));
	}

	@Override
	protected Query getBoolQueryWithWildcardsFirstMustSecondShouldAndMinScore(String firstField, String firstValue,
			String secondField, String secondValue, float minScore) {

		return NativeQuery.builder().withQuery(q -> q //
				.bool(BoolQuery.of(b -> b //
						.must(m -> m.wildcard(w1 -> w1.field(firstField).wildcard(firstValue))) //
						.should(s -> s.wildcard(w2 -> w2.field(secondField).wildcard(secondValue)))))) //
				.withMinScore(minScore) //
				.build();
	}

	@Override
	protected Query getQueryWithCollapse(String collapseField, @Nullable String innerHits, @Nullable Integer size) {
		return NativeQuery.builder() //
				.withQuery(matchAllQueryAsQuery()) //
				.withFieldCollapse(FieldCollapse.of(fc -> {
					fc.field(collapseField);

					if (innerHits != null) {
						fc.innerHits(ih -> ih.name(innerHits).size(size));
					}
					return fc;
				})).build();
	}

	@Override
	protected Query getMatchAllQueryWithFilterForId(String id) {
		return NativeQuery.builder() //
				.withQuery(matchAllQueryAsQuery()) //
				.withFilter(termQueryAsQuery("id", id)) //
				.build();
	}

	@Override
	protected Query getQueryForParentId(String type, String id, @Nullable String route) {

		NativeQueryBuilder queryBuilder = NativeQuery.builder() //
				.withQuery(qb -> qb //
						.parentId(p -> p.type(type).id(id)) //
				);

		if (route != null) {
			queryBuilder.withRoute(route);
		}

		return queryBuilder.build();
	}
}
