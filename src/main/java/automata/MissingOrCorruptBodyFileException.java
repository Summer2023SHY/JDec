package automata;
/**
 * Thrown when the {@code .bdy} file is not able to be interpreted as indicated
 * by its header file. So when anything unexpected happens while reading the
 * {@code .bdy} file, this exception should be thrown.
 *
 * @author Micah Stairs
 **/
public class MissingOrCorruptBodyFileException extends java.io.IOException { }