package smalltalk.compiler;

import org.antlr.symtab.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import smalltalk.compiler.symbols.*;

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

	@Override
	public Code visitMain(SmalltalkParser.MainContext ctx) {
		int blockIndex = 0; //
		Code code = Code.None;//
		currentScope = ctx.scope;//
		currentClassScope = ctx.classScope;//
		pushScope(ctx.scope);//
		if(currentClassScope!=null) //
		{
			STMethod stMethod = ctx.scope; //
			STCompiledBlock block = new STCompiledBlock(currentClassScope, (STBlock) currentScope);//
			block.blocks = new STCompiledBlock[stMethod.getAllNestedScopedSymbols().size()]; //
			code = aggregateResult(code, visitChildren(ctx)); //
			for(Scope symbol : stMethod.getAllNestedScopedSymbols()){
				STCompiledBlock stCompiledBlock = new STCompiledBlock(currentClassScope,(STBlock)symbol);
				stCompiledBlock.bytecode = ((STBlock)symbol).compiledBlock.bytecode;
				block.blocks[blockIndex] = stCompiledBlock;
				blockIndex++;
			}
			ctx.scope.compiledBlock = block;

//			if (ctx.body() instanceof  SmalltalkParser.FullBodyContext) {
//				code = aggregateResult(code, Compiler.pop());
//			}
			code = aggregateResult(code, Compiler.push_self());
			code = aggregateResult(code, Compiler.method_return());
			ctx.scope.compiledBlock.bytecode = code.bytes();
			popScope();
		}
			return code;

	}

	@Override
	public Code visitNamedMethod(SmalltalkParser.NamedMethodContext ctx) {
		int blockIndex = 0;
		Code code = Code.None;
		currentScope = ctx.scope;
			STMethod stMethod = new STMethod(ctx.ID().getText(),ctx);
			STCompiledBlock block = new STCompiledBlock(currentClassScope, (STBlock) currentScope);
			block.blocks = new STCompiledBlock[stMethod.getAllNestedScopedSymbols().size()];
			code = aggregateResult(code, visitChildren(ctx));
			for(Scope symbol : stMethod.getAllNestedScopedSymbols()){
				STCompiledBlock stCompiledBlock = new STCompiledBlock(currentClassScope,(STBlock)symbol);
				stCompiledBlock.bytecode = ((STBlock)symbol).compiledBlock.bytecode;
				block.blocks[blockIndex] = stCompiledBlock;
				blockIndex++;
			}
			ctx.scope.compiledBlock = block;
//			code = aggregateResult(code, Compiler.push_self());
//			code = aggregateResult(code, Compiler.method_return());
			ctx.scope.compiledBlock.bytecode = code.bytes();
			popScope();
		return Code.None;

	}

	@Override
	public Code visitSmalltalkMethodBlock(SmalltalkParser.SmalltalkMethodBlockContext ctx) {
		int blockIndex = 0;
		Code code = visit(ctx.body());
		code = aggregateResult(code, Compiler.push_self());
		code = aggregateResult(code, Compiler.method_return());
		STMethod stMethod = (STMethod) currentScope;
		STCompiledBlock block = new STCompiledBlock(currentClassScope, (STBlock) currentScope);
		block.blocks = new STCompiledBlock[stMethod.getAllNestedScopedSymbols().size()];
		for(Scope symbol : stMethod.getAllNestedScopedSymbols()){
			STCompiledBlock stCompiledBlock = new STCompiledBlock(currentClassScope,(STBlock)symbol);
			stCompiledBlock.bytecode = ((STBlock)symbol).compiledBlock.bytecode;
			block.blocks[blockIndex] = stCompiledBlock;
			blockIndex++;
		}
		((STBlock)currentScope).compiledBlock = block;
//			code = aggregateResult(code, Compiler.push_self());
//			code = aggregateResult(code, Compiler.method_return());
		((STBlock)currentScope).compiledBlock.bytecode = code.bytes();
		return code;
	}

	/**
	 All expressions have values. Must pop each expression value off, except
	 last one, which is the block return value. Visit method for blocks will
	 issue block_return instruction. Visit method for method will issue
	 pop self return.  If last expression is ^expr, the block_return or
	 pop self return is dead code but it is always there as a failsafe.

	 localVars? expr ('.' expr)* '.'?
	 */
	@Override
	public Code visitFullBody(SmalltalkParser.FullBodyContext ctx) {
		// fill in
//		Code code = defaultResult();
//		Code code1 = defaultResult();
//		for(SmalltalkParser.StatContext stat : ctx.stat()){
//			code = visit(stat);
//			code1 = aggregateResult(code1,code);
//		}
//
//		return code1;
		Code code = new Code();
//		code = visit(ctx.localVars());
		List<SmalltalkParser.StatContext> stats = ctx.stat();
		for (int i = 0; i < stats.size(); i++) {
			code = code.join(visit(ctx.stat(i)));
			if (i < stats.size() - 1)
				code = code.join(Compiler.pop());
		}
		if(currentScope instanceof STMethod){
		if(!currentScope.getName().equals("Main")) {

			code = code.join(Compiler.pop());
		}

		}
		return code;
	}

	@Override
	public Code visitEmptyBody(SmalltalkParser.EmptyBodyContext ctx) {

		Code code = Code.None;
		if(currentClassScope.getName().equals("MainClass")){
			code = Compiler.push_nil();
		}
		return code;
	}


	@Override
	public Code visitReturn(SmalltalkParser.ReturnContext ctx) {
//		Code e = visit(ctx.messageExpression());
//		if ( compiler.genDbg ) {
//			e = Code.join(e, dbg(ctx.start)); // put dbg after expression as that is when it executes
//		}
//		Code code = e.join(Compiler.method_return());
//		return code;
		Code e = visit(ctx.messageExpression());
		Code code = e.join(Compiler.method_return());
		return code;
	}

	@Override
	public Code visitKeywordSend(SmalltalkParser.KeywordSendContext ctx) {
		Code code = visit(ctx.recv);
		for(SmalltalkParser.BinaryExpressionContext str : ctx.args){
			code = aggregateResult(code,visit(str));
		}
//		currentClassScope.stringTable.add(ctx.KEYWORD(0).getText());
		System.out.println(ctx.args.size());
		code = sendKeywordMsg(ctx.recv,code,ctx.args,ctx.KEYWORD());
		return code;
	}

	@Override
	public Code visitUnaryIsPrimary(SmalltalkParser.UnaryIsPrimaryContext ctx) {
		Code code = visit(ctx.primary());
		return code;
	}

	@Override
	public Code visitId(SmalltalkParser.IdContext ctx) {
//		int index = currentClassScope.stringTable.add(ctx.ID().getText());
//		Code code = Compiler.push_global(index);
//		return code;
		Code code = Code.None;
		int index = 0;
		if(ctx.sym instanceof STField){
			System.out.println(ctx.ID().getText());
			code = Compiler.push_field(fieldIndex(ctx.sym));
		} else if(ctx.sym instanceof STVariable || ctx.sym instanceof STArg ) {
			System.out.println("Name is "+ctx.sym.getName());
				STBlock stBlock = (STBlock) currentScope;
				int i = stBlock.getLocalIndex(ctx.ID().getText());
				int d = stBlock.getRelativeScopeCount(ctx.ID().getText());
				code = Compiler.push_local(d, i);
		} else {
			index = currentClassScope.stringTable.add(ctx.ID().getText());
			code = Compiler.push_global(index);
		}



		return code;
	}

	@Override
	public Code visitLiteral(SmalltalkParser.LiteralContext ctx) {
		Code code = Code.None;
		if (ctx.NUMBER() != null) {
			if(ctx.NUMBER().getText().contains(".")){
				code = Compiler.push_float(Float.parseFloat(ctx.NUMBER().getText()));

			} else {
				code = Compiler.push_int(Integer.parseInt(ctx.NUMBER().getText()));
			}
		} else if (ctx.CHAR() != null){
			char c = ctx.CHAR().getText().charAt(1);
			code.join(Compiler.push_char(c));
		}
		else if(ctx.STRING()!=null) {
//			if (!(ctx.getText().equals("nil") || ctx.getText().equals("self")
//					|| ctx.getText().equals("true") || ctx.getText().equals("false"))) {
				String stringToBePushed = ctx.getText();
				if (stringToBePushed.contains("\'")) {
					stringToBePushed = stringToBePushed.replace("\'", "");
				}

				int literalIndex = getLiteralIndex(stringToBePushed);
				code = Compiler.push_literal(literalIndex);
			} else {
				switch (ctx.getText()){
					case "nil":
						code = Compiler.push_nil();
						break;
					case "self":
						code = Compiler.push_self();
						break;
					case "true":
						code = Compiler.push_true();
						break;
					case "false":
						code = Compiler.push_false();
						break;
				}

			}

		return code;
	}

	@Override
	public Code visitBlock(SmalltalkParser.BlockContext ctx) {
		currentScope = ctx.scope;
		STBlock stBlock = (STBlock)currentScope;
		Code blockd = Compiler.block(stBlock.index);
		Code code = visit(ctx.body());
		code = aggregateResult(code,Compiler.block_return());
		ctx.scope.compiledBlock = new STCompiledBlock(currentClassScope,(STBlock)currentScope);
		ctx.scope.compiledBlock.bytecode = code.bytes();
		popScope();;
		return blockd;
	}

	@Override
	public Code visitBinaryExpression(SmalltalkParser.BinaryExpressionContext ctx) {
//		Code code = visit(ctx.unaryExpression(0));
//		return code;
		//unaryExpression ( bop unaryExpression )*
		Code code =  visit(ctx.unaryExpression(0));
		if (ctx.bop().size() != 0){
			String str;
			for (int i = 1 ; i <= ctx.bop().size();i++){
				code = aggregateResult(code, visit(ctx.unaryExpression(i)));
				str = ctx.bop().get(i-1).getText();
				currentClassScope.stringTable.add(ctx.bop().get(i-1).getText());
				int index = getLiteralIndex(str);
				//Before you join code for Send
				code = aggregateResult(code,Compiler.send(1,index));
			}
		}
		return code;
	}

	@Override
	public Code visitAssign(SmalltalkParser.AssignContext ctx) {
		Code msgexpr = visit(ctx.messageExpression());
		Code lvalue = visitLvalue(ctx.lvalue());
		Code code = aggregateResult(msgexpr,lvalue);
		//code = aggregateResult(code,Compiler.pop());
		return code;
	}

	@Override
	public Code visitLvalue(SmalltalkParser.LvalueContext ctx) {
		Code code = Code.None;
		STBlock stBlock = (STBlock) currentScope;
		if(ctx.sym instanceof STField){
			code = Compiler.store_field(ctx.sym.getInsertionOrderNumber());
		} else  if(ctx.sym instanceof STVariable){
			int i = stBlock.getInsertionOrderNumber();
			int d = stBlock.getRelativeScopeCount(ctx.getText());
			code = Compiler.store_local(d,i);
		} else {
			return Code.None;
		}
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

		//return 0;
		int index = currentClassScope.stringTable.add(s);
		return index;
	}

	@Override
	public Code visitPassThrough(SmalltalkParser.PassThroughContext ctx) {
		System.out.println("going here");
		return visit(ctx.binaryExpression());
	}

	public Code dbgAtEndMain(Token t) {
		int charPos = t.getCharPositionInLine() + t.getText().length();
		return dbg(t.getLine(), charPos);
	}

	@Override
	public Code visitPrimitiveMethodBlock(SmalltalkParser.PrimitiveMethodBlockContext ctx) {
		Code code = Code.None;
		return code;
	}

	@Override
	public Code visitOperatorMethod(SmalltalkParser.OperatorMethodContext ctx) {

		int blockIndex = 0;
		Code code = Code.None;
		currentScope = ctx.scope;
		STMethod stMethod = new STMethod(ctx.ID().getText(),ctx);
		STCompiledBlock block = new STCompiledBlock(currentClassScope, (STBlock) currentScope);
		block.blocks = new STCompiledBlock[stMethod.getAllNestedScopedSymbols().size()];
		code = aggregateResult(code, visitChildren(ctx));
		for(Scope symbol : stMethod.getAllNestedScopedSymbols()){
			STCompiledBlock stCompiledBlock = new STCompiledBlock(currentClassScope,(STBlock)symbol);
			stCompiledBlock.bytecode = ((STBlock)symbol).compiledBlock.bytecode;
			block.blocks[blockIndex] = stCompiledBlock;
			blockIndex++;
		}
		ctx.scope.compiledBlock = block;
		ctx.scope.compiledBlock.bytecode = code.bytes();
		popScope();
		return Code.None;
	}
