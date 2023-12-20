package com.mawen.repository;

import com.mawen.entity.DynamicPerson;
import com.mawen.search.repository.ElasticsearchRepository;

import org.springframework.stereotype.Repository;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
@Repository
public interface DynamicPersonRepository extends ElasticsearchRepository<DynamicPerson, String> {
}
