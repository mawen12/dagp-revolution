package com.mawen.repository;

import com.mawen.entity.DynamicPerson;
import com.mawen.query.PersonQuery;
import com.mawen.search.core.annotation.ParamQuery;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.repository.ElasticsearchRepository;

import org.springframework.stereotype.Repository;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
@Repository
public interface DynamicPersonRepository extends ElasticsearchRepository<DynamicPerson, String> {

	DynamicPerson search(@ParamQuery PersonQuery query, IndexCoordinates index);
}
