/*
 * Copyright 2021-2023 the original author or authors.
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
package com.mawen.search.core.convert.core.mapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.mawen.search.core.annotation.DateFormat;
import com.mawen.search.core.convert.DatePropertyValueConverter;
import com.mawen.search.core.convert.DateRangePropertyValueConverter;
import com.mawen.search.core.convert.ElasticsearchDateConverter;
import com.mawen.search.core.convert.NumberRangePropertyValueConverter;
import com.mawen.search.core.convert.TemporalPropertyValueConverter;
import com.mawen.search.core.convert.TemporalRangePropertyValueConverter;
import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.mapping.PropertyValueConverter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.mapping.SimpleElasticsearchPersistentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

public class PropertyValueConvertersUnitTests {

	@ParameterizedTest(name = "{0}") // #2018
	@MethodSource("propertyValueConverters")
	@DisplayName("should return original object on write if it cannot be converted")
	void shouldReturnOriginalObjectOnWriteIfItCannotBeConverted(PropertyValueConverter converter) {

		NoConverterForThisClass value = new NoConverterForThisClass();

		Object written = converter.write(value);

		assertThat(written).isEqualTo(value.toString());
	}

	static Stream<Arguments> propertyValueConverters() {

		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
		SimpleElasticsearchPersistentEntity<?> persistentEntity = context
				.getRequiredPersistentEntity(NoConverterForThisClass.class);
		ElasticsearchPersistentProperty persistentProperty = persistentEntity.getRequiredPersistentProperty("property");

		List<PropertyValueConverter> converters = new ArrayList<>();

		converters.add(new DatePropertyValueConverter(persistentProperty,
				Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));
		converters.add(new DateRangePropertyValueConverter(persistentProperty,
				Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));
		converters.add(new NumberRangePropertyValueConverter(persistentProperty));
		converters.add(new TemporalPropertyValueConverter(persistentProperty,
				Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));
		converters.add(new TemporalRangePropertyValueConverter(persistentProperty,
				Collections.singletonList(ElasticsearchDateConverter.of(DateFormat.basic_date))));

		return converters.stream().map(propertyValueConverter -> arguments(
				Named.of(propertyValueConverter.getClass().getSimpleName(), propertyValueConverter)));
	}

	static class NoConverterForThisClass {
		@SuppressWarnings("unused")
		@Nullable Long property;
	}
}
