/*
 * Copyright 2020-2023 the original author or authors.
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

import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.NestedQuery;
import co.elastic.clients.elasticsearch.core.search.InnerHits;
import com.mawen.search.client.query.NativeQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;
import com.mawen.search.utils.IndexNameProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = { InnerHitsELCIntegrationTests.Config.class })
public class InnerHitsELCIntegrationTests extends InnerHitsIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("innerhits");
		}
	}

	@Override
	protected Query buildQueryForInnerHits(String innerHitName, String nestedQueryPath, String matchField,
			String matchValue) {

		return NativeQuery.builder() //
				.withQuery(q -> q.nested( //
						NestedQuery.of(n -> n //
								.path(nestedQueryPath) //
								.query(q2 -> q2.match( //
										MatchQuery.of(m -> m //
												.field(matchField) //
												.query(fv -> fv.stringValue(matchValue)) //
										))) //
								.innerHits(InnerHits.of(ih -> ih.name(innerHitName))) //
						))) //
				.build();
	}

}
