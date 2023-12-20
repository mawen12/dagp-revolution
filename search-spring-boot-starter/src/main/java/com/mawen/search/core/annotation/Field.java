package com.mawen.search.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Field of domain object.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.METHOD})
@Documented
@Inherited
public @interface Field {

	/**
	 * The <em>value</em> to be used to store the field inside the document.
	 * If not set, the name of the annotated property is used.
	 */
	String value() default "";

	FieldType type() default FieldType.Auto;

	DateFormat[] format() default {DateFormat.date_optional_time, DateFormat.epoch_millis};

	String[] pattern() default {};
}
