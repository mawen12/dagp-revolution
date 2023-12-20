package com.mawen.search.core.query.highlight;

import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public class HighlightParameters extends HighlightCommonParameters {
	private final String encoder;
	private final String tagsSchema;

	private HighlightParameters(HighlightParametersBuilder builder) {
		super(builder);
		encoder = builder.encoder;
		tagsSchema = builder.tagsSchema;
	}

	public static HighlightParametersBuilder builder() {
		return new HighlightParametersBuilder();
	}

	public static final class HighlightParametersBuilder
			extends HighlightCommonParametersBuilder<HighlightParametersBuilder> {
		private String encoder = "";
		private String tagsSchema = "";

		public HighlightParametersBuilder withEncoder(String encoder) {
			this.encoder = encoder;
			return this;
		}

		public HighlightParametersBuilder withTagsSchema(String tagsSchema) {
			this.tagsSchema = tagsSchema;
			return this;
		}

		@Override
		public HighlightParameters build() {
			return new HighlightParameters(this);
		}
	}
}
