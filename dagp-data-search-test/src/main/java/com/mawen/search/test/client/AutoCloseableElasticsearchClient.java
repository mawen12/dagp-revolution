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
package com.mawen.search.test.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.cluster.ElasticsearchClusterClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import org.springframework.util.Assert;

public class AutoCloseableElasticsearchClient extends ElasticsearchClient implements AutoCloseable {

	public AutoCloseableElasticsearchClient(ElasticsearchTransport transport) {
		super(transport);
		Assert.notNull(transport, "transport must not be null");
	}

	@Override
	public void close() throws Exception {
		transport.close();
	}

	@Override
	public ElasticsearchClusterClient cluster() {
		return super.cluster();
	}
}
