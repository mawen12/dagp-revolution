package com.mawen.check;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.Test;

import com.enhe.core.api.utils.FillUtil;
import static org.assertj.core.api.Assertions.*;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/4/8
 */
public class ListCheckUnitTest {

	private static List<Address> ADDRESS = new ArrayList<>();

	static {
		ADDRESS.add(new Address(1L, "A"));
		ADDRESS.add(new Address(2L, "B"));
		ADDRESS.add(new Address(3L, "C"));
		ADDRESS.add(new Address(4L, "D"));
	}

	@Test
	void test() {

		List<Person> persons = new ArrayList<>();

		ListCheckBuilder.of((List<Person> ps) -> ps.isEmpty())
				.check(persons);

		assertThatThrownBy(() -> ListCheck.<Person>of(List::isEmpty)
				.negate()
				.orThrow(() -> "用户数必须大于0")
				.check(persons));

		assertThatThrownBy(() -> ListCheck.<Person>of()
				.and(List::isEmpty).negate().orThrow(() -> "用户数必须大于0")
				.check(persons));
	}

	@Test
	void test2() {

		List<Person> persons = new ArrayList<>();

		ListCheck.<Person>of(List::isEmpty).orThrow(() -> "不能超过0")
				.check(persons);
	}

	@Test
	void test3() {

		List<Person> persons = new ArrayList<>();
		persons.add(new Person(1L, "mawen", 2L, null));

		Check<Person> nullCheck = Check.<Person>build()
				.and(Objects::nonNull);

		Check<Person> addressCheck = Check.<Person>build()
				.and(Objects::nonNull)
				.and(person -> person.getAddress() == null).orThrow(() -> "地址不能为空");

		Check<Address> addressCheck2 = Check.<Address>build()
				.and(Objects::nonNull);

		Function<List<Person>, List<Address>> map = ps -> ps.stream().map(Person::getAddress).collect(Collectors.toList());

		ListCheck.<Person>of()
				.and(nullCheck::checkAll).orThrow(() -> "校验失败")
				.peek(list -> FillUtil.fillByKeyWithListLoader(persons, Person::getAddressId, ListCheckUnitTest::listByIds, Address::getId, Person::setAddress))
				.and(addressCheck::checkAll).orThrow(() -> "地址校验失败")
				.and(list -> addressCheck2.checkAll(map.apply(list)))
				.check(persons);
	}

	@Test
	void test4() {

		List<Person> persons = new ArrayList<>();
		persons.add(new Person(1L, "mawen", 2L, null));

		Check<Person> nullCheck = Check.<Person>build()
				.and(Objects::nonNull);

		Check<Person> addressCheck = Check.<Person>build()
				.and(Objects::nonNull)
				.and(person -> person.getAddress() == null).orThrow(() -> "地址不能为空");

		Check<Address> addressCheck2 = Check.<Address>build()
				.and(Objects::nonNull);

		Function<List<Person>, List<Address>> map = ps -> ps.stream().map(Person::getAddress).collect(Collectors.toList());

		ListCheck.<Person>of()
				.tag("空指针校验").and(nullCheck::checkAll).orThrow(() -> "校验失败")
				.tag("用户信息填充").peek(list -> FillUtil.fillByKeyWithListLoader(persons, Person::getAddressId, ListCheckUnitTest::listByIds, Address::getId, Person::setAddress))
				.tag("用户地址校验").and(addressCheck::checkAll).orThrow(() -> "地址校验失败")
				.tag("地址校验2").and(list -> addressCheck2.checkAll(map.apply(list)))
				.check(persons);
	}

	@Test
	void test5() {

		List<Person> persons = new ArrayList<>();
		persons.add(new Person(1L, "mawen", 2L, null));

		Check<Person> nullCheck = Check.<Person>build()
				.and(Objects::nonNull);

		Check<Person> addressCheck = Check.<Person>build()
				.and(Objects::nonNull)
				.and(person -> person.getAddress() == null).orThrow(() -> "地址不能为空");

		Check<Address> addressCheck2 = Check.<Address>build()
				.and(Objects::nonNull);

		Function<List<Person>, List<Address>> map = ps -> ps.stream().map(Person::getAddress).collect(Collectors.toList());

		ListCheck.<Person>of()
				.and(nullCheck::checkAll).orThrow(() -> "校验失败").tag("空指针校验")
				.peek(list -> FillUtil.fillByKeyWithListLoader(persons, Person::getAddressId, ListCheckUnitTest::listByIds, Address::getId, Person::setAddress)).tag("用户信息填充")
				.and(addressCheck::checkAll).orThrow(() -> "地址校验失败").tag("用户地址校验")
				.and(list -> addressCheck2.checkAll(map.apply(list))).tag("地址校验2")
				.check(persons);
	}

	public static List<Address> listByIds(List<Long> ids) {
		return ADDRESS;
	}

	@Data
	@Builder
	public static class Person {
		private Long id;
		private String name;
		private Long addressId;
		private Address address;
	}

	@Data
	@Builder
	public static class Address {

		private Long id;

		private String name;
	}
}
