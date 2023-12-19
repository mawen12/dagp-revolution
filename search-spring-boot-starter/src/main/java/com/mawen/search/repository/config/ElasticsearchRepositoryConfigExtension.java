package com.mawen.search.repository.config;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.mawen.search.core.annotation.Document;
import com.mawen.search.repository.ElasticsearchRepository;
import com.mawen.search.repository.support.ElasticsearchRepositoryFactoryBean;
import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.data.repository.config.AnnotationRepositoryConfigurationSource;
import org.springframework.data.repository.config.RepositoryConfigurationExtensionSupport;
import org.springframework.data.repository.config.XmlRepositoryConfigurationSource;
import org.springframework.data.repository.core.RepositoryMetadata;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchRepositoryConfigExtension extends RepositoryConfigurationExtensionSupport {

	private static final String MODULE_NAME = "Elasticsearch";

	@Override
	public String getRepositoryFactoryBeanClassName() {
		return ElasticsearchRepositoryFactoryBean.class.getName();
	}

	@Override
	protected String getModulePrefix() {
		return "spring-data-elasticsearch";
	}

	@Override
	public String getModuleName() {
		return MODULE_NAME;
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, AnnotationRepositoryConfigurationSource config) {

		AnnotationAttributes attributes = config.getAttributes();
		builder.addPropertyReference("elasticsearchOperations", attributes.getString("elasticsearchTemplateRef"));
	}

	@Override
	public void postProcess(BeanDefinitionBuilder builder, XmlRepositoryConfigurationSource config) {

		Element element = config.getElement();
		builder.addPropertyReference("elasticsearchOperations", element.getAttribute("elasticsearch-template-ref"));
	}

	@Override
	protected Collection<Class<? extends Annotation>> getIdentifyingAnnotations() {
		return Collections.singleton(Document.class);
	}

	@Override
	protected Collection<Class<?>> getIdentifyingTypes() {
		return Arrays.asList(ElasticsearchRepository.class, ElasticsearchRepository.class);
	}

	@Override
	protected boolean useRepositoryConfiguration(RepositoryMetadata metadata) {
		return !metadata.isReactiveRepository();
	}
}