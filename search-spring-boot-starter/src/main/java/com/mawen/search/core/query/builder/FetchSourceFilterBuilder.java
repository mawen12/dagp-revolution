package com.mawen.search.core.query.builder;

import com.mawen.search.core.query.FetchSourceFilter;
import com.mawen.search.core.query.SourceFilter;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class FetchSourceFilterBuilder {


	@Nullable
	private String[] includes;
	@Nullable
	private String[] excludes;

	public FetchSourceFilterBuilder withIncludes(String... includes) {
		this.includes = includes;
		return this;
	}

	public FetchSourceFilterBuilder withExcludes(String... excludes) {
		this.excludes = excludes;
		return this;
	}

	public SourceFilter build() {
		if (includes == null)
			includes = new String[0];
		if (excludes == null)
			excludes = new String[0];

		return new FetchSourceFilter(includes, excludes);
	}
}
