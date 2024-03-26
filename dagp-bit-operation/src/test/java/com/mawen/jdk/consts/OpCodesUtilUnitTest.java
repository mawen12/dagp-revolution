package com.mawen.jdk.consts;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link OpCodesUtil}
 */
class OpCodesUtilUnitTest {

	@Test
	void checkPublicCorrectly() {
		int opcodes = OpCodes.ACC_PUBLIC.getCode();
		assertTrue(OpCodesUtil.isPublic(opcodes));
	}

	@Test
	void checkPrivateCorrectly() {
		int opcodes = OpCodes.ACC_PRIVATE.getCode();
		assertTrue(OpCodesUtil.isPrivate(opcodes));
	}

	@Test
	void checkProtectedCorrectly() {
		int opcodes = OpCodes.ACC_PROTECTED.getCode();
		assertTrue(OpCodesUtil.isProtected(opcodes));
	}

	@Test
	void checkStaticCorrectly() {
		int opcodes = OpCodes.ACC_STATIC.getCode();
		assertTrue(OpCodesUtil.isStatic(opcodes));
	}

	@Test
	void checkFinalCorrectly() {
		int opcodes = OpCodes.ACC_FINAL.getCode();
		assertTrue(OpCodesUtil.isFinal(opcodes));
	}

	@Test
	void checkNumberIsPublicCorrectly() {
		int number = 128;
		number = OpCodesUtil.setPublic(number);
		assertTrue(OpCodesUtil.isPublic(number));
	}

	@Test
	void checkNumberIsPrivateCorrectly() {
		int number = 128;
		number = OpCodesUtil.setPrivate(number);
		assertTrue(OpCodesUtil.isPrivate(number));
	}

	@Test
	void checkNumberIsProtectedCorrectly() {
		int number = 128;
		number = OpCodesUtil.setProtected(number);
		assertTrue(OpCodesUtil.isProtected(number));
	}

	@Test
	void checkNumberIsStaticCorrectly() {
		int number = 128;
		number = OpCodesUtil.setStatic(number);
		assertTrue(OpCodesUtil.isStatic(number));
	}

	@Test
	void checkNumberIsFinalCorrectly() {
		int number = 128;
		number = OpCodesUtil.setFinal(number);
		assertTrue(OpCodesUtil.isFinal(number));
	}

	@Test
	void checkReversePublicCorrectly() {
		int number = 15;
		number = OpCodesUtil.setNonPublic(number);
		assertFalse(OpCodesUtil.isPublic(number));

		int number1 = 14;
		number1 = OpCodesUtil.setNonPublic(number1);
		assertFalse(OpCodesUtil.isPublic(number1));
	}

	@Test
	void checkReversePrivateCorrectly() {
		int number = 15;
		number = OpCodesUtil.setNonPrivate(number);
		assertFalse(OpCodesUtil.isPrivate(number));

		int number1 = 13;
		number1 = OpCodesUtil.setNonPrivate(number1);
		assertFalse(OpCodesUtil.isPrivate(number1));
	}

	@Test
	void checkReverseProtectedCorrectly() {
		int number = 15;
		number = OpCodesUtil.setNonProtected(number);
		assertFalse(OpCodesUtil.isProtected(number));

		int number1 = 11;
		number1 = OpCodesUtil.setNonProtected(number1);
		assertFalse(OpCodesUtil.isProtected(number1));
	}

	@Test
	void checkReverseStaticCorrectly() {
		int number = 15;
		number = OpCodesUtil.setNonStatic(number);
		assertFalse(OpCodesUtil.isStatic(number));

		int number1 = 7;
		number1 = OpCodesUtil.setNonStatic(number1);
		assertFalse(OpCodesUtil.isStatic(number1));
	}

	@Test
	void checkReverseFinalCorrectly() {
		int number = 63;
		number = OpCodesUtil.setNonFinal(number);
		assertFalse(OpCodesUtil.isFinal(number));

		int number1 = 47;
		number1 = OpCodesUtil.setNonFinal(number1);
		assertFalse(OpCodesUtil.isFinal(number1));
	}
}