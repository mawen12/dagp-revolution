package com.mawen.controller;

import java.util.Random;

import com.mawen.entity.DynamicPerson;
import com.mawen.entity.Person;
import com.mawen.query.PersonQuery;
import com.mawen.repository.DynamicPersonRepository;
import com.mawen.repository.PersonRepository;
import com.mawen.search.core.mapping.IndexCoordinates;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@RestController
@RequestMapping("/person")
@Slf4j
public class PersonController {

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private DynamicPersonRepository dynamicPersonRepository;

	@GetMapping("/{id}")
	Person load(@PathVariable("id") String id) {
		log.info("load by id[{}]", id);
		return personRepository.findById(id).orElse(null);
	}

	@GetMapping("/dynamic/{id}")
	DynamicPerson loadDynamic(@PathVariable("id") String id) {
		log.info("load by dynamic[{}]", id);
		return dynamicPersonRepository.findById(id, IndexCoordinates.of("person")).orElse(null);
	}

	@GetMapping("/search/dynamic/{name}")
	DynamicPerson searchDynamic(@PathVariable("name") String name) {
		log.info("search by dynamic[{}]", name);
		PersonQuery personQuery = new PersonQuery();
		personQuery.setName(name);

		return dynamicPersonRepository.search(personQuery, IndexCoordinates.of("person"));
	}

	@GetMapping("/slow/search/{name}")
	DynamicPerson searchSlowly(@PathVariable("name") String name) {

		new Thread(() -> {
			Random random = new Random(1000L);
			StringBuffer buffer = new StringBuffer();
			for (int i = 0; i < 100_000; i++) {
				buffer.append(random.nextInt());
			}
		}).start();


		log.info("search by dynamic[{}]", name);
		PersonQuery personQuery = new PersonQuery();
		personQuery.setName(name);

		return dynamicPersonRepository.search(personQuery, IndexCoordinates.of("person"));
	}
}
