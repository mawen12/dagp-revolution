package com.mawen.search.repository.query;

import java.lang.reflect.Method;
import java.util.List;

import lombok.Getter;

import org.springframework.core.MethodParameter;
import org.springframework.data.repository.query.Parameters;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class ElasticsearchParameters extends Parameters<ElasticsearchParameters, ElasticsearchParameter> {

	protected final int paramQueryIndex;
	protected final int indexCoordinatesIndex;

	public ElasticsearchParameters(Method method) {
		super(method);

		int paramQueryIndex = -1;
		int indexCoordinatesIndex = -1;

		for (int i = 0; i < getNumberOfParameters(); i++) {

			ElasticsearchParameter parameter = getParameter(i);
			if (parameter.isParamQueryParameter()) {
				paramQueryIndex = i;
				continue;
			}
			if (parameter.isIndexCoordinatesParameter()) {
				indexCoordinatesIndex = i;
				continue;
			}
		}

		this.paramQueryIndex = paramQueryIndex;
		this.indexCoordinatesIndex = indexCoordinatesIndex;
	}

	public ElasticsearchParameters(List<ElasticsearchParameter> parameters) {
		super(parameters);

		int paramQueryIndex = -1;
		int indexCoordinatesIndex = -1;

		for (int i = 0; i < getNumberOfParameters(); i++) {

			ElasticsearchParameter parameter = getParameter(i);
			if (parameter.isParamQueryParameter()) {
				paramQueryIndex = i;
				break;
			}
		}

		this.paramQueryIndex = paramQueryIndex;
		this.indexCoordinatesIndex = indexCoordinatesIndex;
	}

	@Override
	protected ElasticsearchParameter createParameter(MethodParameter parameter) {
		return new ElasticsearchParameter(parameter);
	}

	@Override
	protected ElasticsearchParameters createFrom(List<ElasticsearchParameter> parameters) {
		return new ElasticsearchParameters(parameters);
	}

	public boolean hasParamQuery() {
		return paramQueryIndex != -1;
	}

	public boolean hasIndexCoordinates() {
		return indexCoordinatesIndex != -1;
	}
}