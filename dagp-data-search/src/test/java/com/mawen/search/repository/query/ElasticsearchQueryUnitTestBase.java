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
package com.mawen.search.repository.query;

import java.util.ArrayList;
import java.util.Collection;

import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.convert.ElasticsearchCustomConversions;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.lang.Nullable;

public class ElasticsearchQueryUnitTestBase {

	protected ElasticsearchConverter setupConverter() {
		MappingElasticsearchConverter converter = new MappingElasticsearchConverter(
				new SimpleElasticsearchMappingContext());
		Collection<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(ElasticsearchStringQueryUnitTests.CarConverter.INSTANCE);
		CustomConversions customConversions = new ElasticsearchCustomConversions(converters);
		converter.setConversions(customConversions);
		converter.afterPropertiesSet();
		return converter;
	}

	static class Car {
		@Nullable private String name;
		@Nullable private String model;

		@Nullable
		public String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		@Nullable
		public String getModel() {
			return model;
		}

		public void setModel(@Nullable String model) {
			this.model = model;
		}
	}

	enum CarConverter implements Converter<Car, String> {
		INSTANCE;

		@Override
		public String convert(ElasticsearchStringQueryUnitTests.Car car) {
			return (car.getName() != null ? car.getName() : "null") + '-'
					+ (car.getModel() != null ? car.getModel() : "null");
		}
	}

}
