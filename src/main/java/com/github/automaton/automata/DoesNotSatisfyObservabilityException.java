package com.github.automaton.automata;
/**
 * Thrown by the Nash algorithm when the system does not satisfy observability,
 * meaning that there are no feasible protocols that satisfy the
 * control problem.
 *
 * @author Micah Stairs
 * 
 * @see UStructure#findNashEquilibria(Crush.CombiningCosts)
 * @see UStructure#findNashEquilibria(Crush.CombiningCosts, java.util.List)
 */
public class DoesNotSatisfyObservabilityException extends IllegalStateException {
    /**
     * Constructs a {@code DoesNotSatisfyObservabilityException} with no
     * detail message.
     */
    public DoesNotSatisfyObservabilityException() {
        super();
    }

    /**
     * Constructs a {@code DoesNotSatisfyObservabilityException} with the
     * specified detail message.
     * @param message the detail message
     * @since 1.1
     */
    public DoesNotSatisfyObservabilityException(String message) {
        super(message);
    }

    /**
     * Constructs a {@code DoesNotSatisfyObservabilityException} with the
     * specified detail message and cause.
     * @param message the detail message
     * @param cause the cause
     * @since 1.1
     */
    public DoesNotSatisfyObservabilityException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a {@code DoesNotSatisfyObservabilityException} with the
     * specified cause.
     * @param cause the cause
     * @since 1.1
     */
    public DoesNotSatisfyObservabilityException(Throwable cause) {
        super(cause);
    }
}