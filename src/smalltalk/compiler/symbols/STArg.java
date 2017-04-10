package smalltalk.compiler.symbols;

import org.antlr.symtab.ParameterSymbol;

public class STArg extends ParameterSymbol {
	public STArg(String name) {
		super(name);
	}

	@Override
	public String toString() {
		return getName();
	}
}
