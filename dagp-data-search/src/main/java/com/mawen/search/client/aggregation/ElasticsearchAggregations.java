package com.mawen.search.client.aggregation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import com.mawen.search.core.aggregation.AggregationsContainer;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class ElasticsearchAggregations implements AggregationsContainer<List<ElasticsearchAggregation>> {
	private final List<ElasticsearchAggregation> aggregations;
	private final Map<String, ElasticsearchAggregation> aggregationsAsMap;

	public ElasticsearchAggregations(Map<String, Aggregate> aggregations) {

		Assert.notNull(aggregations, "aggregations must not be null");

		aggregationsAsMap = new HashMap<>();
		aggregations.forEach((name, aggregate) -> aggregationsAsMap //
				.put(name, new ElasticsearchAggregation(new Aggregation(name, aggregate))));

		this.aggregations = new ArrayList<>(aggregationsAsMap.values());
	}

	@Override
	public List<ElasticsearchAggregation> aggregations() {
		return aggregations;
	}

	/**
	 * @return the {@link ElasticsearchAggregation}s keyed by aggregation name.
	 */
	public Map<String, ElasticsearchAggregation> aggregationsAsMap() {
		return aggregationsAsMap;
	}

	/**
	 * Returns the aggregation that is associated with the specified name.
	 *
	 * @param name the name of the aggregation
	 * @return the aggregation or {@literal null} if not found
	 */
	@Nullable
	public ElasticsearchAggregation get(String name) {

		Assert.notNull(name, "name must not be null");

		return aggregationsAsMap.get(name);
	}
}
