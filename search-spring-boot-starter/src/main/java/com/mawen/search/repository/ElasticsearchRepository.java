package com.mawen.search.repository;

import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@NoRepositoryBean
public interface ElasticsearchRepository<T, ID> extends
		PagingAndSortingRepository<T, ID>,
		CrudRepository<T, ID> {

	<S extends T> S save(S entity, @Nullable RefreshPolicy refreshPolicy);

	<S extends T> Iterable<S> saveAll(Iterable<S> entities, @Nullable RefreshPolicy refreshPolicy);

	void deleteById(ID id, @Nullable RefreshPolicy refreshPolicy);

	void deleteById(ID id, IndexCoordinates index, @Nullable RefreshPolicy refreshPolicy);

	void delete(T entity, @Nullable RefreshPolicy refreshPolicy);

	void deleteAllById(Iterable<? extends ID> ids, @Nullable RefreshPolicy refreshPolicy);
}
