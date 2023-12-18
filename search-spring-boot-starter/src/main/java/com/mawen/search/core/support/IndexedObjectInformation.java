package com.mawen.search.core.support;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
@Getter
@Setter
@AllArgsConstructor
@EqualsAndHashCode
public class IndexedObjectInformation {

	@Nullable private String id;
	@Nullable private String index;
	@Nullable private Long seqNo;
	@Nullable private Long primaryTerm;
	@Nullable private Long version;
}
