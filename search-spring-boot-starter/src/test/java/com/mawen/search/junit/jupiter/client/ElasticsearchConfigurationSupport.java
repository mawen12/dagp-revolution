/*
 * Copyright 2018-2023 the original author or authors.
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
package com.mawen.search.junit.jupiter.client;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.convert.ElasticsearchCustomConversions;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.refresh.RefreshPolicy;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

@Configuration(proxyBeanMethods = false)
public class ElasticsearchConfigurationSupport {

	@Bean
	public ElasticsearchConverter elasticsearchEntityMapper(SimpleElasticsearchMappingContext elasticsearchMappingContext,
			ElasticsearchCustomConversions elasticsearchCustomConversions) {

		MappingElasticsearchConverter elasticsearchConverter = new MappingElasticsearchConverter(
				elasticsearchMappingContext);
		elasticsearchConverter.setConversions(elasticsearchCustomConversions);
		return elasticsearchConverter;
	}

	@Bean
	public SimpleElasticsearchMappingContext elasticsearchMappingContext(
			ElasticsearchCustomConversions elasticsearchCustomConversions) {

		SimpleElasticsearchMappingContext mappingContext = new SimpleElasticsearchMappingContext();
		mappingContext.setInitialEntitySet(getInitialEntitySet());
		mappingContext.setSimpleTypeHolder(elasticsearchCustomConversions.getSimpleTypeHolder());

		return mappingContext;
	}

	@Bean
	public ElasticsearchCustomConversions elasticsearchCustomConversions() {
		return new ElasticsearchCustomConversions(Collections.emptyList());
	}

	protected Collection<String> getMappingBasePackages() {

		Package mappingBasePackage = getClass().getPackage();
		return Collections.singleton(mappingBasePackage == null ? null : mappingBasePackage.getName());
	}

	protected Set<Class<?>> getInitialEntitySet() {

		Set<Class<?>> initialEntitySet = new HashSet<>();

		for (String basePackage : getMappingBasePackages()) {
			initialEntitySet.addAll(scanForEntities(basePackage));
		}

		return initialEntitySet;
	}

	protected Set<Class<?>> scanForEntities(String basePackage) {

		if (!StringUtils.hasText(basePackage)) {
			return Collections.emptySet();
		}

		Set<Class<?>> initialEntitySet = new HashSet<>();

		ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(
				false);
		componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));

		for (BeanDefinition candidate : componentProvider.findCandidateComponents(basePackage)) {

			String beanClassName = candidate.getBeanClassName();

			if (beanClassName != null) {
				try {
					initialEntitySet
							.add(ClassUtils.forName(beanClassName, ElasticsearchConfigurationSupport.class.getClassLoader()));
				} catch (ClassNotFoundException | LinkageError ignored) {}
			}
		}

		return initialEntitySet;
	}

	@Nullable
	protected RefreshPolicy refreshPolicy() {
		return null;
	}

	protected FieldNamingStrategy fieldNamingStrategy() {
		return PropertyNameFieldNamingStrategy.INSTANCE;
	}


}
