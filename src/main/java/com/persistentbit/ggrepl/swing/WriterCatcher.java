package com.persistentbit.ggrepl.swing;

import java.io.IOException;
import java.io.Writer;
import java.util.function.Consumer;

/**
 * TODOC
 *
 * @author petermuys
 * @since 18/03/17
 */
public class WriterCatcher extends Writer{
	private StringBuffer	text = new StringBuffer(2048);
	private Consumer<String>	consumer;
	public WriterCatcher(Consumer<String> consumer){
		this.consumer = consumer;
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		text.append(cbuf,off,len);
		consumer.accept(text.toString());
	}

	@Override
	public void flush() throws IOException {

	}

	@Override
	public void close() throws IOException {

	}
}
