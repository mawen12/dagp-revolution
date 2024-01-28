package com.mawen.controller;

import java.util.stream.Stream;

import com.google.common.collect.Iterators;
import com.mawen.model.PersonDTO;
import com.mawen.query.PersonQuery;
import com.mawen.service.PersonService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/1/28
 */
@RestController
public class PerformancePersonDTOController {

	@Autowired
	private PersonService personService;

	@GetMapping("/dto/stream/guava")
	public void streamDTOGuava() {// success
		PersonQuery query = new PersonQuery();
		Stream<PersonDTO> stream = personService.stream(query);
		Iterators.partition(stream.iterator(), 500).forEachRemaining(its -> {
			System.out.println("===> it size : " + its.size());
			its.forEach(it -> System.out.println(it.getId()));
		});
	}

}
