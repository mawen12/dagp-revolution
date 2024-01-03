package com.mawen.search.core.query.builder;

import com.mawen.search.core.query.StringQuery;
import lombok.Getter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class StringQueryBuilder extends BaseQueryBuilder<StringQuery, StringQueryBuilder> {

	private final String source;

	public StringQueryBuilder(String source) {

		Assert.notNull(source, "source must not be null");

		this.source = source;
	}

	@Override
	public StringQuery build() {
		return new StringQuery(this);
	}
}
