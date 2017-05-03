package smalltalk.compiler.test;

import org.antlr.symtab.GlobalScope;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.misc.Utils;
import smalltalk.compiler.Compiler;
import smalltalk.compiler.STC;
import smalltalk.compiler.symbols.STClass;
import smalltalk.compiler.symbols.STSymbolTable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class BaseTest {
	public GlobalScope parseAndGetGlobalScope(String input) {
		return parseAndDefineSymbols(input).getSymbolTable().GLOBALS;
	}

	public CompilerWithHooks parseAndDefineSymbols(String input) {
		CompilerWithHooks compiler = new CompilerWithHooks();
		ParserRuleContext tree = compiler.parseClasses(new ANTLRInputStream(input));
		if ( tree!=null ) {
			compiler.defSymbols(tree);
			compiler.resolveSymbols(tree);
		}
		return compiler;
	}

	public String compile(String fileName, String input) {
		return compile(fileName, input, false);
	}

	public String compile(String fileName, String input, boolean genDbg) {
		StringBuilder code = new StringBuilder();
		smalltalk.compiler.Compiler c = new Compiler();
		c.genDbg = genDbg;
		STSymbolTable symtab = c.compile(fileName, input);
		for (Symbol s : symtab.GLOBALS.getSymbols()) {
			if ( s instanceof STClass ) {
				code.append(((STClass) s).toTestString());
			}
		}
		return code.toString();
	}

	public static Collection<Object[]> getAllTestDescriptors(String subdir) {
		List<Object[]> tests = new ArrayList<>();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL codegenURL = cl.getResource(subdir);
		File dir = new File(codegenURL.getFile());
		for (String f : dir.list()) {
			if ( f.endsWith(".st") ) {
				Object[] args = new Object[3];
				args[0] = f;
				File codeF = new File(dir, f);
				try {
					args[1] = new String(Utils.readFile(codeF.toString()));
					args[2] = new String(Utils.readFile(codeF+"-teststring.txt"));
				}
				catch (IOException ioe) {
					ioe.printStackTrace(System.err);
				}
				tests.add(args);
			}
		}
		return tests;
	}

	public static final String tmpdir = System.getProperty("java.io.tmpdir")+"/test";

	protected void eraseFiles(String dir) {
		if (dir == null) {
			return;
		}

		File tmpdirF = new File(dir);
		String[] files = tmpdirF.list();
		for(int i = 0; files!=null && i < files.length; i++) {
			new File(dir+"/"+files[i]).delete();
		}
	}
}
