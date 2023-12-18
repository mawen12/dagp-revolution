package com.mawen.search.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * when {@link Document#dynamicIndex()} is {@literal true},
 * will read {@link IndexName} as the Elasticsearch index of domain object.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface IndexName {

	/**
	 * Name of the Elasticsearch index.
	 */
	String value();
}
