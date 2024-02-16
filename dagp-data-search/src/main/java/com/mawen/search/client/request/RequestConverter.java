package com.mawen.search.client.request;

import co.elastic.clients.elasticsearch._types.*;
import co.elastic.clients.elasticsearch._types.mapping.FieldType;
import co.elastic.clients.elasticsearch._types.query_dsl.FieldAndFormat;
import co.elastic.clients.elasticsearch._types.query_dsl.Like;
import co.elastic.clients.elasticsearch.core.*;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.bulk.CreateOperation;
import co.elastic.clients.elasticsearch.core.bulk.IndexOperation;
import co.elastic.clients.elasticsearch.core.bulk.UpdateOperation;
import co.elastic.clients.elasticsearch.core.mget.MultiGetOperation;
import co.elastic.clients.elasticsearch.core.msearch.MultisearchBody;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.SourceConfig;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.RefreshRequest;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.json.JsonpMapper;
import com.mawen.search.InvalidApiUsageException;
import com.mawen.search.client.MultiSearchQueryParameter;
import com.mawen.search.client.query.CriteriaFilterProcessor;
import com.mawen.search.client.query.CriteriaQueryProcessor;
import com.mawen.search.client.query.NativeQuery;
import com.mawen.search.client.query.Queries;
import com.mawen.search.client.query.builder.HighlightQueryBuilder;
import com.mawen.search.client.util.TypeUtils;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.document.Document;
import com.mawen.search.core.domain.BulkOptions;
import com.mawen.search.core.domain.Order;
import com.mawen.search.core.domain.ScriptData;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.mapping.ElasticsearchPersistentEntity;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.*;
import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.core.support.ScriptType;
import com.mawen.search.support.DefaultStringObjectMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static com.mawen.search.client.util.TypeUtils.*;
import static org.springframework.util.CollectionUtils.isEmpty;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Slf4j
public class RequestConverter {

	public static final Integer INDEX_MAX_RESULT_WINDOW = 10_000;

	private final ElasticsearchConverter elasticsearchConverter;
	private final JsonpMapper jsonpMapper;

	public RequestConverter(ElasticsearchConverter elasticsearchConverter, JsonpMapper jsonpMapper) {
		this.elasticsearchConverter = elasticsearchConverter;

		Assert.notNull(jsonpMapper, "jsonpMapper must not be null");

		this.jsonpMapper = jsonpMapper;
	}

	// region Indices client

	public ExistsRequest indicesExistsRequest(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		return new ExistsRequest.Builder().index(Arrays.asList(indexCoordinates.getIndexNames())).build();
	}

	public RefreshRequest indicesRefreshRequest(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		return new RefreshRequest.Builder().index(Arrays.asList(indexCoordinates.getIndexNames())).build();
	}

	public DeleteIndexRequest indicesDeleteRequest(IndexCoordinates indexCoordinates) {

		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		return new DeleteIndexRequest.Builder().index(Arrays.asList(indexCoordinates.getIndexNames())).build();
	}


	// endregion

	// region documents

	@Nullable
	static Boolean deleteByQueryRefresh(@Nullable RefreshPolicy refreshPolicy) {

		if (refreshPolicy == null) {
			return null;
		}

		switch (refreshPolicy) {
			case IMMEDIATE:
				return true;
			case WAIT_UNTIL:
				return null;
			case NONE:
				return false;
		}

		return null;
	}

	public IndexRequest<?> documentIndexRequest(IndexQuery query, IndexCoordinates indexCoordinates,
			@Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		IndexRequest.Builder<Object> builder = new IndexRequest.Builder<>();

		builder.index(query.getIndexName() != null ? query.getIndexName() : indexCoordinates.getIndexName());

		Object queryObject = query.getObject();

		if (queryObject != null) {
			String id = StringUtils.hasText(query.getId()) ? query.getId() : getPersistentEntityId(queryObject);
			builder //
					.id(id) //
					.document(elasticsearchConverter.mapObject(queryObject));
		}
		else if (query.getSource() != null) {
			builder //
					.id(query.getId()) //
					.document(new DefaultStringObjectMap<>().fromJson(query.getSource()));
		}
		else {
			throw new InvalidApiUsageException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}

		if (query.getVersion() != null) {
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(queryObject != null ? queryObject.getClass() : null);
			builder.version(query.getVersion()).versionType(versionType);
		}

		builder //
				.ifSeqNo(query.getSeqNo()) //
				.ifPrimaryTerm(query.getPrimaryTerm()) //
				.routing(query.getRouting()); //

		if (query.getOpType() != null) {
			switch (query.getOpType()) {
				case INDEX:
					builder.opType(OpType.Index);
				case CREATE:
					builder.opType(OpType.Create);
			}
		}

		builder.refresh(refresh(refreshPolicy));

		return builder.build();
	}

