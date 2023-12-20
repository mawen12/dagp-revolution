package com.mawen.search.core.query.highlight;

import lombok.Getter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public class HighlightCommonParameters {
	private final String boundaryChars;
	private final int boundaryMaxScan;
	private final String boundaryScanner;
	private final String boundaryScannerLocale;
	private final boolean forceSource;
	private final String fragmenter;
	private final int fragmentSize;
	private final int noMatchSize;
	private final int numberOfFragments;
	private final String order;
	private final int phraseLimit;
	private final String[] preTags;
	private final String[] postTags;
	private final boolean requireFieldMatch;
	private final String type;

	protected HighlightCommonParameters(HighlightCommonParametersBuilder<?> builder) {

		Assert.notNull(builder, "builder must not be null");

		boundaryChars = builder.boundaryChars;
		boundaryMaxScan = builder.boundaryMaxScan;
		boundaryScanner = builder.boundaryScanner;
		boundaryScannerLocale = builder.boundaryScannerLocale;
		forceSource = builder.forceSource;
		fragmenter = builder.fragmenter;
		fragmentSize = builder.fragmentSize;
		noMatchSize = builder.noMatchSize;
		numberOfFragments = builder.numberOfFragments;
		order = builder.order;
		phraseLimit = builder.phraseLimit;
		preTags = builder.preTags;
		postTags = builder.postTags;
		requireFieldMatch = builder.requireFieldMatch;
		type = builder.type;
	}

	@SuppressWarnings("unchecked")
	public static abstract class HighlightCommonParametersBuilder<SELF extends HighlightCommonParametersBuilder<SELF>> {
		private String boundaryChars = "";
		private int boundaryMaxScan = -1;
		private String boundaryScanner = "";
		private String boundaryScannerLocale = "";
		private boolean forceSource = false;
		private String fragmenter = "";
		private int fragmentSize = -1;
		private int noMatchSize = -1;
		private int numberOfFragments = -1;
		private String order = "";
		private int phraseLimit = -1;
		private String[] preTags = new String[0];
		private String[] postTags = new String[0];
		private boolean requireFieldMatch = true;
		private String type = "";

		protected HighlightCommonParametersBuilder() {
		}

		public SELF withBoundaryChars(String boundaryChars) {
			this.boundaryChars = boundaryChars;
			return (SELF) this;
		}

		public SELF withBoundaryMaxScan(int boundaryMaxScan) {
			this.boundaryMaxScan = boundaryMaxScan;
			return (SELF) this;
		}

		public SELF withBoundaryScanner(String boundaryScanner) {
			this.boundaryScanner = boundaryScanner;
			return (SELF) this;
		}

		public SELF withBoundaryScannerLocale(String boundaryScannerLocale) {
			this.boundaryScannerLocale = boundaryScannerLocale;
			return (SELF) this;
		}

		public SELF withForceSource(boolean forceSource) {
			this.forceSource = forceSource;
			return (SELF) this;
		}

		public SELF withFragmenter(String fragmenter) {
			this.fragmenter = fragmenter;
			return (SELF) this;
		}

		public SELF withFragmentSize(int fragmentSize) {
			this.fragmentSize = fragmentSize;
			return (SELF) this;
		}

		public SELF withNoMatchSize(int noMatchSize) {
			this.noMatchSize = noMatchSize;
			return (SELF) this;
		}

		public SELF withNumberOfFragments(int numberOfFragments) {
			this.numberOfFragments = numberOfFragments;
			return (SELF) this;
		}

		public SELF withOrder(String order) {
			this.order = order;
			return (SELF) this;
		}

		public SELF withPhraseLimit(int phraseLimit) {
			this.phraseLimit = phraseLimit;
			return (SELF) this;
		}

		public SELF withPreTags(String... preTags) {
			this.preTags = preTags;
			return (SELF) this;
		}

		public SELF withPostTags(String... postTags) {
			this.postTags = postTags;
			return (SELF) this;
		}

		public SELF withRequireFieldMatch(boolean requireFieldMatch) {
			this.requireFieldMatch = requireFieldMatch;
			return (SELF) this;
		}

		public SELF withType(String type) {
			this.type = type;
			return (SELF) this;
		}

		public abstract HighlightCommonParameters build();
	}
}
