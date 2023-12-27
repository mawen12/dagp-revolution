package com.mawen.search.core.domain;

import org.springframework.data.domain.Page;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public interface SearchPage<T> extends Page<SearchHit<T>> {

	SearchHits<T> getSearchHits();

}
