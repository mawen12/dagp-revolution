package com.mawen.jdk.consts;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 3.4.2
 */
public enum OpCodes {

	ACC_PUBLIC(1), // 0000_0000 0000_0001
	ACC_PRIVATE(2), // 0000_0000 0000_0010
	ACC_PROTECTED(4), // 0000_0000 0000_0100
	ACC_STATIC(8), // 0000_0000 0000_1000
	ACC_FINAL(16), // 0000_0000 0001_0000
	;

	private final int code;

	OpCodes(int code) {
		this.code = code;
	}

	public int getCode() {
		return code;
	}
}
