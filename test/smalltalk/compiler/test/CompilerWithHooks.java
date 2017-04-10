package smalltalk.compiler.test;

import org.antlr.v4.runtime.Parser;
import smalltalk.compiler.SmalltalkParser;
import smalltalk.compiler.symbols.STSymbolTable;

/** This class is a compiler that provides hooks for testing */
public class CompilerWithHooks extends smalltalk.compiler.Compiler {
	public STSymbolTable getSymbolTable() { return symtab; }
	public SmalltalkParser.FileContext getFileTree() { return fileTree; }
	public Parser getParser() { return parser; }
}
