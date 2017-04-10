package smalltalk.compiler.symbols;

import org.antlr.v4.runtime.ParserRuleContext;

/** "A primitive [message] response is performed directly by the interpreter
 *  without creating a new context or executing any other bytecodes. A
 *  primitive routine removes the message receiver and arguments from
 *  the stack and replaces them with the appropriate result."
 *  BlueBook p 634 in pdf.
 */
public class STPrimitiveMethod extends STMethod {
	public final String primitiveName;

	public STPrimitiveMethod(String name, ParserRuleContext tree, String primitiveName) {
		super(name, tree);
		this.primitiveName = primitiveName;
	}

	@Override
	public int nlocals() { return 0; }
}
