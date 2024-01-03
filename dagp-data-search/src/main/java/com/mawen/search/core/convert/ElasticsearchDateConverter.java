package com.mawen.search.core.convert;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalQuery;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import com.mawen.search.core.annotation.DateFormat;

import org.springframework.data.mapping.MappingException;
import org.springframework.util.Assert;

/**
 * 提供转换器实例，用于以 Elasticsearch 理解的不同日期和时间格式与日期进行转换
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public final class ElasticsearchDateConverter {

	private static final ConcurrentHashMap<String, ElasticsearchDateConverter> converters = new ConcurrentHashMap<>();

	private final DateFormatter dateFormatter;

	/**
	 * 使用给定 {@link DateFormatter} 创建一个 {@link ElasticsearchDateConverter}
	 *
	 * @param dateFormatter 不能为空
	 */
	private ElasticsearchDateConverter(DateFormatter dateFormatter) {
		this.dateFormatter = dateFormatter;
	}

	/**
	 * 使用给定 {@link DateFormat} 创建一个 {@link ElasticsearchDateConverter}
	 *
	 * @param dateFormat 不能为空
	 * @return 转换器实例
	 */
	public static ElasticsearchDateConverter of(DateFormat dateFormat) {

		Assert.notNull(dateFormat, "dateFormat must not be null");

		return of(dateFormat.name());
	}

	/**
	 * 使用给定的正则表达式创建一个 {@link ElasticsearchDateConverter}
	 *
	 * @param pattern 不能为空
	 * @return 转换器实例
	 */
	public static ElasticsearchDateConverter of(String pattern) {

		Assert.hasText(pattern, "pattern must not be empty");

		String[] subPatterns = pattern.split("\\|\\|");

		return converters.computeIfAbsent(subPatterns[0].trim(), p -> new ElasticsearchDateConverter(forPattern(p)));
	}

	/**
	 * 使用正则表达式创建一个 {@link DateFormatter}。正则表达式可以是 {@link DateFormat} 枚举值的名称，或字符串
	 *
	 * @param pattern 待使用的正则表达式
	 * @return {@link DateFormatter} 实例
	 */
	private static DateFormatter forPattern(String pattern) {

		String resolvedPattern = pattern;

		if (DateFormat.epoch_millis.getPattern().equals(pattern)) {
			return new EpochMillisDateFormatter();
		}

		if (DateFormat.epoch_second.getPattern().equals(pattern)) {
			return new EpochSecondDateFormatter();
		}

		// 枚举值匹配
		for (DateFormat dateFormat : DateFormat.values()) {

			switch (dateFormat) {
				case weekyear:
				case weekyear_week:
				case weekyear_week_day:
					continue;
			}

			if (dateFormat.name().equals(pattern)) {
				resolvedPattern = dateFormat.getPattern();
				break;
			}
		}

		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(resolvedPattern);
		return new PatternDateFormatter(dateTimeFormatter);
	}

	/**
	 * 将时间类型转换为通用的 {@link TemporalQuery}
	 *
	 * @param type 待转换的类型
	 * @param <T> {@link TemporalAccessor} 的实现
	 * @return {@link TemporalQuery} 的实现
	 */
	@SuppressWarnings("unchecked")
	private static <T extends TemporalAccessor> TemporalQuery<T> getTemporalQuery(Class<T> type) {

		return temporal -> {
			if (type == java.time.chrono.HijrahDate.class) {
				return (T) java.time.chrono.HijrahDate.from(temporal);
			}
			if (type == java.time.chrono.JapaneseDate.class) {
				return (T) java.time.chrono.JapaneseDate.from(temporal);
			}
			if (type == java.time.ZonedDateTime.class) {
				return (T) java.time.ZonedDateTime.from(temporal);
			}
			if (type == java.time.LocalDateTime.class) {
				return (T) java.time.LocalDateTime.from(temporal);
			}
			if (type == java.time.chrono.ThaiBuddhistDate.class) {
				return (T) java.time.chrono.ThaiBuddhistDate.from(temporal);
			}
			if (type == java.time.LocalTime.class) {
				return (T) java.time.LocalTime.from(temporal);
			}
			if (type == java.time.ZoneOffset.class) {
				return (T) java.time.ZoneOffset.from(temporal);
			}
			if (type == java.time.OffsetTime.class) {
				return (T) java.time.OffsetTime.from(temporal);
			}
			if (type == java.time.chrono.ChronoLocalDate.class) {
				return (T) java.time.chrono.ChronoLocalDate.from(temporal);
			}
			if (type == java.time.Month.class) {
				return (T) java.time.Month.from(temporal);
			}
			if (type == java.time.chrono.ChronoLocalDateTime.class) {
				return (T) java.time.chrono.ChronoLocalDateTime.from(temporal);
			}
			if (type == java.time.MonthDay.class) {
				return (T) java.time.MonthDay.from(temporal);
			}
			if (type == java.time.Instant.class) {
				return (T) java.time.Instant.from(temporal);
			}
			if (type == java.time.OffsetDateTime.class) {
				return (T) java.time.OffsetDateTime.from(temporal);
			}
			if (type == java.time.chrono.ChronoZonedDateTime.class) {
				return (T) java.time.chrono.ChronoZonedDateTime.from(temporal);
			}
			if (type == java.time.chrono.MinguoDate.class) {
				return (T) java.time.chrono.MinguoDate.from(temporal);
			}
			if (type == java.time.Year.class) {
				return (T) java.time.Year.from(temporal);
			}
			if (type == java.time.DayOfWeek.class) {
				return (T) java.time.DayOfWeek.from(temporal);
			}
			if (type == java.time.LocalDate.class) {
				return (T) java.time.LocalDate.from(temporal);
			}
			if (type == java.time.YearMonth.class) {
				return (T) java.time.YearMonth.from(temporal);
			}

			// 对于上述未涉及的实现，通过反射静态的 from(TemporalAccessor) 方法来检查其实例
			try {
				Method method = type.getMethod("from", TemporalAccessor.class);
				Object o = method.invoke(null, temporal);
				return type.cast(o);
			}
			catch (NoSuchMethodException e) {
				throw new MappingException("no 'from' factory method found in class " + type.getName());
			}
			catch (IllegalAccessException | InvocationTargetException e) {
				throw new MappingException("could not create object of class " + type.getName(), e);
			}
		};
	}

	/**
	 * 将 {@link TemporalAccessor} 格式化为字符串
	 *
	 * @param accessor 不能为空
	 * @return 格式化后的字符串
	 */
	public String format(TemporalAccessor accessor) {

		Assert.notNull(accessor, "accessor must not be null");

		if (accessor instanceof Instant) {
			ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant((Instant) accessor, ZoneId.of("UTC"));
			return dateFormatter.format(zonedDateTime);
		}

		return dateFormatter.format(accessor);
	}

	/**
	 * 将 {@link Date} 格式化为字符串
	 *
	 * @param date 不能为空
	 * @return 格式化后的字符串
	 */
	public String format(Date date) {

		Assert.notNull(date, "accessor must not be null");

		return dateFormatter.format(Instant.ofEpochMilli(date.getTime()));
	}

	/**
	 * 将字符串解析为 {@link TemporalAccessor}
	 *
	 * @param input 待解析的字符串，不能为空
	 * @param type T 的类型
	 * @param <T> {@link TemporalAccessor} 的实现
	 * @return 解析后的实例
	 */
	public <T extends TemporalAccessor> T parse(String input, Class<T> type) {
		return dateFormatter.parse(input, type);
	}

	/**
	 * 将字符串解析为 {@link Date}
	 *
	 * @param input 待解析的字符串，不能为空
	 * @return 解析后的实例
	 */
	public Date parse(String input) {
		return new Date(dateFormatter.parse(input, Instant.class).toEpochMilli());
	}
	// endregion

	/**
	 * Unix 时间戳毫秒的字符串与 {@link TemporalAccessor} 相互转换的 {@link DateFormatter} 实现
	 */
	static class EpochMillisDateFormatter implements DateFormatter {

		@Override
		public String format(TemporalAccessor accessor) {

			Assert.notNull(accessor, "accessor must not be null");

			return Long.toString(Instant.from(accessor).toEpochMilli());
		}

		@Override
		public <T extends TemporalAccessor> T parse(String input, Class<T> type) {

			Assert.notNull(input, "input must not be null");
			Assert.notNull(type, "type must not be null");

			Instant instant = Instant.ofEpochMilli(Long.parseLong(input));
			TemporalQuery<T> query = getTemporalQuery(type);
			return query.queryFrom(instant);
		}
	}

	/**
	 * Unix 时间戳秒的字符串与 {@link TemporalAccessor} 相互转换的 {@link DateFormatter} 实现
	 */
	static class EpochSecondDateFormatter implements DateFormatter {

		@Override
		public String format(TemporalAccessor accessor) {

			Assert.notNull(accessor, "accessor must not be null");

			long epochMilli = Instant.from(accessor).toEpochMilli();
			long fraction = epochMilli % 1_000;
			if (fraction == 0) {
				return Long.toString(epochMilli / 1_000);
			}
			else {
				Double d = ((double) epochMilli) / 1_000;
				return String.format(Locale.ROOT, "%.03f", d);
			}
		}

		@Override
		public <T extends TemporalAccessor> T parse(String input, Class<T> type) {

			Assert.notNull(input, "input must not be null");
			Assert.notNull(type, "type must not be null");

			Double epochMilli = Double.parseDouble(input) * 1_000;
			Instant instant = Instant.ofEpochMilli(epochMilli.longValue());
			TemporalQuery<T> query = getTemporalQuery(type);
			return query.queryFrom(instant);
		}
	}

	/**
	 * 正则表达式与 {@link TemporalAccessor} 相互转换的 {@link DateFormatter} 实现
	 */
	static class PatternDateFormatter implements DateFormatter {

		private final DateTimeFormatter dateTimeFormatter;

		PatternDateFormatter(DateTimeFormatter dateTimeFormatter) {

			Assert.notNull(dateTimeFormatter, "dateTimeFormatter must not be null");

			this.dateTimeFormatter = dateTimeFormatter;
		}

		@Override
		public String format(TemporalAccessor accessor) {

			Assert.notNull(accessor, "accessor must not be null");

			try {
				return dateTimeFormatter.format(accessor);
			}
			catch (Exception e) {
				if (accessor instanceof Instant) {
					// 后备方案，解析 ZonedDateTime 或 LocalDateTime
					return dateTimeFormatter.format(ZonedDateTime.ofInstant((Instant) accessor, ZoneId.of("UTC")));
				}
				else {
					throw e;
				}
			}
		}

		@Override
		public <T extends TemporalAccessor> T parse(String input, Class<T> type) {

			Assert.notNull(input, "input must not be null");
			Assert.notNull(type, "type must not be null");

			try {
				return dateTimeFormatter.parse(input, getTemporalQuery(type));
			}
			catch (Exception e) {

				if (type.equals(Instant.class)) {
					// 后备方案，解析 ZonedDateTime 或 LocalDateTime
					try {
						ZonedDateTime zonedDateTime = dateTimeFormatter.parse(input, getTemporalQuery(ZonedDateTime.class));
						// noinspection unchecked
						return (T) zonedDateTime.toInstant();
					}
					catch (Exception exception) {
						LocalDateTime localDateTime = dateTimeFormatter.parse(input, getTemporalQuery(LocalDateTime.class));
						// noinspection unchecked
						return (T) localDateTime.toInstant(ZoneOffset.UTC);
					}
				}
				else {
					throw e;
				}
			}
		}
	}
}
