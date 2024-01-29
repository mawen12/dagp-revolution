package com.mawen.search.core.domain;

import com.mawen.search.core.query.BaseQuery;

import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/1/29
 */
public interface PitSearchAfterHits<T> extends SearchHits<T> {

	String getPit();

	BaseQuery getQuery();

}
