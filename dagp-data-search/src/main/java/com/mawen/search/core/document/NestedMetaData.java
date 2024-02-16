package com.mawen.search.core.document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NestedMetaData {

	private final String field;
	private final int offset;
	@Nullable
	private final NestedMetaData child;

	public static NestedMetaData of(String field, int offset, @Nullable NestedMetaData nested) {
		return new NestedMetaData(field, offset, nested);
	}
}
