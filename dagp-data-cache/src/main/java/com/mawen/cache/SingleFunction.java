package com.mawen.cache;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/4/4
 */
@FunctionalInterface
public interface SingleFunction<T, R> {

	R apply(T t);
}
