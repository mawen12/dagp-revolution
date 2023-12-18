package com.mawen.search.core.query;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public interface SourceFilter {

	String[] getIncludes();

	String[] getExcludes();
}
