package com.mawen.search.repository.query;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.domain.SearchHitSupport;
import com.mawen.search.core.domain.SearchHits;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.Query;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * {@link RepositoryQuery} 的抽象实现，提供通过 {@link Query} 和 {@link ElasticsearchQueryMethod} 对 Elasticsearch 发起调用
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public abstract class AbstractElasticsearchRepositoryQuery implements RepositoryQuery {

	protected static final int DEFAULT_STREAM_BATCH_SIZE = 500;
	protected final ElasticsearchOperations elasticsearchOperations;
	protected final ElasticsearchConverter elasticsearchConverter;
	protected ElasticsearchQueryMethod queryMethod;

	/**
	 * 使用给定的 {@link QueryMethod} 和 {@link ElasticsearchOperations} 创建一个 {@link AbstractElasticsearchRepositoryQuery}
	 *
	 * @param queryMethod 不能为空
	 * @param elasticsearchOperations 不能为空
	 */
	protected AbstractElasticsearchRepositoryQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations) {

		Assert.notNull(queryMethod, "ElasticsearchQueryMethod cannot be null");
		Assert.notNull(elasticsearchOperations, "ElasticsearchOperations cannot be null");

		this.queryMethod = queryMethod;
		this.elasticsearchOperations = elasticsearchOperations;
		this.elasticsearchConverter = elasticsearchOperations.getElasticsearchConverter();
	}

	@Override
	public Object execute(Object[] parameters) {

		ElasticsearchParametersParameterAccessor parameterAccessor = getParameterAccessor(parameters);
		ResultProcessor resultProcessor = queryMethod.getResultProcessor().withDynamicProjection(parameterAccessor);
		Class<?> clazz = resultProcessor.getReturnedType().getDomainType();

		Query query = createQuery(parameters);

		boolean dynamicIndex = queryMethod.getEntityInformation().getEntity().isDynamicIndex();
		IndexCoordinates index = parameterAccessor.getIndexCoordinates();

		if (dynamicIndex && index == null) {
			throw new IllegalArgumentException(String.format("The Entity %s is dynamic index, so the method %s must provider non-null index", queryMethod.getEntityInformation().getEntity(), queryMethod));
		}

		if (index == null) {
			index = elasticsearchOperations.getIndexCoordinatesFor(clazz);
		}

		return doExecute(query, parameterAccessor, clazz, index);
	}

	private Object doExecute(Query query, ElasticsearchParametersParameterAccessor parameterAccessor, Class<?> clazz, IndexCoordinates index) {
		Object result = null;

		if (isDeleteQuery()) { // delete
			result = elasticsearchOperations.delete(query, clazz, index).getDeleted();
		}
		else if (isCountQuery()) { // count
			result = elasticsearchOperations.count(query, clazz, index);
		}
		else if (isExistsQuery()) { // exists
			result = elasticsearchOperations.count(query, clazz, index) > 0;
		}
		else if (queryMethod.isPageQuery()) { // page Query
			query.setPageable(parameterAccessor.getPageable());
			SearchHits<?> searchHits = elasticsearchOperations.search(query, clazz, index);
			if (queryMethod.isSearchPageMethod()) {
				result = SearchHitSupport.searchPageFor(searchHits, query.getPageable());
			}
			else {
				result = SearchHitSupport.unwrapSearchHits(SearchHitSupport.searchPageFor(searchHits, query.getPageable()));
			}
		}
		else if (queryMethod.isStreamQuery()) { // stream query
			query.setPageable(parameterAccessor.getPageable().isPaged() ? parameterAccessor.getPageable()
					: PageRequest.of(0, DEFAULT_STREAM_BATCH_SIZE));
			result = StreamUtils.createStreamFromIterator(elasticsearchOperations.searchForStream(query, clazz, index));
		}
		else if (queryMethod.isCollectionQuery()) { // collection query
			// 如果给定的分页中没有设置分页参数信息，则先执行 count 查询后，在执行 search
			if (parameterAccessor.getPageable().isUnpaged()) {
				int itemCount = (int) elasticsearchOperations.count(query, clazz, index);
				query.setPageable(PageRequest.of(0, Math.max(1, itemCount)));
			}
			else {
				query.setPageable(parameterAccessor.getPageable());
			}

			result = elasticsearchOperations.search(query, clazz, index);
		}
		else { // single query
			result = elasticsearchOperations.searchOne(query, clazz, index);
		}

		return (queryMethod.isNotSearchHitMethod() && queryMethod.isNotSearchPageMethod())
				? SearchHitSupport.unwrapSearchHits(result)
				: result;
	}

	/**
	 * 使用给定参数信息创建一个 {@link Query}
	 *
	 * @param parameters {@link QueryMethod} 上的方法参数
	 * @return {@link Query} 实例
	 */
	public Query createQuery(Object[] parameters) {

		ElasticsearchParametersParameterAccessor parameterAccessor = getParameterAccessor(parameters);

		BaseQuery query = createQuery(parameterAccessor);
		Assert.notNull(query, "unsupported query");

		queryMethod.addMethodParameter(query, parameterAccessor, elasticsearchConverter);

		return query;
	}

	private ElasticsearchParametersParameterAccessor getParameterAccessor(Object[] parameters) {
		return new ElasticsearchParametersParameterAccessor(queryMethod, parameters);
	}

	/**
	 * 使用给定的 {@link ElasticsearchParametersParameterAccessor} 创建一个 {@link Query}
	 *
	 * @param accessor Elasticsearch 方法参数访问器
	 * @return {@link Query} 实例
	 */
	protected abstract BaseQuery createQuery(ElasticsearchParametersParameterAccessor accessor);

	@Override
	public QueryMethod getQueryMethod() {
		return queryMethod;
	}

	/**
	 * 判断当前查询方法是否支持 count
	 *
	 * @return 如果是 count，则返回 true，反之返回 false
	 */
	public abstract boolean isCountQuery();

	/**
	 * 判断当前查询方法是否支持 delete
	 *
	 * @return 如果是 delete，则返回 true，反之返回 false
	 */
	protected abstract boolean isDeleteQuery();

	/**
	 * 判断当前查询方法是否支持 exists
	 *
	 * @return 如果是 exists，则返回 true，反之返回 false
	 */
	protected abstract boolean isExistsQuery();
}
