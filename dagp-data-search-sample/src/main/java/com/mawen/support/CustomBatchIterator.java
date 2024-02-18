package com.mawen.support;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/1/28
 */
public class CustomBatchIterator<T> implements Iterator<List<T>> {

	private final int batchSize;
	private List<T> currentBatch;
	private final Iterator<T> iterator;

	private CustomBatchIterator(int batchSize, Iterator<T> sourceIterator) {
		this.batchSize = batchSize;
		this.iterator = sourceIterator;
	}

	@Override
	public boolean hasNext() {
		return iterator.hasNext();
	}

	@Override
	public List<T> next() {
		currentBatch = new ArrayList<>(batchSize);
		while (iterator.hasNext() && currentBatch.size() < batchSize) {
			currentBatch.add(iterator.next());
		}
		return currentBatch;
	}

	public static <T> Stream<List<T>> batchStreamOf(Stream<T> stream,  int batchSize) {
		return stream(new CustomBatchIterator<>(batchSize,stream.iterator()));
	}

	public static <T> Stream<T> stream(Iterator<T> iterator) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
	}
}
