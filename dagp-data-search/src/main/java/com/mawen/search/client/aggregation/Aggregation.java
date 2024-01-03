package com.mawen.search.client.aggregation;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class Aggregation {

	private final String name;
	private final Aggregate aggregate;

	public Aggregation(String name, Aggregate aggregate) {
		this.name = name;
		this.aggregate = aggregate;
	}

	public String getName() {
		return name;
	}

	public Aggregate getAggregate() {
		return aggregate;
	}
}
