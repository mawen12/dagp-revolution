package com.mawen.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.mawen.search.client.ElasticsearchTemplate;
import com.mawen.search.core.ElasticsearchOperations;
import com.mawen.search.core.annotation.Document;
import com.mawen.search.core.convert.ElasticsearchConverter;
import com.mawen.search.core.convert.ElasticsearchCustomConversions;
import com.mawen.search.core.convert.MappingElasticsearchConverter;
import com.mawen.search.core.mapping.SimpleElasticsearchMappingContext;
import com.mawen.search.core.refresh.RefreshPolicy;
import com.mawen.search.test.ElasticsearchTemplateConfiguration;
import com.mawen.search.test.client.ElasticsearchConfigurationSupport;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class CustomElasticsearchTemplateConfiguration extends ElasticsearchTemplateConfiguration {

    @Bean
	protected RefreshPolicy refreshPolicy() {
		return RefreshPolicy.IMMEDIATE;
	}



    @Bean(name = { "elasticsearchOperations", "elasticsearchTemplate" })
	public ElasticsearchOperations elasticsearchOperations(ElasticsearchConverter elasticsearchConverter,
                                                           ElasticsearchClient elasticsearchClient) {

		ElasticsearchTemplate template = new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
		template.setRefreshPolicy(refreshPolicy());

		return template;
	}


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

	protected FieldNamingStrategy fieldNamingStrategy() {
		return PropertyNameFieldNamingStrategy.INSTANCE;
	}
}
