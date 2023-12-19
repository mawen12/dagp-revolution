package com.mawen.search.client;

import com.mawen.search.core.mapping.IndexCoordinates;
import com.mawen.search.core.query.Query;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
@AllArgsConstructor
public class MultiSearchQueryParameter {

	private final Query query;
	private final Class<?> clazz;
	private final IndexCoordinates index;

}
