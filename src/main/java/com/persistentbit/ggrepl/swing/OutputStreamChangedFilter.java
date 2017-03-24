package com.persistentbit.ggrepl.swing;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * TODOC
 *
 * @author petermuys
 * @since 18/03/17
 */
public class OutputStreamChangedFilter extends FilterOutputStream{
	@FunctionalInterface
	public interface ChangeListener{
		void changed();
	}

	private final ChangeListener listener;

	public OutputStreamChangedFilter(OutputStream out,ChangeListener changeListener) {
		super(out);
		this.listener = changeListener;
	}

	@Override
	public void flush() throws IOException {
		super.flush();
		this.listener.changed();
	}
}
