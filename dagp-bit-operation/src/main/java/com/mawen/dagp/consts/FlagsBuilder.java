package com.mawen.dagp.consts;

import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/3/27
 */
public class FlagsBuilder {


	public static void handle(AssetAttr assetAttr, Function<AssetAttr, Integer> maskGetter, Function<AssetAttr, Predicate<Integer>> getter, BiConsumer<AssetAttr, Predicate<Integer>> consumer) {
		Integer mask = maskGetter.apply(assetAttr);

		if (mask != null) {
//			Predicate<Integer> predicate = getter.get();
//			consumer.accept(assetAttr, predicate);
		}
	}


	public static void main(String[] args) {
		AssetAttr attr = new AssetAttr();
	}
}
