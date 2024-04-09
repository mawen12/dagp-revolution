package com.mawen.check;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.enhe.core.api.exception.BusinessException;

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

	default Check<T> orThrow(Function<T, String> errorMessage) {
		return t -> {
			boolean check = check(t);
			if (!check) {
				throw new RuntimeException(errorMessage.apply(t));
			}
			return check;
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

	default boolean checkAll(List<T> list) throws BusinessException {
		return list.stream().allMatch(this::check);
	}

	static <T> Check<T> build() {
		return t -> true;
	}
}
