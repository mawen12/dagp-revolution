package com.mawen.service;

import java.util.stream.Stream;

import com.mawen.entity.Person;
import com.mawen.model.PersonDTO;
import com.mawen.query.PersonQuery;
import com.mawen.repository.PersonRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/1/28
 */
@Service
public class PersonService {

	@Autowired
	private PersonRepository personRepository;

	public Stream<PersonDTO> stream(PersonQuery query) {
		Stream<Person> stream = personRepository.searchForStream(query);
		return stream.map(Person.to);
	}

}
