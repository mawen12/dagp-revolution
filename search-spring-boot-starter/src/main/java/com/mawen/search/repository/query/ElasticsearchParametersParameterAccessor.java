package com.mawen.search.repository.query;

import com.mawen.search.core.mapping.IndexCoordinates;
import lombok.Getter;

import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class ElasticsearchParametersParameterAccessor extends ParametersParameterAccessor
		implements ElasticsearchParameterAccessor {

	private final Object[] values;
	private final ElasticsearchParameters elasticsearchParameters;

	ElasticsearchParametersParameterAccessor(ElasticsearchQueryMethod method, Object... values) {

		super(method.getParameters(), values);

		Assert.isTrue(method.getParameters() instanceof ElasticsearchParameters, "The Parameters of Method is not the instance of ElasticsearchParameters.");

		this.elasticsearchParameters = (ElasticsearchParameters) method.getParameters();
		this.values = values;
	}

	@Nullable
	@Override
	public Object getParamQuery() {

		if (!elasticsearchParameters.hasParamQuery()) {
			return null;
		}

		return values[elasticsearchParameters.getParamQueryIndex()];
	}

	@Nullable
	@Override
	public IndexCoordinates getIndexCoordinates() {

		if (!elasticsearchParameters.hasIndexCoordinates()) {
			return null;
		}

		return (IndexCoordinates) values[elasticsearchParameters.getIndexCoordinatesIndex()];
	}
}