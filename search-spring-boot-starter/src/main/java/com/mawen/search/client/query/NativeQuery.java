package com.mawen.search.client.query;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import co.elastic.clients.elasticsearch._types.KnnQuery;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.search.FieldCollapse;
import co.elastic.clients.elasticsearch.core.search.Suggester;
import co.elastic.clients.json.JsonData;
import com.mawen.search.client.query.builder.NativeQueryBuilder;
import com.mawen.search.core.query.BaseQuery;
import lombok.Getter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * 使用新的 Elasticsearch 客户端库构建的查询
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class NativeQuery extends BaseQuery {

	@Nullable
	private final Query query;
	private final Map<String, Aggregation> aggregations = new LinkedHashMap<>();
	@Nullable
	private com.mawen.search.core.query.Query springDataQuery;
	@Nullable
	private Query filter;
	@Nullable
	private Suggester suggester;
	@Nullable
	private FieldCollapse fieldCollapse;
	private List<SortOptions> sortOptions = Collections.emptyList();

	private Map<String, JsonData> searchExtensions = Collections.emptyMap();
	@Nullable
	private KnnQuery knnQuery;

	public NativeQuery(NativeQueryBuilder builder) {
		super(builder);
		this.query = builder.getQuery();
		this.filter = builder.getFilter();
		this.aggregations.putAll(builder.getAggregations());
		this.suggester = builder.getSuggester();
		this.fieldCollapse = builder.getFieldCollapse();
		this.sortOptions = builder.getSortOptions();
		this.searchExtensions = builder.getSearchExtensions();

		if (builder.getSpringDataQuery() != null) {
			Assert.isTrue(!NativeQuery.class.isAssignableFrom(builder.getSpringDataQuery().getClass()),
					"Cannot add an NativeQuery in a NativeQuery");
		}
		this.springDataQuery = builder.getSpringDataQuery();
		this.knnQuery = builder.getKnnQuery();
	}

	public NativeQuery(@Nullable Query query) {
		this.query = query;
	}

	public static NativeQueryBuilder builder() {
		return new NativeQueryBuilder();
	}

	public void setSpringDataQuery(@Nullable com.mawen.search.core.query.Query springDataQuery) {
		this.springDataQuery = springDataQuery;
	}
}
