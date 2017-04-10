package smalltalk.compiler.symbols;

import org.antlr.symtab.GlobalScope;

public class STSymbolTable {
	public final GlobalScope GLOBALS;

	public STSymbolTable() {
		this.GLOBALS = new GlobalScope(null);
	}
}
