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
package com.mawen.search.test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.TransportOptions;
import co.elastic.clients.transport.rest_client.RestClientOptions;
import com.mawen.search.test.client.ClientConfiguration;
import com.mawen.search.test.client.ElasticsearchClients;
import com.mawen.search.test.client.ElasticsearchConfigurationSupport;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;


public abstract class ElasticsearchConfiguration extends ElasticsearchConfigurationSupport {

	@Bean(name = "elasticsearchClientConfiguration")
	public abstract ClientConfiguration clientConfiguration();

	@Bean
	public RestClient elasticsearchRestClient(ClientConfiguration clientConfiguration) {

		Assert.notNull(clientConfiguration, "clientConfiguration must not be null");

		return ElasticsearchClients.getRestClient(clientConfiguration);
	}

	@Bean
	public ElasticsearchTransport elasticsearchTransport(RestClient restClient, JsonpMapper jsonpMapper) {

		Assert.notNull(restClient, "restClient must not be null");
		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		return ElasticsearchClients.getElasticsearchTransport(restClient, ElasticsearchClients.IMPERATIVE_CLIENT,
				transportOptions(), jsonpMapper);
	}

	@Bean
	public ElasticsearchClient elasticsearchClient(ElasticsearchTransport transport) {

		Assert.notNull(transport, "transport must not be null");

		return ElasticsearchClients.createImperative(transport);
	}

//	@Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
//	public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
//			ElasticsearchClient elasticsearchClient) {
//
//		ElasticsearchTemplate template = new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
//		template.setRefreshPolicy(refreshPolicy());
//
//		return template;
//	}

	@Bean
	public JsonpMapper jsonpMapper() {
		return new JacksonJsonpMapper();
	}

	public TransportOptions transportOptions() {
		return new RestClientOptions(RequestOptions.DEFAULT);
	}
}
