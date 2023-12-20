package com.mawen.search.repository.config;

import java.lang.annotation.Annotation;

import org.springframework.data.repository.config.RepositoryBeanDefinitionRegistrarSupport;
import org.springframework.data.repository.config.RepositoryConfigurationExtension;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class ElasticsearchRepositoriesRegistrar extends RepositoryBeanDefinitionRegistrarSupport {


	@Override
	protected Class<? extends Annotation> getAnnotation() {
		return EnableElasticsearchRepositories.class;
	}


	@Override
	protected RepositoryConfigurationExtension getExtension() {
		return new ElasticsearchRepositoryConfigExtension();
	}
}
