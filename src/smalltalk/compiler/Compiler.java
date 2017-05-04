package smalltalk.compiler;

import org.antlr.symtab.Scope;
import org.antlr.symtab.VariableSymbol;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import smalltalk.compiler.symbols.STArg;
import smalltalk.compiler.symbols.STBlock;
import smalltalk.compiler.symbols.STClass;
import smalltalk.compiler.symbols.STField;
import smalltalk.compiler.symbols.STMethod;
import smalltalk.compiler.symbols.STPrimitiveMethod;
import smalltalk.compiler.symbols.STSymbolTable;
import smalltalk.compiler.symbols.STVariable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import static smalltalk.compiler.misc.Utils.*;

public class Compiler {
	protected STSymbolTable symtab;
	protected SmalltalkParser parser;
	protected CommonTokenStream tokens;
	protected SmalltalkParser.FileContext fileTree;
	protected String fileName;
	public boolean genDbg; // generate dbg file,line instructions

	public final List<String> errors = new ArrayList<>();

	public Compiler() {
		symtab = new STSymbolTable();
	}

	public Compiler(STSymbolTable symtab) {
		this.symtab = symtab;
	}

	public STSymbolTable compile(String fileName, String input) {
		ParserRuleContext tree = parseClasses(new ANTLRInputStream(input));
		if(tree!=null){
			defSymbols(tree);
			resolveSymbols(tree);
		}
		CodeGenerator codeGenerator = new CodeGenerator(this);
		codeGenerator.visit(tree);
		return symtab;
	}

	/** Parse classes and/or a chunk of code, returning AST root.
	 *  Return null upon syntax error.
	 */
	public ParserRuleContext parseClasses(CharStream input) {
		SmalltalkLexer l = new SmalltalkLexer(input);
		CommonTokenStream tokens = new CommonTokenStream(l);
		//System.out.println(tokens.getTokens());

		this.parser = new SmalltalkParser(tokens);
		fileTree= parser.file();

		//System.out.println(((Tree)r.getTree()).toStringTree());
		if ( parser.getNumberOfSyntaxErrors()>0 ) return null;
		return fileTree;
	}

	public void defSymbols(ParserRuleContext tree) {
		// Define classes/fields in first pass over tree
		// This allows us to have forward class references
		DefineSymbols def = new DefineSymbols(this);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(def, tree);
	}

	public void resolveSymbols(ParserRuleContext tree) {
		ResolveSymbols def = new ResolveSymbols(this);
		ParseTreeWalker walker = new ParseTreeWalker();
		walker.walk(def, tree);
	}

	public STBlock createBlock(STMethod currentMethod, ParserRuleContext tree) {
//		System.out.println("create block in "+currentMethod+" "+args);
		STBlock stBlock = new STBlock(currentMethod,tree);
		return stBlock;
	}

	public STMethod createMethod(String selector, ParserRuleContext tree) {
//		System.out.println("	create method "+selector+" "+args);
		STMethod stMethod = new STMethod(selector,tree);
		return stMethod;
	}

	public STPrimitiveMethod createPrimitiveMethod(STClass currentClass,
	                                               String selector,
	                                               String primitiveName,
	                                               SmalltalkParser.MethodContext tree)
	{
//		System.out.println("	create primitive "+selector+" "+args+"->"+primitiveName);
		// convert "<classname>_<methodname>" Primitive value
		// warn if classname!=currentClass
		STPrimitiveMethod stPrimitiveMethod = new STPrimitiveMethod(selector,tree,primitiveName);
		return stPrimitiveMethod;
	}


	public void defineVariables(Scope scope, List<String> names, Function<String,? extends VariableSymbol> getter) {
		if ( names!=null ) {
			for (String name : names) {
				VariableSymbol v = getter.apply(name);
				if ( scope.getSymbol(v.getName())!=null ) {
					error("redefinition of "+v.getName()+" in "+scope.toQualifierString(">>"));
				}
				else {
					scope.define(v);
				}
			}
		}
	}

	public void defineFields(Scope scope, List<String> names) {
		defineVariables(scope, names, n -> new STField(n));
	}

	public void defineArguments(Scope scope, List<String> names) {
		defineVariables(scope, names, n -> new STArg(n));
	}

	public void defineLocals(Scope scope, List<String> names) {
		defineVariables(scope, names, n -> new STVariable(n));
	}

	// Convenience methods for code gen

	public static Code push_nil() 				{ return Code.of(Bytecode.NIL); }
	public static Code push_self()				{ return Code.of(Bytecode.SELF); }
	public static Code pop(){return Code.of(Bytecode.POP);}
	public static Code method_return()          { return Code.of(Bytecode.RETURN); }
	public static Code push_int(int i){return Code.of(Bytecode.PUSH_INT).join(intToBytes(i));}
	public static Code push_float(float f){return Code.of(Bytecode.PUSH_FLOAT).join(floatToBytes(f));}
	public static Code push_local(int context,int i){
		return Code.of(Bytecode.PUSH_LOCAL).join(shortToBytes(context)).join(shortToBytes(i));
	}
	public static Code push_field(int i){
		return Code.of(Bytecode.PUSH_FIELD).join(toLiteral(i));
	}

	public static Code push_true(){
		return Code.of(Bytecode.TRUE);
	}

	public static Code push_false(){
		return Code.of(Bytecode.FALSE);
	}

	public static Code push_global(int globalIndex){
		return Code.of(Bytecode.PUSH_GLOBAL).join(toLiteral(globalIndex));
	}

	public static Code push_literal(int literalIndex){
		return Code.of(Bytecode.PUSH_LITERAL).join(toLiteral(literalIndex));
	}
	public static Code push_char(int c)			    { return Code.of(Bytecode.PUSH_CHAR).join(shortToBytes(c)); }

	public static Code block(int b) 			    { return Code.of(Bytecode.BLOCK).join(shortToBytes(b)); }
	public static Code block_return() {
		return Code.of(Bytecode.BLOCK_RETURN);
	}

	public static Code store_field(int index){
		return Code.of(Bytecode.STORE_FIELD).join(shortToBytes(index));
	}


	public static Code dbg(int filenameLitIndex, int line, int charPos) {
		return null;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	// Error support

	public void error(String msg) {
		errors.add(msg);
	}

	public void error(String msg, Exception e) {
		errors.add(msg+"\n"+ Arrays.toString(e.getStackTrace()));
	}

	public static Code send(int args, int keywordIndex){
		return Code.of(Bytecode.SEND).join(shortToBytes(args)).join(toLiteral(keywordIndex));
	}


	public static Code store_local(int d, int i) {
		return Code.of(Bytecode.STORE_LOCAL).join(shortToBytes(d)).join(shortToBytes(i));
	}
	public static Code send_super(int size, int i)  { return Code.of(Bytecode.SEND_SUPER).join(toLiteral(size).join(toLiteral(i))); }
}
