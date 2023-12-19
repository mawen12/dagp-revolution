package com.mawen.search.repository.query;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.core.query.StringQuery;
import com.mawen.search.repository.support.StringQueryUtil;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchStringQuery extends AbstractElasticsearchRepositoryQuery {

	private final String queryString;

	public ElasticsearchStringQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations,
	                                String queryString) {
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
