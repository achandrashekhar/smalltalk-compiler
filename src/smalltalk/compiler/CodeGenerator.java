package smalltalk.compiler;

import org.antlr.symtab.ClassSymbol;
import org.antlr.symtab.Scope;
import org.antlr.symtab.Symbol;
import org.antlr.symtab.Utils;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import smalltalk.compiler.symbols.STBlock;
import smalltalk.compiler.symbols.STClass;
import smalltalk.compiler.symbols.STCompiledBlock;
import smalltalk.compiler.symbols.STPrimitiveMethod;

import java.util.ArrayList;
import java.util.List;

/** Fill STBlock, STMethod objects in Symbol table with bytecode,
 * {@link STCompiledBlock}.
 */
public class CodeGenerator extends SmalltalkBaseVisitor<Code> {
	public static final boolean dumpCode = false;

	public STClass currentClassScope;
	public Scope currentScope;

	/** With which compiler are we generating code? */
	public final Compiler compiler;

	public CodeGenerator(Compiler compiler) {
		this.compiler = compiler;
	}

	/** This and defaultResult() critical to getting code to bubble up the
	 *  visitor call stack when we don't implement every method.
	 */
	@Override
	protected Code aggregateResult(Code aggregate, Code nextResult) {
		if ( aggregate!=Code.None ) {
			if ( nextResult!=Code.None ) {
				return aggregate.join(nextResult);
			}
			return aggregate;
		}
		else {
			return nextResult;
		}
	}

	@Override
	protected Code defaultResult() {
		return Code.None;
	}

	@Override
	public Code visitFile(SmalltalkParser.FileContext ctx) {
		currentScope = compiler.symtab.GLOBALS;
		visitChildren(ctx);
		return Code.None;
	}

	@Override
	public Code visitClassDef(SmalltalkParser.ClassDefContext ctx) {
		currentClassScope = ctx.scope;
		pushScope(ctx.scope);
		Code code = visitChildren(ctx);
		popScope();
		currentClassScope = null;
		return code;
	}

	public STCompiledBlock getCompiledPrimitive(STPrimitiveMethod primitive) {
		STCompiledBlock compiledMethod = new STCompiledBlock(currentClassScope, primitive);
		return compiledMethod;
	}

	/*
	All expressions have values. Must pop each expression value off, except
	last one, which is the block return value. So, we pop after each expr
	unless we're compiling a method block and the expr is not a ^expr. In a
	code block, we pop if we're not the last instruction of the block.

	localVars? expr ('.' expr)* '.'?
	 */
	@Override
	public Code visitFullBody(SmalltalkParser.FullBodyContext ctx) {
		// fill in
		return null;
	}

	@Override
	public Code visitReturn(SmalltalkParser.ReturnContext ctx) {
		Code e = visit(ctx.messageExpression());
		if ( compiler.genDbg ) {
			e = Code.join(e, dbg(ctx.start)); // put dbg after expression as that is when it executes
		}
		Code code = e.join(Compiler.method_return());
		return code;
	}

	public void pushScope(Scope scope) {
		currentScope = scope;
	}

	public void popScope() {
//		if ( currentScope.getEnclosingScope()!=null ) {
//			System.out.println("popping from " + currentScope.getScopeName() + " to " + currentScope.getEnclosingScope().getScopeName());
//		}
//		else {
//			System.out.println("popping from " + currentScope.getScopeName() + " to null");
//		}
		currentScope = currentScope.getEnclosingScope();
	}

	public int getLiteralIndex(String s) {
		return currentClassScope.stringTable.add(s);
	}

	public Code dbgAtEndMain(Token t) {
		int charPos = t.getCharPositionInLine() + t.getText().length();
		return dbg(t.getLine(), charPos);
	}

	public Code dbgAtEndBlock(Token t) {
		int charPos = t.getCharPositionInLine() + t.getText().length();
		charPos -= 1; // point at ']'
		return dbg(t.getLine(), charPos);
	}

	public Code dbg(Token t) {
		return dbg(t.getLine(), t.getCharPositionInLine());
	}

	public Code dbg(int line, int charPos) {
		return Compiler.dbg(getLiteralIndex(compiler.getFileName()), line, charPos);
	}

	public Code store(String id) {
		STBlock scope = (STBlock)currentScope;
		Symbol sym = scope.resolve(id);
		if ( sym==null ) return Code.None;
		if ( sym.getScope() instanceof STBlock ) { // arg or local
			STBlock methodScope = (STBlock)sym.getScope();
			int s = scope.getRelativeScopeCount(id);
			int lit = methodScope.getLocalIndex(id);
			return Compiler.store_local(s, lit);
		}
		else if ( sym.getScope() instanceof STClass ) {
			STClass classWithField = (STClass)sym.getScope();
			int i = classWithField.getFieldIndex(id);
			return Compiler.store_field(i);
		}
		// else must be global; we can't store into globals, only load
		// class names and such.
		return Code.None;
	}

	public Code push(String id) {
		STBlock scope = (STBlock)currentScope;
		Symbol sym = scope.resolve(id);
		if ( sym!=null && sym.getScope() instanceof STClass ) {
			STClass clazz = (STClass)sym.getScope();
			ClassSymbol superClassScope = clazz.getSuperClassScope();
			int numInheritedFields = 0;
			if ( superClassScope!=null ) {
				numInheritedFields = superClassScope.getNumberOfFields();
			}
			int i = numInheritedFields + sym.getInsertionOrderNumber();
			return Compiler.push_field(i);
		}
		else if ( sym!=null && sym.getScope() instanceof STBlock) { // arg or local for block or method
			STBlock methodScope = (STBlock)sym.getScope();
			int s = scope.getRelativeScopeCount(id);
			int lit = methodScope.getLocalIndex(id);
			return Compiler.push_local(s, lit);
		}
		else {
			// must be class or global object; bind late so just use literal
			int lit = getLiteralIndex(id);
			return Compiler.push_global(lit);
		}
	}

	public Code sendKeywordMsg(ParserRuleContext receiver,
							   Code receiverCode,
							   List<SmalltalkParser.BinaryExpressionContext> args,
							   List<TerminalNode> keywords)
	{
		Code code = receiverCode;
		// push all args
		for (SmalltalkParser.BinaryExpressionContext ectx : args) {
			Code elCode = visit(ectx);
			code = code.join(elCode);
		}
		// compute selector and gen a msg send
		String selector = Utils.join(Utils.map(keywords, TerminalNode::getText), "");
		int literalIndex = getLiteralIndex(selector);
		Code send;
		if ( receiver instanceof TerminalNode &&
			 receiver.getStart().getType()==SmalltalkParser.SUPER )
		{
			send = Compiler.send_super(args.size(), literalIndex);
		}
		else {
			send = Compiler.send(args.size(), literalIndex);
		}
		if ( compiler.genDbg ) {
			send = Code.join(dbg(keywords.get(0).getSymbol()), send);
		}
		code = code.join(send);
		return code;
	}

	public String getProgramSourceForSubtree(ParserRuleContext ctx) {
		return compiler.getText(ctx);
	}

}