	private IndexOperation<?> bulkIndexOperation(IndexQuery query, IndexCoordinates indexCoordinates) {

		IndexOperation.Builder<Object> builder = new IndexOperation.Builder<>();

		builder.index(query.getIndexName() != null ? query.getIndexName() : indexCoordinates.getIndexName());

		Object queryObject = query.getObject();

		if (queryObject != null) {
			String id = StringUtils.hasText(query.getId()) ? query.getId() : getPersistentEntityId(queryObject);
			builder //
					.id(id) //
					.document(elasticsearchConverter.mapObject(queryObject));
		}
		else if (query.getSource() != null) {
			builder.document(new DefaultStringObjectMap<>().fromJson(query.getSource()));
		}
		else {
			throw new InvalidApiUsageException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}

		if (query.getVersion() != null) {
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(queryObject != null ? queryObject.getClass() : null);
			builder.version(query.getVersion()).versionType(versionType);
		}

		builder //
				.ifSeqNo(query.getSeqNo()) //
				.ifPrimaryTerm(query.getPrimaryTerm()) //
				.routing(query.getRouting()); //

		return builder.build();
	}

	private CreateOperation<?> bulkCreateOperation(IndexQuery query, IndexCoordinates indexCoordinates) {

		CreateOperation.Builder<Object> builder = new CreateOperation.Builder<>();

		builder.index(query.getIndexName() != null ? query.getIndexName() : indexCoordinates.getIndexName());

		Object queryObject = query.getObject();

		if (queryObject != null) {
			String id = StringUtils.hasText(query.getId()) ? query.getId() : getPersistentEntityId(queryObject);
			builder //
					.id(id) //
					.document(elasticsearchConverter.mapObject(queryObject));
		}
		else if (query.getSource() != null) {
			builder.document(new DefaultStringObjectMap<>().fromJson(query.getSource()));
		}
		else {
			throw new InvalidApiUsageException(
					"object or source is null, failed to index the document [id: " + query.getId() + ']');
		}

		if (query.getVersion() != null) {
			VersionType versionType = retrieveVersionTypeFromPersistentEntity(queryObject != null ? queryObject.getClass() : null);
			builder.version(query.getVersion()).versionType(versionType);
		}

		builder //
				.ifSeqNo(query.getSeqNo()) //
				.ifPrimaryTerm(query.getPrimaryTerm()) //
				.routing(query.getRouting()); //

		return builder.build();
	}

	private UpdateOperation<?, ?> bulkUpdateOperation(UpdateQuery query, IndexCoordinates index,
			@Nullable RefreshPolicy refreshPolicy) {

		UpdateOperation.Builder<Object, Object> uob = new UpdateOperation.Builder<>();
		String indexName = query.getIndexName() != null ? query.getIndexName() : index.getIndexName();

		uob.index(indexName).id(query.getId());
		uob.action(a -> {
			a //
					.script(getScript(query.getScriptData())) //
					.doc(query.getDocument()) //
					.upsert(query.getUpsert()) //
					.scriptedUpsert(query.getScriptedUpsert()) //
					.docAsUpsert(query.getDocAsUpsert()) //
			;

			if (query.getFetchSource() != null) {
				a.source(sc -> sc.fetch(query.getFetchSource()));
			}

			if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
				List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
						: Collections.emptyList();
				List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
						: Collections.emptyList();
				a.source(sc -> sc.filter(sf -> sf.includes(includes).excludes(excludes)));
			}

			return a;
		});

		uob //
				.routing(query.getRouting()) //
				.ifSeqNo(query.getIfSeqNo() != null ? Long.valueOf(query.getIfSeqNo()) : null) //
				.ifPrimaryTerm(query.getIfPrimaryTerm() != null ? Long.valueOf(query.getIfPrimaryTerm()) : null) //
				.retryOnConflict(query.getRetryOnConflict()) //
		;

		// no refresh, timeout, waitForActiveShards on UpdateOperation or UpdateAction

