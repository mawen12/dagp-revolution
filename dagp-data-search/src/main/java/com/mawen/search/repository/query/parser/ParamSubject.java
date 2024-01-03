package com.mawen.search.repository.query.parser;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.Getter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class ParamSubject {

	private static final String QUERY_PATTERN = "find|read|get|query|search|stream|list";
	private static final String COUNT_PATTERN = "count";
	private static final String EXISTS_PATTERN = "exists";
	private static final String DELETE_PATTERN = "delete|remove";
	private static final Pattern PREFIX_TEMPLATE = Pattern.compile( //
			"^(" + QUERY_PATTERN + "|" + COUNT_PATTERN + "|" + EXISTS_PATTERN + "|" + DELETE_PATTERN + ")((\\p{Lu}.*?))??");

	private static final Pattern COUNT_TEMPLATE = Pattern.compile("^count(\\p{Lu}.*?)??");
	private static final Pattern EXISTS_TEMPLATE = Pattern.compile("^(" + EXISTS_PATTERN + ")(\\p{Lu}.*?)??");
	private static final Pattern DELETE_TEMPLATE = Pattern.compile("^(" + DELETE_PATTERN + ")(\\p{Lu}.*?)??");

	private final boolean count;
	private final boolean exists;
	private final boolean delete;

	public ParamSubject(String source) {

		Assert.notNull(source, "Source must not be null");

		Matcher matcher = PREFIX_TEMPLATE.matcher(source);

		if (!matcher.find()) {
			this.count = false;
			this.exists = false;
			this.delete = false;
		}
		else {
			Optional<String> subject = Optional.of(matcher.group(0));
			this.count = matches(subject, COUNT_TEMPLATE);
			this.exists = matches(subject, EXISTS_TEMPLATE);
			this.delete = matches(subject, DELETE_TEMPLATE);
		}
	}

	private boolean matches(Optional<String> subject, Pattern pattern) {
		return subject.map(it -> pattern.matcher(it).find()).orElse(false);
	}
}
