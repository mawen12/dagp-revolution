package com.mawen.repository;

import com.mawen.entity.Person;
import com.mawen.search.repository.ElasticsearchRepository;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
public interface PersonRepository extends ElasticsearchRepository<Person, String> {
}