		return uob.build();
	}

	@Nullable
	private co.elastic.clients.elasticsearch._types.Script getScript(@Nullable ScriptData scriptData) {

		if (scriptData == null) {
			return null;
		}

		Map<String, JsonData> params = new HashMap<>();

		if (scriptData.getParams() != null) {
			scriptData.getParams().forEach((key, value) -> params.put(key, JsonData.of(value, jsonpMapper)));
		}
		return co.elastic.clients.elasticsearch._types.Script.of(sb -> {
			if (scriptData.getType() == ScriptType.INLINE) {
				sb.inline(is -> is //
						.lang(scriptData.getLanguage()) //
						.source(scriptData.getScript()) //
						.params(params)); //
			}
			return sb;
		});
	}

	public BulkRequest documentBulkRequest(List<?> queries, BulkOptions bulkOptions, IndexCoordinates indexCoordinates,
			@Nullable RefreshPolicy refreshPolicy) {

		BulkRequest.Builder builder = new BulkRequest.Builder();

		if (bulkOptions.getTimeout() != null) {
			builder.timeout(tb -> tb.time(bulkOptions.getTimeout().toMillis() + "ms"));
		}

		builder.refresh(refresh(refreshPolicy));
		if (bulkOptions.getRefreshPolicy() != null) {
			builder.refresh(refresh(bulkOptions.getRefreshPolicy()));
		}

		if (bulkOptions.getWaitForActiveShards() != null) {
			builder.waitForActiveShards(wasb -> wasb.count(bulkOptions.getWaitForActiveShards().getValue()));
		}

		if (bulkOptions.getPipeline() != null) {
			builder.pipeline(bulkOptions.getPipeline());
		}

		if (bulkOptions.getRoutingId() != null) {
			builder.routing(bulkOptions.getRoutingId());
		}

		List<BulkOperation> operations = queries.stream().map(query -> {
			BulkOperation.Builder ob = new BulkOperation.Builder();
			if (query instanceof IndexQuery) {
				IndexQuery indexQuery = (IndexQuery) query;
				if (indexQuery.getOpType() == IndexQuery.OpType.CREATE) {
					ob.create(bulkCreateOperation(indexQuery, indexCoordinates));
				}
				else {
					ob.index(bulkIndexOperation(indexQuery, indexCoordinates));
				}
			}
			else if (query instanceof UpdateQuery) {
				UpdateQuery updateQuery = (UpdateQuery) query;
				ob.update(bulkUpdateOperation(updateQuery, indexCoordinates, refreshPolicy));
			}
			return ob.build();
		}).collect(Collectors.toList());

		builder.operations(operations);

		return builder.build();
	}

	public GetRequest documentGetRequest(String id, @Nullable String routing, IndexCoordinates indexCoordinates) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		return GetRequest.of(grb -> grb //
				.index(indexCoordinates.getIndexName()) //
				.id(id) //
				.routing(routing));
	}

	public co.elastic.clients.elasticsearch.core.ExistsRequest documentExistsRequest(String id, @Nullable String routing,
			IndexCoordinates indexCoordinates) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		return co.elastic.clients.elasticsearch.core.ExistsRequest.of(erb -> erb
				.index(indexCoordinates.getIndexName())
				.id(id)
				.routing(routing));
	}

	public <T> MgetRequest documentMgetRequest(Query query, Class<T> clazz, IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(clazz, "clazz must not be null");
		Assert.notNull(index, "index must not be null");

		if (query.getIdsWithRouting().isEmpty()) {
			throw new IllegalArgumentException("query does not contain any ids");
		}

		elasticsearchConverter.updateQuery(query, clazz); // to get the SourceConfig right

		SourceConfig sourceConfig = getSourceConfig(query);

		List<MultiGetOperation> multiGetOperations = query.getIdsWithRouting().stream()
				.map(idWithRouting -> MultiGetOperation.of(mgo -> mgo //
						.index(index.getIndexName()) //
						.id(idWithRouting.getId()) //
						.routing(idWithRouting.getRouting()) //
						.source(sourceConfig)))
				.collect(Collectors.toList());

		return MgetRequest.of(mg -> mg//
				.docs(multiGetOperations));
	}

	public DeleteRequest documentDeleteRequest(String id, @Nullable String routing, IndexCoordinates index,
			@Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(index, "index must not be null");

		return DeleteRequest.of(r -> {
			r.id(id).index(index.getIndexName());

			if (routing != null) {
				r.routing(routing);
			}
			r.refresh(refresh(refreshPolicy));
			return r;
		});
	}

	public DeleteByQueryRequest documentDeleteByQueryRequest(Query query, @Nullable String routing, Class<?> clazz,
			IndexCoordinates index, @Nullable RefreshPolicy refreshPolicy) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		return DeleteByQueryRequest.of(b -> {
			b.index(Arrays.asList(index.getIndexNames())) //
					.query(getQuery(query, clazz))//
					.refresh(deleteByQueryRefresh(refreshPolicy));

			if (query.isLimiting()) {
				b.maxDocs(Long.valueOf(query.getMaxResults()));
			}

			b.scroll(time(query.getScrollTime()));

			if (query.getRoute() != null) {
				b.routing(query.getRoute());
			}
			else if (StringUtils.hasText(routing)) {
				b.routing(routing);
			}

			return b;
		});
	}

	public UpdateRequest<Document, ?> documentUpdateRequest(UpdateQuery query, IndexCoordinates index,
			@Nullable RefreshPolicy refreshPolicy, @Nullable String routing) {

		String indexName = query.getIndexName() != null ? query.getIndexName() : index.getIndexName();
		return UpdateRequest.of(uqb -> {
					uqb.index(indexName).id(query.getId());

					if (query.getScriptData() != null) {
						Map<String, JsonData> params = new HashMap<>();

						if (query.getScriptData().getParams() != null) {
							query.getScriptData().getParams().forEach((key, value) -> params.put(key, JsonData.of(value, jsonpMapper)));
						}

						uqb.script(sb -> {
									if (query.getScriptData().getType() == ScriptType.INLINE) {
										sb.inline(is -> is //
												.lang(query.getScriptData().getLanguage()) //
												.source(query.getScriptData().getScript()) //
												.params(params)); //
									}
									return sb;
								}

						);
					}

					uqb //
							.doc(query.getDocument()) //
							.upsert(query.getUpsert()) //
							.routing(query.getRouting() != null ? query.getRouting() : routing) //
							.scriptedUpsert(query.getScriptedUpsert()) //
							.docAsUpsert(query.getDocAsUpsert()) //
							.ifSeqNo(query.getIfSeqNo() != null ? Long.valueOf(query.getIfSeqNo()) : null) //
							.ifPrimaryTerm(query.getIfPrimaryTerm() != null ? Long.valueOf(query.getIfPrimaryTerm()) : null) //
							.refresh(query.getRefreshPolicy() != null ? refresh(query.getRefreshPolicy()) : refresh(refreshPolicy)) //
							.retryOnConflict(query.getRetryOnConflict()) //
					;

					if (query.getFetchSource() != null) {
						uqb.source(sc -> sc.fetch(query.getFetchSource()));
					}

					if (query.getFetchSourceIncludes() != null || query.getFetchSourceExcludes() != null) {
						List<String> includes = query.getFetchSourceIncludes() != null ? query.getFetchSourceIncludes()
								: Collections.emptyList();
						List<String> excludes = query.getFetchSourceExcludes() != null ? query.getFetchSourceExcludes()
								: Collections.emptyList();
						uqb.source(sc -> sc.filter(sf -> sf.includes(includes).excludes(excludes)));
					}

					if (query.getTimeout() != null) {
						uqb.timeout(tv -> tv.time(query.getTimeout()));
					}

					String waitForActiveShards = query.getWaitForActiveShards();
					if (waitForActiveShards != null) {
						if ("all".equalsIgnoreCase(waitForActiveShards)) {
							uqb.waitForActiveShards(wfa -> wfa.option(WaitForActiveShardOptions.All));
						}
						else {
							int val;
							try {
								val = Integer.parseInt(waitForActiveShards);
							}
							catch (NumberFormatException e) {
								throw new IllegalArgumentException("cannot parse ActiveShardCount[" + waitForActiveShards + ']', e);
							}
							uqb.waitForActiveShards(wfa -> wfa.count(val));
						}
					}

					return uqb;
				} //
		);
	}

	// endregion

	// region search

	public UpdateByQueryRequest documentUpdateByQueryRequest(UpdateQuery updateQuery, IndexCoordinates index,
			@Nullable RefreshPolicy refreshPolicy) {

		return UpdateByQueryRequest.of(ub -> {
			ub //
					.index(Arrays.asList(index.getIndexNames())) //
					.refresh(refreshPolicy == RefreshPolicy.IMMEDIATE) //
					.routing(updateQuery.getRouting()) //
					.script(getScript(updateQuery.getScriptData())) //
					.maxDocs(updateQuery.getMaxDocs() != null ? Long.valueOf(updateQuery.getMaxDocs()) : null) //
					.pipeline(updateQuery.getPipeline()) //
					.requestsPerSecond(updateQuery.getRequestsPerSecond()) //
					.slices(slices(updateQuery.getSlices() != null ? Long.valueOf(updateQuery.getSlices()) : null));

			if (updateQuery.getAbortOnVersionConflict() != null) {
				ub.conflicts(updateQuery.getAbortOnVersionConflict() ? Conflicts.Abort : Conflicts.Proceed);
			}

			if (updateQuery.getQuery() != null) {
				Query queryQuery = updateQuery.getQuery();

				if (updateQuery.getBatchSize() != null) {
					((BaseQuery) queryQuery).setMaxResults(updateQuery.getBatchSize());
				}
				ub.query(getQuery(queryQuery, null));

				// no indicesOptions available like in old client

				ub.scroll(time(queryQuery.getScrollTime()));

			}

			// no maxRetries available like in old client
			// no shouldStoreResult

			if (updateQuery.getRefreshPolicy() != null) {
				ub.refresh(updateQuery.getRefreshPolicy() == RefreshPolicy.IMMEDIATE);
			}

			if (updateQuery.getTimeout() != null) {
				ub.timeout(tb -> tb.time(updateQuery.getTimeout()));
			}

			if (updateQuery.getWaitForActiveShards() != null) {
				ub.waitForActiveShards(w -> w.count(waitForActiveShardsCount(updateQuery.getWaitForActiveShards())));
			}

			return ub;
		});
	}

	public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
			IndexCoordinates indexCoordinates, boolean forCount) {
		return searchRequest(query, routing, clazz, indexCoordinates, forCount, false, null);
	}

	public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
			IndexCoordinates indexCoordinates, boolean forCount, long scrollTimeInMillis) {
		return searchRequest(query, routing, clazz, indexCoordinates, forCount, true, scrollTimeInMillis);
	}

	public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
			IndexCoordinates indexCoordinates, boolean forCount, boolean forBatchedSearch) {
		return searchRequest(query, routing, clazz, indexCoordinates, forCount, forBatchedSearch, null);
	}

	public <T> SearchRequest searchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
			IndexCoordinates indexCoordinates, boolean forCount, boolean forBatchedSearch,
			@Nullable Long scrollTimeInMillis) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(indexCoordinates, "indexCoordinates must not be null");

		elasticsearchConverter.updateQuery(query, clazz);
		SearchRequest.Builder builder = new SearchRequest.Builder();
		prepareSearchRequest(query, routing, clazz, indexCoordinates, builder, forCount, forBatchedSearch);

		if (scrollTimeInMillis != null) {
			builder.scroll(t -> t.time(scrollTimeInMillis + "ms"));
		}

		builder.query(getQuery(query, clazz));

		if (StringUtils.hasText(query.getRoute())) {
			builder.routing(query.getRoute());
		}
		if (StringUtils.hasText(routing)) {
			builder.routing(routing);
		}

		addFilter(query, builder);

		return builder.build();
	}

	public MsearchRequest searchMsearchRequest(
			List<MultiSearchQueryParameter> multiSearchQueryParameters, @Nullable String routing) {

		// basically the same stuff as in prepareSearchRequest, but the new Elasticsearch has different builders for a
		// normal search and msearch
		return MsearchRequest.of(mrb -> {
			multiSearchQueryParameters.forEach(param -> {
				ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(param.getClazz());

				Query query = param.getQuery();
				mrb.searches(sb -> sb //
						.header(h -> {
							SearchType searchType = (query instanceof NativeQuery && ((NativeQuery) query).getKnnQuery() != null) ? null
									: searchType(query.getSearchType());

							h //
									.index(Arrays.asList(param.getIndex().getIndexNames())) //
									.searchType(searchType) //
							;

							if (StringUtils.hasText(query.getRoute())) {
								h.routing(query.getRoute());
							}
							else if (StringUtils.hasText(routing)) {
								h.routing(routing);
							}

							return h;
						}) //
						.body(bb -> {
									bb //
											.query(getQuery(query, param.getClazz()))//
											.seqNoPrimaryTerm(persistentEntity != null ? persistentEntity.hasSeqNoPrimaryTermProperty() : null) //
											.version(true) //
											.trackScores(query.getTrackScores()) //
											.source(getSourceConfig(query)) //
											.timeout(timeStringMs(query.getTimeout())) //
									;

									if (query.getPageable().isPaged()) {
										bb //
												.from((int) query.getPageable().getOffset()) //
												.size(query.getPageable().getPageSize());
									}

									if (!isEmpty(query.getFields())) {
										bb.fields(fb -> {
											query.getFields().forEach(fb::field);
											return fb;
										});
									}

									if (query.isLimiting()) {
										bb.size(query.getMaxResults());
									}

									if (query.getMinScore() > 0) {
										bb.minScore((double) query.getMinScore());
									}

									if (query.getSort() != null) {
										List<SortOptions> sortOptions = getSortOptions(query.getSort(), persistentEntity);

										if (!sortOptions.isEmpty()) {
											bb.sort(sortOptions);
										}
									}

									addHighlight(query, bb);

									if (!isEmpty(query.getSearchAfter())) {
										bb.searchAfter(query.getSearchAfter().stream().map(TypeUtils::toFieldValue).collect(Collectors.toList()))
										;
									}

									if (query instanceof NativeQuery) {
										NativeQuery nativeQuery = (NativeQuery) query;
										prepareNativeSearch(nativeQuery, bb);
									}
									return bb;
								} //
						) //
				);

			});

			return mrb;
		});
	}

	private <T> void prepareSearchRequest(Query query, @Nullable String routing, @Nullable Class<T> clazz,
			IndexCoordinates indexCoordinates, SearchRequest.Builder builder, boolean forCount, boolean forBatchedSearch) {

		String[] indexNames = indexCoordinates.getIndexNames();

		Assert.notEmpty(indexNames, "indexCoordinates does not contain entries");

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(clazz);

		SearchType searchType = (query instanceof NativeQuery && ((NativeQuery) query).getKnnQuery() != null) ? null
				: searchType(query.getSearchType());

		builder //
				.version(true) //
				.trackScores(query.getTrackScores()) //
				.allowNoIndices(query.getAllowNoIndices()) //
				.source(getSourceConfig(query)) //
				.searchType(searchType) //
				.timeout(timeStringMs(query.getTimeout())) //
		;


		builder.index(Arrays.asList(indexNames));

		if (query.getRoute() != null) {
			builder.routing(query.getRoute());
		}
		else if (StringUtils.hasText(routing)) {
			builder.routing(routing);
		}

		if (persistentEntity != null && persistentEntity.hasSeqNoPrimaryTermProperty()) {
			builder.seqNoPrimaryTerm(true);
		}

		if (query.getPageable().isPaged()) {
			builder //
					.from((int) query.getPageable().getOffset()) //
					.size(query.getPageable().getPageSize());
		}
		else {
			builder.from(0).size(INDEX_MAX_RESULT_WINDOW);
		}

		if (!isEmpty(query.getFields())) {
			List<FieldAndFormat> fieldAndFormats = query.getFields().stream().map(field -> FieldAndFormat.of(b -> b.field(field))).collect(Collectors.toList());
			builder.fields(fieldAndFormats);
		}

		if (query.isLimiting()) {
			builder.size(query.getMaxResults());
		}

		if (query.getMinScore() > 0) {
			builder.minScore((double) query.getMinScore());
		}

		if (query.getSort() != null) {
			List<SortOptions> sortOptions = getSortOptions(query.getSort(), persistentEntity);

			if (!sortOptions.isEmpty()) {
				builder.sort(sortOptions);
			}
		}

		addHighlight(query, builder);

		if (query instanceof NativeQuery) {
			NativeQuery nativeQuery = (NativeQuery) query;
			prepareNativeSearch(nativeQuery, builder);
		}

		if (query.getTrackTotalHits() != null) {
			// logic from the RHLC, choose between -1 and Integer.MAX_VALUE
			int value = query.getTrackTotalHits() ? Integer.MAX_VALUE : -1;
			builder.trackTotalHits(th -> th.count(value));
		}
		else if (query.getTrackTotalHitsUpTo() != null) {
			builder.trackTotalHits(th -> th.count(query.getTrackTotalHitsUpTo()));
		}

		if (!isEmpty(query.getSearchAfter())) {
			builder.searchAfter(query.getSearchAfter().stream().map(TypeUtils::toFieldValue).collect(Collectors.toList()));
		}

		if (forCount) {
			builder.size(0) //
					.trackTotalHits(th -> th.count(Integer.MAX_VALUE)) //
					.source(SourceConfig.of(sc -> sc.fetch(false)));
		}
		else if (forBatchedSearch) {
			// request_cache is not allowed on scroll requests.
			builder.requestCache(null);
			// limit the number of documents in a batch if not already set in a pageable
			if (query.getPageable().isUnpaged()) {
				builder.size(query.getReactiveBatchSize());
			}
		}

	}

	public OpenPointInTimeRequest searchOpenPointInTimeRequest(IndexCoordinates index, Duration keepAlive) {

		Assert.notNull(index, "index must not be null");
		Assert.notNull(keepAlive, "keepAlive must not be null");

		return OpenPointInTimeRequest.of(opit -> opit //
				.index(Arrays.asList(index.getIndexNames())) //
				.keepAlive(time(keepAlive)) //
		);
	}

	public ClosePointInTimeRequest searchClosePointInTime(String pit) {

		Assert.notNull(pit, "pit must not be null");

		return ClosePointInTimeRequest.of(cpit -> cpit.id(pit));
	}

	private void addHighlight(Query query, SearchRequest.Builder builder) {

		Highlight highlight = query.getHighlightQuery()
				.map(highlightQuery -> new HighlightQueryBuilder(elasticsearchConverter.getMappingContext())
						.getHighlight(highlightQuery.getHighlight(), highlightQuery.getType()))
				.orElse(null);

		builder.highlight(highlight);
	}

	private void addHighlight(Query query, MultisearchBody.Builder builder) {

		Highlight highlight = query.getHighlightQuery()
				.map(highlightQuery -> new HighlightQueryBuilder(elasticsearchConverter.getMappingContext())
						.getHighlight(highlightQuery.getHighlight(), highlightQuery.getType()))
				.orElse(null);

		builder.highlight(highlight);
	}

	private List<SortOptions> getSortOptions(Sort sort, @Nullable ElasticsearchPersistentEntity<?> persistentEntity) {
		return sort.stream().map(order -> getSortOptions(order, persistentEntity)).collect(Collectors.toList());
	}

	private SortOptions getSortOptions(Sort.Order order, @Nullable ElasticsearchPersistentEntity<?> persistentEntity) {
		SortOrder sortOrder = order.getDirection().isDescending() ? SortOrder.Desc : SortOrder.Asc;

		Order.Mode mode = order.getDirection().isAscending() ? Order.Mode.min : Order.Mode.max;
		String unmappedType = null;
		String missing = null;
		NestedSortValue nestedSortValue = null;

		if (SortOptions.Kind.Score.jsonValue().equals(order.getProperty())) {
			return SortOptions.of(so -> so.score(s -> s.order(sortOrder)));
		}

		if (order instanceof Order) {
			Order o = (Order) order;
			if (o.getMode() != null) {
				mode = o.getMode();
			}
			unmappedType = o.getUnmappedType();
			missing = o.getMissing();
			nestedSortValue = getNestedSort(o.getNested(), persistentEntity);
		}
		Order.Mode finalMode = mode;
		String finalUnmappedType = unmappedType;
		NestedSortValue finalNestedSortValue = nestedSortValue;

		ElasticsearchPersistentProperty property = (persistentEntity != null) //
				? persistentEntity.getPersistentProperty(order.getProperty()) //
				: null;
		String fieldName = property != null ? property.getFieldName() : order.getProperty();

		String finalMissing = missing != null ? missing
				: (order.getNullHandling() == Sort.NullHandling.NULLS_FIRST) ? "_first"
				: ((order.getNullHandling() == Sort.NullHandling.NULLS_LAST) ? "_last" : null);

		return SortOptions.of(so -> so //
				.field(f -> {
					f.field(fieldName) //
							.order(sortOrder) //
							.mode(sortMode(finalMode));

					if (finalUnmappedType != null) {
						FieldType fieldType = fieldType(finalUnmappedType);

						if (fieldType != null) {
							f.unmappedType(fieldType);
						}
					}

					if (finalMissing != null) {
						f.missing(fv -> fv //
								.stringValue(finalMissing));
					}

					if (finalNestedSortValue != null) {
						f.nested(finalNestedSortValue);
					}

					return f;
				}));
	}

	@Nullable
	private NestedSortValue getNestedSort(@Nullable Order.Nested nested,
			@Nullable ElasticsearchPersistentEntity<?> persistentEntity) {
		return (nested == null || persistentEntity == null) ? null
				: NestedSortValue.of(b -> b //
				.path(elasticsearchConverter.updateFieldNames(nested.getPath(), persistentEntity)) //
				.maxChildren(nested.getMaxChildren()) //
				.nested(getNestedSort(nested.getNested(), persistentEntity)) //
				.filter(getQuery(nested.getFilter(), persistentEntity.getType())));
	}

	@SuppressWarnings("DuplicatedCode")
	private void prepareNativeSearch(NativeQuery query, SearchRequest.Builder builder) {

		builder //
				.suggest(query.getSuggester()) //
				.collapse(query.getFieldCollapse()) //
				.sort(query.getSortOptions()) //
		;

		if (query.getKnnQuery() != null) {
			builder.knn(query.getKnnQuery());
		}

		if (!isEmpty(query.getAggregations())) {
			builder.aggregations(query.getAggregations());
		}

		if (!isEmpty(query.getSearchExtensions())) {
			builder.ext(query.getSearchExtensions());
		}
	}

	@SuppressWarnings("DuplicatedCode")
	private void prepareNativeSearch(NativeQuery query, MultisearchBody.Builder builder) {

		builder //
				.suggest(query.getSuggester()) //
				.collapse(query.getFieldCollapse()) //
				.sort(query.getSortOptions());

		if (query.getKnnQuery() != null) {
			builder.knn(query.getKnnQuery());
		}

		if (!isEmpty(query.getAggregations())) {
			builder.aggregations(query.getAggregations());
		}

		if (!isEmpty(query.getSearchExtensions())) {
			builder.ext(query.getSearchExtensions());
		}
	}

	@Nullable
	private co.elastic.clients.elasticsearch._types.query_dsl.Query getQuery(@Nullable Query query,
			@Nullable Class<?> clazz) {

		if (query == null) {
			return null;
		}

		elasticsearchConverter.updateQuery(query, clazz);

		co.elastic.clients.elasticsearch._types.query_dsl.Query esQuery = null;

		if (query instanceof CriteriaQuery) {
			esQuery = CriteriaQueryProcessor.createQuery(((CriteriaQuery) query).getCriteria());
		}
		else if (query instanceof StringQuery) {
			esQuery = Queries.wrapperQueryAsQuery(((StringQuery) query).getSource());
		}
		else if (query instanceof NativeQuery) {
			NativeQuery nativeQuery = (NativeQuery) query;
			if (nativeQuery.getQuery() != null) {
				esQuery = nativeQuery.getQuery();
			}
			else if (nativeQuery.getSpringDataQuery() != null) {
				esQuery = getQuery(nativeQuery.getSpringDataQuery(), clazz);
			}
		}
		else {
			throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
		}

		return esQuery;
	}

	private void addFilter(Query query, SearchRequest.Builder builder) {

		if (query instanceof CriteriaQuery) {
			CriteriaFilterProcessor.createQuery(((CriteriaQuery) query).getCriteria()).ifPresent(builder::postFilter);
		}
		else // noinspection StatementWithEmptyBody
			if (query instanceof StringQuery) {
				// no filter for StringQuery
			}
			else if (query instanceof NativeQuery) {
				builder.postFilter(((NativeQuery) query).getFilter());
			}
			else {
				throw new IllegalArgumentException("unhandled Query implementation " + query.getClass().getName());
			}
	}

	// endregion

	// region helper functions

	public co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery moreLikeThisQuery(MoreLikeThisQuery query,
			IndexCoordinates index) {

		Assert.notNull(query, "query must not be null");
		Assert.notNull(index, "index must not be null");

		return co.elastic.clients.elasticsearch._types.query_dsl.MoreLikeThisQuery
				.of(q -> {
					q.like(Like.of(l -> l.document(ld -> ld.index(index.getIndexName()).id(query.getId()))))
							.fields(query.getFields());

					if (query.getMinTermFreq() != null) {
						q.minTermFreq(query.getMinTermFreq());
					}

					if (query.getMaxQueryTerms() != null) {
						q.maxQueryTerms(query.getMaxQueryTerms());
					}

					if (!isEmpty(query.getStopWords())) {
						q.stopWords(query.getStopWords());
					}

					if (query.getMinDocFreq() != null) {
						q.minDocFreq(query.getMinDocFreq());
					}

					if (query.getMaxDocFreq() != null) {
						q.maxDocFreq(query.getMaxDocFreq());
					}

					if (query.getMinWordLen() != null) {
						q.minWordLength(query.getMinWordLen());
					}

					if (query.getMaxWordLen() != null) {
						q.maxWordLength(query.getMaxWordLen());
					}

					if (query.getBoostTerms() != null) {
						q.boostTerms(Double.valueOf(query.getBoostTerms()));
					}

					return q;
				});
	}

	@Nullable
	private ElasticsearchPersistentEntity<?> getPersistentEntity(Object entity) {
		return elasticsearchConverter.getMappingContext().getPersistentEntity(entity.getClass());
	}

	@Nullable
	private ElasticsearchPersistentEntity<?> getPersistentEntity(@Nullable Class<?> clazz) {
		return clazz != null ? elasticsearchConverter.getMappingContext().getPersistentEntity(clazz) : null;
	}

	@Nullable
	private String getPersistentEntityId(Object entity) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(entity);

		if (persistentEntity != null) {
			Object identifier = persistentEntity //
					.getIdentifierAccessor(entity).getIdentifier();

			if (identifier != null) {
				return identifier.toString();
			}
		}

		return null;
	}

	@Nullable
	private SourceConfig getSourceConfig(Query query) {

		if (query.getSourceFilter() != null) {
			return SourceConfig.of(s -> s //
					.filter(sfb -> {
						SourceFilter sourceFilter = query.getSourceFilter();
						String[] includes = sourceFilter.getIncludes();
						String[] excludes = sourceFilter.getExcludes();

						if (includes != null) {
							sfb.includes(Arrays.asList(includes));
						}

						if (excludes != null) {
							sfb.excludes(Arrays.asList(excludes));
						}

						return sfb;
					}));
		}
		else {
			return null;
		}
	}

	private VersionType retrieveVersionTypeFromPersistentEntity(@Nullable Class<?> clazz) {

		ElasticsearchPersistentEntity<?> persistentEntity = getPersistentEntity(clazz);

		VersionType versionType = null;

		if (persistentEntity != null) {
			com.mawen.search.core.annotation.Document.VersionType entityVersionType = persistentEntity
					.getVersionType();

			if (entityVersionType != null) {
				switch (entityVersionType) {
					case INTERNAL:
						versionType =VersionType.Internal;
						break;
					case EXTERNAL:
						versionType = VersionType.External;
						break;
					case EXTERNAL_GTE:
						versionType =VersionType.ExternalGte;
						break;
					case FORCE:
						versionType = VersionType.Force;
						break;
				};
			}
		}

		return versionType != null ? versionType : VersionType.External;
	}

}
