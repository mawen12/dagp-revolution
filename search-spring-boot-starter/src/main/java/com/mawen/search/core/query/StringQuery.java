package com.mawen.search.core.query;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class StringQuery extends BaseQuery{

	public static String MATCH_ALL = "{ \"match_all\": {} }";

	private final String source;

	public StringQuery(String source) {
		this.source = source;
	}

	public StringQuery(String source, Pageable pageable) {
		this.source = source;
		this.pageable = pageable;
	}

	public StringQuery(String source, Pageable pageable, Sort sort) {
		this.pageable = pageable;
		this.sort = sort;
		this.source = source;
	}

	/**
	 * @since 4.4
	 */
	public StringQuery(StringQueryBuilder builder) {
		super(builder);
		this.source = builder.getSource();
	}

	/**
	 * @since 4.4
	 */
	public static StringQueryBuilder builder(String source) {
		return new StringQueryBuilder(source);
	}

	public String getSource() {
		return source;
	}
}
