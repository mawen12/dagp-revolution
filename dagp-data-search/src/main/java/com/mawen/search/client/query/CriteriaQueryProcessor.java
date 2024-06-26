package com.mawen.search.client.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.ChildScoreMode;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.util.ObjectBuilder;
import com.mawen.search.InvalidApiUsageException;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.Field;
import lombok.extern.slf4j.Slf4j;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import static com.mawen.search.client.query.Queries.*;
import static org.springframework.util.StringUtils.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Slf4j
public class CriteriaQueryProcessor {

	@Nullable
	public static Query createQuery(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		List<Query> shouldQueries = new ArrayList<>();
		List<Query> mustNotQueries = new ArrayList<>();
		List<Query> mustQueries = new ArrayList<>();

		Query firstQuery = null;
		boolean negateFirstQuery = false;

		for (Criteria chainedCriteria : criteria.getCriteriaChain()) {
			Query queryFragment = queryForEntries(chainedCriteria);

			if (queryFragment != null) {

				if (firstQuery == null) {
					firstQuery = queryFragment;
					negateFirstQuery = chainedCriteria.isNegating();
					continue;
				}

				if (chainedCriteria.isOr()) {
					shouldQueries.add(queryFragment);
				}
				else if (chainedCriteria.isNegating()) {
					mustNotQueries.add(queryFragment);
				}
				else {
					mustQueries.add(queryFragment);
				}
			}

			// add nested query
			if (!CollectionUtils.isEmpty(chainedCriteria.getNestedCriteria())) {
				Query nestedQuery = createNestedQuery(chainedCriteria);
				if (nestedQuery != null) {
					if (chainedCriteria.isOr()) {
						shouldQueries.add(nestedQuery);
					}
					else if (chainedCriteria.isNegating()) {
						mustNotQueries.add(nestedQuery);
					}
					else {
						mustQueries.add(nestedQuery);
					}
				}
			}
		}

		for (Criteria subCriteria : criteria.getSubCriteria()) {
			Query subQuery = createQuery(subCriteria);
			if (subQuery != null) {
				if (criteria.isOr()) {
					shouldQueries.add(subQuery);
				}
				else if (criteria.isNegating()) {
					mustNotQueries.add(subQuery);
				}
				else {
					mustQueries.add(subQuery);
				}
			}
		}

		if (firstQuery != null) {

			if (!shouldQueries.isEmpty() && mustNotQueries.isEmpty() && mustQueries.isEmpty()) {
				shouldQueries.add(0, firstQuery);
			}
			else {

				if (negateFirstQuery) {
					mustNotQueries.add(0, firstQuery);
				}
				else {
					mustQueries.add(0, firstQuery);
				}
			}
		}

		if (shouldQueries.isEmpty() && mustNotQueries.isEmpty() && mustQueries.isEmpty()) {
			return null;
		}

		Query query = new Query.Builder().bool(boolQueryBuilder -> {

			if (!shouldQueries.isEmpty()) {
				boolQueryBuilder.should(shouldQueries);
			}

			if (!mustNotQueries.isEmpty()) {
				boolQueryBuilder.mustNot(mustNotQueries);
			}

			if (!mustQueries.isEmpty()) {
				boolQueryBuilder.must(mustQueries);
			}

			return boolQueryBuilder;
		}).build();

		return query;
	}

	@Nullable
	private static Query queryForEntries(Criteria criteria) {

		Field field = criteria.getField();

		if (field == null || criteria.getQueryCriteriaEntries().isEmpty())
			return null;

		String fieldName = field.getName();
		Assert.notNull(fieldName, "Unknown field");

		Iterator<Criteria.CriteriaEntry> it = criteria.getQueryCriteriaEntries().iterator();

		Float boost = Float.isNaN(criteria.getBoost()) ? null : criteria.getBoost();
		Query.Builder queryBuilder;

		if (criteria.getQueryCriteriaEntries().size() == 1) {
			queryBuilder = queryFor(it.next(), field, boost);
		}
		else {
			queryBuilder = new Query.Builder();
			queryBuilder.bool(boolQueryBuilder -> {
				while (it.hasNext()) {
					Criteria.CriteriaEntry entry = it.next();
					boolQueryBuilder.must(queryFor(entry, field, null).build());
				}
				boolQueryBuilder.boost(boost);
				return boolQueryBuilder;
			});

		}

		if (hasText(field.getPath())) {
			final Query query = queryBuilder.build();
			queryBuilder = new Query.Builder();
			queryBuilder.nested(nqb -> nqb //
					.path(field.getPath()) //
					.query(query) //
					.scoreMode(ChildScoreMode.Avg));
		}

		return queryBuilder.build();
	}

