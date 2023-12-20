package com.mawen.search.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.data.repository.query.parser.Part.Type;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface QueryField {

	/**
	 * 字段名称，支持映射到多个字段上
	 */
	String[] value();

	/**
	 * 查询方式
	 */
	Type type() default Type.SIMPLE_PROPERTY;
}
