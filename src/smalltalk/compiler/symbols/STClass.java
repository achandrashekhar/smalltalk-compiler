package smalltalk.compiler.symbols;

import org.antlr.symtab.ClassSymbol;
import org.antlr.symtab.FieldSymbol;
import org.antlr.symtab.MethodSymbol;
import org.antlr.symtab.StringTable;
import org.antlr.symtab.Symbol;
import org.stringtemplate.v4.ST;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.ArrayList;
import java.util.List;

/** Represents a compile-time Smalltalk class in a Smalltalk program; it
 *  corresponds to STMetaClassObject in the VM.
 */
public class STClass extends ClassSymbol {
	/** The set of strings and symbols referenced by the {@link STCompiledBlock#bytecode} field
	 *  for all methods and blocks compiled for this class.  Each class has a
	 *  unique set of strings (which might have strings in common with another
	 *  class's string table).
	 */
	public final StringTable stringTable = new StringTable();

	public STClass(String name, String superClassName) {
		super(name);
		setSuperClass(superClassName);
	}

	public int getFieldIndex(String name) {
		Symbol sym = resolve(name);
		return sym!=null && sym.getScope() instanceof STClass ? sym.getInsertionOrderNumber() : -1;
	}

	public STMethod resolveMethod(String name) {
		return (STMethod)super.resolveMethod(name);
	}

	@Override
	public String toString() {
		return "class "+name;
	}

	/** Return a JSON object with all relevant info about a ST class that
	 *  we can write to the disk.  It includes all compiled blocks.
	 *  The VM loads such JSON to execute code.
	 */
	public JsonObject serialize() {
		JsonObjectBuilder builder =  Json.createObjectBuilder();
		builder.add("name", name);
		if ( superClassName!=null ) {
			builder.add("superClassName", superClassName);
		}
		JsonArrayBuilder litArray = Json.createArrayBuilder();
		if ( stringTable!=null ) {
			for (String literal : stringTable.toArray()) {
				litArray.add(literal);
			}
		}
		builder.add("literals", litArray);
		JsonArrayBuilder fieldArray = Json.createArrayBuilder();
		for (FieldSymbol f : getDefinedFields()) {
			fieldArray.add(f.getName());
		}
		builder.add("fields", fieldArray);
		JsonArrayBuilder methodArray = Json.createArrayBuilder();
		for(MethodSymbol m : getDefinedMethods()) {
			methodArray.add(((STMethod) m).compiledBlock.serialize());
		}
		builder.add("methods", methodArray);
		return builder.build();
	}

	public String toTestString() { return getAsString(); }

	public String getAsString() {
		ST template = new ST(
			"name: <name>\n" +
			"superClass: <superClassName>\n" +
			"fields: <fields; separator={,}>\n" +
			"literals: <literals:{s|'<s>'}; separator={,}>\n"+
			"methods:\n" +
			"    <methods; separator={<\\n>}>"
		);
		template.impl.nativeGroup.setListener(STCompiledBlock.templateErrorListener);
		template.add("name", name);
		if ( !superClassName.equals("Object") ) {
			template.add("superClassName", superClassName);
		}
		else {
			template.add("superClassName", null);
		}
		List<String> fields = new ArrayList<>();
		for (FieldSymbol f : getDefinedFields()) {
			fields.add(f.getName());
		}
		template.add("fields", fields);
		List<String> methods = new ArrayList<>();
		for(MethodSymbol m : getDefinedMethods()) {
			methods.add(((STMethod) m).compiledBlock.toTestString());
		}
		template.add("literals", stringTable.toArray());
		template.add("methods", methods);
		return template.render();
	}
}
