package com.mawen.search.core.routing;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface RoutingResolver {

	static RoutingResolver just(String value) {

		Assert.notNull(value, "value must not be null");

		return new RoutingResolver() {
			@Override
			public String getRouting() {
				return value;
			}

			@Override
			public <T> String getRouting(T bean) {
				return value;
			}
		};
	}

	String getRouting();

	<T> String getRouting(T bean);
}
