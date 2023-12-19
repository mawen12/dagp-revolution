package com.mawen.search.repository.support;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.convert.ConversionService;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.util.NumberUtils;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class StringQueryUtil {

	private static final Pattern PARAMETER_PLACEHOLDER = Pattern.compile("\\?(\\d+)");

	private final ConversionService conversionService;

	public StringQueryUtil(ConversionService conversionService) {
		this.conversionService = conversionService;
	}

	public String replacePlaceholders(String input, ParameterAccessor accessor) {

		Matcher matcher = PARAMETER_PLACEHOLDER.matcher(input);
		String result = input;
		while (matcher.find()) {

			String placeholder = Pattern.quote(matcher.group()) + "(?!\\d+)";
			int index = NumberUtils.parseNumber(matcher.group(1), Integer.class);
			String replacement = Matcher.quoteReplacement(getParameterWithIndex(accessor, index));
			result = result.replaceAll(placeholder, replacement);
			// need to escape backslashes that are not escapes for quotes so that they are sent as double-backslashes
			// to Elasticsearch
			result = result.replaceAll("\\\\([^\"'])", "\\\\\\\\$1");
		}
		return result;
	}

	private String getParameterWithIndex(ParameterAccessor accessor, int index) {

		Object parameter = accessor.getBindableValue(index);
		String parameterValue = "null";

		if (parameter != null) {
			parameterValue = convert(parameter);
		}

		return parameterValue;

	}

	private String convert(Object parameter) {
		if (Collection.class.isAssignableFrom(parameter.getClass())) {
			Collection<?> collectionParam = (Collection<?>) parameter;
			StringBuilder sb = new StringBuilder("[");
			sb.append(collectionParam.stream().map(o -> {
				if (o instanceof String) {
					return "\"" + convert(o) + "\"";
				} else {
					return convert(o);
				}
			}).collect(Collectors.joining(",")));
			sb.append("]");
			return sb.toString();
		} else {
			String parameterValue = "null";
			if (conversionService.canConvert(parameter.getClass(), String.class)) {
				String converted = conversionService.convert(parameter, String.class);

				if (converted != null) {
					parameterValue = converted;
				}
			} else {
				parameterValue = parameter.toString();
			}

			parameterValue = parameterValue.replaceAll("\"", Matcher.quoteReplacement("\\\""));
			return parameterValue;
		}
	}
}
