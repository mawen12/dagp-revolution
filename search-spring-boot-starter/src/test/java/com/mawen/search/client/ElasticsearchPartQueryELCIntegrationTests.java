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
package com.mawen.search.client;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.mawen.search.client.request.RequestConverter;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.Query;
import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;
import com.mawen.search.repository.query.ElasticsearchPartQueryIntegrationTests;
import com.mawen.search.utils.JsonUtils;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

public class ElasticsearchPartQueryELCIntegrationTests extends ElasticsearchPartQueryIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {}

	@Override
	protected String buildQueryString(Query query, Class<?> clazz) {

		JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper();
		RequestConverter requestConverter = new RequestConverter(operations.getElasticsearchConverter(), jsonpMapper);
		SearchRequest request = requestConverter.searchRequest(query, null, clazz, IndexCoordinates.of("dummy"), false);

		return JsonUtils.toJson(request, jsonpMapper);
		// return "{\"query\":" + JsonUtils.toJson(request.query(), jsonpMapper) + "}";
	}
}
