package com.mawen.search.core.domain;

import com.mawen.search.CustomElasticsearchTemplateConfiguration;
import com.mawen.search.test.ElasticsearchTemplateConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@ContextConfiguration(classes = { SourceFilterELCIntegrationTests.Config.class })
public class SourceFilterELCIntegrationTests extends SourceFilterIntegrationTests {

	@Configuration
	@Import({ CustomElasticsearchTemplateConfiguration.class })
	static class Config {

	}
}
