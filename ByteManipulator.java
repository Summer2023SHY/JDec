public abstract class ByteManipulator {
	
	// Joins the specified number of bytes into a long from an array of bytes
	public static long readBytesAsLong(byte[] arr, int index, int nBytes) {

		long n = 0;

		for (int i = nBytes - 1; i >= 0; i--) {
			n <<= 8;
			n += (arr[index++] & 0xFF); // Makes the byte unsigned, before adding it
		}

		return n;

	}

	// Splits the specified number (which is a long) into the proper number of bytes and writes them one at a time into the array
	public static void writeLongAsBytes(byte[] arr, int index, long n, int nBytes) {

		for (int i = nBytes - 1; i >= 0; i--)
			arr[index++] = (byte) (n >> (i*8));

	}

}