	private static Query.Builder queryFor(Criteria.CriteriaEntry entry, Field field, @Nullable Float boost) {

		String fieldName = field.getName();
		boolean isKeywordField = FieldType.Keyword == field.getFieldType();

		Criteria.OperationKey key = entry.getKey();
		Object value = key.hasValue() ? entry.getValue() : null;
		String searchText = value != null ? escape(value.toString()) : "UNKNOWN_VALUE";

		Query.Builder queryBuilder = new Query.Builder();
		switch (key) {
			case EXISTS:
				queryBuilder //
						.exists(eb -> eb //
								.field(fieldName) //
								.boost(boost));
				break;
			case EMPTY:
				queryBuilder //
						.bool(bb -> bb //
								.must(mb -> mb //
										.exists(eb -> eb //
												.field(fieldName) //
										)) //
								.mustNot(mnb -> mnb //
										.wildcard(wb -> wb //
												.field(fieldName) //
												.wildcard("*"))) //
								.boost(boost));
				break;
			case NOT_EMPTY:
				queryBuilder //
						.wildcard(wb -> wb //
								.field(fieldName) //
								.wildcard("*") //
								.boost(boost));
				break;
			case EQUALS:
				queryBuilder.queryString(queryStringQuery(fieldName, searchText, Operator.And, boost));
				break;
			case CONTAINS:
				queryBuilder.queryString(queryStringQuery(fieldName, '*' + searchText + '*', true, boost));
				break;
			case STARTS_WITH:
				queryBuilder.queryString(queryStringQuery(fieldName, searchText + '*', true, boost));
				break;
			case ENDS_WITH:
				queryBuilder.queryString(queryStringQuery(fieldName, '*' + searchText, true, boost));
				break;
			case EXPRESSION:
				queryBuilder.queryString(queryStringQuery(fieldName, value.toString(), boost));
				break;
			case LESS:
				queryBuilder //
						.range(rb -> rb //
								.field(fieldName) //
								.lt(JsonData.of(value)) //
								.boost(boost)); //
				break;
			case LESS_EQUAL:
				queryBuilder //
						.range(rb -> rb //
								.field(fieldName) //
								.lte(JsonData.of(value)) //
								.boost(boost)); //
				break;
			case GREATER:
				queryBuilder //
						.range(rb -> rb //
								.field(fieldName) //
								.gt(JsonData.of(value)) //
								.boost(boost)); //
				break;
			case GREATER_EQUAL:
				queryBuilder //
						.range(rb -> rb //
								.field(fieldName) //
								.gte(JsonData.of(value)) //
								.boost(boost)); //
				break;
			case BETWEEN:
				Object[] ranges = (Object[]) value;
				queryBuilder //
						.range(rb -> {
							rb.field(fieldName);
							if (ranges[0] != null) {
								rb.gte(JsonData.of(ranges[0]));
							}

							if (ranges[1] != null) {
								rb.lte(JsonData.of(ranges[1]));
							}
							rb.boost(boost); //
							return rb;
						}); //

				break;
			case FUZZY:
				queryBuilder //
						.fuzzy(fb -> fb //
								.field(fieldName) //
								.value(FieldValue.of(searchText)) //
								.boost(boost)); //
				break;
			case MATCHES:
				queryBuilder.match(matchQuery(fieldName, value.toString(), Operator.Or, boost));
				break;
			case MATCHES_ALL:
				queryBuilder.match(matchQuery(fieldName, value.toString(), Operator.And, boost));
				break;
			case IN:
				if (value instanceof Iterable<?>) {
					Iterable<?> iterable = (Iterable<?>) value;
					if (isKeywordField) {
						queryBuilder.bool(bb -> bb //
								.must(mb -> mb //
										.terms(tb -> tb //
												.field(fieldName) //
												.terms(tsb -> tsb //
														.value(toFieldValueList(iterable))))) //
								.boost(boost)); //
					}
					else {
						queryBuilder //
								.queryString(qsb -> qsb //
										.fields(fieldName) //
										.query(orQueryString(iterable)) //
										.boost(boost)); //
					}
				}
				else {
					throw new InvalidApiUsageException("value for " + fieldName + " is not an Iterable");
				}
				break;
			case NOT_IN:
				if (value instanceof Iterable<?>) {
					Iterable<?> iterable = (Iterable<?>) value;
					if (isKeywordField) {
						queryBuilder.bool(bb -> bb //
								.mustNot(mnb -> mnb //
										.terms(tb -> tb //
												.field(fieldName) //
												.terms(tsb -> tsb //
														.value(toFieldValueList(iterable))))) //
								.boost(boost)); //
					}
					else {
						queryBuilder //
								.queryString(qsb -> qsb //
										.fields(fieldName) //
										.query("NOT(" + orQueryString(iterable) + ')') //
										.boost(boost)); //
					}
				}
				else {
					throw new InvalidApiUsageException("value for " + fieldName + " is not an Iterable");
				}
				break;
			case REGEXP:
				queryBuilder //
						.regexp(rb -> rb //
								.field(fieldName) //
								.value(value.toString()) //
								.boost(boost)); //
				break;
			default:
				throw new InvalidApiUsageException("Could not build query for " + entry);
		}

		return queryBuilder;
	}

