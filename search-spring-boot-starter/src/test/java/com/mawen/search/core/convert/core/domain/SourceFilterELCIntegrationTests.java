package com.mawen.search.core.convert.core.domain;

import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/21
 */
@ContextConfiguration(classes = { SourceFilterELCIntegrationTests.Config.class })
public class SourceFilterELCIntegrationTests extends SourceFilterIntegrationTests {

	@Configuration
	@Import({ ElasticsearchTemplateConfiguration.class })
	static class Config {

	}
}
