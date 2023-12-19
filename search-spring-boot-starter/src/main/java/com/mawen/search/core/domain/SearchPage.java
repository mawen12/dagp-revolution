package com.mawen.search.core.domain;

import org.springframework.data.domain.Page;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public interface SearchPage<T> extends Page<SearchHit<T>> {

	SearchHits<T> getSearchHits();

}
