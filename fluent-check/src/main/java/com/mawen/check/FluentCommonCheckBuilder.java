package com.mawen.check;

import java.util.Objects;
import java.util.function.Function;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 3.4.2
 */
public class FluentCommonCheckBuilder<T> extends FluentCheckBuilder<T> {

	private FluentCommonCheckBuilder() {
		super();
	}

	public FluentCheckBuilder<T> isNull() {
		this.check = check.and(Objects::isNull);
		return this;
	}

	public FluentCheckBuilder<T> isNotNull() {
		this.check = check.and(Objects::nonNull);
		return this;
	}

	public FluentCheckBuilder<T> isTrue(Function<T, Boolean> booleanFunction) {
		this.check = check.and(t -> Boolean.TRUE.equals(booleanFunction.apply(t)));
		return this;
	}

	public FluentCheckBuilder<T> isFalse(Function<T, Boolean> booleanFunction) {
		this.check = check.and(t -> Boolean.FALSE.equals(booleanFunction.apply(t)));
		return this;
	}
}
