package com.mawen.search.core.query;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public class DocValueField {

	private final String field;
	@Nullable private final String format;

	public DocValueField(String field, @Nullable String format) {

		Assert.notNull(field,"field must not be null");

		this.field = field;
		this.format = format;
	}

	public DocValueField(String field) {
		this(field, null);
	}
}
