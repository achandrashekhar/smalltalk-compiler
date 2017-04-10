package smalltalk.compiler;

import org.antlr.symtab.ClassSymbol;
import org.antlr.symtab.Symbol;
import org.antlr.v4.runtime.misc.Utils;
import smalltalk.compiler.symbols.STClass;
import smalltalk.compiler.symbols.STSymbolTable;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Smalltalk compiler.
 *
 *  I use an alias so command-line is nice:
 *
 *  alias stc='java -cp "/Users/parrt/.m2/repository/edu/usfca/cs652/smalltalk-compiler/1.0/smalltalk-compiler-1.0-complete.jar:$CLASSPATH" smalltalk.compiler.STC'
 *
 *  That lets you say `stc file.st`.
 *
 *  You must do `mvn -DskipTests install` before that will work.
 *
 *  You can also use `java -jar /Users/parrt/.m2/repository/edu/usfca/cs652/smalltalk-compiler/1.0/smalltalk-compiler-1.0-complete.jar`
 *  and it knows the main class to execute.
 */
public class STC {
	public static void main(String[] args) throws Exception {
		int fi = 0;
		boolean dbg = false;
		boolean dis = false; // disassemble
		String outputDir = ".";
		String stFileName = null;

		while (fi<args.length) {
			switch ( args[fi] ) {
				case "-dbg" :
					dbg = true;
					break;
				case "-dis" :
					dis = true;
					break;
				case "-o" :
					fi++;
					outputDir = args[fi];
					break;
				default :
					stFileName = args[fi];
					break;
			}
			fi++;
		}

		if ( stFileName==null ) {
			System.err.println("$ java smalltalk.compiler.STC [-dis] [-o outputdir] file.st");
			System.exit(1);
		}
		STSymbolTable symtab = compile(stFileName, dbg);
		writeObjectFiles(outputDir, stFileName, symtab);
		if ( dis ) {
			disassembleOutput(outputDir, stFileName, symtab);
		}
	}

	public static void disassembleOutput(String dir, String stFileName, STSymbolTable symtab) throws IOException {
		for (Symbol s : symtab.GLOBALS.getSymbols()) {
			if ( s instanceof ClassSymbol ) {
				String obj = ((STClass) s).toTestString();
				Files.write(Paths.get(dir, stFileName+"-teststring.txt"), obj.getBytes());
			}
		}
	}

	public static void writeObjectFiles(String dir, String stFileName, STSymbolTable symtab) throws IOException {
		for (Symbol s : symtab.GLOBALS.getSymbols()) {
			if ( s instanceof ClassSymbol ) {
				String obj = ((STClass) s).serialize().toString();
				Files.write(Paths.get(dir, s.getName()+".sto"), obj.getBytes());
			}
		}
	}

	public static STSymbolTable compile(String fileName, boolean genDbg) {
		STSymbolTable symtab = new STSymbolTable();
		compile(symtab, fileName, genDbg);
		return symtab;
	}

	public static STSymbolTable compile(STSymbolTable symtab, String fileName, boolean genDbg) {
		Compiler c;
		if ( symtab!=null ) {
			c = new Compiler(symtab);
		}
		else {
			c = new Compiler();
		}
		c.genDbg = genDbg;

		URL imageURL = getFileURL(fileName);
		try {
			fileName = Paths.get(fileName).getFileName().toString();
			symtab = c.compile(fileName, new String(Utils.readFile(imageURL.getFile())));
			// TODO: semantic checks for unknown vars/fields
		}
		catch (IOException e ) {
			throw new RuntimeException("can't load "+imageURL, e);
		}
		if ( c.errors.size()>0 ) {
			throw new RuntimeException("compile errors: "+c.errors.toString(),null);
		}
		return symtab;
	}

	public static URL getFileURL(String fileName) {
		URL url;
		File dir = new File(fileName);
		if ( dir.exists() ) {
			try {
				url = dir.toURI().toURL();
			}
			catch (MalformedURLException mue) {
				throw new IllegalArgumentException("bad filename: "+fileName);
			}
		}
		else { // try in classpath
			ClassLoader cl = Thread.currentThread().getContextClassLoader();
			url = cl.getResource(fileName);
			if ( url==null ) {
				throw new IllegalArgumentException("No such image file: "+
					                                   fileName);
			}
		}
		return url;
	}
}
