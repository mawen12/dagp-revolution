package com.mawen.search.repository.query;

import com.mawen.search.core.mapping.IndexCoordinates;

import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface ElasticsearchParameterAccessor extends ParameterAccessor {

	Object[] getValues();

	@Nullable
	Object getParamQuery();

	@Nullable
	IndexCoordinates getIndexCoordinates();
}
