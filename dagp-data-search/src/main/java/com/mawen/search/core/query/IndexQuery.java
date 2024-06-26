package com.mawen.search.core.query;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * IndexQuery
 *
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class IndexQuery {

	@Nullable
	private String id;
	@Nullable
	private Object object;
	@Nullable
	private Long version;
	@Nullable
	private String source;
	@Nullable
	private Long seqNo;
	@Nullable
	private Long primaryTerm;
	@Nullable
	private String routing;
	@Nullable
	private OpType opType;
	@Nullable
	private String indexName;

	@Getter
	public enum OpType {

		INDEX("index"),

		CREATE("create"),
		;

		private final String esName;

		OpType(String esName) {
			this.esName = esName;
		}
	}
}
