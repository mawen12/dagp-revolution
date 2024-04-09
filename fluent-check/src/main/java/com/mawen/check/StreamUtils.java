package com.mawen.check;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/4/9
 */
public final class StreamUtils {

	private StreamUtils() {}

	public static <T> boolean allMatch(List<T> elements, Predicate<T> predicate) {
		return elements.stream().allMatch(predicate);
	}

	public static <T> boolean anyMatch(List<T> elements, Predicate<T> predicate) {
		return elements.stream().anyMatch(predicate);
	}

	public static <T> boolean noneMatch(List<T> elements, Predicate<T> predicate) {
		return elements.stream().noneMatch(predicate);
	}
}
