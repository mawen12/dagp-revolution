package com.mawen.search.repository.query;

import com.mawen.search.core.annotation.ParamQuery;
import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchParameter extends Parameter {

	protected final MethodParameter parameter;

	ElasticsearchParameter(MethodParameter parameter) {
		super(parameter);

		this.parameter = parameter;
	}

	@Override
	public boolean isSpecialParameter() {
		return super.isSpecialParameter();
	}

	public boolean isParamQueryParameter() {
		return parameter.hasParameterAnnotation(ParamQuery.class);
	}

	public boolean isIndexCoordinatesParameter() {
		return IndexCoordinates.class.equals(parameter.getParameterType());
	}

}
