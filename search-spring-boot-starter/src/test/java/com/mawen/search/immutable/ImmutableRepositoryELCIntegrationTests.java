package com.mawen.search.immutable;

import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import com.mawen.search.utils.IndexNameProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration(classes = ImmutableRepositoryELCIntegrationTests.Config.class)
public class ImmutableRepositoryELCIntegrationTests extends ImmutableRepositoryIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	@EnableElasticsearchRepositories(basePackages = { "com.mawen.search.immutable" },
			considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("immutable");
		}
	}
}
