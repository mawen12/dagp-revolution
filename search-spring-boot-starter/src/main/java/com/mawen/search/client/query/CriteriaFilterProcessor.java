package com.mawen.search.client.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.GeoDistanceType;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.GeoDistanceQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryVariant;
import co.elastic.clients.util.ObjectBuilder;
import com.mawen.search.core.domain.Criteria;

import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class CriteriaFilterProcessor {

	public static Optional<Query> createQuery(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		List<Query> filterQueries = new ArrayList<>();

		for (Criteria chainedCriteria : criteria.getCriteriaChain()) {

			if (chainedCriteria.isOr()) {
				BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
				queriesForEntries(chainedCriteria).forEach(boolQueryBuilder::should);
				filterQueries.add(new Query(boolQueryBuilder.build()));
			}
			else if (chainedCriteria.isNegating()) {
				Collection<? extends Query> negatingFilters = buildNegatingFilter(criteria.getField().getName(),
						criteria.getFilterCriteriaEntries());
				filterQueries.addAll(negatingFilters);
			}
			else {
				filterQueries.addAll(queriesForEntries(chainedCriteria));
			}
		}

		if (filterQueries.isEmpty()) {
			return Optional.empty();
		}
		else {

			if (filterQueries.size() == 1) {
				return Optional.of(filterQueries.get(0));
			}
			else {
				BoolQuery.Builder boolQueryBuilder = QueryBuilders.bool();
				filterQueries.forEach(boolQueryBuilder::must);
				BoolQuery boolQuery = boolQueryBuilder.build();
				return Optional.of(new Query(boolQuery));
			}
		}
	}

	private static Collection<? extends Query> buildNegatingFilter(String fieldName,
			Set<Criteria.CriteriaEntry> filterCriteriaEntries) {

		List<Query> negationFilters = new ArrayList<>();

		filterCriteriaEntries.forEach(criteriaEntry -> {
			Optional<Query> query = queryFor(criteriaEntry.getKey(), criteriaEntry.getValue(), fieldName);

			if (query.isPresent()) {
				BoolQuery negatingFilter = QueryBuilders.bool().mustNot(query.get()).build();
				negationFilters.add(new Query(negatingFilter));
			}
		});

		return negationFilters;
	}

	private static Collection<? extends Query> queriesForEntries(Criteria criteria) {

		Assert.notNull(criteria.getField(), "criteria must have a field");
		String fieldName = criteria.getField().getName();
		Assert.notNull(fieldName, "Unknown field");

		return criteria.getFilterCriteriaEntries().stream()
				.map(entry -> queryFor(entry.getKey(), entry.getValue(), fieldName)) //
				.filter(Optional::isPresent) //
				.map(Optional::get) //
				.collect(Collectors.toList());
	}

	private static Optional<Query> queryFor(Criteria.OperationKey key, Object value, String fieldName) {

		ObjectBuilder<? extends QueryVariant> queryBuilder = null;

		switch (key) {
			case WITHIN: {
				Assert.isTrue(value instanceof Object[], "Value of a geo distance filter should be an array of two values.");
				queryBuilder = withinQuery(fieldName, (Object[]) value);
			}
		}

		return Optional.ofNullable(queryBuilder != null ? queryBuilder.build()._toQuery() : null);
	}

	private static ObjectBuilder<GeoDistanceQuery> withinQuery(String fieldName, Object... values) {

		Assert.noNullElements(values, "Geo distance filter takes 2 not null elements array as parameter.");
		Assert.isTrue(values.length == 2, "Geo distance filter takes a 2-elements array as parameter.");
		Assert.isTrue(values[1] instanceof String || values[1] instanceof Distance,
				"Second element of a geo distance filter must be a text or a Distance");

		String dist = (values[1] instanceof Distance) ? extractDistanceString((Distance) values[1]) : (String) values[1];

		return QueryBuilders.geoDistance() //
				.field(fieldName) //
				.distance(dist) //
				.distanceType(GeoDistanceType.Plane) //
				.location(location -> {
					String loc = (String) values[0];
					if (loc.contains(",")) {
						String[] c = loc.split(",");
						location.latlon(latlon -> latlon.lat(Double.parseDouble(c[0])).lon(Double.parseDouble(c[1])));
					}
					else {
						location.geohash(geohash -> geohash.geohash(loc));
					}

					return location;
				});
	}


	/**
	 * extract the distance string from a {@link org.springframework.data.geo.Distance} object.
	 *
	 * @param distance distance object to extract string from
	 */
	private static String extractDistanceString(Distance distance) {

		StringBuilder sb = new StringBuilder();
		sb.append((int) distance.getValue());
		switch ((Metrics) distance.getMetric()) {
			case KILOMETERS:
				sb.append("km");
			case MILES:
				sb.append("mi");
		}

		return sb.toString();
	}

}
