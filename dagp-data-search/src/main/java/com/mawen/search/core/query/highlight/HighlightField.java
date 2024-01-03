package com.mawen.search.core.query.highlight;

import lombok.Getter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class HighlightField {
	private final String name;
	private final HighlightFieldParameters parameters;

	public HighlightField(String name) {

		Assert.notNull(name, "name must not be null");

		this.name = name;
		this.parameters = HighlightFieldParameters.builder().build();
	}

	public HighlightField(String name, HighlightFieldParameters parameters) {

		Assert.notNull(name, "name must not be null");
		Assert.notNull(parameters, "parameters must not be null");

		this.name = name;
		this.parameters = parameters;
	}

	public static HighlightField of(com.mawen.search.core.annotation.HighlightField field) {

		com.mawen.search.core.annotation.HighlightParameters parameters = field.parameters();
		HighlightFieldParameters highlightParameters = HighlightFieldParameters.builder() //
				.withBoundaryChars(parameters.boundaryChars()) //
				.withBoundaryMaxScan(parameters.boundaryMaxScan()) //
				.withBoundaryScanner(parameters.boundaryScanner()) //
				.withBoundaryScannerLocale(parameters.boundaryScannerLocale()) //
				.withForceSource(parameters.forceSource()) //
				.withFragmenter(parameters.fragmenter()) //
				.withFragmentOffset(parameters.fragmentOffset()) //
				.withFragmentSize(parameters.fragmentSize()) //
				.withMatchedFields(parameters.matchedFields()) //
				.withNoMatchSize(parameters.noMatchSize()) //
				.withNumberOfFragments(parameters.numberOfFragments()) //
				.withOrder(parameters.order()) //
				.withPhraseLimit(parameters.phraseLimit()) //
				.withPreTags(parameters.preTags()) //
				.withPostTags(parameters.postTags()) //
				.withRequireFieldMatch(parameters.requireFieldMatch()) //
				.withType(parameters.type()) //
				.build();

		return new HighlightField(field.name(), highlightParameters);
	}
}
