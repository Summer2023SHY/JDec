package automata;
/**
 * IncompatibleAutomataException - This exception is thrown when two automata are being combined in some way
 *								                 and they are not compatible with one another. For example, this can happen
 *                                 if they have a different number of controllers or if they both share an
 *                                 event with the same name but with different properties.
 *
 * @author Micah Stairs
 **/

public class IncompatibleAutomataException extends IllegalArgumentException { }