package com.mawen.search.core;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.document.SearchDocumentResponse;
import com.mawen.search.core.domain.BulkOptions;
import com.mawen.search.core.domain.SearchHitMapping;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SearchHitsIterator;
import com.mawen.search.core.domain.SearchScrollHits;
import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import com.mawen.search.core.event.AfterConvertCallback;
import com.mawen.search.core.event.AfterLoadCallback;
import com.mawen.search.core.event.AfterSaveCallback;
import com.mawen.search.core.event.BeforeConvertCallback;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.query.ByQueryResponse;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.query.MoreLikeThisQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.StreamQueries;
import com.mawen.search.core.query.UpdateQuery;
import com.mawen.search.core.query.UpdateResponse;
import com.mawen.search.core.query.builder.IndexQueryBuilder;
import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.core.routing.DefaultRoutingResolver;
import com.mawen.search.core.routing.RoutingResolver;
import com.mawen.search.core.support.IndexedObjectInformation;
import com.mawen.search.core.support.MultiGetItem;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.convert.EntityReader;
import org.springframework.data.mapping.PersistentPropertyAccessor;
import org.springframework.data.mapping.callback.EntityCallbacks;
import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.util.Streamable;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public abstract class AbstractElasticsearchTemplate implements ElasticsearchOperations, ApplicationContextAware {

	protected ElasticsearchConverter elasticsearchConverter;
	protected EntityOperations entityOperations;
	@Nullable
	protected EntityCallbacks entityCallbacks;
	@Nullable
	protected RefreshPolicy refreshPolicy;
	protected RoutingResolver routingResolver;

	public AbstractElasticsearchTemplate() {
		this(null);
	}

	public AbstractElasticsearchTemplate(@Nullable ElasticsearchConverter elasticsearchConverter) {

		this.elasticsearchConverter = elasticsearchConverter != null ? elasticsearchConverter
				: createElasticsearchConverter();
		MappingContext<? extends ElasticsearchPersistentEntity<?>, ElasticsearchPersistentProperty> mappingContext = this.elasticsearchConverter
				.getMappingContext();
		this.entityOperations = new EntityOperations(mappingContext);
		this.routingResolver = new DefaultRoutingResolver(mappingContext);
	}

	private AbstractElasticsearchTemplate copy() {

		AbstractElasticsearchTemplate copy = doCopy();

		if (entityCallbacks != null) {
			copy.setEntityCallbacks(entityCallbacks);
		}

		copy.setRoutingResolver(routingResolver);
		copy.setRefreshPolicy(refreshPolicy);

		return copy;
	}

	protected abstract AbstractElasticsearchTemplate doCopy();

	private ElasticsearchConverter createElasticsearchConverter() {
		MappingElasticsearchConverter mappingElasticsearchConverter = new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());
		mappingElasticsearchConverter.afterPropertiesSet();
		return mappingElasticsearchConverter;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {

		if (entityCallbacks == null) {
			setEntityCallbacks(EntityCallbacks.create(applicationContext));
		}

		if (elasticsearchConverter instanceof ApplicationContextAware) {
			ApplicationContextAware contextAware = (ApplicationContextAware) elasticsearchConverter;
			contextAware.setApplicationContext(applicationContext);
		}
	}

	public void setEntityCallbacks(EntityCallbacks entityCallbacks) {

		Assert.notNull(entityCallbacks, "entityCallbacks must not be null");

		this.entityCallbacks = entityCallbacks;
	}

	@Nullable
	public RefreshPolicy getRefreshPolicy() {
		return refreshPolicy;
	}

	public void setRefreshPolicy(@Nullable RefreshPolicy refreshPolicy) {
		this.refreshPolicy = refreshPolicy;
	}

	// region DocumentOperations
	@Override
	public <T> T save(T entity) {

		Assert.notNull(entity, "entity must not be null");

		return save(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public <T> T save(T entity, IndexCoordinates index) {

		Assert.notNull(entity, "entity must not be null");
		Assert.notNull(index, "index must not be null");

		T entityAfterBeforeConvert = maybeCallbackBeforeConvert(entity, index);

		IndexQuery query = getIndexQuery(entityAfterBeforeConvert);
		doIndex(query, index);

		// noinspection unchecked
		return (T) maybeCallbackAfterSave(Objects.requireNonNull(query.getObject()), index);
	}

	@Override
	public <T> Iterable<T> save(Iterable<T> entities) {

		Assert.notNull(entities, "entities must not be null");

		Iterator<T> iterator = entities.iterator();
		IndexCoordinates index = null;
		if (iterator.hasNext()) {
			index = getIndexCoordinatesFor(iterator.next().getClass());
		}

		return save(entities, index);
	}

	@Override
	public <T> Iterable<T> save(Iterable<T> entities, IndexCoordinates index) {

		Assert.notNull(entities, "entities must not be null");
		Assert.notNull(index, "index must not be null");

		Iterator<T> it = entities.iterator();
		if (!it.hasNext()) {
			return entities;
		}

		List<IndexQuery> indexQueries = Streamable.of(entities).stream().map(this::getIndexQuery)
				.collect(Collectors.toList());

		List<IndexedObjectInformation> indexedObjectInformationList = bulkIndex(indexQueries, index);
		Iterator<IndexedObjectInformation> iterator = indexedObjectInformationList.iterator();

		return indexQueries.stream() //
				.map(IndexQuery::getObject) //
				.map(entity -> (T) updateIndexedObject(entity, iterator.next())) //
				.collect(Collectors.toList()); //
	}

	protected <T> Iterable<T> save(List<IndexQuery> indexQueries) {
		if (indexQueries.isEmpty()) {
			return Collections.emptyList();
		}

		List<IndexedObjectInformation> indexedObjectInformationList = bulkIndex(indexQueries);
		Iterator<IndexedObjectInformation> iterator = indexedObjectInformationList.iterator();

		// noinspection unchecked
		return indexQueries.stream() //
				.map(IndexQuery::getObject) //
				.map(entity -> (T) updateIndexedObject(entity, iterator.next())) //
				.collect(Collectors.toList()); //
	}

	@Override
	public final <T> Iterable<T> save(T... entities) {
		return save(Arrays.asList(entities));
	}

	@Override
	public String index(IndexQuery query, IndexCoordinates index) {

		maybeCallbackBeforeConvertWithQuery(query, index);

		String documentId = doIndex(query, index);

		maybeCallbackAfterSaveWithQuery(query, index);

		return documentId;
	}

	public abstract String doIndex(IndexQuery query, IndexCoordinates indexCoordinates);

	@Override
	@Nullable
	public <T> T get(String id, Class<T> clazz) {
		return get(id, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public <T> List<MultiGetItem<T>> multiGet(Query query, Class<T> clazz) {
		return multiGet(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public boolean exists(String id, Class<?> clazz) {
		return exists(id, getIndexCoordinatesFor(clazz));
	}

	@Override
	public boolean exists(String id, IndexCoordinates index) {
		return doExists(id, index);
	}

	protected abstract boolean doExists(String id, IndexCoordinates index);

	@Override
	public String delete(String id, Class<?> entityType) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(entityType, "entityType must not be null");

		return this.delete(id, getIndexCoordinatesFor(entityType));
	}

	@Override
	public ByQueryResponse delete(Query query, Class<?> clazz) {
		return doDelete(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public ByQueryResponse delete(Query query, Class<?> clazz, IndexCoordinates index) {
		return doDelete(query, clazz, index);
	}

	@Override
	public String delete(Object entity) {
		return delete(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public String delete(Object entity, IndexCoordinates index) {
		String entityId = getEntityId(entity);
		Assert.notNull(entityId, "entity must have an id that is notnull");
		return this.delete(entityId, index);
	}

	@Override
	public String delete(String id, IndexCoordinates index) {
		return doDelete(id, routingResolver.getRouting(), index);
	}

	protected abstract String doDelete(String id, @Nullable String routing, IndexCoordinates index);

	protected abstract ByQueryResponse doDelete(Query query, Class<?> clazz, IndexCoordinates index);

	@Override
	public List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries) {

		Assert.isTrue(queries.stream().anyMatch(query -> query.getIndexName() != null), String.format("IndexQuery %s must all have indexName.", queries.stream().filter(query -> query.getIndexName() == null).map(IndexQuery::getId).collect(Collectors.joining(","))));

		return doBulkOperation(queries, BulkOptions.defaultOptions(), null);
	}

	@Override
	public List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, Class<?> clazz) {
		return bulkIndex(queries, getIndexCoordinatesFor(clazz));
	}

	@Override
	public List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions, Class<?> clazz) {
		return bulkIndex(queries, bulkOptions, getIndexCoordinatesFor(clazz));
	}

	@Override
	public final List<IndexedObjectInformation> bulkIndex(List<IndexQuery> queries, BulkOptions bulkOptions,
			IndexCoordinates index) {

		Assert.notNull(queries, "List of IndexQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		return bulkOperation(queries, bulkOptions, index);
	}

	@Override
	public void bulkUpdate(List<UpdateQuery> queries, Class<?> clazz) {
		bulkUpdate(queries, getIndexCoordinatesFor(clazz));
	}

	public List<IndexedObjectInformation> bulkOperation(List<IndexQuery> queries, BulkOptions bulkOptions,
			IndexCoordinates index) {

		Assert.notNull(queries, "List of IndexQuery must not be null");
		Assert.notNull(bulkOptions, "BulkOptions must not be null");

		maybeCallbackBeforeConvertWithQueries(queries, index);

		List<IndexedObjectInformation> indexedObjectInformationList = doBulkOperation(queries, bulkOptions, index);

		maybeCallbackAfterSaveWithQueries(queries, index);

		return indexedObjectInformationList;
	}

	public abstract List<IndexedObjectInformation> doBulkOperation(List<?> queries, BulkOptions bulkOptions,
			IndexCoordinates index);

	@Override
	public <T> UpdateResponse update(T entity) {

		Assert.notNull(entity, "entity must not be null");

		return update(entity, getIndexCoordinatesFor(entity.getClass()));
	}

	@Override
	public <T> UpdateResponse update(T entity, IndexCoordinates index) {

		Assert.notNull(entity, "entity must not be null");
		Assert.notNull(index, "index must not be null");

		return update(buildUpdateQueryByEntity(entity), index);
	}

	protected <T> UpdateQuery buildUpdateQueryByEntity(T entity) {

		Assert.notNull(entity, "entity must not be null");

		String id = getEntityId(entity);
		Assert.notNull(id, "entity must have an id that is notnull");

		UpdateQuery.Builder updateQueryBuilder = UpdateQuery.builder(id)
				.withDocument(elasticsearchConverter.mapObject(entity));

		String routing = getEntityRouting(entity);
		if (StringUtils.hasText(routing)) {
			updateQueryBuilder.withRouting(routing);
		}

		return updateQueryBuilder.build();
	}

	// endregion

	protected <T> T updateIndexedObject(T entity, IndexedObjectInformation indexedObjectInformation) {

		ElasticsearchPersistentEntity<?> persistentEntity = elasticsearchConverter.getMappingContext()
				.getPersistentEntity(entity.getClass());

		if (persistentEntity != null) {
			PersistentPropertyAccessor<Object> propertyAccessor = persistentEntity.getPropertyAccessor(entity);
			ElasticsearchPersistentProperty idProperty = persistentEntity.getIdProperty();

			// Only deal with text because ES generated Ids are strings!
			if (indexedObjectInformation.getId() != null && idProperty != null && idProperty.isReadable()
					&& idProperty.getType().isAssignableFrom(String.class)) {
				propertyAccessor.setProperty(idProperty, indexedObjectInformation.getId());
			}

			if (indexedObjectInformation.getSeqNo() != null && indexedObjectInformation.getPrimaryTerm() != null
					&& persistentEntity.hasSeqNoPrimaryTermProperty()) {
				ElasticsearchPersistentProperty seqNoPrimaryTermProperty = persistentEntity.getSeqNoPrimaryTermProperty();
				// noinspection ConstantConditions
				propertyAccessor.setProperty(seqNoPrimaryTermProperty,
						new SeqNoPrimaryTerm(indexedObjectInformation.getSeqNo(), indexedObjectInformation.getPrimaryTerm()));
			}

			if (indexedObjectInformation.getVersion() != null && persistentEntity.hasVersionProperty()) {
				ElasticsearchPersistentProperty versionProperty = persistentEntity.getVersionProperty();
				// noinspection ConstantConditions
				propertyAccessor.setProperty(versionProperty, indexedObjectInformation.getVersion());
			}

			// noinspection unchecked
			return (T) propertyAccessor.getBean();
		}
		return entity;
	}

	// region SearchOperations
	@Override
	public long count(Query query, Class<?> clazz) {
		return count(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public <T> SearchHitsIterator<T> searchForStream(Query query, Class<T> clazz) {
		return searchForStream(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public <T> SearchHitsIterator<T> searchForStream(Query query, Class<T> clazz, IndexCoordinates index) {

		Duration scrollTime = query.getScrollTime() != null ? query.getScrollTime() : Duration.ofMinutes(1);
		long scrollTimeInMillis = scrollTime.toMillis();
		// noinspection ConstantConditions
		int maxCount = query.isLimiting() ? query.getMaxResults() : 0;

		return StreamQueries.streamResults( //
				maxCount, //
				searchScrollStart(scrollTimeInMillis, query, clazz, index), //
				scrollId -> searchScrollContinue(scrollId, scrollTimeInMillis, clazz, index), //
				this::searchScrollClear);
	}

	@Override
	public <T> SearchHits<T> search(MoreLikeThisQuery query, Class<T> clazz) {
		return search(query, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public <T> SearchHits<T> search(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(query.getId(), "No document id defined for MoreLikeThisQuery");

		return doSearch(query, clazz, index);
	}

	protected abstract <T> SearchHits<T> doSearch(MoreLikeThisQuery query, Class<T> clazz, IndexCoordinates index);

	@Override
	public <T> List<SearchHits<T>> multiSearch(List<? extends Query> queries, Class<T> clazz) {
		return multiSearch(queries, clazz, getIndexCoordinatesFor(clazz));
	}

	@Override
	public <T> SearchHits<T> search(Query query, Class<T> clazz) {
		return search(query, clazz, getIndexCoordinatesFor(clazz));
	}

	abstract public <T> SearchScrollHits<T> searchScrollStart(long scrollTimeInMillis, Query query, Class<T> clazz,
			IndexCoordinates index);

	abstract public <T> SearchScrollHits<T> searchScrollContinue(String scrollId, long scrollTimeInMillis, Class<T> clazz,
			IndexCoordinates index);

	public void searchScrollClear(String scrollId) {
		searchScrollClear(Collections.singletonList(scrollId));
	}

	// endregion

	public abstract void searchScrollClear(List<String> scrollIds);

	// region Helper methods
	@Override
	public ElasticsearchConverter getElasticsearchConverter() {

		Assert.notNull(elasticsearchConverter, "elasticsearchConverter is not initialized.");

		return elasticsearchConverter;
	}

	@Override
	public IndexCoordinates getIndexCoordinatesFor(Class<?> clazz) {
		return getRequiredPersistentEntity(clazz).getIndexCoordinates();
	}

	ElasticsearchPersistentEntity<?> getRequiredPersistentEntity(Class<?> clazz) {
		return elasticsearchConverter.getMappingContext().getRequiredPersistentEntity(clazz);
	}

	@Nullable
	private String getEntityId(Object entity) {

		Object id = entityOperations.forEntity(entity, elasticsearchConverter.getConversionService(), routingResolver)
				.getId();

		return convertId(id);
	}

	@Nullable
	public String getEntityRouting(Object entity) {
		return entityOperations.forEntity(entity, elasticsearchConverter.getConversionService(), routingResolver)
				.getRouting();
	}

	@Nullable
	private Long getEntityVersion(Object entity) {

		Number version = entityOperations.forEntity(entity, elasticsearchConverter.getConversionService(), routingResolver)
				.getVersion();

		if (version != null && Long.class.isAssignableFrom(version.getClass())) {
			return ((Long) version);
		}

		return null;
	}

	@Nullable
	private SeqNoPrimaryTerm getEntitySeqNoPrimaryTerm(Object entity) {

		EntityOperations.AdaptableEntity<Object> adaptableEntity = entityOperations.forEntity(entity,
				elasticsearchConverter.getConversionService(), routingResolver);
		return adaptableEntity.hasSeqNoPrimaryTerm() ? adaptableEntity.getSeqNoPrimaryTerm() : null;
	}

	private <T> IndexQuery getIndexQuery(T entity) {

		String id = getEntityId(entity);

		if (id != null) {
			id = elasticsearchConverter.convertId(id);
		}

		// noinspection ConstantConditions
		IndexQueryBuilder builder = new IndexQueryBuilder() //
				.withId(id) //
				.withObject(entity);

		SeqNoPrimaryTerm seqNoPrimaryTerm = getEntitySeqNoPrimaryTerm(entity);

		if (seqNoPrimaryTerm != null) {
			builder.withSeqNoPrimaryTerm(seqNoPrimaryTerm);
		}
		else {
			// version cannot be used together with seq_no and primary_term
			// noinspection ConstantConditions
			builder.withVersion(getEntityVersion(entity));
		}

		String routing = getEntityRouting(entity);

		if (routing != null) {
			builder.withRouting(routing);
		}

		return builder.build();
	}

	protected <T> SearchDocumentResponse.EntityCreator<T> getEntityCreator(ReadDocumentCallback<T> documentCallback) {
		return searchDocument -> CompletableFuture.completedFuture(documentCallback.doWith(searchDocument));
	}

	// endregion

	// region Entity callbacks
	protected <T> T maybeCallbackBeforeConvert(T entity, IndexCoordinates index) {

		if (entityCallbacks != null) {
			return entityCallbacks.callback(BeforeConvertCallback.class, entity, index);
		}

		return entity;
	}

	protected void maybeCallbackBeforeConvertWithQuery(Object query, IndexCoordinates index) {

		if (query instanceof IndexQuery) {
			IndexQuery indexQuery = (IndexQuery) query;
			Object queryObject = indexQuery.getObject();

			if (queryObject != null) {
				queryObject = maybeCallbackBeforeConvert(queryObject, index);
				indexQuery.setObject(queryObject);
				// the callback might have set som values relevant for the IndexQuery
				IndexQuery newQuery = getIndexQuery(queryObject);

				if (indexQuery.getRouting() == null && newQuery.getRouting() != null) {
					indexQuery.setRouting(newQuery.getRouting());
				}

				if (indexQuery.getSeqNo() == null && newQuery.getSeqNo() != null) {
					indexQuery.setSeqNo(newQuery.getSeqNo());
				}

				if (indexQuery.getPrimaryTerm() == null && newQuery.getPrimaryTerm() != null) {
					indexQuery.setPrimaryTerm(newQuery.getPrimaryTerm());
				}
			}
		}
	}

	// this can be called with either a List<IndexQuery> or a List<UpdateQuery>; these query classes
	// don't have a common base class, therefore the List<?> argument
	protected void maybeCallbackBeforeConvertWithQueries(List<?> queries, IndexCoordinates index) {
		queries.forEach(query -> maybeCallbackBeforeConvertWithQuery(query, index));
	}

	protected <T> T maybeCallbackAfterSave(T entity, IndexCoordinates index) {

		if (entityCallbacks != null) {
			return entityCallbacks.callback(AfterSaveCallback.class, entity, index);
		}

		return entity;
	}

	protected void maybeCallbackAfterSaveWithQuery(Object query, IndexCoordinates index) {

		if (query instanceof IndexQuery) {
			IndexQuery indexQuery = (IndexQuery) query;
			Object queryObject = indexQuery.getObject();

			if (queryObject != null) {
				queryObject = maybeCallbackAfterSave(queryObject, index);
				indexQuery.setObject(queryObject);
			}
		}
	}

	// this can be called with either a List<IndexQuery> or a List<UpdateQuery>; these query classes
	// don't have a common base class, therefore the List<?> argument
	protected void maybeCallbackAfterSaveWithQueries(List<?> queries, IndexCoordinates index) {
		queries.forEach(query -> maybeCallbackAfterSaveWithQuery(query, index));
	}

	protected <T> T maybeCallbackAfterConvert(T entity, Document document, IndexCoordinates index) {

		if (entityCallbacks != null) {
			return entityCallbacks.callback(AfterConvertCallback.class, entity, document, index);
		}

		return entity;
	}

	protected <T> Document maybeCallbackAfterLoad(Document document, Class<T> type, IndexCoordinates indexCoordinates) {

		if (entityCallbacks != null) {
			return entityCallbacks.callback(AfterLoadCallback.class, document, type, indexCoordinates);
		}

		return document;
	}

	// endregion

	protected void updateIndexedObjectsWithQueries(List<?> queries,
			List<IndexedObjectInformation> indexedObjectInformationList) {

		for (int i = 0; i < queries.size(); i++) {
			Object query = queries.get(i);

			if (query instanceof IndexQuery) {
				IndexQuery indexQuery = (IndexQuery) query;
				Object queryObject = indexQuery.getObject();

				if (queryObject != null) {
					indexQuery.setObject(updateIndexedObject(queryObject, indexedObjectInformationList.get(i)));
				}
			}
		}
	}

	// region customization
	private void setRoutingResolver(RoutingResolver routingResolver) {

		Assert.notNull(routingResolver, "routingResolver must not be null");

		this.routingResolver = routingResolver;
	}

	@Override
	public ElasticsearchOperations withRouting(RoutingResolver routingResolver) {

		Assert.notNull(routingResolver, "routingResolver must not be null");

		AbstractElasticsearchTemplate copy = copy();
		copy.setRoutingResolver(routingResolver);
		return copy;
	}

	@Override
	public ElasticsearchOperations withRefreshPolicy(@Nullable RefreshPolicy refreshPolicy) {

		AbstractElasticsearchTemplate copy = copy();
		copy.setRefreshPolicy(refreshPolicy);
		return copy;
	}

	// region Document callbacks
	protected interface DocumentCallback<T> {
		@Nullable
		T doWith(@Nullable Document document);
	}

	protected interface SearchDocumentResponseCallback<T> {
		@NonNull
		T doWith(@NonNull SearchDocumentResponse response);
	}
	// endregion

	protected class ReadDocumentCallback<T> implements DocumentCallback<T> {
		private final EntityReader<? super T, Document> reader;
		private final Class<T> type;
		private final IndexCoordinates index;

		public ReadDocumentCallback(EntityReader<? super T, Document> reader, Class<T> type, IndexCoordinates index) {

			Assert.notNull(reader, "reader is null");
			Assert.notNull(type, "type is null");

			this.reader = reader;
			this.type = type;
			this.index = index;
		}

		@Nullable
		public T doWith(@Nullable Document document) {

			if (document == null) {
				return null;
			}
			Document documentAfterLoad = maybeCallbackAfterLoad(document, type, index);

			T entity = reader.read(type, documentAfterLoad);

			IndexedObjectInformation indexedObjectInformation = new IndexedObjectInformation( //
					documentAfterLoad.hasId() ? documentAfterLoad.getId() : null, //
					documentAfterLoad.getIndex(), //
					documentAfterLoad.hasSeqNo() ? documentAfterLoad.getSeqNo() : null, //
					documentAfterLoad.hasPrimaryTerm() ? documentAfterLoad.getPrimaryTerm() : null, //
					documentAfterLoad.hasVersion() ? documentAfterLoad.getVersion() : null); //
			entity = updateIndexedObject(entity, indexedObjectInformation);

			return maybeCallbackAfterConvert(entity, documentAfterLoad, index);
		}
	}

	protected class ReadSearchDocumentResponseCallback<T> implements SearchDocumentResponseCallback<SearchHits<T>> {
		private final DocumentCallback<T> delegate;
		private final Class<T> type;

		public ReadSearchDocumentResponseCallback(Class<T> type, IndexCoordinates index) {

			Assert.notNull(type, "type is null");

			this.delegate = new ReadDocumentCallback<>(elasticsearchConverter, type, index);
			this.type = type;
		}

		@NonNull
		@Override
		public SearchHits<T> doWith(SearchDocumentResponse response) {
			List<T> entities = response.getSearchDocuments().stream().map(delegate::doWith).collect(Collectors.toList());
			return SearchHitMapping.mappingFor(type, elasticsearchConverter).mapHits(response, entities);
		}
	}

	protected class ReadSearchScrollDocumentResponseCallback<T>
			implements SearchDocumentResponseCallback<SearchScrollHits<T>> {
		private final DocumentCallback<T> delegate;
		private final Class<T> type;

		public ReadSearchScrollDocumentResponseCallback(Class<T> type, IndexCoordinates index) {

			Assert.notNull(type, "type is null");

			this.delegate = new ReadDocumentCallback<>(elasticsearchConverter, type, index);
			this.type = type;
		}

		@NonNull
		@Override
		public SearchScrollHits<T> doWith(SearchDocumentResponse response) {
			List<T> entities = response.getSearchDocuments().stream().map(delegate::doWith).collect(Collectors.toList());
			return SearchHitMapping.mappingFor(type, elasticsearchConverter).mapScrollHits(response, entities);
		}
	}

}
