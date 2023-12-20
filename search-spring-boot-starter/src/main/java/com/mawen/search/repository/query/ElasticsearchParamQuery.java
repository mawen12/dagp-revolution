package com.mawen.search.repository.query;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.ParamQuery;
import com.mawen.search.core.query.BaseQuery;

import org.springframework.data.repository.query.Parameter;
import org.springframework.data.repository.query.Parameters;
import org.springframework.util.Assert;

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
	private Parameter paramQueryParameter;

	protected ElasticsearchParamQuery(ElasticsearchQueryMethod queryMethod, ElasticsearchOperations elasticsearchOperations) {
		super(queryMethod, elasticsearchOperations);

		this.methodName = queryMethod.getName();
		this.returnType = queryMethod.methodReturnType();
		this.parameters = queryMethod.getParameters();
		this.paramQueryParameter = getParamQueryParameter();
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



		return null;
	}

	private Parameter getParamQueryParameter() {

		Parameter paramQueryParameter = null;
		for (Parameter parameter : parameters) {
			if (parameter.getType().isAnnotationPresent(ParamQuery.class)) {
				paramQueryParameter = parameter;
			}
		}

		return paramQueryParameter;
	}
}
