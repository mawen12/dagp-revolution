/*
 * Copyright 2012-2023 the original author or authors.
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

package com.mawen.search.autoconfigure;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.client.RestClient;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

public class ElasticsearchClientConfigurations {

	@ConditionalOnMissingBean(JsonpMapper.class)
	@Configuration(proxyBeanMethods = false)
	static class JacksonJsonpMapperConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ObjectMapper objectMapper() {
			return new ObjectMapper();
		}

		@Bean
		JacksonJsonpMapper jacksonJsonpMapper() {
			return new JacksonJsonpMapper();
		}
	}

	@Import({JacksonJsonpMapperConfiguration.class})
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(RestClientTransport.class)
	static class ElasticsearchTransportConfiguration {

		@Bean
		RestClientTransport restClientTransport(RestClient restClient, JsonpMapper jsonMapper) {
			return new RestClientTransport(restClient, jsonMapper, null);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ElasticsearchClient.class)
	static class ElasticsearchClientConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {
			return new ElasticsearchClient(transport);
		}

	}

}
