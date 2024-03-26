package com.mawen.jdk.consts;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 3.4.2
 */
public class ExclusiveOrExample {

	public static void main(String[] args) {
		// 0000 ^ 0000 = 0000
		int a = 0, b = 0;
		System.out.println(a ^ b);

		// 0000 ^ 0001 = 0001
		a = 0;
		b = 1;
		System.out.println(a ^ b);

		// 0001 ^ 0000 = 0001
		a = 1;
		b = 0;
		System.out.println(a ^ b);

		// 0001 ^ 0001 = 0000
		a = 1;
		b = 1;
		System.out.println(a ^ b);
	}

}
