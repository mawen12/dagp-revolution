package com.mawen.search.core;

import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.core.routing.RoutingResolver;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface ElasticsearchOperations extends DocumentOperations, SearchOperations {

	ElasticsearchConverter getElasticsearchConverter();

	IndexCoordinates getIndexCoordinatesFor(Class<?> clazz);

	@Nullable
	default String convertId(@Nullable Object idValue) {
		return idValue != null ? getElasticsearchConverter().convertId(idValue) : null;
	}

	ElasticsearchOperations withRouting(RoutingResolver routingResolver);

	ElasticsearchOperations withRefreshPolicy(RefreshPolicy refreshPolicy);
}
