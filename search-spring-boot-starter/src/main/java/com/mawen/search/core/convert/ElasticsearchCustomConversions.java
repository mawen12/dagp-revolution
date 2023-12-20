package com.mawen.search.core.convert;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.mawen.search.core.mapping.ElasticsearchSimpleTypes;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.CustomConversions;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.util.NumberUtils;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2023/12/20
 */
public class ElasticsearchCustomConversions extends CustomConversions {

	private static final StoreConversions STORE_CONVERSIONS;
	private static final List<Converter<?, ?>> STORE_CONVERTERS;

	static {

		List<Converter<?, ?>> converters = new ArrayList<>();
		converters.add(StringToUUIDConverter.INSTANCE);
		converters.add(UUIDToStringConverter.INSTANCE);
		converters.add(BigDecimalToDoubleConverter.INSTANCE);
		converters.add(DoubleToBigDecimalConverter.INSTANCE);
		converters.add(ByteArrayToBase64Converter.INSTANCE);
		converters.add(Base64ToByteArrayConverter.INSTANCE);

		STORE_CONVERTERS = Collections.unmodifiableList(converters);
		STORE_CONVERSIONS = StoreConversions.of(ElasticsearchSimpleTypes.HOLDER, STORE_CONVERTERS);
	}

	/**
	 * Creates a new {@link CustomConversions} instance registering the given converters.
	 *
	 * @param converters must not be {@literal null}.
	 */
	public ElasticsearchCustomConversions(Collection<?> converters) {
		super(STORE_CONVERSIONS, converters);
	}

	/**
	 * {@link Converter} to read a {@link UUID} from its {@link String} representation.
	 */
	@ReadingConverter
	enum StringToUUIDConverter implements Converter<String, UUID> {

		INSTANCE;

		@Override
		public UUID convert(String source) {
			return UUID.fromString(source);
		}
	}

	/**
	 * {@link Converter} to write a {@link UUID} to its {@link String} representation.
	 */
	@WritingConverter
	enum UUIDToStringConverter implements Converter<UUID, String> {

		INSTANCE;

		@Override
		public String convert(UUID source) {
			return source.toString();
		}
	}

	/**
	 * {@link Converter} to read a {@link BigDecimal} from a {@link Double} value.
	 */
	@ReadingConverter
	enum DoubleToBigDecimalConverter implements Converter<Double, BigDecimal> {

		INSTANCE;

		@Override
		public BigDecimal convert(Double source) {
			return NumberUtils.convertNumberToTargetClass(source, BigDecimal.class);
		}
	}

	/**
	 * {@link Converter} to write a {@link BigDecimal} to a {@link Double} value.
	 */
	@WritingConverter
	enum BigDecimalToDoubleConverter implements Converter<BigDecimal, Double> {

		INSTANCE;

		@Override
		public Double convert(BigDecimal source) {
			return NumberUtils.convertNumberToTargetClass(source, Double.class);
		}
	}

	/**
	 * {@link Converter} to write a byte[] to a base64 encoded {@link String} value.
	 *
	 * @since 4.0
	 */
	@WritingConverter
	enum ByteArrayToBase64Converter implements Converter<byte[], String> {

		INSTANCE,
		;

		@Override
		public String convert(byte[] source) {
			return Base64.getEncoder().encodeToString(source);
		}
	}

	/**
	 * {@link Converter} to read a byte[] from a base64 encoded {@link String} value.
	 *
	 * @since 4.0
	 */
	@ReadingConverter
	enum Base64ToByteArrayConverter implements Converter<String, byte[]> {

		INSTANCE;

		@Override
		public byte[] convert(String source) {
			return Base64.getDecoder().decode(source);
		}
	}
}
