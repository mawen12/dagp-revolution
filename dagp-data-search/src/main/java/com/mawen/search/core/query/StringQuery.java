package com.mawen.search.core.query;

import com.mawen.search.core.query.builder.StringQueryBuilder;
import lombok.Getter;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * 以字符串类型构建的查询
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class StringQuery extends BaseQuery {

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

	public StringQuery(StringQueryBuilder builder) {
		super(builder);
		this.source = builder.getSource();
	}

	public static StringQueryBuilder builder(String source) {
		return new StringQueryBuilder(source);
	}
}
