package com.persistentbit.ggrepl.swing;

import com.persistentbit.core.ModuleCore;
import com.persistentbit.core.collections.PList;
import com.persistentbit.core.glasgolia.compiler.GlasgoliaCompiler;
import com.persistentbit.core.glasgolia.compiler.frames.ReplCompileFrame;
import com.persistentbit.core.glasgolia.compiler.rexpr.RExpr;
import com.persistentbit.core.glasgolia.compiler.rexpr.RJavaField;
import com.persistentbit.core.glasgolia.compiler.rexpr.RJavaMethods;
import com.persistentbit.core.glasgolia.compiler.rexpr.RLambda;
import com.persistentbit.core.glasgolia.gexpr.GExpr;
import com.persistentbit.core.glasgolia.repl.GGReplCmd;
import com.persistentbit.core.glasgolia.repl.GGReplCmdParser;
import com.persistentbit.core.glasgolia.repl.ReplConfig;
import com.persistentbit.core.logging.printing.LogPrint;
import com.persistentbit.core.logging.printing.LogPrintStream;
import com.persistentbit.core.parser.ParseResult;
import com.persistentbit.core.parser.source.Source;
import com.persistentbit.core.result.Result;
import com.persistentbit.core.io.IO;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * TODOC
 *
 * @author petermuys
 * @since 18/03/17
 */
public class ReplPanel extends JPanel{
	private ReplConfig	config;
	private RSyntaxTextArea textArea;
	private GlasgoliaCompiler compiler;
	private GGReplCmdParser cmdParser = new GGReplCmdParser();

