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
public class DoesNotSatisfyObservabilityException extends IllegalStateException { }