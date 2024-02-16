package com.mawen.search.core.annotation;

import java.lang.annotation.*;

/**
 * 标记指定属性可以写入 es，但是在从 es 中读取时不设置
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
@Documented
public @interface WriteOnlyProperty {
}
