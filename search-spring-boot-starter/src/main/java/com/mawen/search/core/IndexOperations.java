package com.mawen.search.core;

import com.mawen.search.core.mapping.IndexCoordinates;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/26
 */
public interface IndexOperations {

	boolean delete();

	boolean exists();

	void refresh();

	void refresh(IndexCoordinates index);

	IndexCoordinates getIndexCoordinates();

}
