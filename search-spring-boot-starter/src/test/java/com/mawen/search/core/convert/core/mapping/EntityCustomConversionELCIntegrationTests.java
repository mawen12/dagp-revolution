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
package com.mawen.search.core.convert.core.mapping;

import java.util.Arrays;

import com.mawen.search.core.convert.ElasticsearchCustomConversions;
import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import com.mawen.search.utils.IndexNameProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Peter-Josef Meisch
 * @since 4.4
 */
@ContextConfiguration(classes = { EntityCustomConversionELCIntegrationTests.Config.class })
public class EntityCustomConversionELCIntegrationTests extends EntityCustomConversionIntegrationTests {

	@Configuration
	@Import({ EntityCustomConversionIntegrationTests.Config.class })
	@EnableElasticsearchRepositories(basePackages = { "org.springframework.data.elasticsearch.core.mapping" },
			considerNestedRepositories = true)
	static class Config extends ElasticsearchTemplateConfiguration {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("entity-customconversions-operations");
		}

		@Override
		public ElasticsearchCustomConversions elasticsearchCustomConversions() {
			return new ElasticsearchCustomConversions(Arrays.asList(new EntityToMapConverter(), new MapToEntityConverter()));
		}
	}
}
