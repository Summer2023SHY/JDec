package com.github.automaton.io.legacy;

import java.io.*;
import java.util.*;

import org.apache.commons.io.RandomAccessFileMode;

/**
 * I/O handler for {@code .hdr} files.
 * 
 * @author Sung Ho Yoon
 * @since 1.1
 */
public final class HeaderAccessFile extends AutomatonAccessFile {

    /** The fixed amount of space needed to hold the main variables in the {@code .hdr} file, which apply to all automaton types. */
    public static final int HEADER_SIZE = 45; 

    /** Contains basic information about automaton, needed in order to read the bodyFile, as well as the events */
    private RandomAccessFile headerRAFile;

    /**
     * Constructs a new {@code HeaderAccessFile} for a given file
     * @param headerFile a header file
     * @throws NullPointerException if argument is {@code null}
     * @throws FileNotFoundException if the given file object cannot be opened
     */
    public HeaderAccessFile(File headerFile) throws FileNotFoundException {
        super(headerFile);
        headerRAFile = RandomAccessFileMode.READ_WRITE.create(headerFile);
    }

    /**
     * Clears the content of the header file
     * @return {@code true} the content is cleared; {@code false} otherwise.
     */
    @Override
    public boolean clearFile() {
        try {
            headerRAFile.close();
            getFile().delete();
            getFile().createNewFile();
            headerRAFile = RandomAccessFileMode.READ_WRITE.create(getFile());
            return true;
        } catch (IOException e) {
            getLogger().catching(e);
            return false;
        }
    }

    /**
     * Reads specified number of bytes from the header, and stores
     * them in an array.
     * 
     * @param length length of the returned array
     * @return a {@code byte} array containing the bytes read from the header
     * @throws IllegalArgumentException if argument is negative
     * @throws IOException if I/O error occurs
     */
    public byte[] readHeaderBytes(int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException(
                String.format("Invalid number of bytes: %d", length)
            );
        }
        byte[] buffer = new byte[length];
        if (headerRAFile.read(buffer) == -1) {
            throw new EOFException();
        }
        return buffer;
    }

    /**
     * Closes this header access file and releases any system resource
     * associated with this.
     * 
     * @throws IOException if I/O error occurs
     */
    @Override
    public void close() throws IOException {
        headerRAFile.close();
    }

    /**
     * Determines whether the underlying header file is empty
     * 
     * @return {@code true} if the underlying header file is empty
     * @throws IOException if I/O error occurs
     */
    public boolean isEmpty() throws IOException {
        return headerRAFile.length() == 0;
    }

    /**
     * Copies the content of the underlying header file to a new file
     * 
     * @param newFile the target destination
     * @throws IOException if I/O error occurs
     * @throws NullPointerException if argument is {@code null}
     */
    @Override
    public void copyTo(File newFile) throws IOException {
        headerRAFile.close();
        super.copyTo(newFile);
        headerRAFile = RandomAccessFileMode.READ_WRITE.create(getFile());
    }

    /**
     * Sets the file pointer offset
     * 
     * @param pos the new file pointer offset
     * @throws IOException if I/O error occurs
     * 
     * @see java.io.RandomAccessFile#seek(long)
     */
    public void seek(long pos) throws IOException {
        headerRAFile.seek(pos);
    }

    /**
     * Writes the specified bytes to the header file
     * @param data bytes to write to the header
     * @throws IOException if I/O error occurs
     * 
     * @see java.io.RandomAccessFile#write(byte[])
     */
    public void write(byte[] data) throws IOException {
        headerRAFile.write(Objects.requireNonNull(data));
    }

    /**
     * Trim the underlying {@code .hdr} file so that there is no garbage at the end 
     * @throws IOException if I/O error occurs
     */
    public void trim() throws IOException {
        headerRAFile.setLength(headerRAFile.getFilePointer());
    }

    /**
     * Returns the underlying file object
     * @return the underlying file object
     */
    public File getHeaderFile() {
        return getFile();
    }

    /**
     * Returns the string representation of this header access file
     * @return the string representation of this header access file
     */
    @Override
    public String toString() {
        return super.toString();
    }
}