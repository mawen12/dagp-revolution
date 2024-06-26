package com.mawen.search.core.query;

import com.mawen.search.CustomElasticsearchTemplateConfiguration;
import com.mawen.search.test.ElasticsearchTemplateConfiguration;
import com.mawen.search.utils.IndexNameProvider;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@ContextConfiguration(classes = { CriteriaQueryELCIntegrationTests.Config.class })
public class CriteriaQueryELCIntegrationTests extends CriteriaQueryIntegrationTests {

	@Configuration
	@Import({ CustomElasticsearchTemplateConfiguration.class })
	static class Config {
		@Bean
		IndexNameProvider indexNameProvider() {
			return new IndexNameProvider("criteria");
		}
	}
}
