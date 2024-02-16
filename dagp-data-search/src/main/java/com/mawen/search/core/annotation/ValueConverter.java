package com.mawen.search.core.annotation;

import com.mawen.search.core.mapping.PropertyValueConverter;

import java.lang.annotation.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Documented
@Inherited
public @interface ValueConverter {

	Class<? extends PropertyValueConverter> value();
}
