package com.mawen.search.core.annotation;

import java.lang.annotation.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Highlight {
	HighlightParameters parameters() default @HighlightParameters;

	HighlightField[] fields();
}
