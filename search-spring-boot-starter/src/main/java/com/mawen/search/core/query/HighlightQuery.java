package com.mawen.search.core.query;

import com.mawen.search.core.query.highlight.Highlight;
import lombok.Getter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public class HighlightQuery {

	private final Highlight highlight;
	@Nullable
	private final Class<?> type;

	public HighlightQuery(Highlight highlight, @Nullable Class<?> type) {

		Assert.notNull(highlight, "highlight must not be null");

		this.highlight = highlight;
		this.type = type;
	}
}
