package com.mawen.search.core.domain;

import java.time.Duration;

import lombok.Getter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class PointInTime {

	private final String id;
	private final Duration keepAlive;

	public PointInTime(String id, Duration keepAlive) {

		Assert.notNull(id, "id must not be null");
		Assert.notNull(keepAlive, "keepAlive must not be null");

		this.id = id;
		this.keepAlive = keepAlive;
	}
}
