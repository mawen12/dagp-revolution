package com.mawen.search.client.aggregation;

import com.mawen.search.core.aggregation.AggregationContainer;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchAggregation implements AggregationContainer<Aggregation> {

	private final Aggregation aggregation;

	public ElasticsearchAggregation(Aggregation aggregation) {
		this.aggregation = aggregation;
	}

	@Override
	public Aggregation aggregation() {
		return aggregation;
	}
}
