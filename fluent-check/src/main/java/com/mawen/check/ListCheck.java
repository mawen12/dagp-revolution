package com.mawen.check;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/4/8
 */
@FunctionalInterface
public interface ListCheck<T> {

	Logger log = LoggerFactory.getLogger(ListCheck.class);

	boolean check(List<T> list) throws RuntimeException;

	default ListCheck<T> and(ListCheck<T> other) {
		return (t) -> check(t) && other.check(t);
	}

	default ListCheck<T> or(ListCheck<T> other) {
		return (t) -> check(t) || other.check(t);
	}

	default ListCheck<T> orThrow(Supplier<String> messageSupplier) {
		return t -> {
			boolean ret = check(t);
			if (!ret) {
				throw new RuntimeException(messageSupplier.get());
			}
			return ret;
		};
	}

	default ListCheck<T> orThrow(Function<List<? super T>, String> errorMessage) {
		return t -> {
			boolean check = check(t);
			if (!check) {
				throw new RuntimeException(errorMessage.apply(t));
			}
			return check;
		};
	}

	default <E extends RuntimeException> ListCheck<T> orThrowWith(Supplier<E> exceptionSupplier) {
		return t -> {
			boolean check = check(t);
			if (!check) {
				throw exceptionSupplier.get();
			}
			return check;
		};
	}

	default <E extends RuntimeException> ListCheck<T> orThrowWith(Function<List<T>, E> exceptionSupplier) {
		return t -> {
			boolean check = check(t);
			if (!check) {
				throw exceptionSupplier.apply(t);
			}
			return check;
		};
	}

	default ListCheck<T> peek(Consumer<List<? super T>> action) {
		return t -> {
			boolean ret = check(t);
			if (ret) {
				action.accept(t);
			}
			return ret;
		};
	}

	default ListCheck<T> tag(String tagName) {
		return t -> {
			try {
				boolean ret = check(t);
				if (ret) {
					if (log.isTraceEnabled()) {
						log.trace("Check {} Pass.", tagName);
					}
				} else {
					if (log.isTraceEnabled()) {
						log.trace("Check {} Failed.", tagName);
					}
				}
				return ret;
			}
			catch (Throwable e) {
				log.warn("Check {} Failed.", tagName, e);
				throw e;
			}
		};
	}

	default ListCheck<T> negate() {
		return t -> !check(t);
	}

	static <T> ListCheck<T> of() {
		return list -> true;
	}

	static <T> ListCheck<T> of(ListCheck<T> listCheck) {
		Assert.notNull(listCheck, "ListCheck must not be null");
		return listCheck;
	}
}
