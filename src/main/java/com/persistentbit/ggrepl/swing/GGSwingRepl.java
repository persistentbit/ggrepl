package com.persistentbit.ggrepl.swing;

import com.persistentbit.core.glasgolia.repl.ReplConfig;

import javax.swing.*;

/**
 * TODOC
 *
 * @author petermuys
 * @since 18/03/17
 */
public class GGSwingRepl{

	static public void start(ReplConfig config){
		SwingUtilities.invokeLater(() -> {
			try {
				String laf = UIManager.getSystemLookAndFeelClassName();
				UIManager.setLookAndFeel(laf);
			} catch (Exception e) { /* never happens */ }
			GGFrame	frame = new GGFrame(config);
			frame.setVisible(true);
			//frame.textArea.requestFocusInWindow();
		});
	}

	public static void main(String[] args) {
		start(new ReplConfig());
	}
}
