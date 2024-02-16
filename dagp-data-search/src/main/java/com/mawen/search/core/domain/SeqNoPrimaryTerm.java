package com.mawen.search.core.domain;

import lombok.Getter;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 0.0.1
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
	public int hashCode() {
		int result = ObjectUtils.nullSafeHashCode(primaryTerm);
		result = 31 * result + ObjectUtils.nullSafeHashCode(sequenceNumber);
		return result;	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this) {
			return true;
		}

		if (!(obj instanceof SeqNoPrimaryTerm)) {
			return false;
		}

		SeqNoPrimaryTerm that = (SeqNoPrimaryTerm) obj;

		return ObjectUtils.nullSafeEquals(this.primaryTerm, that.primaryTerm)
				&& ObjectUtils.nullSafeEquals(that.sequenceNumber, that.sequenceNumber);
	}

	@Override
	public String toString() {
		return "SeqNoPrimaryTerm{" +
				"sequenceNumber=" + sequenceNumber +
				", primaryTerm=" + primaryTerm +
				'}';
	}
}
