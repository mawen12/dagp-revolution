package com.mawen.search.core.query;

import java.util.function.Function;

import com.mawen.search.core.query.builder.FetchSourceFilterBuilder;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
public class FetchSourceFilter implements SourceFilter {

	@Nullable
	private final String[] includes;
	@Nullable
	private final String[] excludes;

	public FetchSourceFilter(@Nullable final String[] includes, @Nullable final String[] excludes) {
		this.includes = includes;
		this.excludes = excludes;
	}

	/**
	 * @since 5.2
	 */
	public static SourceFilter of(@Nullable final String[] includes, @Nullable final String[] excludes) {
		return new FetchSourceFilter(includes, excludes);
	}

	/**
	 * @since 5.2
	 */
	public static SourceFilter of(Function<FetchSourceFilterBuilder, FetchSourceFilterBuilder> builderFunction) {

		Assert.notNull(builderFunction, "builderFunction must not be null");

		return builderFunction.apply(new FetchSourceFilterBuilder()).build();
	}

	@Override
	public String[] getIncludes() {
		return includes;
	}

	@Override
	public String[] getExcludes() {
		return excludes;
	}
}
