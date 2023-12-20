package com.mawen.search.repository.dynamic;

import java.util.Optional;

import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
@NoRepositoryBean
public interface DynamicCrudRepository<T, ID> extends Repository<T, ID> {

	<S extends T> S save(S entity);

	<S extends T> S save(S entity, IndexCoordinates index);

	<S extends T> Iterable<S> saveAll(Iterable<S> entities);

	<S extends T> Iterable<S> saveAll(Iterable<S> entities, IndexCoordinates index);

	Optional<T> findById(ID id, IndexCoordinates index);

	boolean existsById(ID id, IndexCoordinates index);

	Iterable<T> findAllById(Iterable<? extends ID> ids, IndexCoordinates index);

	long count(IndexCoordinates index);

	void deleteById(ID id, IndexCoordinates index);

	void deleteAllById(Iterable<? extends ID> ids, IndexCoordinates index);

	void delete(T entity);

	void delete(T entity, IndexCoordinates index);

	void deleteAll(Iterable<? extends T> entities);

	void deleteAll(Iterable<? extends T> entities, IndexCoordinates index);

	void deleteAll(IndexCoordinates index);
}
