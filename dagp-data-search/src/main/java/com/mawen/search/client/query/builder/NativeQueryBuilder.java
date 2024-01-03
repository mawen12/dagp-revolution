package com.mawen.search.client.query.builder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import com.mawen.search.client.query.NativeQuery;
import com.mawen.search.core.query.builder.BaseQueryBuilder;
import lombok.Getter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class NativeQueryBuilder extends BaseQueryBuilder<NativeQuery, NativeQueryBuilder> {

	private final Map<String, Aggregation> aggregations = new LinkedHashMap<>();
	@Nullable
	private Query query;
	@Nullable
	private Query filter;
	@Nullable
	private Suggester suggester;
	@Nullable
	private FieldCollapse fieldCollapse;
	private List<SortOptions> sortOptions = new ArrayList<>();
	private Map<String, JsonData> searchExtensions = new LinkedHashMap<>();

	@Nullable
	private com.mawen.search.core.query.Query springDataQuery;
	@Nullable
	private KnnQuery knnQuery;

	public NativeQueryBuilder() {
	}


	public NativeQueryBuilder withQuery(Query query) {

		Assert.notNull(query, "query must not be null");

		this.query = query;
		return this;
	}

	public NativeQueryBuilder withQuery(Function<Query.Builder, ObjectBuilder<Query>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return withQuery(fn.apply(new Query.Builder()).build());
	}

	public NativeQueryBuilder withFilter(@Nullable Query filter) {
		this.filter = filter;
		return this;
	}

	public NativeQueryBuilder withFilter(Function<Query.Builder, ObjectBuilder<Query>> fn) {

		Assert.notNull(fn, "fn must not be null");

		return withFilter(fn.apply(new Query.Builder()).build());
	}

	public NativeQueryBuilder withAggregation(String name, Aggregation aggregation) {

		Assert.notNull(name, "name must not be null");
		Assert.notNull(aggregation, "aggregation must not be null");

		this.aggregations.put(name, aggregation);
		return this;
	}

	public NativeQueryBuilder withSuggester(@Nullable Suggester suggester) {
		this.suggester = suggester;
		return this;
	}

	public NativeQueryBuilder withFieldCollapse(@Nullable FieldCollapse fieldCollapse) {
		this.fieldCollapse = fieldCollapse;
		return this;
	}

	public NativeQueryBuilder withSort(List<SortOptions> values) {

		Assert.notEmpty(values, "values must not be empty");

		sortOptions.clear();
		sortOptions.addAll(values);

		return this;
	}

	public NativeQueryBuilder withSort(SortOptions value, SortOptions... values) {

		Assert.notNull(value, "value must not be null");
		sortOptions.add(value);
		if (values.length > 0) {
			sortOptions.addAll(Arrays.asList(values));
		}

		return this;
	}

	public NativeQueryBuilder withSort(Function<SortOptions.Builder, ObjectBuilder<SortOptions>> fn) {

		Assert.notNull(fn, "fn must not be null");
		withSort(fn.apply(new SortOptions.Builder()).build());

		return this;
	}

	public NativeQueryBuilder withSearchExtension(String key, JsonData value) {

		Assert.notNull(key, "key must not be null");
		Assert.notNull(value, "value must not be null");

		searchExtensions.put(key, value);
		return this;
	}

	public NativeQueryBuilder withSearchExtensions(Map<String, JsonData> searchExtensions) {

		Assert.notNull(searchExtensions, "searchExtensions must not be null");

		this.searchExtensions.putAll(searchExtensions);
		return this;
	}


	public NativeQueryBuilder withQuery(com.mawen.search.core.query.Query query) {
		this.springDataQuery = query;
		return this;
	}

	/**
	 * @since 5.1
	 */
	public NativeQueryBuilder withKnnQuery(KnnQuery knnQuery) {
		this.knnQuery = knnQuery;
		return this;
	}

	public NativeQuery build() {
		Assert.isTrue(query == null || springDataQuery == null, "Cannot have both a native query and a Spring Data query");
		return new NativeQuery(this);
	}
}
