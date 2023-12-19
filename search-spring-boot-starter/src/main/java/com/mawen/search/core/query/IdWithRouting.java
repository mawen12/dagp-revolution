package com.mawen.search.core.query;

import lombok.Getter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public class IdWithRouting {

	private final String id;
	@Nullable
	private final String routing;

	public IdWithRouting(String id, @Nullable String routing) {

		Assert.notNull(id, "id must not be null");

		this.id = id;
		this.routing = routing;
	}
}
