package com.mawen.search.core.query.highlight;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class Highlight {
	private final HighlightParameters parameters;
	private final List<HighlightField> fields;

	/**
	 * @since 4.4
	 */
	public Highlight(List<HighlightField> fields) {

		Assert.notNull(fields, "fields must not be null");

		this.parameters = HighlightParameters.builder().build();
		this.fields = fields;
	}

	public Highlight(HighlightParameters parameters, List<HighlightField> fields) {

		Assert.notNull(parameters, "parameters must not be null");
		Assert.notNull(fields, "fields must not be null");

		this.parameters = parameters;
		this.fields = fields;
	}

	/**
	 * Creates a {@link Highlight} from an Annotation instance.
	 *
	 * @param highlight must not be {@literal null}
	 * @return highlight definition
	 */
	public static Highlight of(com.mawen.search.core.annotation.Highlight highlight) {

		Assert.notNull(highlight, "highlight must not be null");

		com.mawen.search.core.annotation.HighlightParameters parameters = highlight.parameters();
		HighlightParameters highlightParameters = HighlightParameters.builder() //
				.withBoundaryChars(parameters.boundaryChars()) //
				.withBoundaryMaxScan(parameters.boundaryMaxScan()) //
				.withBoundaryScanner(parameters.boundaryScanner()) //
				.withBoundaryScannerLocale(parameters.boundaryScannerLocale()) //
				.withEncoder(parameters.encoder()) //
				.withForceSource(parameters.forceSource()) //
				.withFragmenter(parameters.fragmenter()) //
				.withFragmentSize(parameters.fragmentSize()) //
				.withNoMatchSize(parameters.noMatchSize()) //
				.withNumberOfFragments(parameters.numberOfFragments()) //
				.withOrder(parameters.order()) //
				.withPhraseLimit(parameters.phraseLimit()) //
				.withPreTags(parameters.preTags()) //
				.withPostTags(parameters.postTags()) //
				.withRequireFieldMatch(parameters.requireFieldMatch()) //
				.withTagsSchema(parameters.tagsSchema()) //
				.withType(parameters.type()) //
				.build();

		List<HighlightField> highlightFields = Arrays.stream(highlight.fields()) //
				.map(HighlightField::of) //
				.collect(Collectors.toList());

		return new Highlight(highlightParameters, highlightFields);
	}

	public HighlightParameters getParameters() {
		return parameters;
	}

	public List<HighlightField> getFields() {
		return fields;
	}
}
