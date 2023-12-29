package com.mawen.search.repository.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mawen.search.core.AbstractElasticsearchTemplate;
import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.IndexOperations;
import com.mawen.search.core.domain.SearchHit;
import com.mawen.search.core.domain.SearchHitSupport;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SearchPage;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.repository.ElasticsearchRepository;
import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class SimpleElasticsearchRepository<T, ID> implements ElasticsearchRepository<T, ID> {

	private static final BiFunction<Class<?>, String, String> messageFunction = (clazz, methodInfo) -> String.format("The Entity %s is dynamic index, should use %s replace it.", clazz, methodInfo);

	protected ElasticsearchOperations operations;
	protected IndexOperations indexOperations;

	protected ElasticsearchEntityInformation<T, ID> entityInformation;
	protected Class<T> entityClass;
	protected boolean isDynamicIndex;

	public SimpleElasticsearchRepository(ElasticsearchEntityInformation<T, ID> metadata, ElasticsearchOperations operations) {

		this.operations = operations;

		Assert.notNull(metadata, "ElasticsearchEntityInformation must not be null!");

		this.entityInformation = metadata;
		this.entityClass = this.entityInformation.getJavaType();
		this.isDynamicIndex = this.entityInformation.isDynamicIndex();
		this.indexOperations = operations.indexOps(this.entityClass);
	}

	@Override
	public Optional<T> findById(ID id) {
		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "findById(ID id, IndexCoordinates index)"));

		return findById(id, getIndexCoordinates(null));
	}

	@Override
	public Optional<T> findById(ID id, IndexCoordinates index) {
		return Optional.ofNullable(execute(operations -> operations.get(stringIdRepresentation(id), entityClass, index)));
	}

	@Override
	public Iterable<T> findAll() {
		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "findAll(Sort sort, IndexCoordinates index)"));

		int itemCount = (int) this.count();

		if (itemCount == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		return this.findAll(PageRequest.of(0, Math.max(1, itemCount)));
	}

	@Override
	public Page<T> findAll(Pageable pageable) {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "findAll(Pageable pageable, IndexCoordinates index)"));
		return findAll(pageable, getIndexCoordinates(null));
	}

	@Override
	public Page<T> findAll(Pageable pageable, IndexCoordinates index) {

		Assert.notNull(pageable, "pageable must not be null");

		Query query = Query.findAll();
		query.setPageable(pageable);
		SearchHits<T> searchHits = execute(operations -> operations.search(query, entityClass, index));
		SearchPage<T> page = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
		return (Page<T>) SearchHitSupport.unwrapSearchHits(page);
	}


	@Override
	public Iterable<T> findAll(Sort sort) {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "findAll(Sort sort, IndexCoordinates index)"));

		return findAll(sort, getIndexCoordinates(null));
	}

	@Override
	public Iterable<T> findAll(Sort sort, IndexCoordinates index) {

		Assert.notNull(sort, "sort must not be null");

		int itemCount = (int) this.count(index);

		if (itemCount == 0) {
			return new PageImpl<>(Collections.emptyList());
		}
		Pageable pageable = PageRequest.of(0, itemCount, sort);
		Query query = Query.findAll();
		query.setPageable(pageable);
		List<SearchHit<T>> searchHitList = execute(
				operations -> operations.search(query, entityClass, index).getSearchHits());
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public Iterable<T> findAllById(Iterable<ID> ids) {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "findAllById(Iterable<? extends ID> ids, IndexCoordinates index)"));

		return findAllById(ids, getIndexCoordinates(null));
	}

	@Override
	public Iterable<T> findAllById(Iterable<? extends ID> ids, IndexCoordinates index) {

		Assert.notNull(ids, "ids can't be null.");

		List<String> stringIds = stringIdsRepresentation(ids);
		Query query = getIdQuery(stringIds);
		if (!stringIds.isEmpty()) {
			query.setPageable(PageRequest.of(0, stringIds.size()));
		}
		List<SearchHit<T>> searchHitList = execute(operations -> operations.search(query, entityClass, index).getSearchHits());
		return (List<T>) SearchHitSupport.unwrapSearchHits(searchHitList);
	}

	@Override
	public long count() {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "count(IndexCoordinates index)"));
		return count(getIndexCoordinates(null));
	}

	@Override
	public long count(IndexCoordinates index) {

		Query query = Query.findAll();
		((BaseQuery) query).setMaxResults(0);
		Long count = execute(operations -> operations.count(query, entityClass, index));
		return count != null ? count : 0L;
	}

	@Override
	public <S extends T> S save(S entity) {

		Assert.notNull(entity, "Cannot save 'null' entity.");
		return save(entity, getIndexCoordinates(entity));
	}

	@Override
	public <S extends T> S save(S entity, IndexCoordinates index) {

		Assert.notNull(entity, "Cannot save 'null' entity.");
		return executeAndRefresh(operations -> operations.save(entity, index));
	}

	public <S extends T> List<S> save(List<S> entities) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		return Streamable.of(saveAll(entities)).stream().collect(Collectors.toList());
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities) {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "saveAll(Iterable<S> entities, IndexCoordinates index)"));
		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		executeAndRefresh(operations -> operations.save(entities));

		return entities;
	}

	@Override
	public <S extends T> Iterable<S> saveAll(Iterable<S> entities, IndexCoordinates index) {

		Assert.notNull(entities, "Cannot insert 'null' as a List.");

		executeAndRefresh(operations -> operations.save(entities, index));
		return entities;
	}

	@Override
	public boolean existsById(ID id) {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "existsById(ID id, IndexCoordinates index)"));

		return existsById(id, getIndexCoordinates(null));
	}

	@Override
	public boolean existsById(ID id, IndexCoordinates index) {

		return Boolean.TRUE.equals(execute(operations -> operations.exists(stringIdRepresentation(id), index)));
	}

	@Override
	public void deleteById(ID id) {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "deleteById(ID id, IndexCoordinates index)"));
		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, getIndexCoordinates(null));
	}

	@Override
	public void deleteById(ID id, IndexCoordinates index) {

		Assert.notNull(id, "Cannot delete entity with id 'null'.");

		doDelete(id, index);
	}

	@Override
	public void delete(T entity) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		delete(entity, getIndexCoordinates(entity));
	}

	@Override
	public void delete(T entity, IndexCoordinates index) {

		Assert.notNull(entity, "Cannot delete 'null' entity.");

		doDelete(extractIdFromBean(entity), getIndexCoordinates(entity));
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids) {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "deleteAllById(Iterable<? extends ID> ids, IndexCoordinates index)"));
		Assert.notNull(ids, "Cannot delete 'null' list.");

		List<String> idStrings = new ArrayList<>();
		for (ID id : ids) {
			idStrings.add(stringIdRepresentation(id));
		}

		if (idStrings.isEmpty()) {
			return;
		}

		Query query = operations.idsQuery(idStrings);
		executeAndRefresh(operations -> operations.delete(query, entityClass, getIndexCoordinates(null)));
	}

	@Override
	public void deleteAllById(Iterable<? extends ID> ids, IndexCoordinates index) {

		Assert.notNull(ids, "Cannot delete 'null' list.");

		List<String> idStrings = new ArrayList<>();
		for (ID id : ids) {
			idStrings.add(stringIdRepresentation(id));
		}

		if (idStrings.isEmpty()) {
			return;
		}

		Query query = operations.idsQuery(idStrings);
		executeAndRefresh(operations -> operations.delete(query, entityClass, index));
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities) {
		deleteAllById(getEntityIds(entities));
	}

	@Override
	public void deleteAll(Iterable<? extends T> entities, IndexCoordinates index) {
		deleteAllById(getEntityIds(entities), index);
	}

	@Override
	public void deleteAll() {

		Assert.isTrue(!isDynamicIndex, () -> messageFunction.apply(entityClass, "deleteAll(IndexCoordinates index)"));

		deleteAll(getIndexCoordinates(null));
	}

	@Override
	public void deleteAll(IndexCoordinates index) {

		executeAndRefresh(operations -> operations.delete(Query.findAll(), entityClass, index));
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
			executeAndRefresh(operations -> operations.delete(stringIdRepresentation(id), indexCoordinates));
		}
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

	private IndexCoordinates getIndexCoordinates(@Nullable T entity) {
		return isDynamicIndex
				? IndexCoordinates.of(entityInformation.getIndexName(entity))
				: entityInformation.getIndexCoordinates();
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

	private void doRefresh() {

		RefreshPolicy refreshPolicy = null;
		if (operations instanceof AbstractElasticsearchTemplate) {
			refreshPolicy = ((AbstractElasticsearchTemplate) operations).getRefreshPolicy();
		}

		if (refreshPolicy == null) {
			indexOperations.refresh();
		}
	}
}
