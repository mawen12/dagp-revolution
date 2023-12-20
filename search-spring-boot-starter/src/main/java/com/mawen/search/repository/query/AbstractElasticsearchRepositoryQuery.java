package com.mawen.search.repository.query;

import java.util.Collections;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.domain.SearchHitSupport;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.domain.SearchHitsImpl;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.TotalHitsRelation;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public abstract class AbstractElasticsearchRepositoryQuery implements RepositoryQuery {

	protected static final int DEFAULT_STREAM_BATCH_SIZE = 500;
	protected final ElasticsearchOperations elasticsearchOperations;
	protected final ElasticsearchConverter elasticsearchConverter;
	protected ElasticsearchQueryMethod queryMethod;

	protected AbstractElasticsearchRepositoryQuery(ElasticsearchQueryMethod queryMethod,
			ElasticsearchOperations elasticsearchOperations) {
		this.queryMethod = queryMethod;
		this.elasticsearchOperations = elasticsearchOperations;
		this.elasticsearchConverter = elasticsearchOperations.getElasticsearchConverter();
	}

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	public abstract boolean isCountQuery();

	protected abstract boolean isDeleteQuery();

	protected abstract boolean isExistsQuery();

	@Override
	public Object execute(Object[] parameters) {

		ElasticsearchParametersParameterAccessor parameterAccessor = getParameterAccessor(parameters);
		ResultProcessor resultProcessor = queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);
		Class<?> clazz = resultProcessor.getReturnedType().getDomainType();

		Query query = createQuery(parameters);

		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(clazz);

		Object result = null;

		if (isDeleteQuery()) {
			result = countOrGetDocumentsForDelete(query, parameterAccessor);
			elasticsearchOperations.delete(query, clazz, index);
		}
		else if (isCountQuery()) {
			result = elasticsearchOperations.count(query, clazz, index);
		}
		else if (isExistsQuery()) {
			result = elasticsearchOperations.count(query, clazz, index) > 0;
		}
		else if (queryMethod.isPageQuery()) {
			query.setPageable(parameterAccessor.getPageable());
			SearchHits<?> searchHits = elasticsearchOperations.search(query, clazz, index);
			if (queryMethod.isSearchPageMethod()) {
				result = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
			}
			else {
				result = SearchHitSupport.unwrapSearchHits(SearchHitSupport.searchPageFor(searchHits, query.getPageable()));
			}
		}
		else if (queryMethod.isStreamQuery()) {
			query.setPageable(parameterAccessor.getPageable().isPaged() ? parameterAccessor.getPageable()
					: PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			result = StreamUtils.createStreamFromIterator(elasticsearchOperations.searchForStream(query, clazz, index));
		}
		else if (queryMethod.isCollectionQuery()) {

			if (parameterAccessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, clazz, index);

				if (itemCount == 0) {
					result = new SearchHitsImpl<>(0, TotalHitsRelation.EQUAL_TO, Float.NaN, null, Collections.emptyList(), null);
				}
				else {
					query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
				}
			}
			else {
				query.setPageable(parameterAccessor.getPageable());
			}

			if (result == null) {
				result = elasticsearchOperations.search(query, clazz, index);
			}

		}
		else {
			result = elasticsearchOperations.searchOne(query, clazz, index);
		}

		return (queryMethod.isNotSearchHitMethod() && queryMethod.isNotSearchPageMethod())
				? SearchHitSupport.unwrapSearchHits(result)
				: result;
	}

	public Query createQuery(Object[] parameters) {

		ElasticsearchParametersParameterAccessor parameterAccessor = getParameterAccessor(parameters);

		BaseQuery query = createQuery(parameterAccessor);
		Assert.notNull(query, "unsupported query");

		queryMethod.addMethodParameter(query, parameterAccessor, elasticsearchOperations.getElasticsearchConverter());

		return query;
	}

	private ElasticsearchParametersParameterAccessor getParameterAccessor(Object[] parameters) {
		return new ElasticsearchParametersParameterAccessor(queryMethod, parameters);
	}

	@Nullable
	private Object countOrGetDocumentsForDelete(Query query, ParametersParameterAccessor accessor) {

		Object result = null;
		Class<?> entityClass = queryMethod.getEntityInformation().getJavaType();
		IndexCoordinates index = elasticsearchOperations.getIndexCoordinatesFor(entityClass);

		if (queryMethod.isCollectionQuery()) {

			if (accessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, entityClass, index);
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			}
			else {
				query.setPageable(accessor.getPageable());
			}
			result = elasticsearchOperations.search(query, entityClass, index);
		}

		if (ClassUtils.isAssignable(Number.class, queryMethod.getReturnedObjectType())) {
			result = elasticsearchOperations.count(query, entityClass, index);
		}

		return result;
	}

	protected abstract BaseQuery createQuery(ElasticsearchParametersParameterAccessor accessor);
}
