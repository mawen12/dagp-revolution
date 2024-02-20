package com.mawen.search.microbenchmark.support;

import jmh.mbr.core.ResultsWriter;
import jmh.mbr.core.ResultsWriterFactory;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/2/18
 */
public class MicrobenchmarkResultsWriterFactory implements ResultsWriterFactory {

	@Override
	public ResultsWriter forUri(String uri) {

		if (uri.startsWith("http")) {
			return new HttpResultsWriter(uri);
		}

		return null;
	}
}
