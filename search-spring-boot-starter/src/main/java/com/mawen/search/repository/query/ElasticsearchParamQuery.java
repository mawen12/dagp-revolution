package com.mawen.search.repository.query;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.query.BaseQuery;
import com.mawen.search.repository.query.parser.ElasticsearchParamQueryCreator;

import org.springframework.data.repository.query.Parameters;

/**
 *
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
public class ElasticsearchParamQuery extends AbstractElasticsearchRepositoryQuery {

	private final String methodName;
	private final Class<?> returnType;
	private final Parameters<?, ?> parameters;

	public ElasticsearchParamQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations) {
		super(queryMethod, elasticsearchOperations);

		this.methodName = queryMethod.getName();
		this.returnType = queryMethod.methodReturnType();
		this.parameters = queryMethod.getParameters();
	}

	@Override
	public boolean isCountQuery() {
		return methodName.startsWith("count")
				&& (Integer.class.isAssignableFrom(returnType) || Long.class.isAssignableFrom(returnType));
	}

	@Override
	protected boolean isDeleteQuery() {
		return methodName.startsWith("delete") && (Void.class.isAssignableFrom(returnType));
	}

	@Override
	protected boolean isExistsQuery() {
		return methodName.startsWith("exists") && (Boolean.class.isAssignableFrom(returnType));
	}

	@Override
	protected BaseQuery createQuery(ElasticsearchParametersParameterAccessor accessor) {

		return new ElasticsearchParamQueryCreator(accessor.getParamQuery(), accessor).createQuery();
	}
}
