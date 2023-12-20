package com.mawen.search.core.query.highlight;

import lombok.Getter;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public class HighlightFieldParameters extends HighlightCommonParameters {
	private final int fragmentOffset;
	private final String[] matchedFields;

	private HighlightFieldParameters(HighlightFieldParametersBuilder builder) {
		super(builder);
		fragmentOffset = builder.fragmentOffset;
		matchedFields = builder.matchedFields;
	}

	public static HighlightFieldParametersBuilder builder() {
		return new HighlightFieldParametersBuilder();
	}

	public static final class HighlightFieldParametersBuilder
			extends HighlightCommonParametersBuilder<HighlightFieldParametersBuilder> {
		private int fragmentOffset = -1;
		private String[] matchedFields = new String[0];

		public HighlightFieldParametersBuilder withFragmentOffset(int fragmentOffset) {
			this.fragmentOffset = fragmentOffset;
			return this;
		}

		public HighlightFieldParametersBuilder withMatchedFields(String... matchedFields) {
			this.matchedFields = matchedFields;
			return this;
		}

		@Override
		public HighlightFieldParameters build() {
			return new HighlightFieldParameters(this);
		}
	}
}
