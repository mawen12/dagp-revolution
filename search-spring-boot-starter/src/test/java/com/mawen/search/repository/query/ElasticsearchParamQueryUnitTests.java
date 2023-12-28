package com.mawen.search.repository.query;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.ParamQuery;
import com.mawen.search.core.annotation.QueryField;
import com.mawen.search.core.annotation.QueryField.Type;
import com.mawen.search.core.annotation.SourceFilters;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.Criteria.OperationKey;
import com.mawen.search.core.domain.Range;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.core.query.Query;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ElasticsearchParamQuery}
 */
@ExtendWith(MockitoExtension.class)
class ElasticsearchParamQueryUnitTests extends ElasticsearchQueryUnitTestBase {

	@Mock
	ElasticsearchOperations operations;

	@BeforeEach
	public void setup() {
		when(operations.getElasticsearchConverter()).thenReturn(setupConverter());
	}

	@Test
	void shouldIgnoreQuery() {
		IgnoreQuery ignoreQuery = new IgnoreQuery();
		ignoreQuery.setAge(1);
		assertThatIllegalArgumentException().isThrownBy(() -> createQuery("listByQuery", ignoreQuery));
	}

	@Test
	void shouldParseNullQueryCorrectly() throws NoSuchMethodException {

		Query query = createQuery("listByQuery", new NullQuery());

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
	}

