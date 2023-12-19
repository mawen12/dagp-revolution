package com.mawen.search.core.query;

import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
@AllArgsConstructor
public class PointInTime {

	private final String id;
	private final Duration keepAlive;
}