	public ReplPanel(ReplConfig config) {
		this.config = config;
		textArea = new RSyntaxTextArea(20, 120);
		textArea.addKeyListener(new KeyListener(){
			@Override
			public void keyTyped(KeyEvent e) {
				//System.out.println(e);
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
					runSelected();
				}

			}

			@Override
			public void keyReleased(KeyEvent e) {
				//System.out.println("char = " + (int)e.getKeyChar());
			}
		});
		JTextArea	output = new JTextArea(20,120);
		JScrollPane sp = new JScrollPane(output);
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,textArea,sp);
		setLayout(new BorderLayout());
		add(split);
		/*PrintStream prevOut = System.out;
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		OutputStreamChangedFilter changedFilter = new OutputStreamChangedFilter(bout,() -> {
			String str = bout.toString();
			output.setText(str);
			/*try {
				//SwingUtilities.invokeAndWait(() -> output.setText(str));

			} catch(InterruptedException e) {
				throw new RuntimeException("TODO ERROR HANDLING", e);
			} catch(InvocationTargetException e) {
				throw new RuntimeException("TODO ERROR HANDLING", e);
			}
		});
		System.setOut(new PrintStream(changedFilter,true));
		*/
		PrintStream con=new PrintStream(new TextAreaOutputStream(output,3000));
		System.setOut(con);
		System.setErr(con);
		startRepl();
	}

	private LogPrint lp = LogPrintStream.sysOut(ModuleCore.createLogFormatter(false)).registerAsGlobalHandler();
	private void runSelected(){
		String selected = textArea.getSelectedText();
		if(selected == null){
			selected = getSelectionAt(textArea.getText(),textArea.getCaretPosition());

		}
		selected = selected.trim();
		if(selected.isEmpty()){
			return;
		}
		String code = selected;
		Result<Object> async = Result.async(() -> {
			if(code.startsWith(":")) {
				ParseResult<GGReplCmd> cmdResult =
					cmdParser.command(config.getExprParser()).parse(Source.asSource(code));
				if(cmdResult.isFailure()) {
					throw cmdResult.getError();
				}
				execCmd(cmdResult.getValue());
				return Result.empty("Command executed");
			}else {
				try {
					System.out.println(">>" + code);
					Result<Object> evalResult = compileCode(code).map(v -> v.get());
					if(evalResult.isError()) {
						Throwable error = evalResult.getEmptyOrFailureException().orElse(null);

						lp.print(evalResult.getEmptyOrFailureException().get());
					}
					else {

						System.out.println("Success:" + evalResult.orElse(null));
					}

					return evalResult;
				} catch(Exception e) {
					lp.print(e);
					return Result.failure(e);
				}
			}
		});
		new Thread(() -> async.getOpt()).start();
	}
	static private String getSelectionAt(String text, int pos){
		int start = pos;
		int end = pos;

		boolean prevNew = false;
		start = start-1;
		while(start>=0){
			char c = text.charAt(start);
			if(c == '\n'){
				if(prevNew){
					start+=2;
					break;
				}
				prevNew = true;
			} else {
				prevNew = false;
			}
			start --;
		}
		if(start<0){ start = 0; }
		prevNew = false;
		while(end<text.length()){
			char c = text.charAt(end);
			if(c == '\n'){
				if(prevNew){
					end-=2;
					break;
				}
				prevNew = true;
			} else {
				prevNew = false;
			}
			end ++;
		}
		if(end<0){
			end = 0;
		}
		if(end>text.length()){
			end = text.length();
		}
		return text.substring(start,end);
	}

	private void startRepl() {
		compiler = GlasgoliaCompiler.replCompiler(config.getExprParser(), config.getModuleResourceLoader());
		config.getReplInitResourceName().ifPresent(name -> {
			if(name.endsWith(".glasg") == false){
				name = name + ".glasg";
			}
			String code = config.getModuleResourceLoader().apply(name)
								.map(pb -> pb.toText(IO.utf8)).orElseThrow();
			compileCode(code).orElseThrow().get();
		});
		/*execute.forEach(cmd -> {
			try {
				System.out.println(">>" + cmd);
				System.out.println(compileCode(cmd).orElseThrow().get());
			} catch(Exception e) {
				lp.print(e);
			}
		});*/
	}

	void run(){
		runSelected();
	}

	void reload(){

	}
	private void execCmd(GGReplCmd cmd) {
		switch(cmd.name) {
			case "exit":
				System.exit(0);
				return;
			case "show":
				showCmd(cmd);
				return;
			case "reload":
				reloadCmd(cmd);
				return;
			case "save":
				saveCmd(cmd);
				return;
			case "load":
				loadCmd(cmd);
				return;
			case "reset":
				resetCmd(cmd);
				return;
			default:
				System.out.println("Unknown command:" + cmd.name);
		}
	}
	private void reloadCmd(GGReplCmd cmd) {
		//throw new ReplImpl.ReloadException();
	}

	private void showCmd(GGReplCmd cmd) {
		switch(cmd.params.get(0).toString()) {
			case "context":
				showContextCmd(cmd);
				return;
			case "members":
				showMembersCmd(cmd);
				return;
			default:
				throw new RuntimeException("Expected 'context' or 'members' after show");
		}

	}

	private void showContextCmd(GGReplCmd cmd) {
		ReplCompileFrame                replFrame = (ReplCompileFrame) compiler.getCompileFrame();
		PList<ReplCompileFrame.ReplVar> defs      = replFrame.getDefs();
		defs.forEach(def -> {
			System.out.println(def.nameDef.name + " = " + def.get());
		});
	}

	private void showMembersCmd(GGReplCmd cmd) {
		RExpr  expr  = compiler.compile((GExpr) cmd.params.get(1));
		Object value = expr.get();
		if(value == null) {
			System.out.println("Can't show a null value members");
		}
		Class cls = null;
		if(value instanceof RLambda) {
			RLambda lambda = (RLambda) value;
			System.out.println("Lambda " + lambda.typeDefToString());
		}
		else if(value instanceof RJavaMethods) {
			RJavaMethods jm = (RJavaMethods) value;
			for(Method m : jm.getMethods()) {
				System.out.println(m);
			}
		}
		else if(value instanceof RJavaField) {
			RJavaField javaField = (RJavaField) value;
			System.out.println(javaField.getParentValue() + "." + javaField.getField());
		}
		else if(value instanceof Class) {
			cls = (Class) value;
		}
		else {
			cls = value.getClass();
		}
		if(cls != null) {
			System.out.println("Class: " + cls.getName());
			for(Field f : cls.getFields()) {
				if(Modifier.isPublic(f.getModifiers())) {
					System.out.println("\t" + f);
				}
			}
			for(Constructor m : cls.getConstructors()) {
				if(Modifier.isPublic(m.getModifiers())) {
					System.out.println("\t" + m);
				}
			}
			for(Method m : cls.getMethods()) {
				if(Modifier.isPublic(m.getModifiers())) {
					System.out.println("\t" + m);
				}
			}
		}
	}

	private void saveCmd(GGReplCmd cmd) {
		/*File   f    = new File(cmd.params.getOpt(0).map(s -> s.toString()).orElse("session.glasg"));
		String code = history.fold("", (a, b) -> a + UString.NL + b);
		IO.write(code, f, IO.utf8);
		System.out.println("Session saved to " + f.getAbsolutePath());
		*/
	}

	private void loadCmd(GGReplCmd cmd) {
		/*File f = new File(cmd.params.getOpt(0).map(s -> s.toString()).orElse("session.glasg"));
		String res = IO.readTextFile(f, IO.utf8)
					   .ifPresent(s -> history = PList.val(s.getValue().trim()))
					   .ifPresent(s -> System.out.println("loaded " + f.getAbsolutePath()))
					   .orElseThrow().trim();
		throw new ReplImpl.ReloadException();*/
	}

	public void resetCmd(GGReplCmd cmd) {
		//history = PList.empty();
		//throw new ReplImpl.ReloadException();
	}



	private Result<RExpr> compileCode(String code) {
		return compiler.compileCode(code);
	}
}
