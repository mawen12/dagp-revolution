package com.mawen.check;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@FunctionalInterface
public interface Check<T> {

	boolean check(T t) throws RuntimeException;

	default Check<T> and(Check<? super T> other) {
		return (t) -> check(t) && other.check(t);
	}

	default Check<T> or(Check<? super T> other) {
		return (t) -> check(t) || other.check(t);
	}

	default Check<T> orThrow(Supplier<String> messageSupplier) {
		return t -> {
			boolean ret = check(t);
			if (!ret) {
				throw new RuntimeException(messageSupplier.get());
			}
			return ret;
		};
	}

	default Check<T> peek(Consumer<? super T> action) {
		return t -> {
			boolean ret = check(t);
			if (ret) {
				action.accept(t);
			}
			return ret;
		};
	}

	default Check<T> negate() {
		return t -> !check(t);
	}
}
