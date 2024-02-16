package com.mawen.search.core.annotation;

import java.lang.annotation.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Documented
public @interface SourceFilters {

	String[] includes() default "";

	String[] excludes() default "";
}
