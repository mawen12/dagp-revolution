package com.mawen.search.microbenchmark.support;

import jmh.mbr.junit4.Microbenchmark;
import org.junit.runner.RunWith;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * @author <a href="1181963012mw@gmail.com">mawen12</a>
 * @since 2024/2/18
 * @see <a href="https://github.com/spring-projects/spring-data-dev-tools/blob/main/benchmark/support/src/main/java/org/springframework/data/microbenchmark/common/AbstractMicrobenchmark.java">AbstractMicrobenchmark</a>
 */
@Warmup(iterations = 1)
@Measurement(iterations = 1)
@Fork(value = 1, jvmArgs = { "-server", "-XX:+HeapDumpOnOutOfMemoryError", "-Xms1024m", "-Xmx1024m",
		"-XX:MaxDirectMemorySize=1024m", "-noverify" })
@State(Scope.Thread)
@RunWith(Microbenchmark.class)
public abstract class AbstractMicrobenchmark {


}
