package com.mawen.search.repository.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mawen.search.repository.support.ElasticsearchRepositoryFactoryBean;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.repository.config.DefaultRepositoryBaseClass;
import org.springframework.data.repository.query.QueryLookupStrategy;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(ElasticsearchRepositoriesRegistrar.class)
public @interface EnableElasticsearchRepositories {


	String[] value() default {};


	String[] basePackages() default {};


	Class<?>[] basePackageClasses() default {};


	ComponentScan.Filter[] includeFilters() default {};


	ComponentScan.Filter[] excludeFilters() default {};


	String repositoryImplementationPostfix() default "Impl";


	String namedQueriesLocation() default "";


	QueryLookupStrategy.Key queryLookupStrategy() default QueryLookupStrategy.Key.CREATE_IF_NOT_FOUND;


	Class<?> repositoryFactoryBeanClass() default ElasticsearchRepositoryFactoryBean.class;


	Class<?> repositoryBaseClass() default DefaultRepositoryBaseClass.class;

	String elasticsearchTemplateRef() default "elasticsearchTemplate";

	boolean considerNestedRepositories() default false;
}
