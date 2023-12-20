package com.mawen.search.repository.query;

import org.springframework.data.repository.query.ParametersParameterAccessor;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchParametersParameterAccessor extends ParametersParameterAccessor
		implements ElasticsearchParameterAccessor {

	private final Object[] values;

	/**
	 * Creates a new {@link ElasticsearchParametersParameterAccessor}.
	 *
	 * @param method must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 */
	ElasticsearchParametersParameterAccessor(ElasticsearchQueryMethod method, Object... values) {

		super(method.getParameters(), values);
		this.values = values;
	}

	@Override
	public Object[] getValues() {
		return values;
	}
}