	@Test
	void shouldIgnoreNonQueryFieldAnnotatedField() throws NoSuchMethodException {

		NonQueryFieldQuery nonQueryFieldQuery = new NonQueryFieldQuery();
		nonQueryFieldQuery.setAge(1);
		Query query = createQuery("listByQuery", nonQueryFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
	}

	@Test
	void shouldParseSingleFieldQueryCorrectly() throws NoSuchMethodException {

		// field value is null
		SingleFieldQuery singleFieldQuery = new SingleFieldQuery();
		Query query = createQuery("listByQuery", singleFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);

		// field has value
		singleFieldQuery.setAge(1);
		query = createQuery("listByQuery", singleFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		criteriaQuery = (CriteriaQuery) query;
		criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.EQUALS);
			assertThat(entry.getValue()).isEqualTo(String.valueOf(singleFieldQuery.age));
		});

	}

	@Test
	void shouldParseRangeFieldQueryCorrectly() throws NoSuchMethodException {

		RangeFieldQuery rangeFieldQuery = new RangeFieldQuery();
		Range.Bound<Integer> left = Range.Bound.inclusive(1);
		Range.Bound<Integer> right = Range.Bound.inclusive(2);
		rangeFieldQuery.setAges(Range.of(left, right));
		Query query = createQuery("listByQuery", rangeFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.BETWEEN);
			assertThat(entry.getValue()).isInstanceOf(Object[].class);
			assertThat((Object[]) entry.getValue()).hasSize(2);
			Object[] value = (Object[]) entry.getValue();
			assertThat(value[0]).isEqualTo(left);
			assertThat(value[1]).isEqualTo(right);

		});

	}

	@Test
	void shouldParseLessThanFieldQueryCorrectly() throws NoSuchMethodException {

		LessThanFieldQuery lessThanFieldQuery = new LessThanFieldQuery();
		lessThanFieldQuery.setAge(10);
		Query query = createQuery("listByQuery", lessThanFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.LESS);
			assertThat(entry.getValue()).isEqualTo(lessThanFieldQuery.getAge());
		});
	}

	@Test
	void shouldParseLessThanEqualsFieldQueryCorrectly() throws NoSuchMethodException {

		LessThanEqualsFieldQuery lessThanEqualsFieldQuery = new LessThanEqualsFieldQuery();
		lessThanEqualsFieldQuery.setAge(20);
		Query query = createQuery("listByQuery", lessThanEqualsFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.LESS_EQUAL);
			assertThat(entry.getValue()).isEqualTo(lessThanEqualsFieldQuery.getAge());
		});
	}

	@Test
	void shouldParseGreaterThanFieldQueryCorrectly() throws NoSuchMethodException {

		GreaterThanFieldQuery greaterThanFieldQuery = new GreaterThanFieldQuery();
		greaterThanFieldQuery.setAge(20);
		Query query = createQuery("listByQuery", greaterThanFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.GREATER);
			assertThat(entry.getValue()).isEqualTo(greaterThanFieldQuery.getAge());
		});
	}

	@Test
	void shouldParseGreaterThanEqualsFieldQueryCorrectly() throws NoSuchMethodException {

		GreaterThanEqualsFieldQuery greaterThanEqualsFieldQuery = new GreaterThanEqualsFieldQuery();
		greaterThanEqualsFieldQuery.setAge(20);
		Query query = createQuery("listByQuery", greaterThanEqualsFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.GREATER_EQUAL);
			assertThat(entry.getValue()).isEqualTo(greaterThanEqualsFieldQuery.getAge());
		});
	}

	@Test
	void shouldParseBeforeFieldQueryCorrectly() throws NoSuchMethodException {

		BeforeFieldQuery beforeFieldQuery = new BeforeFieldQuery();
		beforeFieldQuery.setAge(20);
		Query query = createQuery("listByQuery", beforeFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.LESS_EQUAL);
			assertThat(entry.getValue()).isEqualTo(beforeFieldQuery.getAge());
		});
	}

	@Test
	void shouldParseAfterFieldQueryCorrectly() throws NoSuchMethodException {

		AfterFieldQuery afterFieldQuery = new AfterFieldQuery();
		afterFieldQuery.setAge(20);
		Query query = createQuery("listByQuery", afterFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("age");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.GREATER_EQUAL);
			assertThat(entry.getValue()).isEqualTo(afterFieldQuery.getAge());
		});
	}

	@Test
	void shouldParseLikeFieldQueryCorrectly() throws NoSuchMethodException {

		LikeFieldQuery likeFieldQuery = new LikeFieldQuery();
		likeFieldQuery.setName("mawen");
		Query query = createQuery("listByQuery", likeFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.CONTAINS);
			assertThat(entry.getValue()).isEqualTo(likeFieldQuery.name);
		});
	}

	@Test
	void shouldParseStartingFieldQueryCorrectly() throws NoSuchMethodException {

		StartingWithFieldQuery startingWithFieldQuery = new StartingWithFieldQuery();
		startingWithFieldQuery.setName("mawen");
		Query query = createQuery("listByQuery", startingWithFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.STARTS_WITH);
			assertThat(entry.getValue()).isEqualTo(startingWithFieldQuery.name);
		});
	}

	@Test
	void shouldParseEndingFieldQueryCorrectly() throws NoSuchMethodException {

		EndingWithFieldQuery endingWithFieldQuery = new EndingWithFieldQuery();
		endingWithFieldQuery.setName("mawen");
		Query query = createQuery("listByQuery", endingWithFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.ENDS_WITH);
			assertThat(entry.getValue()).isEqualTo(endingWithFieldQuery.name);
		});
	}

	@Test
	void shouldParseEmptyFieldQueryCorrectly() throws NoSuchMethodException {

		EmptyFieldQuery emptyFieldQuery = new EmptyFieldQuery();
		emptyFieldQuery.setNameEmpty(true);
		Query query = createQuery("listByQuery", emptyFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.EMPTY);
		});


		emptyFieldQuery.setNameEmpty(false);
		query = createQuery("listByQuery", emptyFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		criteriaQuery = (CriteriaQuery) query;
		criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.NOT_EMPTY);
		});
	}

	@Test
	void shouldParseContainingFieldQueryCorrectly() throws NoSuchMethodException {

		ContainingFieldQuery containingFieldQuery = new ContainingFieldQuery();
		containingFieldQuery.setName("mawen");
		Query query = createQuery("listByQuery", containingFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.CONTAINS);
			assertThat(entry.getValue()).isEqualTo(containingFieldQuery.name);
		});
	}

	@Test
	void shouldParseNotInFieldQueryCorrectly() throws NoSuchMethodException {

		NotInFieldQuery notInFieldQuery = new NotInFieldQuery();
		notInFieldQuery.setNames(new ArrayList<>());
		Query query = createQuery("listByQuery", notInFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);


		List<String> names = new ArrayList<>();
		names.add("Jack");
		names.add("Blue");
		notInFieldQuery.setNames(names);
		query = createQuery("listByQuery", notInFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		criteriaQuery = (CriteriaQuery) query;
		criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.NOT_IN);
			assertThat(entry.getValue()).isEqualTo(names);
		});
	}

	@Test
	void shouldParseInFieldQueryCorrectly() throws NoSuchMethodException {

		InFieldQuery inFieldQuery = new InFieldQuery();
		inFieldQuery.setNames(new ArrayList<>());
		Query query = createQuery("listByQuery", inFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);


		List<String> names = new ArrayList<>();
		names.add("Jack");
		names.add("Blue");
		inFieldQuery.setNames(names);
		query = createQuery("listByQuery", inFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		criteriaQuery = (CriteriaQuery) query;
		criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.IN);
			assertThat(entry.getValue()).isEqualTo(names);
		});
	}

	@Test
	void shouldParseRegexFieldQueryCorrectly() throws NoSuchMethodException {

		RegexFieldQuery regexFieldQuery = new RegexFieldQuery();
		regexFieldQuery.setName("mawen");
		Query query = createQuery("listByQuery", regexFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.EXPRESSION);
			assertThat(entry.getValue()).isEqualTo(regexFieldQuery.name);
		});
	}

	@Test
	void shouldParseExistsFieldQueryCorrectly() throws NoSuchMethodException {

		ExistsFieldQuery existsFieldQuery = new ExistsFieldQuery();
		existsFieldQuery.setNameExists(true);
		Query query = createQuery("listByQuery", existsFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.isNegating()).isFalse();
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.EXISTS);
		});

		existsFieldQuery.setNameExists(false);
		query = createQuery("listByQuery", existsFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		criteriaQuery = (CriteriaQuery) query;
		criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.isNegating()).isTrue();
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.EXISTS);
		});
	}

	@Test
	void shouldParseNegatingSimplePropertyFieldQueryCorrectly() throws NoSuchMethodException {

		NegatingSimplePropertyFieldQuery negatingSimplePropertyFieldQuery = new NegatingSimplePropertyFieldQuery();
		negatingSimplePropertyFieldQuery.setName("mawen");
		Query query = createQuery("listByQuery", negatingSimplePropertyFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo("name");
		assertThat(criteria.getQueryCriteriaEntries()).satisfies(it -> {
			assertThat(it).hasSize(1);
			Criteria.CriteriaEntry entry = it.iterator().next();
			assertThat(entry.getKey()).isEqualTo(OperationKey.EQUALS);
			assertThat(entry.getValue()).isEqualTo(negatingSimplePropertyFieldQuery.name);
		});
	}

	@Test
	void shouldParseAllFieldQueryCorrectly() throws NoSuchMethodException {

		AllFieldQuery allFieldQuery = new AllFieldQuery();
		Range.Bound<Date> left = Range.Bound.inclusive(new Date());
		Range.Bound<Date> right = Range.Bound.exclusive(new Date());
		allFieldQuery.setTimes(Range.of(left, right));
		allFieldQuery.setPrice1LessThan(10.0);
		allFieldQuery.setPrice2LessThanEqual(20.0);
		allFieldQuery.setPrice3GreaterThan(15.0);
		allFieldQuery.setPrice4GreaterThanEqual(25.0);
		allFieldQuery.setCreateTimeBefore(new Date());
		allFieldQuery.setCreateTimeAfter(new Date());
		allFieldQuery.setNameLike("mawen");
		allFieldQuery.setNickNameStart("ma");
		allFieldQuery.setNickNameEnd("wen");
		allFieldQuery.setCode("awe");
		List<Long> idsNotIn = new ArrayList<>();
		idsNotIn.add(1L);
		idsNotIn.add(2L);
		idsNotIn.add(3L);
		allFieldQuery.setIdsNotIn(idsNotIn);
		List<Long> idsIn = new ArrayList<>();
		idsIn.add(5L);
		idsIn.add(6L);
		allFieldQuery.setIdsIn(idsIn);
		allFieldQuery.setNameRegex("a");
		allFieldQuery.setCode3Negate("Jack");
		allFieldQuery.setCode3("Mawen");

		Query query = createQuery("listByQuery", allFieldQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);

		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);
		List<Criteria> criteriaList = criteria.getCriteriaChain();
		checkCriteria(criteriaList.get(0), "times", false, OperationKey.BETWEEN, true, new Object[]{left, right});
		checkCriteria(criteriaList.get(1), "price1", false, OperationKey.LESS, true, allFieldQuery.price1LessThan);
		checkCriteria(criteriaList.get(2), "price2", false, OperationKey.LESS_EQUAL, true, allFieldQuery.price2LessThanEqual);
		checkCriteria(criteriaList.get(3), "price3", false, OperationKey.GREATER, true, allFieldQuery.price3GreaterThan);
		checkCriteria(criteriaList.get(4), "price4", false, OperationKey.GREATER_EQUAL, true, allFieldQuery.price4GreaterThanEqual);
		checkCriteria(criteriaList.get(5), "time", false, OperationKey.LESS_EQUAL, true, allFieldQuery.createTimeBefore);
		checkCriteria(criteriaList.get(6), "time", false, OperationKey.GREATER_EQUAL, true, allFieldQuery.createTimeAfter);
		checkCriteria(criteriaList.get(7), "name", false, OperationKey.CONTAINS, true, allFieldQuery.nameLike);
		checkCriteria(criteriaList.get(8), "nickName", false, OperationKey.STARTS_WITH, true, allFieldQuery.nickNameStart);
		checkCriteria(criteriaList.get(9), "nickName", false, OperationKey.ENDS_WITH, true, allFieldQuery.nickNameEnd);
		checkCriteria(criteriaList.get(10), "gender", false, OperationKey.EMPTY, false, allFieldQuery.genderEmpty);
		checkCriteria(criteriaList.get(11), "gender", false, OperationKey.NOT_EMPTY, false, allFieldQuery.genderNotEmpty);
		checkCriteria(criteriaList.get(12), "code", false, OperationKey.CONTAINS, true, allFieldQuery.code);
		checkCriteria(criteriaList.get(13), "ids", false, OperationKey.NOT_IN, true, allFieldQuery.idsNotIn);
		checkCriteria(criteriaList.get(14), "ids", false, OperationKey.IN, true, allFieldQuery.idsIn);
		checkCriteria(criteriaList.get(15), "name", false, OperationKey.EXPRESSION, true, allFieldQuery.nameRegex);
		checkCriteria(criteriaList.get(16), "code2", false, OperationKey.EXISTS, false, allFieldQuery.code2Exists);
		checkCriteria(criteriaList.get(17), "code2", true, OperationKey.EXISTS, false, allFieldQuery.code2NonExists);
		checkCriteria(criteriaList.get(18), "code3", true, OperationKey.EQUALS, true, allFieldQuery.code3Negate);
		checkCriteria(criteriaList.get(19), "code3", false, OperationKey.EQUALS, true, allFieldQuery.code3);
	}

	@Test
	void shouldParseSortQueryCorrectly() throws NoSuchMethodException {

		// sort value is null
		SortQuery sortQuery = new SortQuery();
		Query query = createQuery("listByQuery", sortQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);
		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);

		// sort value is unsorted
		sortQuery.setSort(Sort.unsorted());
		query = createQuery("listByQuery", sortQuery);

		checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(query);
		criteriaQuery = (CriteriaQuery) query;
		criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);

		// sort value exists
		sortQuery.setSort(Sort.by("name").ascending().and(Sort.by("age").descending()));
		query = createQuery("listByQuery", sortQuery);

		assertThat(query).isInstanceOf(CriteriaQuery.class);
		assertThat(Objects.requireNonNull(query.getSort()).isSorted()).isTrue();
		assertThat(query.getPageable().isUnpaged()).isTrue();
		assertThat(query.getHighlightQuery()).isEmpty();
		assertThat(query.getSourceFilter()).isNull();
		criteriaQuery = (CriteriaQuery) query;
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteriaQuery.getCriteria());
		assertThat(query.getSort()).isEqualTo(sortQuery.sort);
	}

	@Test
	void shouldParseFilterCorrectly() throws NoSuchMethodException {

		Query query = createQuery("listWithFilter", new NullQuery());

		assertThat(query).isInstanceOf(CriteriaQuery.class);
		assertThat(Objects.requireNonNull(query.getSort()).isUnsorted()).isTrue();
		assertThat(query.getPageable().isUnpaged()).isTrue();
		assertThat(query.getHighlightQuery()).isEmpty();
		assertThat(query.getSourceFilter()).isNotNull();
		CriteriaQuery criteriaQuery = (CriteriaQuery) query;
		Criteria criteria = criteriaQuery.getCriteria();
		checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(criteria);

		assertThat(criteriaQuery.getSourceFilter().getIncludes()).containsExactly("a", "b", "c");
		assertThat(criteriaQuery.getSourceFilter().getExcludes()).containsExactly("d", "e", "f");
	}

	private void checkQueryThenUnSortAndUnpagedAndHighlightQueryIsNullAndSourceFilterIsNull(Query query) {
		assertThat(query).isInstanceOf(CriteriaQuery.class);
		assertThat(Objects.requireNonNull(query.getSort()).isUnsorted()).isTrue();
		assertThat(query.getPageable().isUnpaged()).isTrue();
		assertThat(query.getHighlightQuery()).isEmpty();
		assertThat(query.getSourceFilter()).isNull();
	}

	private void checkCriteriaThenQueryCriteriaEntriesIsEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(Criteria criteria) {
		assertThat(criteria.getQueryCriteriaEntries()).isEmpty();
		assertThat(criteria.getFilterCriteriaEntries()).isEmpty();
		assertThat(criteria.getSubCriteria()).isEmpty();
	}

	private void checkCriteriaThenQueryCriteriaEntriesIsNotEmptyAndFilterCriteriaEntriesIsEmptyAndSubCriteriaIsEmpty(Criteria criteria) {
		assertThat(criteria.getQueryCriteriaEntries()).isNotEmpty();
		assertThat(criteria.getFilterCriteriaEntries()).isEmpty();
		assertThat(criteria.getSubCriteria()).isEmpty();
	}

	private void checkCriteria(Criteria criteria, String fieldName, boolean isNegating, OperationKey type, boolean hasValue, Object value) {
		assertThat(Objects.requireNonNull(criteria.getField()).getName()).isEqualTo(fieldName);
		assertThat(criteria.isNegating()).isEqualTo(isNegating);
		assertThat(criteria.getQueryCriteriaEntries()).hasSize(1);
		Criteria.CriteriaEntry entry = criteria.getQueryCriteriaEntries().iterator().next();
		assertThat(entry.getKey()).isEqualTo(type);
		if (hasValue) {
			assertThat(entry.getValue()).isEqualTo(value);
		}
	}

	private Query createQuery(String methodName, Object... args) throws NoSuchMethodException {

		Class<?>[] argTypes = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
		ElasticsearchQueryMethod queryMethod = getQueryMethod(methodName, argTypes);
		ElasticsearchParamQuery elasticsearchParamQuery = queryForMethod(queryMethod);
		return elasticsearchParamQuery.createQuery(new ElasticsearchParametersParameterAccessor(queryMethod, args));
	}

	private ElasticsearchParamQuery queryForMethod(ElasticsearchQueryMethod method) {
		return new ElasticsearchParamQuery(method, operations);
	}

	private ElasticsearchQueryMethod getQueryMethod(String name, Class<?>... parameters) throws NoSuchMethodException {
		Method method = SampleRepository.class.getMethod(name, parameters);
		return new ElasticsearchQueryMethod(method, new DefaultRepositoryMetadata(SampleRepository.class),
				new SpelAwareProxyProjectionFactory(), operations.getElasticsearchConverter().getMappingContext());
	}

	private interface SampleRepository extends Repository<Person, String> {
		List<Person> listByQuery(IgnoreQuery query);
		List<Person> listByQuery(@ParamQuery NullQuery query);
		List<Person> listByQuery(@ParamQuery NonQueryFieldQuery query);
		List<Person> listByQuery(@ParamQuery SingleFieldQuery query);
		List<Person> listByQuery(@ParamQuery RangeFieldQuery query);
		List<Person> listByQuery(@ParamQuery LessThanFieldQuery query);
		List<Person> listByQuery(@ParamQuery LessThanEqualsFieldQuery query);
		List<Person> listByQuery(@ParamQuery GreaterThanFieldQuery query);
		List<Person> listByQuery(@ParamQuery GreaterThanEqualsFieldQuery query);
		List<Person> listByQuery(@ParamQuery BeforeFieldQuery query);
		List<Person> listByQuery(@ParamQuery AfterFieldQuery query);
		List<Person> listByQuery(@ParamQuery LikeFieldQuery query);
		List<Person> listByQuery(@ParamQuery StartingWithFieldQuery query);
		List<Person> listByQuery(@ParamQuery EndingWithFieldQuery query);
		List<Person> listByQuery(@ParamQuery EmptyFieldQuery query);
		List<Person> listByQuery(@ParamQuery ContainingFieldQuery query);
		List<Person> listByQuery(@ParamQuery NotInFieldQuery query);
		List<Person> listByQuery(@ParamQuery InFieldQuery query);
		List<Person> listByQuery(@ParamQuery RegexFieldQuery query);
		List<Person> listByQuery(@ParamQuery ExistsFieldQuery query);
		List<Person> listByQuery(@ParamQuery NegatingSimplePropertyFieldQuery query);
		List<Person> listByQuery(@ParamQuery AllFieldQuery query);
		List<Person> listByQuery(@ParamQuery SortQuery query);

		@SourceFilters(
				includes = {"a", "b", "c"},
				excludes = {"d", "e", "f"}
		)
		List<Person> listWithFilter(@ParamQuery NullQuery query);
	}

	@Data
	static class IgnoreQuery {

		@QueryField("age")
		private Integer age;
	}

	@Data
	static class NullQuery {}

	@Data
	static class NonQueryFieldQuery {

		private Integer age;
	}

	@Data
	static class SingleFieldQuery {

		@QueryField("age")
		private Integer age;
	}

	@Data
	static class RangeFieldQuery {

		@QueryField(value = "age", type = Type.BETWEEN)
		private Range<Integer> ages;
	}

	@Data
	static class LessThanFieldQuery {
		@QueryField(value = "age", type = Type.LESS_THAN)
		private Integer age;
	}

	@Data
	static class LessThanEqualsFieldQuery {
		@QueryField(value = "age", type = Type.LESS_THAN_EQUAL)
		private Integer age;
	}

	@Data
	static class GreaterThanFieldQuery {
		@QueryField(value = "age", type = Type.GREATER_THAN)
		private Integer age;
	}

	@Data
	static class GreaterThanEqualsFieldQuery {
		@QueryField(value = "age", type = Type.GREATER_THAN_EQUAL)
		private Integer age;
	}

	@Data
	static class BeforeFieldQuery {
		@QueryField(value = "age", type = Type.BEFORE)
		private Integer age;
	}

	@Data
	static class AfterFieldQuery {
		@QueryField(value = "age", type = Type.AFTER)
		private Integer age;
	}

	@Data
	static class LikeFieldQuery {
		@QueryField(value = "name", type = Type.LIKE)
		private String name;
	}

	@Data
	static class StartingWithFieldQuery {
		@QueryField(value = "name", type = Type.STARTING_WITH)
		private String name;
	}

	@Data
	static class EndingWithFieldQuery {
		@QueryField(value = "name", type = Type.ENDING_WITH)
		private String name;
	}

	@Data
	static class EmptyFieldQuery {
		@QueryField(value = "name", type = Type.EMPTY)
		private Boolean nameEmpty;
	}

	@Data
	static class ContainingFieldQuery {
		@QueryField(value = "name", type = Type.CONTAINING)
		private String name;
	}

	@Data
	static class NotInFieldQuery {
		@QueryField(value = "name", type = Type.NOT_IN)
		private List<String> names;
	}

	@Data
	static class InFieldQuery {
		@QueryField(value = "name", type = Type.IN)
		private List<String> names;
	}

	@Data
	static class RegexFieldQuery {
		@QueryField(value = "name", type = Type.REGEX)
		private String name;
	}

	@Data
	static class ExistsFieldQuery {
		@QueryField(value = "name", type = Type.EXISTS)
		private Boolean nameExists;
	}

	@Data
	static class NegatingSimplePropertyFieldQuery {
		@QueryField(value = "name", type = Type.NEGATING_SIMPLE_PROPERTY)
		private String name;
	}

	@Data
	static class AllFieldQuery {

		@QueryField(value = "times", type = Type.BETWEEN)
		private Range<Date> times;

		@QueryField(value = "price1", type = Type.LESS_THAN)
		private Double price1LessThan;

		@QueryField(value = "price2", type = Type.LESS_THAN_EQUAL)
		private Double price2LessThanEqual;

		@QueryField(value = "price3", type = Type.GREATER_THAN)
		private Double price3GreaterThan;

		@QueryField(value = "price4", type = Type.GREATER_THAN_EQUAL)
		private Double price4GreaterThanEqual;

		@QueryField(value = "time", type = Type.BEFORE)
		private Date createTimeBefore;

		@QueryField(value = "time", type = Type.AFTER)
		private Date createTimeAfter;

		@QueryField(value = "name", type = Type.LIKE)
		private String nameLike;

		@QueryField(value = "nickName", type = Type.STARTING_WITH)
		private String nickNameStart;

		@QueryField(value = "nickName", type = Type.ENDING_WITH)
		private String nickNameEnd;

		@QueryField(value = "gender", type = Type.EMPTY)
		private Boolean genderEmpty = true;

		@QueryField(value = "gender", type = Type.EMPTY)
		private Boolean genderNotEmpty = false;

		@QueryField(value = "code", type = Type.CONTAINING)
		private String code;

		@QueryField(value = "ids", type = Type.NOT_IN)
		private List<Long> idsNotIn;

		@QueryField(value = "ids", type = Type.IN)
		private List<Long> idsIn;

		@QueryField(value = "name", type = Type.REGEX)
		private String nameRegex;

		@QueryField(value = "code2", type = Type.EXISTS)
		private Boolean code2Exists = true;

		@QueryField(value = "code2", type = Type.EXISTS)
		private Boolean code2NonExists = false;

		@QueryField(value = "code3", type = Type.NEGATING_SIMPLE_PROPERTY)
		private String code3Negate;

		@QueryField(value = "code3", type = Type.SIMPLE_PROPERTY)
		private String code3;
	}

	@Data
	static class SortQuery {

		Sort sort;

	}

	@Data
	@Document(indexName = "test1")
	static class Person {

		@Id
		private String id;
		public int age;
	}
}