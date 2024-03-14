package com.mawen.search.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.mawen.search.core.query.Query;

import org.springframework.data.annotation.QueryAnnotation;

/**
 * Match to {@link Query} configuration.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.2-SNAPSHOT
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Documented
@QueryAnnotation
public @interface SearchConfig {

	/**
	 * match to {@link Query#getIgnoreUnavailable()} ()}
	 * @return true when index
	 */
	boolean ignoreUnavailable() default false;
}
