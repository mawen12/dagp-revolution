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
package com.mawen.search.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.LocalDate;

import com.mawen.search.core.mapping.ElasticsearchPersistentProperty;
import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.mapping.SimpleElasticsearchPersistentEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.AliasFor;
import org.springframework.data.annotation.Id;
import org.springframework.lang.Nullable;

import static org.assertj.core.api.Assertions.*;

class ComposableAnnotationUnitTests {

	private static SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();


	@Test
	@DisplayName("Document annotation should be composable")
	void documentAnnotationShouldBeComposable() {

		SimpleElasticsearchPersistentEntity<?> entity = mappingContext
				.getRequiredPersistentEntity(ComposedAnnotationEntity.class);

		assertThat(entity.getIndexCoordinates()).isEqualTo(IndexCoordinates.of("test-no-create"));
	}

	@Test
	@DisplayName("Field annotation should be composable")
	void fieldAnnotationShouldBeComposable() {
		SimpleElasticsearchPersistentEntity<?> entity = mappingContext
				.getRequiredPersistentEntity(ComposedAnnotationEntity.class);

		ElasticsearchPersistentProperty property = entity.getRequiredPersistentProperty("nullValue");

		assertThat(property.getFieldName()).isEqualTo("null-value");
		assertThat(property.storeNullValue()).isTrue();
	}

	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ ElementType.TYPE })
	@Document(indexName = "")
	public @interface DocumentNoCreate {

		@AliasFor(value = "indexName", annotation = Document.class)
		String indexName();
	}

	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Field(storeNullValue = true)
	public @interface NullValueField {
		@AliasFor(value = "value", annotation = Field.class)
		String name();
	}

	@Inherited
	@Documented
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.FIELD)
	@Field(type = FieldType.Date, format = DateFormat.date)
	public @interface LocalDateField {
		@AliasFor(value = "value", annotation = Field.class)
		String name() default "";
	}
	@DocumentNoCreate(indexName = "test-no-create")
	static class ComposedAnnotationEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@NullValueField(name = "null-value") private String nullValue;
		@Nullable
		@LocalDateField private LocalDate theDate;

		@Nullable
		public String getId() {
			return id;
		}

		public void setId(@Nullable String id) {
			this.id = id;
		}

		@Nullable
		public String getNullValue() {
			return nullValue;
		}

		public void setNullValue(@Nullable String nullValue) {
			this.nullValue = nullValue;
		}

		@Nullable
		public LocalDate getTheDate() {
			return theDate;
		}

		public void setTheDate(@Nullable LocalDate theDate) {
			this.theDate = theDate;
		}
	}
}
