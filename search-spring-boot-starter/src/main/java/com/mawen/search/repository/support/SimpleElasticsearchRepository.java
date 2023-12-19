package com.mawen.search.repository.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHitSupport;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SearchPage;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.repository.ElasticsearchRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.util.StreamUtils;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Elasticsearch specific repository implementation.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class SimpleElasticsearchRepository<T, ID> implements ElasticsearchRepository<T, ID> {

	protected ElasticsearchOperations operations;

	protected Class<T> entityClass;
	protected ElasticsearchEntityInformation<T, ID> entityInformation;

	public SimpleElasticsearchRepository(ElasticsearchEntityInformation<T, ID> metadata,
			ElasticsearchOperations operations) {
		this.operations = operations;

		Assert.notNull(metadata, "ElasticsearchEntityInformation must not be null!");

		this.entityInformation = metadata;
		this.entityClass = this.entityInformation.getJavaType();
	}


	@Override
	public Optional<T> findById(ID id) {
		return Optional.ofNullable(
				execute(operations -> operations.get(stringIdRepresentation(id), entityClass, getIndexCoordinates())));
	}

	@Override
	public Iterable<T> findAll() {
		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		return this.findAll(PageRequest.of(0, Math.max(1, itemCount)));
	}


	@Override
	public Page<T> findAll(Pageable pageable) {

		Assert.notNull(pageable, "pageable must not be null");

		Query query = Query.findAll();
		query.setPageable(pageable);
		SearchHits<T> searchHits = execute(operations -> operations.search(query, entityClass, getIndexCoordinates()));
		SearchPage<T> page = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
		// noinspection ConstantConditions
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterable<T> findAll(Sort sort) {

		Assert.notNull(sort, "sort must not be null");

		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		Pageable pageable = PageRequest.of(0, itemCount, sort);
		Query query = Query.findAll();
		query.setPageable(pageable);
		List<SearchHit<T>> searchHitList = execute(
				operations -> operations.search(query, entityClass, getIndexCoordinates()).getSearchHits());
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Assert.notNull(ids, "ids can't be null.");

		List<String> stringIds = stringIdsRepresentation(ids);
		Query query = getIdQuery(stringIds);
		if (!stringIds.isEmpty()) {
			query.setPageable(PageRequest.of(0, stringIds.size()));
		}
		List<SearchHit<T>> searchHitList = execute(
				operations -> operations.search(query, entityClass, getIndexCoordinates()).getSearchHits());
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public long count() {
		Query query = Query.findAll();
		((BaseQuery) query).setMaxResults(0);
		return execute(operations -> operations.count(query, entityClass, getIndexCoordinates()));
	}

	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Cannot save 'null' entity.");

		return executeAndRefresh(operations -> operations.save(entity, getIndexCoordinates()));
	}

	@Override
	public <S extends T> S save(S entity, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(entity, "entity must not be null");

		return executeAndRefresh(operations -> operations.save(entity, getIndexCoordinates()), refreshPolicy);
	}

	public <S extends T> List<S> save(List<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		return Streamable.of(saveAll(entities)).stream().collect(Collectors.toList());
	}

	public <S extends T> List<S> save(List<S> entities, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		return Streamable.of(saveAll(entities, refreshPolicy)).stream().collect(Collectors.toList());
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		executeAndRefresh(operations -> operations.save(entities, indexCoordinates));

		return entities;
	}


	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities, @Nullable RefreshPolicy refreshPolicy) {
		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		IndexCoordinates indexCoordinates = getIndexCoordinates();
		executeAndRefresh(operations -> operations.save(entities, indexCoordinates), refreshPolicy);

		return entities;
	}

	@Override
	public boolean existsById(ID id) {
		// noinspection ConstantConditions
		return execute(operations -> operations.exists(stringIdRepresentation(id), getIndexCoordinates()));
	}

	@Override
	public void deleteById(ID id) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, getIndexCoordinates());
	}

	@Override
	public void deleteById(ID id, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, getIndexCoordinates(), refreshPolicy);
	}

	@Override
	public void deleteById(ID id, IndexCoordinates index, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, index, refreshPolicy);
	}

	@Override
	public void delete(T entity) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		doDelete(extractIdFromBean(entity), getIndexCoordinates());
	}

	@Override
	public void delete(T entity, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		doDelete(extractIdFromBean(entity), getIndexCoordinates(), refreshPolicy);
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {

		// noinspection DuplicatedCode
		Assert.notNull(ids, "Cannot delete 'null' list.");

		List<String> idStrings = new ArrayList<>();
		for (ID id : ids) {
			idStrings.add(stringIdRepresentation(id));
		}

		if (idStrings.isEmpty()) {
			return;
		}

		Query query = operations.idsQuery(idStrings);
		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(query, entityClass, getIndexCoordinates());
			return null;
		});
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(ids, "Cannot delete 'null' list.");

		List<String> idStrings = new ArrayList<>();
		for (ID id : ids) {
			idStrings.add(stringIdRepresentation(id));
		}

		if (idStrings.isEmpty()) {
			return;
		}

		Query query = operations.idsQuery(idStrings);
		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(query, entityClass, getIndexCoordinates());
			return null;
		});
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		deleteAllById(getEntityIds(entities));
	}

	private List<ID> getEntityIds(Iterable<? extends T> entities) {
		Assert.notNull(entities, "Cannot delete 'null' list.");

		List<ID> ids = new ArrayList<>();
		for (T entity : entities) {
			ID id = extractIdFromBean(entity);
			if (id != null) {
				ids.add(id);
			}
		}
		return ids;
	}

	private void doDelete(@Nullable ID id, IndexCoordinates indexCoordinates) {

		if (id != null) {
			executeAndRefresh(operations -> operations.delete(stringIdRepresentation(id), indexCoordinates));
		}
	}

	private void doDelete(@Nullable ID id, IndexCoordinates indexCoordinates, @Nullable RefreshPolicy refreshPolicy) {

		if (id != null) {
			executeAndRefresh(operations -> operations.delete(stringIdRepresentation(id), indexCoordinates), refreshPolicy);
		}
	}

	@Override
	public void deleteAll() {

		executeAndRefresh((OperationsCallback<Void>) operations -> {
			operations.delete(Query.findAll(), entityClass, getIndexCoordinates());
			return null;
		});
	}

	private void doRefresh() {
	}

	// region helper functions
	@Nullable
	protected ID extractIdFromBean(T entity) {
		return entityInformation.getId(entity);
	}

	private List<String> stringIdsRepresentation(Iterable<? extends ID> ids) {

		Assert.notNull(ids, "ids can't be null.");

		return StreamUtils.createStreamFromIterator(ids.iterator()).map(this::stringIdRepresentation)
				.collect(Collectors.toList());
	}

	protected @Nullable String stringIdRepresentation(@Nullable ID id) {
		return operations.convertId(id);
	}

	private IndexCoordinates getIndexCoordinates() {
		return operations.getIndexCoordinatesFor(entityClass);
	}

	private Query getIdQuery(List<String> stringIds) {
		return operations.idsQuery(stringIds);
	}
	// endregion

	@Nullable
	public <R> R execute(OperationsCallback<R> callback) {
		return callback.doWithOperations(operations);
	}

	@Nullable
	public <R> R executeAndRefresh(OperationsCallback<R> callback) {
		R result = callback.doWithOperations(operations);
		doRefresh();
		return result;
	}

	@Nullable
	public <R> R executeAndRefresh(OperationsCallback<R> callback, @Nullable RefreshPolicy refreshPolicy) {
		R result = callback.doWithOperations(operations.withRefreshPolicy(refreshPolicy));
		doRefresh();
		return result;
	}
}
