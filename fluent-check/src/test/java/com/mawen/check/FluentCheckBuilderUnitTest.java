package com.mawen.check;

import java.util.Objects;

import lombok.Builder;
import lombok.Data;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test for {@link FluentCheckBuilder}
 */
class FluentCheckBuilderUnitTest {

	@Test
	@Order(1)
	@DisplayName("单个校验，预期结果为true")
	void shouldSingleCheckIsTrueCorrectly() {

		// given
		Person person = Person.builder().build();

		// when
		boolean result = FluentCheckBuilder.register(Person.class)
				.customCheck(Objects::nonNull)
				.check(person);

		// then
		assertThat(result).isTrue();
	}


	@Test
	@Order(2)
	@DisplayName("单个校验，预期结果为false")
	void shouldSingleCheckIsFalseCorrectly() throws Exception {

		// given
		Person person = Person.builder().id(10L).build();

		// when
		boolean result = FluentCheckBuilder.register(Person.class)
				.customCheck(it -> it.getId() == null)
				.check(person);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@Order(3)
	@DisplayName("单个校验，抛出异常")
	void shouldSingleCheckIsThrowingCorrectly() {

		// given
		Person person = Person.builder().name(" ").build();
		FluentCheckBuilder<Person> builder = FluentCheckBuilder.register(Person.class)
				.customCheck(it -> it.getName() == null).orThrow(() -> "名称中不能包含空格");

		// then
		assertThatThrownBy(() -> builder.check(person))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("名称中不能包含空格");
	}

	@Test
	@Order(4)
	@DisplayName("多个校验，结果为true")
	void shouldMultiCheckIsTrueCorrectly() {

		// given
		Person person = Person.builder().name("abc").id(1L).build();
		FluentCheckBuilder<Person> builder = FluentCheckBuilder.register(Person.class)
				.customCheck(it -> it.getId() != null)
				.customCheck(it -> it.getName() != null);

		// when
		boolean result = builder.check(person);

		// then
		assertThat(result).isTrue();
	}

	@Test
	@Order(5)
	@DisplayName("多个校验，结果为false")
	void shouldMultiCheckIsFalseCorrectly() {

		// given
		Person person = Person.builder().name("abc").id(1L).build();
		FluentCheckBuilder<Person> builder = FluentCheckBuilder.register(Person.class)
				.customCheck(it -> it.getName() == null)
				.peek(System.err::println)
				.customCheck(it -> it.getId() == null)
				.peek(System.err::println);

		// when
		boolean result = builder.check(person);

		// then
		assertThat(result).isFalse();
	}

	@Test
	@Order(6)
	@DisplayName("多个校验，抛出异常")
	void shouldMultiCheckIsThrowingCorrectly() {

		// given
		Person person = Person.builder().name("abc").id(1L).build();
		FluentCheckBuilder<Person> builder = FluentCheckBuilder.register(Person.class)
				.customCheck(it -> it.getId() == null).orThrow(() -> "ID必须为空")
				.peek(System.err::println)
				.customCheck(it -> it.getName() == null).orThrow(() -> "姓名必须为空")
				.peek(System.err::println);

		// then
		assertThatThrownBy(() -> builder.check(person))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("ID必须为空");
	}

	@Data
	@Builder
	public static class Person {

		private Long id;
		private String name;
	}
}