package com.mawen.jdk.consts;

import java.util.Optional;
import java.util.stream.Stream;

import static com.mawen.jdk.consts.OpCodes.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @see <a href="https://docs.oracle.com/javase/tutorial/java/nutsandbolts/op3.htm">Bitwise and Bit Shift Operators</a>
 * @since 3.4.2
 */
public class OpCodesUtil {

	// region check &

	public static boolean isPublic(int mask) {
		return (mask & ACC_PUBLIC.getCode()) != 0;
	}

	public static boolean isPrivate(int mask) {
		return (mask & ACC_PRIVATE.getCode()) != 0;
	}

	public static boolean isProtected(int mask) {
		return (mask & ACC_PROTECTED.getCode()) != 0;
	}

	public static boolean isStatic(int mask) {
		return (mask & ACC_STATIC.getCode()) != 0;
	}

	public static boolean isFinal(int mask) {
		return (mask & ACC_FINAL.getCode()) != 0;
	}

	public static boolean all(int mask, OpCodes... opcodes) {
		return Stream.of(opcodes)
				.allMatch(opcode -> (mask & opcode.getCode()) != 0);
	}

	public static boolean any(int mask, OpCodes... opcodes) {
		return Stream.of(opcodes)
				.anyMatch(opCodes -> (mask & opCodes.getCode()) != 0);
	}

	public static boolean none(int mask, OpCodes... opcodes) {
		return Stream.of(opcodes)
				.noneMatch(opcode -> (mask & opcode.getCode()) != 0);
	}

	// endregion

	// region set |

	public static int setPublic(int mask) {
		return mask | ACC_PUBLIC.getCode();
	}

	public static int setPrivate(int mask) {
		return mask | ACC_PRIVATE.getCode();
	}

	public static int setProtected(int mask) {
		return mask | ACC_PROTECTED.getCode();
	}

	public static int setStatic(int mask) {
		return mask | ACC_STATIC.getCode();
	}

	public static int setFinal(int mask) {
		return mask | ACC_FINAL.getCode();
	}

	public static int setMulti(int mask, OpCodes... opcodes) {
		Optional<Integer> result = Stream.of(opcodes)
				.map(OpCodes::getCode)
				.reduce((first, next) -> first | next);
		return result.map(integer -> mask | integer).orElse(mask);
	}

	// endregion

	// region set non- ^

	public static int setNonPublic(int mask) {
		return isPublic(mask) ? mask ^ ACC_PUBLIC.getCode() : mask;
	}

	public static int setNonPrivate(int mask) {
		return isPrivate(mask) ? mask ^ ACC_PRIVATE.getCode() : mask;
	}

	public static int setNonProtected(int mask) {
		return isProtected(mask) ? mask ^ ACC_PROTECTED.getCode() : mask;
	}

	public static int setNonStatic(int mask) {
		return isStatic(mask) ? mask ^ ACC_STATIC.getCode() : mask;
	}

	public static int setNonFinal(int mask) {
		return isFinal(mask) ? mask ^ ACC_FINAL.getCode() : mask;
	}

	// endregion
}
