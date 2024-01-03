package com.mawen.search.core.convert;

import java.time.temporal.TemporalAccessor;

/**
 * {@link String} 与 {@link TemporalAccessor} 相互转换的接口
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface DateFormatter {

	/**
	 * 将 {@link TemporalAccessor} 格式化为字符串
	 *
	 * @param accessor 不能为空
	 * @return 格式化后的字符串
	 */
	String format(TemporalAccessor accessor);

	/**
	 * 将字符串解析为 {@link TemporalAccessor}
	 *
	 * @param input 待解析的字符串，不能为空
	 * @param type T 的类型
	 * @param <T> {@link TemporalAccessor} 的实现
	 * @return 解析后的实例
	 */
	<T extends TemporalAccessor> T parse(String input, Class<T> type);
}
