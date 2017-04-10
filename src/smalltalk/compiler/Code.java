package smalltalk.compiler;

import smalltalk.compiler.misc.ByteList;

public class Code extends ByteList { // just an alias
	public static final Code None = new Code();

	public static Code of(short... args) {
		Code bytes = new Code();
		for (short b : args) bytes.add(b);
		return bytes;
	}

	public static Code join(Code... chunks) {
		Code bytes = new Code();
		for (Code c : chunks) {
			bytes.join(c);
		}
		return bytes;
	}

	// Support code.join(morecode).join(evenmorecode) chains
	public Code join(Code bytes) {
		if ( this == None ) {
			return bytes;
		}
		if ( bytes == None ) {
			return this;
		}
		for (int i=0; i<bytes.n; i++) {
			add(bytes.elements[i]);
		}
		return this;
	}
}
