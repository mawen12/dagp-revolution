package com.mawen.check;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 3.4.2
 */
public class FluentCheckBuilder<T> implements Check<T> {

	protected Check<T> check = t -> true;

	protected FluentCheckBuilder() {}

	public static <T> FluentCheckBuilder<T> register(Class<T> clazz) {
		return new FluentCheckBuilder<>();
	}

	@Override
	public boolean check(T t) throws RuntimeException {
		return check.check(t);
	}

	@Override
	public FluentCheckBuilder<T> or(Check<? super T> other) {
		check = this.check.or(other);
		return this;
	}

	@Override
	public FluentCheckBuilder<T> orThrow(Supplier<String> messageSupplier) {
		check = check.orThrow(messageSupplier);
		return this;
	}

	@Override
	public FluentCheckBuilder<T> peek(Consumer<? super T> action) {
		check = check.peek(action);
		return this;
	}

	@Override
	public FluentCheckBuilder<T> negate() {
		check = check.negate();
		return this;
	}

	public FluentCheckBuilder<T> customCheck(Check<? super T> other) {
		check = this.check.and(other);
		return this;
	}
}
