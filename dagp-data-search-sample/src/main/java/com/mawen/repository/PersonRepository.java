package com.mawen.repository;

import java.util.stream.Stream;

import com.mawen.entity.Person;
import com.mawen.query.PersonQuery;
import com.mawen.search.core.annotation.ParamQuery;
import com.mawen.search.core.annotation.Query;
import com.mawen.search.repository.ElasticsearchRepository;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface PersonRepository extends ElasticsearchRepository<Person, String> {

	Stream<Person> searchForStream(@ParamQuery PersonQuery query);

}
