/**
 * ByteManipulator - An abstract class used to perform conversions between a long and bytes.
 *
 * @author Micah Stairs
 **/

public abstract class ByteManipulator {
  
  /**
   * Joins the specified number of bytes into a long from an array of bytes.
   * @param arr    The array of bytes to read from
   * @param index  The index in the array to start reading bytes from
   * @param nBytes The number of bytes to be read
   * @return       The long value
   **/
  public static long readBytesAsLong(byte[] arr, int index, int nBytes) {

    long n = 0;

    // Read bytes one at a time from the array, building the long value
    for (int i = nBytes - 1; i >= 0; i--) {
      n <<= 8;
      n += (arr[index++] & 0xFF); // Makes the byte unsigned, before adding it
    }

    return n;

  }

  /**
   * Splits the specified number (which is a long) into the proper number of bytes and writes them
   * one at a time into the array.
   * @param arr     The array of bytes to write to
   * @param index   The index in the array to start writing bytes to
   * @param n       The long value to be broken up into bytes
   * @param nBytes  The number of bytes to be written (which should not be larger than the size of the array)
   **/
  public static void writeLongAsBytes(byte[] arr, int index, long n, int nBytes) {

    for (int i = nBytes - 1; i >= 0; i--)
      arr[index++] = (byte) (n >> (i*8));

    // Error-checking
    if (JDec.DEBUG_MODE && readBytesAsLong(arr, index - nBytes, nBytes) != n)
      System.err.println("CRUCIAL ERROR: Byte manipulator wrote a value which will not be read properly.");

  }

}