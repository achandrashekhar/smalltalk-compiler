package smalltalk.compiler.symbols;

import org.antlr.symtab.Utils;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.ErrorBuffer;
import org.stringtemplate.v4.misc.STMessage;
import smalltalk.compiler.Bytecode;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

/** This object represents the compiled code for a block or method and is
 *  more or less equivalent to the class with same name in VM.
 *  Fields enclosingClass is STClass in compiler and STMetaClassObject in VM.
 *  Field primitiveName in STClass corresponds to field primitive in VM.
 *
 *  This holds all of the bytecode and meta information about the block, such
 *  as the number of arguments and the number of local variables.
 *
 *  If this object is a placeholder for a primitive method, field primitive
 *  will be non-null.
 *
 *  If this object represents a compiled method, field blocks will be
 *  an array of pointers to the compiled code for all nested blocks of the
 *  method.
 *
 *  All blocks can reference literals method names and string literals ref'd within.
 *  These are stored in {@link STClass#stringTable} during compilation.
 *  During VM execution, they are stored in STMetaClassObject's literals field.
 */
public class STCompiledBlock {
	// Used to trap stringtemplate errors (e.g., can set breakpoint in these methods).
	public static final ErrorBuffer templateErrorListener = new ErrorBuffer() {
		@Override
		public void compileTimeError(STMessage stMessage) {
			super.compileTimeError(stMessage);
		}

		@Override
		public void runTimeError(STMessage stMessage) {
			super.runTimeError(stMessage);
		}
	};

	public static final String testStringTemplate =
		"name: <if(isClassMethod)>static <endif><name>\n" +
		"qualifiedName: <qualifiedName>\n" +
		"nargs: <nargs>\n" +
		"nlocals: <nlocals>\n"+
		"<assembly>"+
		"<if(blocks)>" +
		"blocks:\n"+
		"    <blocks; separator={<\\n>}>" +
		"<endif>";

	/** This method or block is part of which class? */
	public final STClass enclosingClass;

	/** The simple name for a block or method like at:put: or foo:-local0 */
	public final String name;

	/** The fully qualified name for this block or method like foo>>x or T>>x */
	public final String qualifiedName;

	/** The byte code instructions for this specific block, if not primitive. */
	public byte[] bytecode;

	/** If this is a compiled method, not just a block, this is the list
	 *  of all nested blocks within the method. The BLOCK instruction refers to
	 *  them by unique integer and finds them by indexing into this array.
	 *  The outermost method block is blocks[0].
	 *
	 *  This is unused for [...] blocks (i.e., not methods).
 	 */
	public STCompiledBlock[] blocks;

	/** The fixed number of arguments taken by this method */
	public final int nargs;

	/** The number of local variables defined within the block, not including the arguments */
	public final int nlocals;

	/** In the compiler, this is the primitive name. In the VM, the equivalent
	 *  class has a 'primitive' field that points at an actual Primitive object.
 	 */
	public final String primitiveName;

	/** True if method was defined as a class method in Smalltalk code */
	public final boolean isClassMethod;

	public STCompiledBlock(STClass enclosingClass, STBlock blk) {
		this.enclosingClass = enclosingClass;
		this.name = blk.getName();
		this.qualifiedName = blk.getQualifiedName(">>");
		nargs = blk.nargs();
		nlocals = blk.nlocals();
		if ( blk instanceof STPrimitiveMethod ) {
			primitiveName = ((STPrimitiveMethod) blk).primitiveName;
		}
		else {
			primitiveName = null;
		}
		isClassMethod = blk instanceof STMethod && ((STMethod) blk).isClassMethod;
	}

	public String toTestString() { return getAsString(); }

	/** Return a JSON object with all relevant info about a ST block/method,
	 *  which is wrapped in the JSON for an ST class via {@link STClass#serialize()}.
	 *  The VM loads such JSON to execute code.
	 */
	public JsonObject serialize() {
		JsonObjectBuilder builder = Json.createObjectBuilder();
		builder.add("name", name);
		builder.add("isClassMethod", isClassMethod);
		builder.add("qualifiedName", qualifiedName);
		if ( primitiveName!=null ) {
			builder.add("primitiveName", primitiveName);
		}
		builder.add("nargs", nargs);
		builder.add("nlocals", nlocals);
		JsonArrayBuilder codeArray = Json.createArrayBuilder();
		if ( bytecode!=null ) {
			for (byte b : bytecode) {
				codeArray.add(b);
			}
		}
		builder.add("bytecode", codeArray);
		JsonArrayBuilder blockArray = Json.createArrayBuilder();
		if ( blocks!=null ) {
			for (STCompiledBlock block : blocks) {
				blockArray.add(block.serialize());
			}
		}
		builder.add("blocks", blockArray);
		return builder.build();
	}

	public String getAsString() {
		ST template = new ST(testStringTemplate);
		template.impl.nativeGroup.setListener(templateErrorListener);
		template.add("name", name);
		template.add("isClassMethod", isClassMethod);
		template.add("qualifiedName", qualifiedName);
		template.add("nargs", nargs);
		template.add("nlocals", nlocals);
		template.add("bytecode", bytecode);
		template.add("assembly", Bytecode.disassemble(this.name, this.bytecode, enclosingClass.stringTable.toArray(), 0));
		template.add("nblocks", blocks!=null ? blocks.length : 0);
        template.add("blocks", Utils.map(blocks, STCompiledBlock::toTestString));
		return template.render();
	}

	@Override
	public String toString() {
		return name;
	}
}
