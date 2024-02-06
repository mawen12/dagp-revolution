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

import java.time.Duration;

import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.test.client.ClientConfiguration;

import org.springframework.context.annotation.Configuration;

import static org.springframework.util.StringUtils.*;

@Configuration
public class ElasticsearchTemplateConfiguration extends ElasticsearchConfiguration {

	@Override
	public ClientConfiguration clientConfiguration() {
		System.out.println(ClusterConnection.clusterConnectionInfo());
		String elasticsearchHostPort = ClusterConnection.clusterConnectionInfo().getHost() + ':' + ClusterConnection.clusterConnectionInfo().getHttpPort();

		ClientConfiguration.TerminalClientConfigurationBuilder configurationBuilder = ClientConfiguration.builder()
				.connectedTo(elasticsearchHostPort);

		String proxy = System.getenv("DATAES_ELASTICSEARCH_PROXY");

		if (proxy != null) {
			configurationBuilder = configurationBuilder.withProxy(proxy);
		}

		if (ClusterConnection.clusterConnectionInfo().isUseSsl()) {
			configurationBuilder = ((ClientConfiguration.MaybeSecureClientConfigurationBuilder) configurationBuilder)
					.usingSsl();
		}

		String user = System.getenv("DATAES_ELASTICSEARCH_USER");
		String password = System.getenv("DATAES_ELASTICSEARCH_PASSWORD");

		if (hasText(user) && hasText(password)) {
			configurationBuilder.withBasicAuth(user, password);
		}

		// noinspection UnnecessaryLocalVariable
		ClientConfiguration clientConfiguration = configurationBuilder //
				.withConnectTimeout(Duration.ofSeconds(20)) //
				.withSocketTimeout(Duration.ofSeconds(20)) //
				.build();

		return clientConfiguration;
	}

	@Override
	protected RefreshPolicy refreshPolicy() {
		return RefreshPolicy.IMMEDIATE;
	}


}
