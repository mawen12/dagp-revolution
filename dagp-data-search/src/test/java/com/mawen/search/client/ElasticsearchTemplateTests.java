package com.mawen.search.client;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.junit.jupiter.ElasticsearchTemplateConfiguration;
import com.mawen.search.junit.jupiter.SpringIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.*;

@SpringIntegrationTest
@ContextConfiguration(classes = { ElasticsearchTemplateConfiguration.class })
@DisplayName("a sample JUnit 5 test with the new rest client")
class ElasticsearchTemplateTests {

	@Autowired
	private ElasticsearchOperations elasticsearchOperations;

	@Test
	@DisplayName("should have an ElasticsearchTemplate")
	void shouldHaveAElasticsearchTemplate() {
		assertThat(elasticsearchOperations).isNotNull().isInstanceOf(ElasticsearchTemplate.class);
	}

}