package com.mawen.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

class ListFunctionTest {

	public static List<User> DB_USER = new ArrayList<>();

	static {
		DB_USER.add(new User(1L, "mawen", 20));
		DB_USER.add(new User(2L, "lucy", 30));
		DB_USER.add(new User(3L, "jake", 40));
		DB_USER.add(new User(4L, "bob", 50));
	}

	public static void main(String[] args) {
		List<Long> ids = new ArrayList<>();
		ids.add(1L);
		ids.add(2L);
		ids.add(3L);

		fun1(ids);
	}

	public static void fun1(List<Long> ids) {
		Function<List<Long>, List<User>> sourceGetter = ListFunctionTest::findInDb;

		Function<List<Long>, List<User>> wrapperGetter = ListFunction.ofMap(sourceGetter, User::getId);

		print(ids, wrapperGetter);
		System.out.println();
		print(ids, wrapperGetter);
		System.out.println();
		print(ids, wrapperGetter);
	}

	public static List<User> findInDb(List<Long> ids) {
		System.out.println("Call findInDb with parameter: " + ids + ".");
		return DB_USER.stream()
				.filter(user -> ids.contains(user.getId()))
				.collect(Collectors.toList());
	}

	private static void print(List<Long> ids, Function<List<Long>, List<User>> userGetter) {
		List<User> result = userGetter.apply(ids);
		if (result.size() != ids.size()) {
			System.err.println("ids: " + ids + ", size: " + result.size());
		}
		else {
			System.out.println(result);
		}
	}

	public static class User {
		private Long id;
		private String name;
		private Integer age;

		public User(Long id, String name, Integer age) {
			this.id = id;
			this.name = name;
			this.age = age;
		}

		public Long getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getAge() {
			return age;
		}

		@Override
		public String toString() {
			return "User{" +
			       "id=" + id +
			       ", name='" + name + '\'' +
			       ", age=" + age +
			       '}';
		}
	}
}