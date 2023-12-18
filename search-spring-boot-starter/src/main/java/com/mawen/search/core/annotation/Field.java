package com.mawen.search.core.annotation;

/**
 * Field of domain object.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public @interface Field {

	/**
	 * The <em>value</em> to be used to store the field inside the document.
	 * If not set, the name of the annotated property is used.
	 */
	String value() default "";

	FieldType type() default FieldType.Auto;
}
