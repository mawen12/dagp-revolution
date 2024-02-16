package com.mawen.search.core.annotation;

import org.springframework.data.annotation.QueryAnnotation;

import java.lang.annotation.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Documented
@QueryAnnotation
public @interface Query {

	String value() default "";

	boolean count() default false;
}
