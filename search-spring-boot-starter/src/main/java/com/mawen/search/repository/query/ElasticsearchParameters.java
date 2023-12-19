package com.mawen.search.repository.query;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameters;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Deprecated
public class ElasticsearchParameters extends Parameters<ElasticsearchParameters, ElasticsearchParameter> {

	public ElasticsearchParameters(Method method) {

		super(method);

	}

	private ElasticsearchParameter parameterFactory(MethodParameter methodParameter) {
		return new ElasticsearchParameter(methodParameter);
	}

	private ElasticsearchParameters(List<ElasticsearchParameter> parameters) {
		super(parameters);
	}

	@Override
	protected ElasticsearchParameter createParameter(MethodParameter parameter) {
		return null;
	}

	@Override
	protected ElasticsearchParameters createFrom(List<ElasticsearchParameter> parameters) {
		return new ElasticsearchParameters(parameters);
	}

}