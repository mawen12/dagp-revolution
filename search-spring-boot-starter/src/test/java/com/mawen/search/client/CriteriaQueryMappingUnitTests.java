package com.mawen.search.client;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.mawen.search.client.query.CriteriaQueryProcessor;
import com.mawen.search.core.annotation.DateFormat;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.domain.Criteria;
import com.mawen.search.core.domain.SourceFilter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.query.CriteriaQuery;
import com.mawen.search.core.query.Query;
import com.mawen.search.core.query.builder.FetchSourceFilterBuilder;
import org.assertj.core.api.SoftAssertions;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;
import static com.mawen.search.utils.JsonUtils.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;

public class CriteriaQueryMappingUnitTests {

	private JsonpMapper mapper = new JacksonJsonpMapper();

	MappingElasticsearchConverter mappingElasticsearchConverter;

	// region setup
	@BeforeEach
	void setUp() {
		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(Collections.singleton(Person.class));
		mappingContext.afterPropertiesSet();

		mappingElasticsearchConverter = new MappingElasticsearchConverter(mappingContext, new GenericConversionService());
		mappingElasticsearchConverter.afterPropertiesSet();

	}
	// endregion

	// region tests
	@Test // DATAES-716
	void shouldMapNamesAndConvertValuesInCriteriaQuery() throws JSONException {

		// use POJO properties and types in the query building
		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				new Criteria("birthDate") //
						.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9)) //
						.or("birthDate").is(LocalDate.of(2019, 12, 28)) //
		);

		// mapped field name and converted parameter
		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"should\": [\n" +
		                  "			{\n" +
		                  "				\"range\": {\n" +
		                  "					\"birth-date\": {\n" +
		                  "						\"gte\": \"09.11.1989\",\n" +
		                  "						\"lte\": \"09.11.1990\"\n" +
		                  "					}\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"default_operator\": \"and\",\n" +
		                  "					\"fields\": [\n" +
		                  "						\"birth-date\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"28.12.2019\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1668
	void shouldMapNamesAndConvertValuesInCriteriaQueryForSubCriteria() throws JSONException {

		// use POJO properties and types in the query building
		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				Criteria.or().subCriteria(Criteria.where("birthDate") //
						.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9))) //
						.subCriteria(Criteria.where("birthDate").is(LocalDate.of(2019, 12, 28))) //
		);

		// mapped field name and converted parameter
		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"should\": [\n" +
		                  "			{\n" +
		                  "				\"bool\": {\n" +
		                  "					\"must\": [\n" +
		                  "						{\n" +
		                  "							\"range\": {\n" +
		                  "								\"birth-date\": {\n" +
		                  "									\"gte\": \"09.11.1989\",\n" +
		                  "									\"lte\": \"09.11.1990\"\n" +
		                  "								}\n" +
		                  "							}\n" +
		                  "						}\n" +
		                  "					]\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"bool\": {\n" +
		                  "					\"must\": [\n" +
		                  "						{\n" +
		                  "							\"query_string\": {\n" +
		                  "								\"default_operator\": \"and\",\n" +
		                  "								\"fields\": [\n" +
		                  "									\"birth-date\"\n" +
		                  "								],\n" +
		                  "								\"query\": \"28.12.2019\"\n" +
		                  "							}\n" +
		                  "						}\n" +
		                  "					]\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1668
	void shouldMapNamesAndConvertValuesInCriteriaQueryForSubCriteriaWithDate() throws JSONException {
		// use POJO properties and types in the query building
		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				Criteria.or().subCriteria(Criteria.where("birthDate") //
						.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9))) //
						.subCriteria(Criteria.where("createdDate").is(new Date(383745721653L))) //
		);

		// mapped field name and converted parameter
		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"should\": [\n" +
		                  "			{\n" +
		                  "				\"bool\": {\n" +
		                  "					\"must\": [\n" +
		                  "						{\n" +
		                  "							\"range\": {\n" +
		                  "								\"birth-date\": {\n" +
		                  "									\"gte\": \"09.11.1989\",\n" +
		                  "									\"lte\": \"09.11.1990\"\n" +
		                  "								}\n" +
		                  "							}\n" +
		                  "						}\n" +
		                  "					]\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"bool\": {\n" +
		                  "					\"must\": [\n" +
		                  "						{\n" +
		                  "							\"query_string\": {\n" +
		                  "								\"default_operator\": \"and\",\n" +
		                  "								\"fields\": [\n" +
		                  "									\"created-date\"\n" +
		                  "								],\n" +
		                  "								\"query\": \"383745721653\"\n" +
		                  "							}\n" +
		                  "						}\n" +
		                  "					]\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // DATAES-706
	void shouldMapNamesAndValuesInSubCriteriaQuery() throws JSONException {

		CriteriaQuery criteriaQuery = new CriteriaQuery( //
				new Criteria("firstName").matches("John") //
						.subCriteria(new Criteria("birthDate") //
								.between(LocalDate.of(1989, 11, 9), LocalDate.of(1990, 11, 9)) //
								.or("birthDate").is(LocalDate.of(2019, 12, 28))));

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"match\": {\n" +
		                  "					\"first-name\": {\n" +
		                  "						\"query\": \"John\"\n" +
		                  "					}\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"bool\": {\n" +
		                  "					\"should\": [\n" +
		                  "						{\n" +
		                  "							\"range\": {\n" +
		                  "								\"birth-date\": {\n" +
		                  "									\"gte\": \"09.11.1989\",\n" +
		                  "									\"lte\": \"09.11.1990\"\n" +
		                  "								}\n" +
		                  "							}\n" +
		                  "						},\n" +
		                  "						{\n" +
		                  "							\"query_string\": {\n" +
		                  "								\"default_operator\": \"and\",\n" +
		                  "								\"fields\": [\n" +
		                  "									\"birth-date\"\n" +
		                  "								],\n" +
		                  "								\"query\": \"28.12.2019\"\n" +
		                  "							}\n" +
		                  "						}\n" +
		                  "					]\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}";

		mappingElasticsearchConverter.updateQuery(criteriaQuery, Person.class);
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1753
	@DisplayName("should map names and value in nested entities")
	void shouldMapNamesAndValueInNestedEntities() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"nested\": {\n" +
		                  "					\"path\": \"per-sons\",\n" +
		                  "					\"query\": {\n" +
		                  "						\"query_string\": {\n" +
		                  "							\"default_operator\": \"and\",\n" +
		                  "							\"fields\": [\n" +
		                  "								\"per-sons.birth-date\"\n" +
		                  "							],\n" +
		                  "							\"query\": \"03.10.1999\"\n" +
		                  "						}\n" +
		                  "					},\n" +
		                  "					\"score_mode\": \"avg\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.birthDate").is(LocalDate.of(1999, 10, 3)));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, House.class);
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1753
	@DisplayName("should map names and value in nested entities with sub-fields")
	void shouldMapNamesAndValueInNestedEntitiesWithSubfields() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"nested\": {\n" +
		                  "					\"path\": \"per-sons\",\n" +
		                  "					\"query\": {\n" +
		                  "						\"query_string\": {\n" +
		                  "							\"default_operator\": \"and\",\n" +
		                  "							\"fields\": [\n" +
		                  "								\"per-sons.nick-name.keyword\"\n" +
		                  "							],\n" +
		                  "							\"query\": \"Foobar\"\n" +
		                  "						}\n" +
		                  "					},\n" +
		                  "					\"score_mode\": \"avg\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}";

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.nickName.keyword").is("Foobar"));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, House.class);
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1761
	@DisplayName("should map names and value in object entities")
	void shouldMapNamesAndValueInObjectEntities() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"default_operator\": \"and\",\n" +
		                  "					\"fields\": [\n" +
		                  "						\"per-sons.birth-date\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"03.10.1999\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}";

		CriteriaQuery criteriaQuery = new CriteriaQuery(new Criteria("persons.birthDate").is(LocalDate.of(1999, 10, 3)));
		mappingElasticsearchConverter.updateQuery(criteriaQuery, ObjectWithPerson.class);
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteriaQuery.getCriteria()), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1778
	@DisplayName("should map names in source fields and SourceFilters")
	void shouldMapNamesInSourceFieldsAndSourceFilters() {

		Query query = Query.findAll();
		// Note: we don't care if these filters make sense here, this test is only about name mapping
		query.addFields("firstName", "lastName");
		query.addSourceFilter(new FetchSourceFilterBuilder().withIncludes("firstName").withExcludes("lastName").build());

		mappingElasticsearchConverter.updateQuery(query, Person.class);

		SoftAssertions softly = new SoftAssertions();
		softly.assertThat(query.getFields()).containsExactly("first-name", "last-name");
		SourceFilter sourceFilter = query.getSourceFilter();
		softly.assertThat(sourceFilter).isNotNull();
		softly.assertThat(sourceFilter.getIncludes()).containsExactly("first-name");
		softly.assertThat(sourceFilter.getExcludes()).containsExactly("last-name");
		softly.assertAll();
	}

	// endregion
	// region helper functions

	// endregion

	// region test entities
	static class Person {

		@Nullable
		@Id String id;
		@Nullable
		@Field(value = "first-name") String firstName;
		@Nullable
		@Field(value = "last-name") String lastName;
		@Nullable
		@Field(value = "created-date", type = FieldType.Date, format = DateFormat.epoch_millis) Date createdDate;
		@Nullable
		@Field(value = "birth-date", type = FieldType.Date, format = {}, pattern = "dd.MM.uuuu") LocalDate birthDate;
	}

	static class House {
		@Nullable
		@Id String id;
		@Nullable
		@Field(value = "per-sons", type = FieldType.Nested) List<Person> persons;
	}

	static class ObjectWithPerson {
		@Nullable
		@Id String id;
		@Nullable
		@Field(value = "per-sons", type = FieldType.Object) List<Person> persons;
	}

	// endregion
}
