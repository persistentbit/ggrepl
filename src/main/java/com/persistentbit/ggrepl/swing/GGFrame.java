package com.persistentbit.ggrepl.swing;

import com.persistentbit.core.glasgolia.repl.ReplConfig;

import javax.swing.*;
import java.awt.*;

/**
 * TODOC
 *
 * @author petermuys
 * @since 18/03/17
 */
public class GGFrame extends JFrame{
	private JPanel	contentPane;

	private JToolBar	toolBar;
	private JButton		btnRun;
	private JButton		btnReload;
	private ReplPanel	replPanel;
	public GGFrame(ReplConfig config){
		super("Glasgolia REPL");

		//textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		//textArea.setCodeFoldingEnabled(true);
		replPanel = new ReplPanel(config);
		toolBar = new JToolBar();
		btnRun = new JButton(USwing.icon("arrows-24px-outline-1_circle-right-09.png"));
		btnRun.setToolTipText("RUN selected text");
		btnReload = new JButton(USwing.icon("arrows-24px-outline-1_refresh-69.png"));
		toolBar.add(btnReload);
		toolBar.add(btnRun);

		btnReload.addActionListener(e -> replPanel.reload());
		btnRun.addActionListener(e -> replPanel.run());

		contentPane = new JPanel(new BorderLayout());

		contentPane.add(toolBar, BorderLayout.NORTH);
		contentPane.add(replPanel,BorderLayout.CENTER);
		setContentPane(contentPane);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}
}
