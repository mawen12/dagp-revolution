package com.mawen.search;

import lombok.Getter;

/**
 * represent {@code index_not_found_exception}.
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Getter
public class NoSuchIndexException extends SearchException {

	private final String index;

	public NoSuchIndexException(String index) {
		super(String.format("Index %s not found.", index));
		this.index = index;
	}

	public NoSuchIndexException(String index, Throwable cause) {
		super(String.format("Index %s not found.", index), cause);
		this.index = index;
	}

}
