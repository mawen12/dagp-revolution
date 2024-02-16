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
package com.mawen.search.repository.support;

import com.mawen.search.CustomElasticsearchTemplateConfiguration;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import com.mawen.search.test.ElasticsearchTemplateConfiguration;
import com.mawen.search.utils.IndexNameProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = {ElasticsearchRepositoryELCIntegrationTests.Config.class })
public class ElasticsearchRepositoryELCIntegrationTests extends ElasticsearchRepositoryIntegrationTests {

	@Configuration
	@Import({CustomElasticsearchTemplateConfiguration.class })
	@EnableElasticsearchRepositories(basePackages = {"com.mawen.search.repository.support" },
			considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("repository");
		}

	}
}
