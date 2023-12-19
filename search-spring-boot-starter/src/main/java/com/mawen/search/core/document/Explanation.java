package com.mawen.search.core.document;

import java.util.List;
import java.util.Objects;

import lombok.Getter;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
public class Explanation {

	@Nullable
	private final Boolean match;
	private final Double value;
	@Nullable
	private final String description;
	private final List<Explanation> details;

	public Explanation(@Nullable Boolean match, Double value, @Nullable String description, List<Explanation> details) {

		Assert.notNull(value, "value must not be null");
		Assert.notNull(details, "details must not be null");

		this.match = match;
		this.value = value;
		this.description = description;
		this.details = details;
	}

	public boolean isMatch() {
		return match != null && match;
	}

	public Double getValue() {
		return value;
	}

	@Nullable
	public String getDescription() {
		return description;
	}

	public List<Explanation> getDetails() {
		return details;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		Explanation that = (Explanation) o;

		if (match != that.match)
			return false;
		if (!value.equals(that.value))
			return false;
		if (!Objects.equals(description, that.description))
			return false;
		return details.equals(that.details);
	}

	@Override
	public int hashCode() {
		int result = (match ? 1 : 0);
		result = 31 * result + value.hashCode();
		result = 31 * result + (description != null ? description.hashCode() : 0);
		result = 31 * result + details.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "Explanation{" + //
				"match=" + match + //
				", value=" + value + //
				", description='" + description + '\'' + //
				", details=" + details + //
				'}'; //
	}
}
