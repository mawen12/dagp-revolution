package com.mawen.search.core.domain;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface SearchScrollHits<T> extends SearchHits<T> {

	@Nullable
	String getScrollId();
}
