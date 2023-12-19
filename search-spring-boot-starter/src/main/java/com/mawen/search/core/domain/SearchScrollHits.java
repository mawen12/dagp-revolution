package com.mawen.search.core.domain;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface SearchScrollHits<T> extends SearchHits<T> {

	@Nullable
	String getScrollId();
}