	private static List<FieldValue> toFieldValueList(Iterable<?> iterable) {
		List<FieldValue> list = new ArrayList<>();
		for (Object item : iterable) {
			list.add(item != null ? FieldValue.of(item.toString()) : null);
		}
		return list;
	}

	private static String orQueryString(Iterable<?> iterable) {
		StringBuilder sb = new StringBuilder();

		for (Object item : iterable) {

			if (item != null) {

				if (sb.length() > 0) {
					sb.append(' ');
				}
				sb.append('"');
				sb.append(escape(item.toString()));
				sb.append('"');
			}
		}

		return sb.toString();
	}

	@Nullable
	private static Query createNestedQuery(Criteria criteria) {

		boolean containsNestedCriteria = !CollectionUtils.isEmpty(criteria.getNestedCriteria());
		String name = Optional.ofNullable(criteria.getField()).map(Field::getName).orElse(null);

		if (!containsNestedCriteria || !hasText(name)) {
			if (log.isTraceEnabled()) {
				log.trace("Cannot create nested query for {}, maybe criteria has no name [{}] or nested criteria [{}]", criteria, name, criteria.getNestedCriteria());
			}
			return null;
		}

		List<Criteria> validNestedCriterias = criteria.getNestedCriteria().stream()
				.filter(it -> it.getField() != null && !CollectionUtils.isEmpty(it.getQueryCriteriaEntries()))
				.collect(Collectors.toList());

		if (CollectionUtils.isEmpty(validNestedCriterias)) {
			if (log.isTraceEnabled()) {
				log.trace("Cannot create nested query for {}, maybe nested criteria has no field or query criteria entry", criteria);
			}
			return null;
		}

		BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
		for (Criteria validNestedCriteria : validNestedCriterias) {
			nestedQueryForEntries(validNestedCriteria, boolQueryBuilder);
		}

		Query.Builder queryBuilder = new Query.Builder();
		queryBuilder.nested(nqb -> nqb //
				.path(name) //
				.query(new Query.Builder().bool(boolQueryBuilder.build()).build()) //
				.scoreMode(ChildScoreMode.Avg));

		return queryBuilder.build();
	}

	@Nullable
	private static void nestedQueryForEntries(Criteria nestedCriteria, BoolQuery.Builder boolQueryBuilder) {

		Field field = nestedCriteria.getField();

		if (field == null || nestedCriteria.getQueryCriteriaEntries().isEmpty())
			return;

		String fieldName = field.getName();
		Assert.notNull(fieldName, "Unknown field");

		Iterator<Criteria.CriteriaEntry> it = nestedCriteria.getQueryCriteriaEntries().iterator();

		Float boost = Float.isNaN(nestedCriteria.getBoost()) ? null : nestedCriteria.getBoost();


		while (it.hasNext()) {
			Criteria.CriteriaEntry entry = it.next();
			boolQueryBuilder.must(queryFor(entry, field, null).build());
		}
		boolQueryBuilder.boost(boost);
	}

	/**
	 * Returns a String where those characters that TextParser expects to be escaped are escaped by a preceding
	 * <code>\</code>. Copied from Apachae 2 licensed org.apache.lucene.queryparser.flexible.standard.QueryParserUtil
	 * class
	 */
	public static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			// These characters are part of the query syntax and must be escaped
			if (c == '\\' || c == '+' || c == '-' || c == '!' || c == '(' || c == ')' || c == ':' || c == '^' || c == '['
					|| c == ']' || c == '\"' || c == '{' || c == '}' || c == '~' || c == '*' || c == '?' || c == '|' || c == '&'
					|| c == '/') {
				sb.append('\\');
			}
			sb.append(c);
		}
		return sb.toString();
	}
}
