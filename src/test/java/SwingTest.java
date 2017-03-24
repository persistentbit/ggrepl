import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.TextAction;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * TODOC
 *
 * @author petermuys
 * @since 18/03/17
 */
public class SwingTest extends JFrame  implements ActionListener{
	private RSyntaxTextArea textArea;
	private JTextField searchField;
	private JCheckBox regexCB;
	private JCheckBox matchCaseCB;
	public SwingTest() {

		JPanel cp = new JPanel(new BorderLayout());

		textArea = new RSyntaxTextArea(20, 60);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setCodeFoldingEnabled(true);

		try {
			Theme theme = Theme.load(getClass().getResourceAsStream(
				"/org/fife/ui/rsyntaxtextarea/themes/dark.xml"));
			theme.apply(textArea);
		} catch (IOException ioe) { // Never happens
			ioe.printStackTrace();
		}
		// Add an item to the popup menu that opens the file whose name is
		// specified at the current caret position.
		JPopupMenu popup = textArea.getPopupMenu();
		popup.addSeparator();
		popup.add(new JMenuItem(new OpenFileAction()));

		RTextScrollPane sp = new RTextScrollPane(textArea);
		cp.add(sp);

		// Create a toolbar with searching options.
		JToolBar toolBar = new JToolBar();
		searchField = new JTextField(30);
		toolBar.add(searchField);
		final JButton nextButton = new JButton("Find Next");
		nextButton.setActionCommand("FindNext");
		nextButton.addActionListener(this);
		toolBar.add(nextButton);
		searchField.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				nextButton.doClick(0);
			}
		});
		JButton prevButton = new JButton("Find Previous");
		prevButton.setActionCommand("FindPrev");
		prevButton.addActionListener(this);
		toolBar.add(prevButton);
		regexCB = new JCheckBox("Regex");
		toolBar.add(regexCB);
		matchCaseCB = new JCheckBox("Match Case");
		toolBar.add(matchCaseCB);
		cp.add(toolBar, BorderLayout.NORTH);

		setContentPane(cp);
		setTitle("Find and Replace Demo");
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);

	}
	public void actionPerformed(ActionEvent e) {

		// "FindNext" => search forward, "FindPrev" => search backward
		String command = e.getActionCommand();
		boolean forward = "FindNext".equals(command);

		// Create an object defining our search parameters.
		SearchContext context = new SearchContext();
		String        text    = searchField.getText();
		if (text.length() == 0) {
			return;
		}
		context.setSearchFor(text);
		context.setMatchCase(matchCaseCB.isSelected());
		context.setRegularExpression(regexCB.isSelected());
		context.setSearchForward(forward);
		context.setWholeWord(false);

		boolean found = SearchEngine.find(textArea, context).wasFound();
		if (!found) {
			JOptionPane.showMessageDialog(this, "Text not found");
		}

	}
	/**
	 * Loads a file's contents into the text area, or displays an error message
	 * if the file does not exist.
	 *
	 * @param file The file to load.
	 */
	public void loadFile(File file) {

		System.out.println("DEBUG: " + file.getAbsolutePath());
		if (file.isDirectory()) { // Clicking on a space character
			JOptionPane.showMessageDialog(this, file.getAbsolutePath()
				+ " is a directory", "Error", JOptionPane.ERROR_MESSAGE);
			return;
		} else if (!file.isFile()) {
			JOptionPane.showMessageDialog(this, "No such file: "
				+ file.getAbsolutePath(), "Error", JOptionPane.ERROR_MESSAGE);
			return;
		}

		try {
			BufferedReader r = new BufferedReader(new FileReader(file));
			textArea.read(r, null);
			r.close();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			UIManager.getLookAndFeel().provideErrorFeedback(textArea);
		}

	}



	/**
	 * An action that gets the filename at the current caret position and tries
	 * to open that file. If there is a selection, it uses the selected text as
	 * the filename.
	 */
	private class OpenFileAction extends TextAction{

		public OpenFileAction() {
			super("Open File");
		}

		public void actionPerformed(ActionEvent e) {

			JTextComponent tc = getTextComponent(e);
			String filename = null;

			// Get the name of the file to load. If there is a selection, use
			// that as the file name, otherwise, scan for a filename around
			// the caret.
			try {
				int selStart = tc.getSelectionStart();
				int selEnd = tc.getSelectionEnd();
				if (selStart != selEnd) {
					filename = tc.getText(selStart, selEnd - selStart);
				} else {
					filename = getFilenameAtCaret(tc);
				}
			} catch (BadLocationException ble) {
				ble.printStackTrace();
				UIManager.getLookAndFeel().provideErrorFeedback(tc);
				return;
			}

			loadFile(new File(filename));

		}

		/**
		 * Gets the filename that the caret is sitting on. Note that this is a
		 * somewhat naive implementation and assumes filenames do not contain
		 * whitespace or other "funny" characters, but it will catch most common
		 * filenames.
		 *
		 * @param tc The text component to look at.
		 * @return The filename at the caret position.
		 * @throws BadLocationException Shouldn't actually happen.
		 */
		public String getFilenameAtCaret(JTextComponent tc) throws BadLocationException {
			int      caret = tc.getCaretPosition();
			int      start = caret;
			Document doc   = tc.getDocument();
			while (start > 0) {
				char ch = doc.getText(start - 1, 1).charAt(0);
				if (isFilenameChar(ch)) {
					start--;
				} else {
					break;
				}
			}
			int end = caret;
			while (end < doc.getLength()) {
				char ch = doc.getText(end, 1).charAt(0);
				if (isFilenameChar(ch)) {
					end++;
				} else {
					break;
				}
			}
			return doc.getText(start, end - start);
		}

		public boolean isFilenameChar(char ch) {
			return Character.isLetterOrDigit(ch) || ch == ':' || ch == '.'
				|| ch == File.separatorChar;
		}

	}
	public static void main(String[] args) {
		// Start all Swing applications on the EDT.
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try {
					String laf = UIManager.getSystemLookAndFeelClassName();
					UIManager.setLookAndFeel(laf);
				} catch (Exception e) { /* never happens */ }
				SwingTest demo = new SwingTest();
				demo.setVisible(true);
				demo.textArea.requestFocusInWindow();
			}
		});
	}

}