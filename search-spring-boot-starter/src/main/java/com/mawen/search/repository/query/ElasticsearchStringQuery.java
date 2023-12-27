package com.mawen.search.repository.query;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Query;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.StringQuery;
import com.mawen.search.repository.support.StringQueryUtil;

import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * 通过提取方法上的 {@link Query} 注解中的内容，构造 {@link StringQuery} 来进行查询的 {@link RepositoryQuery} 实现
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ElasticsearchStringQuery extends AbstractElasticsearchRepositoryQuery {

	private final String queryString;

	/**
	 * 使用给定的 {@link ElasticsearchQueryMethod}、{@link ElasticsearchOperations} 和
	 *
	 * @param queryMethod 不能为空
	 * @param elasticsearchOperations 不能为空
	 * @param queryString 不能为空
	 */
	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations, String queryString) {

		super(queryMethod, elasticsearchOperations);

		Assert.notNull(queryString, "Query cannot be empty");

		this.queryString = queryString;
	}

	@Override
	public boolean isCountQuery() {
		return queryMethod.hasCountQueryAnnotation();
	}

	@Override
	protected boolean isDeleteQuery() {
		return false;
	}

	@Override
	protected boolean isExistsQuery() {
		return false;
	}

	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor parameterAccessor) {

		String queryString = new StringQueryUtil(elasticsearchOperations.getElasticsearchConverter().getConversionService())
				.replacePlaceholders(this.queryString, parameterAccessor);

		BaseQuery query = new StringQuery(queryString);
		query.addSort(parameterAccessor.getSort());
		return query;
	}
}
