package com.mawen.controller;

import com.mawen.entity.DynamicPerson;
import com.mawen.entity.Person;
import com.mawen.query.PersonQuery;
import com.mawen.repository.DynamicPersonRepository;
import com.mawen.repository.PersonRepository;
import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
@RestController
@RequestMapping("/person")
public class PersonController {

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private DynamicPersonRepository dynamicPersonRepository;

	@GetMapping("/{id}")
	Person load(@PathVariable("id") String id) {
		return personRepository.findById(id).orElse(null);
	}

	@GetMapping("/dynamic/{id}")
	DynamicPerson loadDynamic(@PathVariable("id") String id) {
		return dynamicPersonRepository.findById(id, IndexCoordinates.of("person")).orElse(null);
	}

	@GetMapping("/search/dynamic/{name}")
	DynamicPerson searchDynamic(@PathVariable("name") String name) {
		PersonQuery personQuery = new PersonQuery();
		personQuery.setName(name);

		return dynamicPersonRepository.search(personQuery, IndexCoordinates.of("person"));
	}
}
