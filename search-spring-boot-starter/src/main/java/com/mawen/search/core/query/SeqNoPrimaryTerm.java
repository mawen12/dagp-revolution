package com.mawen.search.core.query;

import lombok.Getter;

import org.springframework.util.Assert;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/19
 */
@Getter
public class SeqNoPrimaryTerm {

	private final long sequenceNumber;
	private final long primaryTerm;

	public SeqNoPrimaryTerm(long sequenceNumber, long primaryTerm) {

		Assert.isTrue(sequenceNumber >= 0, "seq_no should not be negative, but it's " + sequenceNumber);
		Assert.isTrue(primaryTerm >= 0, "primary_term should not be negative, but it's " + primaryTerm);

		this.sequenceNumber = sequenceNumber;
		this.primaryTerm = primaryTerm;
	}

	@Override
	public String toString() {
		return "SeqNoPrimaryTerm{" +
				"sequenceNumber=" + sequenceNumber +
				", primaryTerm=" + primaryTerm +
				'}';
	}
}
