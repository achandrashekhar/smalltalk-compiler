package smalltalk.compiler.misc;

public class ByteList {
	private static final int DEFAULT_CAPACITY = 10;
	public int n = 0;
	public byte[] elements = null;

	public ByteList() {
		this(DEFAULT_CAPACITY);
	}

	public ByteList(int initialCapacity) {
		elements = new byte[initialCapacity];
	}

	public static ByteList of(short... args) {
		ByteList bytes = new ByteList();
		for (short b : args) bytes.add(b);
		return bytes;
	}

	/** Set the ith element.  Like ArrayList, this does NOT affect size. */
	public void set(int i, short b) {
		if ( i>=n ) {
			setSize(i); // unlike definition of set in ArrayList, set size
		}
		elements[i] = (byte)b;
	}

	public void add(short b) {
		if ( n>=elements.length ) {
			grow();
		}
		elements[n] = (byte)b;
		n++;
	}

	public void setSize(int newSize) {
		if ( newSize>=elements.length ) {
            ensureCapacity(newSize);
		}
		n = newSize;
	}

	protected void grow() {
		ensureCapacity((elements.length * 3)/2 + 1);
	}

	public void ensureCapacity(int newCapacity) {
		int oldCapacity = elements.length;
		if (n>=oldCapacity) {
			byte[] oldData = elements;
			elements = new byte[newCapacity];
			System.arraycopy(oldData, 0, elements, 0, n);
		}
	}

	public byte get(int i) {
		return elements[i];
	}

	public byte lastByte() {
		if ( n>0 ) return elements[n-1];
		return 0;
	}

	public byte[] bytes() {
		byte[] b = new byte[n];
		System.arraycopy(elements, 0, b, 0, n);
		return b;
	}

	public int size() { return n; }
}
