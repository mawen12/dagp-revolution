/*
 * Copyright 2012-2022 the original author or authors.
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

package com.mawen.search.autoconfigure.data;

import java.util.Collections;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mawen.search.client.ElasticsearchTemplate;
import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.convert.ElasticsearchCustomConversions;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.domain.EntityScanner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

abstract class ElasticsearchDataConfiguration {

	@Configuration(proxyBeanMethods = false)
	static class BaseConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ElasticsearchCustomConversions elasticsearchCustomConversions() {
			return new ElasticsearchCustomConversions(Collections.emptyList());
		}

		@Bean
		@ConditionalOnMissingBean
		SimpleElasticsearchMappingContext mappingContext(ApplicationContext applicationContext,
				ElasticsearchCustomConversions elasticsearchCustomConversions) throws ClassNotFoundException {
			SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
			mappingContext.setInitialEntitySet(new EntityScanner(applicationContext).scan(Document.class));
			mappingContext.setSimpleTypeHolder(elasticsearchCustomConversions.getSimpleTypeHolder());
			return mappingContext;
		}

		@Bean
		@ConditionalOnMissingBean
		ElasticsearchConverter elasticsearchConverter(SimpleElasticsearchMappingContext mappingContext,
				ElasticsearchCustomConversions elasticsearchCustomConversions) {
			MappingElasticsearchConverter converter = new MappingElasticsearchConverter(mappingContext);
			converter.setConversions(elasticsearchCustomConversions);
			return converter;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(ElasticsearchClient.class)
	static class JavaClientConfiguration {

		@Bean
		@ConditionalOnMissingBean(value = ElasticsearchOperations.class, name = "elasticsearchTemplate")
		@ConditionalOnBean(ElasticsearchClient.class)
		ElasticsearchTemplate elasticsearchTemplate(ElasticsearchClient client, ElasticsearchConverter converter) {
			return new ElasticsearchTemplate(client, converter);
		}

	}

}
