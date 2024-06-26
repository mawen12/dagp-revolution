/*
 * Copyright 2020-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mawen.search.client;

import co.elastic.clients.json.JsonpMapper;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import com.google.common.collect.Lists;
import com.mawen.search.client.query.CriteriaQueryProcessor;
import com.mawen.search.core.domain.Criteria;
import org.intellij.lang.annotations.Language;
import org.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.mawen.search.utils.JsonUtils.*;
import static org.skyscreamer.jsonassert.JSONAssert.*;


@SuppressWarnings("ConstantConditions")
class CriteriaQueryProcessorUnitTests {

	private JsonpMapper mapper = new JacksonJsonpMapper();

	private final CriteriaQueryProcessor queryProcessor = new CriteriaQueryProcessor();

	@Test
	void shouldProcessTwoCriteriaWithAnd() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field1\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value1\"\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field2\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value2\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n" +
		                  "\n"; //

		Criteria criteria = new Criteria("field1").is("value1").and("field2").is("value2");

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldProcessTwoCriteriaWithOr() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"should\": [\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field1\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value1\"\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field2\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value2\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		Criteria criteria = new Criteria("field1").is("value1").or("field2").is("value2");

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldProcessMixedCriteriaWithOrAnd() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field1\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value1\"\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field3\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value3\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		],\n" +
		                  "		\"should\": [\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field2\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value2\"\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"field4\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"value4\"\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		Criteria criteria = new Criteria("field1").is("value1") //
				.or("field2").is("value2") //
				.and("field3").is("value3") //
				.or("field4").is("value4"); //

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldAddSubQuery() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"query_string\": {\n" +
		                  "					\"fields\": [\n" +
		                  "						\"lastName\"\n" +
		                  "					],\n" +
		                  "					\"query\": \"Miller\"\n" +
		                  "				}\n" +
		                  "			},\n" +
		                  "			{\n" +
		                  "				\"bool\": {\n" +
		                  "					\"should\": [\n" +
		                  "						{\n" +
		                  "							\"query_string\": {\n" +
		                  "								\"fields\": [\n" +
		                  "									\"firstName\"\n" +
		                  "								],\n" +
		                  "								\"query\": \"John\"\n" +
		                  "							}\n" +
		                  "						},\n" +
		                  "						{\n" +
		                  "							\"query_string\": {\n" +
		                  "								\"fields\": [\n" +
		                  "									\"firstName\"\n" +
		                  "								],\n" +
		                  "								\"query\": \"Jack\"\n" +
		                  "							}\n" +
		                  "						}\n" +
		                  "					]\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}";

		Criteria criteria = new Criteria("lastName").is("Miller")
				.subCriteria(new Criteria().or("firstName").is("John").or("firstName").is("Jack"));

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldProcessNestedSubCriteria() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"should\": [\n" +
		                  "			{\n" +
		                  "				\"bool\": {\n" +
		                  "					\"must\": [\n" +
		                  "						{\n" +
		                  "							\"query_string\": {\n" +
		                  "								\"fields\": [\n" +
		                  "									\"lastName\"\n" +
		                  "								],\n" +
		                  "								\"query\": \"Miller\"\n" +
		                  "							}\n" +
		                  "						},\n" +
		                  "						{\n" +
		                  "							\"bool\": {\n" +
		                  "								\"should\": [\n" +
		                  "									{\n" +
		                  "										\"query_string\": {\n" +
		                  "											\"fields\": [\n" +
		                  "												\"firstName\"\n" +
		                  "											],\n" +
		                  "											\"query\": \"John\"\n" +
		                  "										}\n" +
		                  "									},\n" +
		                  "									{\n" +
		                  "										\"query_string\": {\n" +
		                  "											\"fields\": [\n" +
		                  "												\"firstName\"\n" +
		                  "											],\n" +
		                  "											\"query\": \"Jack\"\n" +
		                  "										}\n" +
		                  "									}\n" +
		                  "								]\n" +
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
		                  "								\"fields\": [\n" +
		                  "									\"lastName\"\n" +
		                  "								],\n" +
		                  "								\"query\": \"Smith\"\n" +
		                  "							}\n" +
		                  "						},\n" +
		                  "						{\n" +
		                  "							\"bool\": {\n" +
		                  "								\"should\": [\n" +
		                  "									{\n" +
		                  "										\"query_string\": {\n" +
		                  "											\"fields\": [\n" +
		                  "												\"firstName\"\n" +
		                  "											],\n" +
		                  "											\"query\": \"Emma\"\n" +
		                  "										}\n" +
		                  "									},\n" +
		                  "									{\n" +
		                  "										\"query_string\": {\n" +
		                  "											\"fields\": [\n" +
		                  "												\"firstName\"\n" +
		                  "											],\n" +
		                  "											\"query\": \"Lucy\"\n" +
		                  "										}\n" +
		                  "									}\n" +
		                  "								]\n" +
		                  "							}\n" +
		                  "						}\n" +
		                  "					]\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		Criteria criteria = Criteria.or()
				.subCriteria(new Criteria("lastName").is("Miller")
						.subCriteria(new Criteria().or("firstName").is("John").or("firstName").is("Jack")))
				.subCriteria(new Criteria("lastName").is("Smith")
						.subCriteria(new Criteria().or("firstName").is("Emma").or("firstName").is("Lucy")));

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldBuildMatchQuery() throws JSONException {

		String expected = " {\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"match\": {\n" +
		                  "					\"field1\": {\n" +
		                  "						\"operator\": \"or\",\n" +
		                  "						\"query\": \"value1 value2\"\n" +
		                  "					}\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		Criteria criteria = new Criteria("field1").matches("value1 value2");

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldBuildMatchAllQuery() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"match\": {\n" +
		                  "					\"field1\": {\n" +
		                  "						\"operator\": \"and\",\n" +
		                  "						\"query\": \"value1 value2\"\n" +
		                  "					}\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}";

		Criteria criteria = new Criteria("field1").matchesAll("value1 value2");

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1753
	@DisplayName("should build nested query")
	void shouldBuildNestedQuery() throws JSONException {

		String expected = "{\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"nested\": {\n" +
		                  "					\"path\": \"houses.inhabitants\",\n" +
		                  "					\"query\": {\n" +
		                  "						\"query_string\": {\n" +
		                  "							\"fields\": [\n" +
		                  "								\"houses.inhabitants.lastName\"\n" +
		                  "							],\n" +
		                  "							\"query\": \"murphy\"\n" +
		                  "						}\n" +
		                  "					}\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}";

		Criteria criteria = new Criteria("houses.inhabitants.lastName").is("murphy");
		criteria.getField().setPath("houses.inhabitants");

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1909
	@DisplayName("should build query for empty property")
	void shouldBuildQueryForEmptyProperty() throws JSONException {

		String expected = "{\n" +
		                  "  \"bool\" : {\n" +
		                  "    \"must\" : [\n" +
		                  "      {\n" +
		                  "        \"bool\" : {\n" +
		                  "          \"must\" : [\n" +
		                  "            {\n" +
		                  "              \"exists\" : {\n" +
		                  "                \"field\" : \"lastName\"              }\n" +
		                  "            }\n" +
		                  "          ],\n" +
		                  "          \"must_not\" : [\n" +
		                  "            {\n" +
		                  "              \"wildcard\" : {\n" +
		                  "                \"lastName\" : {\n" +
		                  "                  \"wildcard\" : \"*\"                }\n" +
		                  "              }\n" +
		                  "            }\n" +
		                  "          ]\n" +
		                  "        }\n" +
		                  "      }\n" +
		                  "    ]\n" +
		                  "  }\n" +
		                  "}"; //

		Criteria criteria = new Criteria("lastName").empty();

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #1909
	@DisplayName("should build query for non-empty property")
	void shouldBuildQueryForNonEmptyProperty() throws JSONException {

		String expected = "{\n" +
		                  "  \"bool\" : {\n" +
		                  "    \"must\" : [\n" +
		                  "      {\n" +
		                  "        \"wildcard\" : {\n" +
		                  "          \"lastName\" : {\n" +
		                  "            \"wildcard\" : \"*\"\n" +
		                  "          }\n" +
		                  "        }\n" +
		                  "      }\n" +
		                  "    ]\n" +
		                  "  }\n" +
		                  "}\n"; //

		Criteria criteria = new Criteria("lastName").notEmpty();

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test // #2418
	void shouldBuildRegexpQuery() throws JSONException {
		String expected = " {\n" +
		                  "	\"bool\": {\n" +
		                  "		\"must\": [\n" +
		                  "			{\n" +
		                  "				\"regexp\": {\n" +
		                  "					\"field1\": {\n" +
		                  "						\"value\": \"[^abc]\"\n" +
		                  "					}\n" +
		                  "				}\n" +
		                  "			}\n" +
		                  "		]\n" +
		                  "	}\n" +
		                  "}\n";

		Criteria criteria = new Criteria("field1").regexp("[^abc]");

		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);

		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldBuildExpressionQuery() throws JSONException {

		// given
		Criteria expression = new Criteria("name").expression("姓 名");

		// when
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(expression), mapper);
		System.out.println(queryString);

		// then
		@Language("JSON") String expected = "{\n" +
				"  \"bool\": {\n" +
				"    \"must\": [\n" +
				"      {\n" +
				"        \"query_string\": {\n" +
				"          \"fields\": [\n" +
				"            \"name\"\n" +
				"          ],\n" +
				"          \"query\": \"姓 名\"\n" +
				"        }\n" +
				"      }\n" +
				"    ]\n" +
				"  }\n" +
				"}";
		assertEquals(expected, queryString, false);
	}
	
	@Test
	void shouldBuildSingleNestedCriteriaQuery() throws JSONException {

		// given
		Criteria criteria = new Criteria("extAttr").nested(
				Criteria.where("extAttr.keyCode").is("1"),
				Criteria.where("extAttr.valueId").is("3")
		);
		criteria.getField().setPath("extAttr");

		// when
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(criteria), mapper);
		System.out.println(queryString);

		// then
		@Language("JSON")
		String expected = "{\n" +
				"  \"bool\": {\n" +
				"    \"must\": [\n" +
				"      {\n" +
				"        \"nested\": {\n" +
				"          \"path\": \"extAttr\",\n" +
				"          \"query\": {\n" +
				"            \"bool\": {\n" +
				"              \"must\": [\n" +
				"                {\n" +
				"                  \"query_string\": {\n" +
				"                    \"default_operator\": \"and\",\n" +
				"                    \"fields\": [\n" +
				"                      \"extAttr.keyCode\"\n" +
				"                    ],\n" +
				"                    \"query\": \"1\"\n" +
				"                  }\n" +
				"                },\n" +
				"                {\n" +
				"                  \"query_string\": {\n" +
				"                    \"default_operator\": \"and\",\n" +
				"                    \"fields\": [\n" +
				"                      \"extAttr.valueId\"\n" +
				"                    ],\n" +
				"                    \"query\": \"3\"\n" +
				"                  }\n" +
				"                }\n" +
				"              ]\n" +
				"            }\n" +
				"          },\n" +
				"          \"score_mode\": \"avg\"\n" +
				"        }\n" +
				"      }\n" +
				"    ]\n" +
				"  }\n" +
				"}";
		assertEquals(expected, queryString, false);
	}

	@Test
	void shouldBuildMultiNestedCriteriaQuery() throws JSONException {

		// given
		Criteria first = new Criteria("extAttr").nested(
				Criteria.where("extAttr.keyCode").is("1"),
				Criteria.where("extAttr.valueId").is("3")
		);
		Criteria second = new Criteria("extAttr").nested(
				Criteria.where("extAttr.keyCode").is("2"),
				Criteria.where("extAttr.valueId").is("4")
		);

		first.getField().setPath("extAttr");
		second.getField().setPath("extAttr");

		// when
		String queryString = queryToJson(CriteriaQueryProcessor.createQuery(first.and(second)), mapper);
		System.out.println(queryString);

		// then
		@Language("JSON")
		String expected = "{\n" +
				"  \"bool\": {\n" +
				"    \"must\": [\n" +
				"      {\n" +
				"        \"nested\": {\n" +
				"          \"path\": \"extAttr\",\n" +
				"          \"query\": {\n" +
				"            \"bool\": {\n" +
				"              \"must\": [\n" +
				"                {\n" +
				"                  \"query_string\": {\n" +
				"                    \"default_operator\": \"and\",\n" +
				"                    \"fields\": [\n" +
				"                      \"extAttr.keyCode\"\n" +
				"                    ],\n" +
				"                    \"query\": \"1\"\n" +
				"                  }\n" +
				"                },\n" +
				"                {\n" +
				"                  \"query_string\": {\n" +
				"                    \"default_operator\": \"and\",\n" +
				"                    \"fields\": [\n" +
				"                      \"extAttr.valueId\"\n" +
				"                    ],\n" +
				"                    \"query\": \"3\"\n" +
				"                  }\n" +
				"                }" +
				"              ]\n" +
				"            }\n" +
				"          },\n" +
				"          \"score_mode\": \"avg\"\n" +
				"        }\n" +
				"      },\n" +
				"      {\n" +
				"        \"nested\": {\n" +
				"          \"path\": \"extAttr\",\n" +
				"          \"query\": {\n" +
				"            \"bool\": {\n" +
				"              \"must\": [\n" +
				"                {\n" +
				"                  \"query_string\": {\n" +
				"                    \"default_operator\": \"and\",\n" +
				"                    \"fields\": [\n" +
				"                      \"extAttr.keyCode\"\n" +
				"                    ],\n" +
				"                    \"query\": \"2\"\n" +
				"                  }\n" +
				"                },\n" +
				"                {\n" +
				"                  \"query_string\": {\n" +
				"                    \"default_operator\": \"and\",\n" +
				"                    \"fields\": [\n" +
				"                      \"extAttr.valueId\"\n" +
				"                    ],\n" +
				"                    \"query\": \"4\"\n" +
				"                  }\n" +
				"                }\n" +
				"              ]\n" +
				"            }\n" +
				"          },\n" +
				"          \"score_mode\": \"avg\"\n" +
				"        }\n" +
				"      }\n" +
				"    ]\n" +
				"  }\n" +
				"}";
		assertEquals(expected, queryString, false);
	}
}
