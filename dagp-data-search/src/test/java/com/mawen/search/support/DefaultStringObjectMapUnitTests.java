package com.mawen.search.support;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DefaultStringObjectMapUnitTests {

	private final SOM stringObjectMap = new SOM();

	@BeforeEach
	void setUp() {
		String json = "{\n" +
		              "  \"index\": {\n" +
		              "    \"some\": {\n" +
		              "      \"deeply\": {\n" +
		              "        \"nested\": {\n" +
		              "          \"answer\": 42\n" +
		              "        }\n" +
		              "      }\n" +
		              "    }\n" +
		              "  }\n" +
		              "}\n";
		stringObjectMap.fromJson(json);
	}

	@Test
	@DisplayName("should parse key path")
	void shouldParseKeyPath() {
		assertThat(stringObjectMap.path("index.some.deeply.nested.answer")).isEqualTo(42);
	}

	@Test
	@DisplayName("should return null on non existing path")
	void shouldReturnNullOnNonExistingPath() {
		assertThat(stringObjectMap.path("index.some.deeply.nested.question")).isNull();
	}

	@Test
	@DisplayName("should return map object on partial path")
	void shouldReturnMapObjectOnPartialPath() {
		Object object = stringObjectMap.path("index.some.deeply.nested");
		assertThat(object).isNotNull().isInstanceOf(Map.class);
		// noinspection unchecked
		Map<String, Object> map = (Map<String, Object>) object;
		assertThat(map.get("answer")).isEqualTo(42);
	}

	static class SOM extends DefaultStringObjectMap<SOM> {}
}
