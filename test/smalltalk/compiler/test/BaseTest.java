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
import smalltalk.vm.VirtualMachine;
import smalltalk.vm.primitive.STObject;

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
	public void execAndCheck(String input, String expecting,
	                         boolean trace, boolean genDbg)
	{
		try {
			VirtualMachine vm = new VirtualMachine();

			// load linked list, dict, and core stuff
//			ClassLoader cl = Thread.currentThread().getContextClassLoader();
//			URL lib = cl.getResource("stlib.zip");
//			vm.loadZippedLib(lib.getFile());

			File outputFile = new File(tmpdir+"/test.st");
			String outputPath = outputFile.getParent();
			eraseFiles(outputPath);
			File outputDir = new File(outputPath);
			outputDir.mkdirs();

			STSymbolTable symtab = new STSymbolTable();
			Files.write(outputFile.toPath(), input.getBytes());
			STC.compile(symtab, outputFile.toString(), genDbg);
			STC.writeObjectFiles(outputPath, Paths.get(outputFile.toURI()).getFileName().toString(), symtab);
			vm.loadLibDir(outputPath); // load all .sto in that dir compiled from test.st

			STObject result = vm.execMain();
			assertEquals(expecting, result.toString());
		}
		catch (IOException ioe) {
			ioe.printStackTrace(System.err);
		}
	}

	public void execAndCheck(String input, String expecting, boolean genDbg) {
		boolean trace = false;
		execAndCheck(input, expecting, trace, genDbg);
	}

	public void execAndCheck(String input, String expecting) {
		boolean genDbg = true;
		execAndCheck(input, expecting, genDbg);
	}

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