//
//	@Override
//	public Code visitLocalVars(SmalltalkParser.LocalVarsContext ctx) {
////		System.out.println(ctx.ID().size());
//		Code code= Code.None;
//		for(int i = 0;i<ctx.ID().size();i++) {
//			System.out.println("yep "+ctx.ID(i).getText());
//			STBlock stBlock = (STBlock)currentScope;
//			int j = stBlock.getLocalIndex(ctx.ID(i).getText());
//			int d = stBlock.getRelativeScopeCount(ctx.ID(i).getText());
//			code = Compiler.push_local(d,j);
//
//		}
//		return code;
//	}

	@Override
	public Code visitBop(SmalltalkParser.BopContext ctx) {
		System.out.println("hmmmmmm");
		return Code.None;
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
		return null;
	}

	public Code push(String id) {
		return null;
	}

	public Code sendKeywordMsg(ParserRuleContext receiver,
							   Code receiverCode,
							   List<SmalltalkParser.BinaryExpressionContext> args,
							   List<TerminalNode> keywords)
	{
		StringBuilder sb = new StringBuilder();
		for(int i =0;i<keywords.size();i++){
			sb.append(keywords.get(i));
		}
		Code code = receiverCode;
		Code e = Compiler.send(args.size(),currentClassScope.stringTable.add(sb.toString()));
		code = aggregateResult(code,e);
		return code;
	}

	public String getProgramSourceForSubtree(ParserRuleContext ctx) {
		return null;
	}

	private int fieldIndex(Symbol sym){
		int index = ((STClass)sym.getScope()).getFieldIndex(sym.getName());
		ClassSymbol s = ((STClass)sym.getScope()).getSuperClassScope();
		while(s!=null){
			index = index + s.getNumberOfDefinedFields();
			s = s.getSuperClassScope();
		}
		return index;
	}


	@Override
	public Code visitUnarySuperMsgSend(SmalltalkParser.UnarySuperMsgSendContext ctx) {
		Code code = new Code();
		String str = ctx.ID().getText();
		int index = getLiteralIndex(str);
		code.join(Compiler.push_self()).join(Compiler.send_super(0, index));
		return code;
	}

	@Override
	public Code visitKeywordMethod(SmalltalkParser.KeywordMethodContext ctx) {
		currentScope = ctx.scope;
		pushScope(currentScope);
		Code code = visit(ctx.methodBlock());
		popScope();
		return code;
	}

	@Override
	public Code visitUnaryMsgSend(SmalltalkParser.UnaryMsgSendContext ctx) {
		Code code = new Code();
		String str = ctx.ID().getText();
		int index = getLiteralIndex(str);
		code = Compiler.push_field(0);
		code.join(Compiler.send(0, index));
		return code;
	}
}
