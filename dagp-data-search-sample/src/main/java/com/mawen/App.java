package com.mawen;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.mawen.entity.Person;
import com.mawen.repository.PersonRepository;
import com.mawen.search.repository.config.EnableElasticsearchRepositories;
import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.jeasy.random.EasyRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.data.elasticsearch.ReactiveElasticsearchRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
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
@Slf4j
public class App {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Autowired
	private PersonRepository personRepository;

	private static final EasyRandom random = new EasyRandom();

//	@Bean
//	public CommandLineRunner runner() {
//		return args -> {
//			for (int i = 0; i < 100; i++) {
//				List<Person> list = new ArrayList<>(10_000);
//				for (int j = 0; j < 10_000; j++) {
//					list.add(random.nextObject(Person.class));
//				}
//				list.forEach(it -> it.setId(null));
//
//				personRepository.saveAll(list);
//			}
//		};
//	}
}
