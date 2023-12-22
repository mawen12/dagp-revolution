package com.mawen.search.core.convert.core.query;

import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import com.mawen.search.utils.IndexNameProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/22
 */
@ContextConfiguration(classes = { QueryKeywordsELCIntegrationTests.Config.class })
public class QueryKeywordsELCIntegrationTests extends QueryKeywordsIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	@EnableElasticsearchRepositories(considerNestedRepositories = true)
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("query-keywords");
		}
	}
}
