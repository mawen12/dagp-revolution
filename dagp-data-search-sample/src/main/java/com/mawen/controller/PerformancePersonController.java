package com.mawen.controller;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Iterators;
import com.mawen.entity.Person;
import com.mawen.query.PersonQuery;
import com.mawen.repository.PersonRepository;
import com.mawen.support.CustomBatchIterator;
import org.apache.commons.collections4.ListUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <a href="https://www.baeldung.com/java-stream-batch-processing">Java Stream Batch Processing</a>
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/1/28
 */
@RestController
public class PerformancePersonController {

	@Autowired
	private PersonRepository personRepository;


	@GetMapping("/stream")
	public void stream() {// failed with OutOfMemoryError
		PersonQuery query = new PersonQuery();
		Stream<Person> stream = personRepository.searchForStream(query);
		stream.forEach(it -> System.out.println(it.getId()));
	}

	@GetMapping("/stream/guava")
	public void streamGuava() {// success
		PersonQuery query = new PersonQuery();
		try (Stream<Person> stream = personRepository.searchForStream(query)) {
			AtomicLong count = new AtomicLong(0L);
			Iterators.partition(stream.iterator(), 500).forEachRemaining(its -> {
				System.out.println("===> it size : " + its.size() + " : " + count.incrementAndGet());
				its.forEach(it -> System.out.println(it.getId()));
			});
		}
	}


	@GetMapping("/stream/apache")
	public void streamApache() {// failed with OutOfMemoryError
		PersonQuery query = new PersonQuery();
		Stream<Person> stream = personRepository.searchForStream(query);
		ListUtils.partition(stream.collect(Collectors.toList()), 500)
				.stream().forEach(its -> {
					System.out.println("===> it size : " + its.size());
					its.forEach(it -> System.out.println(it.getId()));
				});
	}

	@GetMapping("/stream/vavr")
	public void streamVavr() {// success
		PersonQuery query = new PersonQuery();
		Stream<Person> stream = personRepository.searchForStream(query);
		io.vavr.collection.Stream.ofAll(stream)
				.grouped(500)
				.forEachRemaining(itStream -> {
					List<Person> list = itStream.toStream().collect(Collectors.toList());
					list.forEach(it -> System.out.println(it.getId()));
				});
	}

	@GetMapping("/stream/custom")
	public void streamCustom() {// success
		PersonQuery query = new PersonQuery();
		Stream<Person> stream = personRepository.searchForStream(query);
		Stream<List<Person>> listStream = CustomBatchIterator.batchStreamOf(stream, 500);
		listStream.forEach(its -> {
			System.out.println("===> it size : " + its.size());
			its.forEach(it -> System.out.println(it.getId()));
		});
	}

}
