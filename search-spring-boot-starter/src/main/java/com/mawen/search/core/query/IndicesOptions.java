package com.mawen.search.core.query;

import java.util.EnumSet;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class IndicesOptions {
	public static final IndicesOptions STRICT_EXPAND_OPEN = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES), EnumSet.of(IndicesOptions.WildcardStates.OPEN));
	public static final IndicesOptions STRICT_EXPAND_OPEN_HIDDEN = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN, IndicesOptions.WildcardStates.HIDDEN));
	public static final IndicesOptions LENIENT_EXPAND_OPEN = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES, IndicesOptions.Option.IGNORE_UNAVAILABLE),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN));
	public static final IndicesOptions LENIENT_EXPAND_OPEN_HIDDEN = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES, IndicesOptions.Option.IGNORE_UNAVAILABLE),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN, IndicesOptions.WildcardStates.HIDDEN));
	public static final IndicesOptions LENIENT_EXPAND_OPEN_CLOSED = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES, IndicesOptions.Option.IGNORE_UNAVAILABLE),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN, IndicesOptions.WildcardStates.CLOSED));
	public static final IndicesOptions LENIENT_EXPAND_OPEN_CLOSED_HIDDEN = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES, IndicesOptions.Option.IGNORE_UNAVAILABLE),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN, IndicesOptions.WildcardStates.CLOSED,
					IndicesOptions.WildcardStates.HIDDEN));
	public static final IndicesOptions STRICT_EXPAND_OPEN_CLOSED = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN, IndicesOptions.WildcardStates.CLOSED));
	public static final IndicesOptions STRICT_EXPAND_OPEN_CLOSED_HIDDEN = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES), EnumSet.of(IndicesOptions.WildcardStates.OPEN,
			IndicesOptions.WildcardStates.CLOSED, IndicesOptions.WildcardStates.HIDDEN));
	public static final IndicesOptions STRICT_EXPAND_OPEN_FORBID_CLOSED = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES, IndicesOptions.Option.FORBID_CLOSED_INDICES),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN));
	public static final IndicesOptions STRICT_EXPAND_OPEN_HIDDEN_FORBID_CLOSED = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES, IndicesOptions.Option.FORBID_CLOSED_INDICES),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN, IndicesOptions.WildcardStates.HIDDEN));
	public static final IndicesOptions STRICT_EXPAND_OPEN_FORBID_CLOSED_IGNORE_THROTTLED = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.ALLOW_NO_INDICES, IndicesOptions.Option.FORBID_CLOSED_INDICES,
					IndicesOptions.Option.IGNORE_THROTTLED),
			EnumSet.of(IndicesOptions.WildcardStates.OPEN));
	public static final IndicesOptions STRICT_SINGLE_INDEX_NO_EXPAND_FORBID_CLOSED = new IndicesOptions(
			EnumSet.of(IndicesOptions.Option.FORBID_ALIASES_TO_MULTIPLE_INDICES, IndicesOptions.Option.FORBID_CLOSED_INDICES),
			EnumSet.noneOf(IndicesOptions.WildcardStates.class));
	private EnumSet<Option> options;
	private EnumSet<WildcardStates> expandWildcards;

	public IndicesOptions(EnumSet<Option> options, EnumSet<WildcardStates> expandWildcards) {
		this.options = options;
		this.expandWildcards = expandWildcards;
	}

	public static IndicesOptions ofOptions(EnumSet<Option> options) {
		return of(options, EnumSet.noneOf(WildcardStates.class));
	}

	public static IndicesOptions oFExpandWildcards(EnumSet<WildcardStates> expandWildcards) {
		return of(EnumSet.noneOf(Option.class), expandWildcards);
	}

	public static IndicesOptions of(EnumSet<Option> options, EnumSet<WildcardStates> expandWildcards) {
		return new IndicesOptions(options, expandWildcards);
	}

	public EnumSet<Option> getOptions() {
		return options;
	}

	public EnumSet<WildcardStates> getExpandWildcards() {
		return expandWildcards;
	}

	public enum WildcardStates {
		OPEN, CLOSED, HIDDEN, ALL, NONE;
	}

	public enum Option {
		IGNORE_UNAVAILABLE,
		IGNORE_ALIASES,
		ALLOW_NO_INDICES,
		FORBID_ALIASES_TO_MULTIPLE_INDICES,
		FORBID_CLOSED_INDICES,
		IGNORE_THROTTLED;
	}
}
