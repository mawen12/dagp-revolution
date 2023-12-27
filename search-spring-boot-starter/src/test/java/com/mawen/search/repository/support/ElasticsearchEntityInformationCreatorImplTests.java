/*
 * Copyright 2014-2023 the original author or authors.
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
package com.mawen.search.repository.support;

import java.util.HashSet;
import java.util.Set;

import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.mapping.MappingException;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Florian Hopf
 * @author Mark Paluch
 */
public class ElasticsearchEntityInformationCreatorImplTests {

	ElasticsearchEntityInformationCreatorImpl entityInfoCreator;

	@BeforeEach
	public void before() {
		SimpleElasticsearchMappingContext context = new SimpleElasticsearchMappingContext();
		Set<Class<?>> entites = new HashSet<>();
		entites.add(EntityNoId.class);
		context.setInitialEntitySet(entites);
		entityInfoCreator = new ElasticsearchEntityInformationCreatorImpl(context);
	}

	@Test
	public void shouldThrowMappingExceptionOnMissingEntity() {
		assertThatThrownBy(() -> entityInfoCreator.getEntityInformation(String.class)).isInstanceOf(MappingException.class);
	}

	@Test
	public void shouldThrowIllegalArgumentExceptionOnMissingIdAnnotation() {
		assertThatThrownBy(() -> entityInfoCreator.getEntityInformation(EntityNoId.class))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No id property found");
	}

	@Document(indexName = "whatever")
	static class EntityNoId {

	}
}
