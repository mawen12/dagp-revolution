package com.mawen.search.core.mapping;

import java.util.Arrays;

import lombok.Getter;

import org.springframework.util.Assert;

/**
 * Immutable Value object encapsulating index names(s).
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class IndexCoordinates {

	private final String[] indexNames;

	private IndexCoordinates(String... indexNames) {
		Assert.notEmpty(indexNames, "indexNames must not be null or empty");
		this.indexNames = indexNames;
	}

	public static IndexCoordinates of(String... indexNames) {
		Assert.notEmpty(indexNames, "indexNames must not be null");
		return new IndexCoordinates(indexNames);
	}

	public String getIndexName() {
		return indexNames[0];
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(indexNames);
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}

		if (!(obj instanceof IndexCoordinates)) {
			return false;
		}

		IndexCoordinates that = (IndexCoordinates) obj;

		return Arrays.equals(this.indexNames, that.indexNames);
	}

	@Override
	public String toString() {
		return "IndexCoordinates{" + "indexNames=" + Arrays.toString(indexNames) + '}';
	}
}
