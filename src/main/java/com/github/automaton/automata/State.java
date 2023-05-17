package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Class Constants
 *  -Instance Variables
 *  -Constructors
 *  -Working With Files
 *  -Mutator Methods
 *  -Accessor Methods
 *  -Overridden Methods
 */

import java.util.*;

/**
 * Represents a state in an automaton, complete with a label and transitions.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class State {

    /* CLASS CONSTANTS */

  // These masks allow us to store and access multiple true/false values within the same byte
  /** Bitmask for checking whether or not a state actually exists here */
  public static final int EXISTS_MASK = 0b00000010;
  /** Bitmask for checking whether or not a state is marked */
  public static final int MARKED_MASK = 0b00000001;
  /**
   * Bitmask for checking whether or not a state is an enablement state
   * 
   * @since 2.0
   */
  public static final int ENABLEMENT_MASK = 0b00000100;
  /**
   * Bitmask for checking whether or not a state is a disablement state
   * 
   * @since 2.0
   */
  public static final int DISABLEMENT_MASK = 0b00001000;
    
    /* INSTANCE VARIABLES */
  
  private String label;
  private long id;
  private boolean marked;
  private boolean enablement;
  private boolean disablement;
  private List<Transition> transitions;

    /* CONSTRUCTORS */

  /**
   * Default constructor. All instances created using this constructor
   * must update the instance variables using the respective mutator methods.
   * 
   * @since 2.0
   */
  State() {
    transitions = new ArrayList<Transition>();
  }

  /**
   * Construct a state (including transitions).
   * @param label       The state's label
   * @param id          The state ID
   * @param marked      Whether or not the state is marked
   * @param transitions The list of transitions leading out from this state
   * @param enablement  Whether or not the state is an enablement state
   * @param disablement Whether or not the state is an disablement state
   * @throws IllegalArgumentException if {@code enablement && disablement} is {@code true}
   * 
   * @since 2.0
   **/
  public State(String label, long id, boolean marked, List<Transition> transitions, boolean enablement, boolean disablement) {
    this(label, id, marked, transitions);
    if (enablement && disablement) {
      throw new IllegalArgumentException("A state cannot be an enablement and a disablement simultaneously");
    }
    this.enablement  = enablement;
    this.disablement = disablement;
  }

  /**
   * Construct a state (including transitions).
   * @param label       The state's label
   * @param id          The state ID
   * @param marked      Whether or not the state is marked
   * @param transitions The list of transitions leading out from this state
   **/
  public State(String label, long id, boolean marked, List<Transition> transitions) {
    this.label       = label;
    this.id          = id;
    this.marked      = marked;
    this.transitions = transitions;
  }

  /**
   * Construct a state (with 0 transitions).
   * @param label       The state's label
   * @param id          The state ID
   * @param marked      Whether or not the state is marked
   * @param enablement  Whether or not the state is an enablement state
   * @param disablement Whether or not the state is an disablement state
   * @throws IllegalArgumentException if {@code enablement && disablement} is {@code true}
   * 
   * @since 2.0
   **/
  public State(String label, long id, boolean marked, boolean enablement, boolean disablement) {
    this(label, id, marked, new ArrayList<Transition>(), enablement, disablement);
  }

  /**
   * Construct a state (with 0 transitions).
   * @param label       The state's label
   * @param id          The state ID
   * @param marked      Whether or not the state is marked
   **/
  public State(String label, long id, boolean marked) {
    this.label  = label;
    this.id     = id;
    this.marked = marked;
    transitions = new ArrayList<Transition>();
  }

    /* MUTATOR METHODS */

  /**
   * Changes the label of this state.
   * @param label new label for this state
   * @throws NullPointerException if argument is {@code null}
   * @since 2.0
   */
  void setLabel(String label) {
    this.label = Objects.requireNonNull(label);
  }

  /**
   * Change the ID of this state.
   * @param id  The new ID
   **/
  public void setID(long id) {
    this.id = id;
  }

  /**
   * Change the marked status of this state.
   * @param marked  Whether or not this state should be marked
   **/
  public void setMarked(boolean marked) {
    this.marked = marked;
  }

  /**
   * Change the enablement status of this state.
   * @param enablement  Whether or not this state is an enablement state
   * 
   * @since 2.0
   */
  void setEnablement(boolean enablement) {
    this.enablement = enablement;
  }

  /**
   * Change the disablement status of this state.
   * @param disablement  Whether or not this state is an disablement state
   * 
   * @since 2.0
   */
  void setDisablement(boolean disablement) {
    this.disablement = disablement;
  }

  /**
   * Add a transition to the list.
   * @param transition  The new transition
   **/
  public void addTransition(Transition transition) {
    transitions.add(transition);
  }

  /**
   * Remove a transition from the list.
   * @param transition  The transition to be removed
   * @return            Whether or not the removal was successful
   **/
  public boolean removeTransition(Transition transition) {
    return transitions.remove(transition);
  }

    /* ACCESSOR METHODS */

  /**
   * Get the marked status of this state.
   * @return Whether or not the state is marked
   **/
  public boolean isMarked() {
    return marked;
  }

  /**
   * Checks whether this state is an enablement state
   * @return Whether or not this state is an enablement state
   *
   * @since 2.0
   */
  public boolean isEnablementState() {
    return enablement;
  }

  /**
   * Checks whether this state is a disablement state
   * @return Whether or not this state is a disablement state
   * 
   * @since 2.0
   */
  public boolean isDisablementState() {
    return disablement;
  }

  /**
   * Get the state's label.
   * @return  The state's label
   **/
  public String getLabel() {
    return label;
  }

  /**
   * Get the ID of this state.
   * @return  The state's ID
   **/
  public long getID() {
    return id;
  }

  /**
   * Get the list of transitions leading out from this state.
   * @return  The list of transitions
   **/
  public List<Transition> getTransitions() {
    return transitions;
  }

  /**
   * Get the number of transitions leading out from this state.
   * @return  The number of transitions
   **/
  public int getNumberOfTransitions() {
    return transitions.size();
  }

    /* OVERRIDDEN METHODS */

  /**
   * Returns string representation of this state
   * @return string representation of this state
   */
  @Override
  public String toString() {
    return "("
      + "\"" + label + "\",ID:"
      + id + ","
      + (marked ? "Marked" : "Unmarked") + ","
      + "# Transitions: " + transitions.size()
      + ")";
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  /**
   * Indicates whether an object is "equal to" this state
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this state is the same as the argument
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    else if (other instanceof State) {
      State s = (State) other;
      return Objects.equals(this.label, s.label) && this.id == s.id;
    }
    else return false;
  }

}