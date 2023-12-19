package com.mawen.search.core.domain;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/18
 */
public class IndexBoost {

	private String indexName;
	private float boost;

	public IndexBoost(String indexName, float boost) {
		this.indexName = indexName;
		this.boost = boost;
	}

	public String getIndexName() {
		return indexName;
	}

	public float getBoost() {
		return boost;
	}
}
