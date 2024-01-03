package com.mawen.search.core.support;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ActiveShardCount {
	public static final ActiveShardCount NONE = new ActiveShardCount(0);
	public static final ActiveShardCount ONE = new ActiveShardCount(1);
	private static final int ACTIVE_SHARD_COUNT_DEFAULT = -2;
	public static final ActiveShardCount DEFAULT = new ActiveShardCount(ACTIVE_SHARD_COUNT_DEFAULT);
	private static final int ALL_ACTIVE_SHARDS = -1;
	public static final ActiveShardCount ALL = new ActiveShardCount(ALL_ACTIVE_SHARDS);
	private final int value;
}
