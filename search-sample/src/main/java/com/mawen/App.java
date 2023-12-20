package com.mawen;

import com.mawen.search.repository.config.EnableElasticsearchRepositories;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
@SpringBootApplication(
		exclude = {
				ElasticsearchDataAutoConfiguration.class,
				ElasticsearchRepositoriesAutoConfiguration.class,
				ReactiveElasticsearchRepositoriesAutoConfiguration.class,
				ElasticsearchRestClientAutoConfiguration.class,
		}
)
@EnableElasticsearchRepositories(basePackages = "com.mawen.repository")
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}
}
