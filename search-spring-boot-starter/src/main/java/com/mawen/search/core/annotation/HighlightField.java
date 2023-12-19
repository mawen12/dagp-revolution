package com.mawen.search.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface HighlightField {

	String name() default "";

	HighlightParameters parameters() default @HighlightParameters;
}
