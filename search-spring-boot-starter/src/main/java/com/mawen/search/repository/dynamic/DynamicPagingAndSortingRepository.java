package com.mawen.search.repository.dynamic;

import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@NoRepositoryBean
public interface DynamicPagingAndSortingRepository<T, ID> extends DynamicCrudRepository<T, ID> {

	Iterable<T> findAll(Sort sort, IndexCoordinates index);

	Page<T> findAll(Pageable pageable, IndexCoordinates index);
}
