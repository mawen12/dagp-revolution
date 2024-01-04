/*
 * Copyright 2013-2023 the original author or authors.
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
package com.mawen.search.core.mapping;

import com.mawen.search.core.MappingContextBaseTests;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.annotation.Field;
import com.mawen.search.core.annotation.FieldType;
import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mapping.MappingException;
import org.springframework.data.mapping.model.Property;
import org.springframework.data.mapping.model.SimpleTypeHolder;
import org.springframework.data.util.ClassTypeInformation;
import org.springframework.data.util.TypeInformation;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.*;

public class SimpleElasticsearchPersistentEntityTests extends MappingContextBaseTests {

	@Nested
	@DisplayName("properties setup")
	class PropertiesTests {

		@Test
		void shouldThrowExceptionGivenVersionPropertyIsNotLong() {

			TypeInformation<EntityWithWrongVersionType> typeInformation = ClassTypeInformation.from(EntityWithWrongVersionType.class);
			SimpleElasticsearchPersistentEntity<EntityWithWrongVersionType> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);

			assertThatThrownBy(() -> createProperty(entity, "version")).isInstanceOf(MappingException.class);
		}

		@Test
		void shouldThrowExceptionGivenMultipleVersionPropertiesArePresent() {

			TypeInformation<EntityWithMultipleVersionField> typeInformation = ClassTypeInformation.from(EntityWithMultipleVersionField.class);
			SimpleElasticsearchPersistentEntity<EntityWithMultipleVersionField> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);
			SimpleElasticsearchPersistentProperty persistentProperty1 = createProperty(entity, "version1");
			SimpleElasticsearchPersistentProperty persistentProperty2 = createProperty(entity, "version2");
			entity.addPersistentProperty(persistentProperty1);

			assertThatThrownBy(() -> entity.addPersistentProperty(persistentProperty2)).isInstanceOf(MappingException.class);
		}

		@Test
		void shouldFindPropertiesByMappedName() {

			SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
			SimpleElasticsearchPersistentEntity<?> persistentEntity = context
					.getRequiredPersistentEntity(FieldNameEntity.class);

			ElasticsearchPersistentProperty persistentProperty = persistentEntity
					.getPersistentPropertyWithFieldName("renamed-field");

			assertThat(persistentProperty).isNotNull();
			assertThat(persistentProperty.getName()).isEqualTo("renamedField");
			assertThat(persistentProperty.getFieldName()).isEqualTo("renamed-field");
		}

		@Test
		// DATAES-799
		void shouldReportThatThereIsNoSeqNoPrimaryTermPropertyWhenThereIsNoSuchProperty() {
			TypeInformation<EntityWithoutSeqNoPrimaryTerm> typeInformation = ClassTypeInformation.from(EntityWithoutSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithoutSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);

			assertThat(entity.hasSeqNoPrimaryTermProperty()).isFalse();
		}

		@Test
		// DATAES-799
		void shouldReportThatThereIsSeqNoPrimaryTermPropertyWhenThereIsSuchProperty() {
			TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation.from(EntityWithSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);

			entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));

			assertThat(entity.hasSeqNoPrimaryTermProperty()).isTrue();
		}

		@Test
		// DATAES-799
		void shouldReturnSeqNoPrimaryTermPropertyWhenThereIsSuchProperty() {

			TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation.from(EntityWithSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);
			entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));
			EntityWithSeqNoPrimaryTerm instance = new EntityWithSeqNoPrimaryTerm();
			SeqNoPrimaryTerm seqNoPrimaryTerm = new SeqNoPrimaryTerm(1, 2);

			ElasticsearchPersistentProperty property = entity.getSeqNoPrimaryTermProperty();
			assertThat(property).isNotNull();

			entity.getPropertyAccessor(instance).setProperty(property, seqNoPrimaryTerm);

			assertThat(instance.seqNoPrimaryTerm).isSameAs(seqNoPrimaryTerm);
		}

		@Test
		// DATAES-799
		void shouldNotAllowMoreThanOneSeqNoPrimaryTermProperties() {
			TypeInformation<EntityWithSeqNoPrimaryTerm> typeInformation = ClassTypeInformation.from(EntityWithSeqNoPrimaryTerm.class);
			SimpleElasticsearchPersistentEntity<EntityWithSeqNoPrimaryTerm> entity = new SimpleElasticsearchPersistentEntity<>(
					typeInformation);
			entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm"));

			assertThatThrownBy(() -> entity.addPersistentProperty(createProperty(entity, "seqNoPrimaryTerm2")))
					.isInstanceOf(MappingException.class);
		}

		@Test // #1680
		@DisplayName("should allow fields with id property names")
		void shouldAllowFieldsWithIdPropertyNames() {
			elasticsearchConverter.get().getMappingContext().getRequiredPersistentEntity(EntityWithIdNameFields.class);
		}

		@Test
		@DisplayName("should read field from read method")
		void shouldReadFieldFromReadMethod() {
			ElasticsearchPersistentEntity<?> entity = elasticsearchConverter.get().getMappingContext().getRequiredPersistentEntity(EntityWithReadMethod.class);
			ElasticsearchPersistentProperty persistentProperty = entity.getPersistentProperty("message");

		}

	}

	// region helper functions
	private static SimpleElasticsearchPersistentProperty createProperty(SimpleElasticsearchPersistentEntity<?> entity,
			String fieldName) {

		TypeInformation<?> type = entity.getTypeInformation();
		java.lang.reflect.Field field = ReflectionUtils.findField(entity.getType(), fieldName);
		assertThat(field).isNotNull();
		Property property = Property.of(type, field);
		return new SimpleElasticsearchPersistentProperty(property, entity, SimpleTypeHolder.DEFAULT);

	}
	// endregion

	// region entities
	private static class EntityWithWrongVersionType {

		@Nullable
		@Version private String version;

		@Nullable
		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
	}

	@SuppressWarnings("unused")
	private static class EntityWithMultipleVersionField {

		@Nullable
		@Version private Long version1;
		@Nullable
		@Version private Long version2;

		@Nullable
		public Long getVersion1() {
			return version1;
		}

		public void setVersion1(Long version1) {
			this.version1 = version1;
		}

		@Nullable
		public Long getVersion2() {
			return version2;
		}

		public void setVersion2(Long version2) {
			this.version2 = version2;
		}
	}

	@SuppressWarnings("unused")
	private static class FieldNameEntity {
		@Nullable
		@Id private String id;
		@Nullable
		@Field(value = "renamed-field") private String renamedField;
	}

	private static class EntityWithoutSeqNoPrimaryTerm {}

	@SuppressWarnings("unused")
	private static class EntityWithSeqNoPrimaryTerm {
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm;
		@Nullable private SeqNoPrimaryTerm seqNoPrimaryTerm2;
	}

	@SuppressWarnings("unused")
	@Document(indexName = "fieldnames")
	private static class EntityWithIdNameFields {
		@Nullable
		@Id private String theRealId;
		@Nullable
		@Field(type = FieldType.Text, value = "document") private String document;
		@Nullable
		@Field(value = "id") private String renamedId;
	}

	@Document(indexName = "test")
	private static class EntityWithReadMethod {

		@Id
		private String id;

		private String message;

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		@Field(value = "message", type = FieldType.Text)
		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}
	}


	private static class DisableTypeHintNoSetting {
		@Nullable
		@Id String id;
	}

	@Document(indexName = "foo")
	private static class DisableTypeHintExplicitSetting {
		@Nullable
		@Id String id;
	}

	// endregion
}
