package com.mawen.search.core.domain;

import com.mawen.search.InvalidApiUsageException;
import lombok.Getter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import java.util.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class Criteria {
	public static final String CRITERIA_VALUE_SEPARATOR = " ";
	private final CriteriaChain criteriaChain = new CriteriaChain();
	private final Set<CriteriaEntry> queryCriteriaEntries = new LinkedHashSet<>();
	private final Set<CriteriaEntry> filterCriteriaEntries = new LinkedHashSet<>();
	private final Set<Criteria> subCriteria = new LinkedHashSet<>();
	private @Nullable Field field;
	private float boost = Float.NaN;
	private boolean negating = false;

	// region criteria creation

	public Criteria() {
	}

	public Criteria(String fieldName) {
		this(new SimpleField(fieldName));
	}

	public Criteria(Field field) {

		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.field = field;
		this.criteriaChain.add(this);
	}

	public Criteria(List<Criteria> criteriaChain, String fieldName) {
		this(criteriaChain, new SimpleField(fieldName));
	}

	public Criteria(List<Criteria> criteriaChain, Field field) {

		Assert.notNull(criteriaChain, "CriteriaChain must not be null");
		Assert.notNull(field, "Field for criteria must not be null");
		Assert.hasText(field.getName(), "Field.name for criteria must not be null/empty");

		this.field = field;
		this.criteriaChain.addAll(criteriaChain);
		this.criteriaChain.add(this);
	}

	public static Criteria and() {
		return new Criteria();
	}

	public static Criteria or() {
		return new OrCriteria();
	}

	public static Criteria where(String fieldName) {
		return new Criteria(fieldName);
	}

	public static Criteria where(Field field) {
		return new Criteria(field);
	}
	// endregion

	// region criteria attributes

	@Nullable
	public Field getField() {
		return this.field;
	}

	public Set<CriteriaEntry> getQueryCriteriaEntries() {
		return Collections.unmodifiableSet(this.queryCriteriaEntries);
	}

	public Set<CriteriaEntry> getFilterCriteriaEntries() {
		return Collections.unmodifiableSet(this.filterCriteriaEntries);
	}

	public Operator getOperator() {
		return Operator.AND;
	}

	public List<Criteria> getCriteriaChain() {
		return Collections.unmodifiableList(this.criteriaChain);
	}

	public Criteria not() {
		this.negating = true;
		return this;
	}

	public Criteria boost(float boost) {

		Assert.isTrue(boost >= 0, "boost must not be negative");

		this.boost = boost;
		return this;
	}

	public boolean isAnd() {
		return getOperator() == Operator.AND;
	}

	public boolean isOr() {
		return getOperator() == Operator.OR;
	}

	// endregion

	// region criteria chaining

	public Criteria and(Field field) {
		return new Criteria(criteriaChain, field);
	}

	public Criteria and(String fieldName) {
		return new Criteria(criteriaChain, fieldName);
	}

	public Criteria and(Criteria criteria) {

		Assert.notNull(criteria, "Cannot chain 'null' criteria.");

		this.criteriaChain.add(criteria);
		return this;
	}

	public Criteria and(Criteria... criterias) {

		Assert.notNull(criterias, "Cannot chain 'null' criterias.");

		this.criteriaChain.addAll(Arrays.asList(criterias));
		return this;
	}

	public Criteria or(Field field) {
		return new OrCriteria(this.criteriaChain, field);
	}

	public Criteria or(String fieldName) {
		return or(new SimpleField(fieldName));
	}

	public Criteria or(Criteria criteria) {

		Assert.notNull(criteria, "Cannot chain 'null' criteria.");
		Assert.notNull(criteria.getField(), "Cannot chain Criteria with no field");

		Criteria orCriteria = new OrCriteria(this.criteriaChain, criteria.getField());
		orCriteria.queryCriteriaEntries.addAll(criteria.queryCriteriaEntries);
		orCriteria.filterCriteriaEntries.addAll(criteria.filterCriteriaEntries);
		return orCriteria;
	}

	public Criteria subCriteria(Criteria criteria) {

		Assert.notNull(criteria, "criteria must not be null");

		subCriteria.add(criteria);
		return this;
	}

	// endregion

	// region criteria entries - query

	public Criteria is(Object o) {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EQUALS, o));
		return this;
	}

	public Criteria exists() {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EXISTS));
		return this;
	}

	public Criteria between(@Nullable Object lowerBound, @Nullable Object upperBound) {

		if (lowerBound == null && upperBound == null) {
			throw new InvalidApiUsageException("Range [* TO *] is not allowed");
		}

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.BETWEEN, new Object[]{lowerBound, upperBound}));
		return this;
	}

	public Criteria startsWith(String s) {

		Assert.notNull(s, "s may not be null");

		assertNoBlankInWildcardQuery(s, false, true);
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.STARTS_WITH, s));
		return this;
	}

	public Criteria contains(String s) {

		Assert.notNull(s, "s may not be null");

		assertNoBlankInWildcardQuery(s, true, true);
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.CONTAINS, s));
		return this;
	}

	public Criteria endsWith(String s) {

		Assert.notNull(s, "s may not be null");

		assertNoBlankInWildcardQuery(s, true, false);
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.ENDS_WITH, s));
		return this;
	}

	public Criteria in(Object... values) {
		return in(toCollection(values));
	}

	public Criteria in(Iterable<?> values) {

		Assert.notNull(values, "Collection of 'in' values must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.IN, values));
		return this;
	}

	public Criteria notIn(Object... values) {
		return notIn(toCollection(values));
	}

	public Criteria notIn(Iterable<?> values) {

		Assert.notNull(values, "Collection of 'NotIn' values must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.NOT_IN, values));
		return this;
	}

	public Criteria expression(String s) {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EXPRESSION, s));
		return this;
	}

	public Criteria fuzzy(String s) {
		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.FUZZY, s));
		return this;
	}

	public Criteria lessThanEqual(Object upperBound) {

		Assert.notNull(upperBound, "upperBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.LESS_EQUAL, upperBound));
		return this;
	}

	public Criteria lessThan(Object upperBound) {

		Assert.notNull(upperBound, "upperBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.LESS, upperBound));
		return this;
	}

	public Criteria greaterThanEqual(Object lowerBound) {

		Assert.notNull(lowerBound, "lowerBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.GREATER_EQUAL, lowerBound));
		return this;
	}

	public Criteria greaterThan(Object lowerBound) {

		Assert.notNull(lowerBound, "lowerBound must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.GREATER, lowerBound));
		return this;
	}

	public Criteria matches(Object value) {

		Assert.notNull(value, "value must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.MATCHES, value));
		return this;
	}

	public Criteria matchesAll(Object value) {

		Assert.notNull(value, "value must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.MATCHES_ALL, value));
		return this;
	}

	public Criteria empty() {

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.EMPTY));
		return this;
	}

	public Criteria notEmpty() {

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.NOT_EMPTY));
		return this;
	}

	/**
	 * Add a {@link OperationKey#REGEXP} entry to the {@link #queryCriteriaEntries}.
	 *
	 * @param value the regexp value to match
	 * @return this object
	 * @since 5.1
	 */
	public Criteria regexp(String value) {

		Assert.notNull(value, "value must not be null");

		queryCriteriaEntries.add(new CriteriaEntry(OperationKey.REGEXP, value));
		return this;
	}

	// endregion

	// region helper functions
	private void assertNoBlankInWildcardQuery(String searchString, boolean leadingWildcard, boolean trailingWildcard) {

		if (searchString.contains(CRITERIA_VALUE_SEPARATOR)) {
			throw new InvalidApiUsageException("Cannot constructQuery '" + (leadingWildcard ? "*" : "") + '"'
					+ searchString + '"' + (trailingWildcard ? "*" : "") + "'. Use expression or multiple clauses instead.");
		}
	}

	private List<Object> toCollection(Object... values) {
		return Arrays.asList(values);
	}

	// endregion

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Criteria criteria = (Criteria) o;

		if (Float.compare(criteria.boost, boost) != 0)
			return false;
		if (negating != criteria.negating)
			return false;
		if (!Objects.equals(field, criteria.field))
			return false;
		if (!queryCriteriaEntries.equals(criteria.queryCriteriaEntries))
			return false;
		if (!filterCriteriaEntries.equals(criteria.filterCriteriaEntries))
			return false;
		return subCriteria.equals(criteria.subCriteria);
	}

	@Override
	public int hashCode() {
		int result = field != null ? field.hashCode() : 0;
		result = 31 * result + (boost != +0.0f ? Float.floatToIntBits(boost) : 0);
		result = 31 * result + (negating ? 1 : 0);
		result = 31 * result + queryCriteriaEntries.hashCode();
		result = 31 * result + filterCriteriaEntries.hashCode();
		result = 31 * result + subCriteria.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Criteria{" + //
				"field=" + field + //
				", boost=" + boost + //
				", negating=" + negating + //
				", queryCriteriaEntries=" + queryCriteriaEntries + //
				", filterCriteriaEntries=" + filterCriteriaEntries + //
				", subCriteria=" + subCriteria + //
				'}'; //
	}

	public enum Operator {
		AND, //
		OR //
	}

	public enum OperationKey { //
		EQUALS, //
		CONTAINS, //
		STARTS_WITH, //
		ENDS_WITH, //
		EXPRESSION, //
		BETWEEN, //
		FUZZY, //
		MATCHES, //
		MATCHES_ALL, //
		IN, //
		NOT_IN, //
		WITHIN, //
		BBOX, //
		LESS, //
		LESS_EQUAL, //
		GREATER, //
		GREATER_EQUAL, //
		EXISTS, //
		GEO_INTERSECTS, //
		GEO_IS_DISJOINT, //
		GEO_WITHIN, //
		GEO_CONTAINS, //
		EMPTY, //
		NOT_EMPTY, //
		REGEXP;

		public boolean hasNoValue() {
			return this == OperationKey.EXISTS || this == OperationKey.EMPTY || this == OperationKey.NOT_EMPTY;
		}

		public boolean hasValue() {
			return !hasNoValue();
		}
	}

	@SuppressWarnings("unused")
	static class OrCriteria extends Criteria {

		public OrCriteria() {
			super();
		}

		public OrCriteria(String fieldName) {
			super(fieldName);
		}

		public OrCriteria(Field field) {
			super(field);
		}

		public OrCriteria(List<Criteria> criteriaChain, String fieldName) {
			super(criteriaChain, fieldName);
		}

		public OrCriteria(List<Criteria> criteriaChain, Field field) {
			super(criteriaChain, field);
		}

		@Override
		public Operator getOperator() {
			return Operator.OR;
		}
	}

	public static class CriteriaChain extends LinkedList<Criteria> {}

	public static class CriteriaEntry {

		private final OperationKey key;
		@Nullable
		private Object value;

		protected CriteriaEntry(OperationKey key) {

			Assert.isTrue(key.hasNoValue(),
					"key must be OperationKey.EXISTS, OperationKey.EMPTY or OperationKey.NOT_EMPTY for this call");

			this.key = key;
		}

		CriteriaEntry(OperationKey key, Object value) {

			Assert.notNull(key, "key must not be null");
			Assert.notNull(value, "value must not be null");

			this.key = key;
			this.value = value;
		}

		public OperationKey getKey() {
			return key;
		}

		public Object getValue() {

			Assert.isTrue(key != OperationKey.EXISTS, key.name() + " has no value");
			Assert.notNull(value, "unexpected null value");

			return value;
		}

		public void setValue(Object value) {

			Assert.notNull(value, "value must not be null");

			this.value = value;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;

			CriteriaEntry that = (CriteriaEntry) o;

			if (key != that.key)
				return false;
			return Objects.equals(value, that.value);
		}

		@Override
		public int hashCode() {
			int result = key.hashCode();
			result = 31 * result + (value != null ? value.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			return "CriteriaEntry{" + "key=" + key + ", value=" + value + '}';
		}
	}
}
