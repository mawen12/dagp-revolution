package com.mawen.search.core.domain;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public final class Range<T> {

	private static final Range<?> UNBOUNDED = Range.of(Range.Bound.unbounded(), Range.Bound.unbounded());

	private final Bound<T> lowerBound;

	private final Range.Bound<T> upperBound;

	private Range(Range.Bound<T> lowerBound, Range.Bound<T> upperBound) {

		Assert.notNull(lowerBound, "Lower bound must not be null");
		Assert.notNull(upperBound, "Upper bound must not be null");

		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	@SuppressWarnings("unchecked")
	public static <T> Range<T> unbounded() {
		return (Range<T>) UNBOUNDED;
	}

	public static <T> Range<T> closed(T from, T to) {
		return new Range<>(Range.Bound.inclusive(from), Range.Bound.inclusive(to));
	}

	public static <T> Range<T> open(T from, T to) {
		return new Range<>(Range.Bound.exclusive(from), Range.Bound.exclusive(to));
	}

	public static <T> Range<T> leftOpen(T from, T to) {
		return new Range<>(Range.Bound.exclusive(from), Range.Bound.inclusive(to));
	}

	public static <T> Range<T> rightOpen(T from, T to) {
		return new Range<>(Range.Bound.inclusive(from), Range.Bound.exclusive(to));
	}

	public static <T> Range<T> leftUnbounded(Range.Bound<T> to) {
		return new Range<>(Range.Bound.unbounded(), to);
	}

	public static <T> Range<T> rightUnbounded(Range.Bound<T> from) {
		return new Range<>(from, Range.Bound.unbounded());
	}

	public static <T> Range.RangeBuilder<T> from(Range.Bound<T> lower) {

		Assert.notNull(lower, "Lower bound must not be null");
		return new Range.RangeBuilder<>(lower);
	}

	public static <T> Range<T> of(Range.Bound<T> lowerBound, Range.Bound<T> upperBound) {
		return new Range<>(lowerBound, upperBound);
	}

	public static <T> Range<T> just(T value) {
		return Range.closed(value, value);
	}

	@SuppressWarnings({"unchecked"})
	public boolean contains(Comparable<T> value) {

		return contains((T) value, (o1, o2) -> {

			Assert.isInstanceOf(Comparable.class, o1, "Range value must be an instance of Comparable to use contains(Comparable<T>)");
			return ((Comparable<T>) o1).compareTo(o2);
		});
	}

	public boolean contains(T value, Comparator<T> comparator) {

		Assert.notNull(value, "Reference value must not be null");

		boolean greaterThanLowerBound = lowerBound.getValue() //
				.map(it -> lowerBound.isInclusive() ? comparator.compare(it, value) <= 0 : comparator.compare(it, value) < 0) //
				.orElse(true);

		boolean lessThanUpperBound = upperBound.getValue() //
				.map(it -> upperBound.isInclusive() ? comparator.compare(it, value) >= 0 : comparator.compare(it, value) > 0) //
				.orElse(true);

		return greaterThanLowerBound && lessThanUpperBound;
	}

	public <R> Range<R> map(Function<? super T, ? extends R> mapper) {

		Assert.notNull(mapper, "Mapping function must not be null");

		return Range.of(lowerBound.map(mapper), upperBound.map(mapper));
	}

	@Override
	public String toString() {
		return String.format("%s-%s", lowerBound.toPrefixString(), upperBound.toSuffixString());
	}

	public Range.Bound<T> getLowerBound() {
		return this.lowerBound;
	}

	public Range.Bound<T> getUpperBound() {
		return this.upperBound;
	}

	@Override
	public boolean equals(Object o) {

		if (this == o) {
			return true;
		}

		if (!(o instanceof Range<?>)) {
			return false;
		}

		Range range = (Range) o;

		if (!ObjectUtils.nullSafeEquals(lowerBound, range.lowerBound)) {
			return false;
		}

		return ObjectUtils.nullSafeEquals(upperBound, range.upperBound);
	}

	@Override
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(lowerBound);
		result = 31 * result + ObjectUtils.nullSafeHashCode(upperBound);
		return result;
	}

	public static final class Bound<T> {

		private static final Range.Bound<?> UNBOUNDED = new Range.Bound<>(Optional.empty(), true);

		private final Optional<T> value;
		private final boolean inclusive;

		private Bound(Optional<T> value, boolean inclusive) {
			this.value = value;
			this.inclusive = inclusive;
		}

		@SuppressWarnings("unchecked")
		public static <T> Range.Bound<T> unbounded() {
			return (Range.Bound<T>) UNBOUNDED;
		}

		public static <T> Range.Bound<T> inclusive(T value) {

			Assert.notNull(value, "Value must not be null");
			return Range.Bound.of(Optional.of(value), true);
		}

		public static Range.Bound<Integer> inclusive(int value) {
			return inclusive((Integer) value);
		}

		public static Range.Bound<Long> inclusive(long value) {
			return inclusive((Long) value);
		}

		public static Range.Bound<Float> inclusive(float value) {
			return inclusive((Float) value);
		}

		public static Range.Bound<Double> inclusive(double value) {
			return inclusive((Double) value);
		}

		public static <T> Range.Bound<T> exclusive(T value) {

			Assert.notNull(value, "Value must not be null");
			return Range.Bound.of(Optional.of(value), false);
		}

		public static Range.Bound<Integer> exclusive(int value) {
			return exclusive((Integer) value);
		}

		public static Range.Bound<Long> exclusive(long value) {
			return exclusive((Long) value);
		}

		public static Range.Bound<Float> exclusive(float value) {
			return exclusive((Float) value);
		}

		public static Range.Bound<Double> exclusive(double value) {
			return exclusive((Double) value);
		}

		private static <R> Range.Bound<R> of(Optional<R> value, boolean inclusive) {

			if (value.isPresent()) {
				return new Range.Bound<>(value, inclusive);
			}

			return unbounded();
		}

		public boolean isBounded() {
			return value.isPresent();
		}

		String toPrefixString() {

			return getValue() //
					.map(Object::toString) //
					.map(it -> isInclusive() ? "[".concat(it) : "(".concat(it)) //
					.orElse("unbounded");
		}

		String toSuffixString() {

			return getValue() //
					.map(Object::toString) //
					.map(it -> isInclusive() ? it.concat("]") : it.concat(")")) //
					.orElse("unbounded");
		}

		@Override
		public String toString() {
			return value.map(Object::toString).orElse("unbounded");
		}

		public Optional<T> getValue() {
			return this.value;
		}

		public boolean isInclusive() {
			return this.inclusive;
		}

		@Override
		public boolean equals(Object o) {

			if (this == o) {
				return true;
			}

			if (!(o instanceof Range.Bound<?>)) {
				return false;
			}

			Bound bound = (Bound) o;

			if (!value.isPresent() && !bound.value.isPresent()) {
				return true;
			}

			if (inclusive != bound.inclusive) return false;

			return ObjectUtils.nullSafeEquals(value, bound.value);
		}

		@Override
		public int hashCode() {

			if (!value.isPresent()) {
				return ObjectUtils.nullSafeHashCode(value);
			}

			int result = ObjectUtils.nullSafeHashCode(value);
			result = 31 * result + (inclusive ? 1 : 0);
			return result;
		}

		public <R> Range.Bound<R> map(Function<? super T, ? extends R> mapper) {

			Assert.notNull(mapper, "Mapping function must not be null");

			return Range.Bound.of(value.map(mapper), inclusive);
		}

	}


	public static class RangeBuilder<T> {

		private final Range.Bound<T> lower;

		RangeBuilder(Range.Bound<T> lower) {
			this.lower = lower;
		}

		public Range<T> to(Range.Bound<T> upper) {

			Assert.notNull(upper, "Upper bound must not be null");
			return new Range<>(lower, upper);
		}
	}

}
