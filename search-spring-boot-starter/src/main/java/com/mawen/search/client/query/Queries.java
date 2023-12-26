package com.mawen.search.client.query;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.LatLonGeoLocation;
import co.elastic.clients.elasticsearch._types.aggregations.Aggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.IdsQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchAllQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryStringQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.TermQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WildcardQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.WrapperQuery;
import co.elastic.clients.util.ObjectBuilder;
import com.mawen.search.core.query.builder.BaseQueryBuilder;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class Queries {


	private Queries() {
	}

	public static IdsQuery idsQuery(List<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		return IdsQuery.of(i -> i.values(ids));
	}

	public static Query idsQueryAsQuery(List<String> ids) {

		Assert.notNull(ids, "ids must not be null");

		Function<Query.Builder, ObjectBuilder<Query>> builder = b -> b.ids(idsQuery(ids));

		return builder.apply(new Query.Builder()).build();
	}

	public static MatchQuery matchQuery(String fieldName, String query, @Nullable Operator operator,
			@Nullable Float boost) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(query, "query must not be null");

		return MatchQuery.of(mb -> mb.field(fieldName).query(FieldValue.of(query)).operator(operator).boost(boost));
	}

	public static Query matchQueryAsQuery(String fieldName, String query, @Nullable Operator operator,
			@Nullable Float boost) {

		Function<Query.Builder, ObjectBuilder<Query>> builder = b -> b.match(matchQuery(fieldName, query, operator, boost));

		return builder.apply(new Query.Builder()).build();
	}

	public static MatchAllQuery matchAllQuery() {

		return MatchAllQuery.of(b -> b);
	}

	public static Query matchAllQueryAsQuery() {

		Function<Query.Builder, ObjectBuilder<Query>> builder = b -> b.matchAll(matchAllQuery());

		return builder.apply(new Query.Builder()).build();
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, @Nullable Float boost) {
		return queryStringQuery(fieldName, query, null, null, boost);
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, Operator defaultOperator,
			@Nullable Float boost) {
		return queryStringQuery(fieldName, query, null, defaultOperator, boost);
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, @Nullable Boolean analyzeWildcard,
			@Nullable Float boost) {
		return queryStringQuery(fieldName, query, analyzeWildcard, null, boost);
	}

	public static QueryStringQuery queryStringQuery(String fieldName, String query, @Nullable Boolean analyzeWildcard,
			@Nullable Operator defaultOperator, @Nullable Float boost) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(query, "query must not be null");

		return QueryStringQuery.of(qs -> qs.fields(fieldName).query(query).analyzeWildcard(analyzeWildcard)
				.defaultOperator(defaultOperator).boost(boost));
	}

	public static TermQuery termQuery(String fieldName, String value) {

		Assert.notNull(fieldName, "fieldName must not be null");
		Assert.notNull(value, "value must not be null");

		return TermQuery.of(t -> t.field(fieldName).value(FieldValue.of(value)));
	}

	public static Query termQueryAsQuery(String fieldName, String value) {

		Function<Query.Builder, ObjectBuilder<Query>> builder = q -> q.term(termQuery(fieldName, value));
		return builder.apply(new Query.Builder()).build();
	}

	public static Query termQueryAsQuery(TermQuery query) {
		return query._toQuery();
	}

	public static WildcardQuery wildcardQuery(String field, String value) {

		Assert.notNull(field, "field must not be null");
		Assert.notNull(value, "value must not be null");

		return WildcardQuery.of(w -> w.field(field).wildcard(value));
	}

	public static Query wildcardQueryAsQuery(String field, String value) {
		Function<Query.Builder, ObjectBuilder<Query>> builder = q -> q.wildcard(wildcardQuery(field, value));
		return builder.apply(new Query.Builder()).build();
	}

	public static Query wrapperQueryAsQuery(String query) {

		Function<Query.Builder, ObjectBuilder<Query>> builder = q -> q.wrapper(wrapperQuery(query));

		return builder.apply(new Query.Builder()).build();
	}

	public static WrapperQuery wrapperQuery(String query) {

		Assert.notNull(query, "query must not be null");

		String encodedValue = Base64.getEncoder().encodeToString(query.getBytes(StandardCharsets.UTF_8));

		return WrapperQuery.of(wq -> wq.query(encodedValue));
	}


	public static LatLonGeoLocation latLon(double lat, double lon) {
		return LatLonGeoLocation.of(_0 -> _0.lat(lat).lon(lon));
	}

	public static com.mawen.search.core.query.Query getTermsAggsQuery(String aggsName,
			String aggsField) {
		return NativeQuery.builder() //
				.withQuery(Queries.matchAllQueryAsQuery()) //
				.withAggregation(aggsName, Aggregation.of(a -> a //
						.terms(ta -> ta.field(aggsField)))) //
				.withMaxResults(0) //
				.build();
	}

	public static com.mawen.search.core.query.Query queryWithIds(String... ids) {
		return NativeQuery.builder().withIds(ids).build();
	}

	public static BaseQueryBuilder<?, ?> getBuilderWithMatchAllQuery() {
		return NativeQuery.builder().withQuery(matchAllQueryAsQuery());
	}

	public static BaseQueryBuilder<?, ?> getBuilderWithTermQuery(String field, String value) {
		return NativeQuery.builder().withQuery(termQueryAsQuery(field, value));
	}
}
