package com.mawen.search.core.convert;

import java.time.temporal.TemporalAccessor;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface DateFormatter {
	/**
	 * Formats a {@link TemporalAccessor} into a String.
	 *
	 * @param accessor must not be {@literal null}
	 * @return the formatted String
	 */
	String format(TemporalAccessor accessor);

	/**
	 * Parses a String into a {@link TemporalAccessor}.
	 *
	 * @param input the String to parse, must not be {@literal null}
	 * @param type  the class of T
	 * @param <T>   the {@link TemporalAccessor} implementation
	 * @return the parsed instance
	 */
	<T extends TemporalAccessor> T parse(String input, Class<T> type);
}
