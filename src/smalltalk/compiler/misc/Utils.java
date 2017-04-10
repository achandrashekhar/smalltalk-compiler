package smalltalk.compiler.misc;

import smalltalk.compiler.Code;

public class Utils {
	public static void dumpBytes(byte[] data) {
		for (int i=0; data!=null && i<data.length; i++) {
			if ( i%8==0 && i!=0 ) System.out.println();
			if ( i%8==0 ) System.out.printf("%04d:", i);
			System.out.printf(" %3d", ((int)data[i]));
		}
		System.out.println();
	}

	public static Code toLiteral(int v) {
		return shortToBytes(v);
	}

	public static Code shortToBytes(int v) {
		return Code.of(
		(byte) ((v >> (8*1))&0xFF),
		(byte) ((v >> (8*0))&0xFF)
					  );
	}

	public static Code intToBytes(int v)	{
		return Code.of(
			(byte) ((v >> (8 * 3)) & 0xFF),
			(byte) ((v >> (8 * 2)) & 0xFF),
			(byte) ((v >> (8 * 1)) & 0xFF),
			(byte) ((v >> (8 * 0)) & 0xFF)
		);
	}

	public static Code floatToBytes(float v) {
		int bits = Float.floatToIntBits(v);
		return intToBytes(bits);
	}
}
