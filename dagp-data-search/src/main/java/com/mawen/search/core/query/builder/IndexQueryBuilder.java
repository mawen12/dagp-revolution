package com.mawen.search.core.query.builder;

import com.mawen.search.core.domain.SeqNoPrimaryTerm;
import com.mawen.search.core.query.IndexQuery;
import com.mawen.search.core.refresh.RefreshPolicy;
import org.springframework.lang.Nullable;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
 */
public class IndexQueryBuilder {

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
	private IndexQuery.OpType opType;
	@Nullable
	private RefreshPolicy refreshPolicy;
	@Nullable
	private String indexName;

	public IndexQueryBuilder() {
	}

	public IndexQueryBuilder withId(String id) {
		this.id = id;
		return this;
	}

	public IndexQueryBuilder withObject(Object object) {
		this.object = object;
		return this;
	}

	public IndexQueryBuilder withVersion(Long version) {
		this.version = version;
		return this;
	}

	public IndexQueryBuilder withSource(String source) {
		this.source = source;
		return this;
	}

	public IndexQueryBuilder withSeqNoPrimaryTerm(SeqNoPrimaryTerm seqNoPrimaryTerm) {
		this.seqNo = seqNoPrimaryTerm.getSequenceNumber();
		this.primaryTerm = seqNoPrimaryTerm.getPrimaryTerm();
		return this;
	}

	public IndexQueryBuilder withRouting(@Nullable String routing) {
		this.routing = routing;
		return this;
	}

	public IndexQueryBuilder withOpType(IndexQuery.OpType opType) {
		this.opType = opType;
		return this;
	}

	public IndexQuery build() {
		return new IndexQuery(id, object, version, source, seqNo, primaryTerm, routing, opType, indexName);
	}

	public IndexQueryBuilder withIndex(@Nullable String indexName) {
		this.indexName = indexName;
		return this;
	}
}
