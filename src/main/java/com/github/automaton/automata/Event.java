package com.github.automaton.automata;

/*
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Mutator Method
 *  -Accessor Methods
 *  -Overridden Methods
 */

import java.util.*;

/**
 * Represents an event in an automaton. It supports both centralized and
 * decentralized control, which means that it can have observability and
 * controllability properties for each controller. It also has support for
 * events that have labels formatted as vectors.
 *
 * @author Micah Stairs
 */
public class Event {
    
    /* INSTANCE VARIABLES */

  private String label;
  private int id;
  private boolean[] observable, controllable;

  /**
   * Events can sometimes be vectors (for example, automata created by synchonrized composition use them).
   * Example of syntax: "&lt;a,b,d>" actually represents an event vector: {"a", "b", "d"}. This instance
   * variable holds a reference to the vectorized event label.
   **/
  private LabelVector vector = null;

    /* CONSTRUCTOR */

  /**
   * Construct a new event with the specified properties.
   * @param label         The name of the event
   * @param id            The ID of the event
   * @param observable    Whether or not the event can be observed
   * @param controllable  Whether or not the event can be controlled
   **/
  public Event(String label, int id, boolean[] observable, boolean[] controllable) {

    this.label = label;
    this.id = id;
    this.observable = observable;
    this.controllable = controllable;
    this.vector = new LabelVector(label);

  }

    /* MUTATOR METHOD */

  /**
   * Set the event's ID number.
   * @param id  The new ID for the event
   **/
  public void setID(int id) {
    this.id = id;
  }

    /* ACCESSOR METHODS */

  /**
   * Get the label of the event.
   * @return  The event's label
   **/
  public String getLabel() {
    return label;
  }

  /**
   * Get the ID number of the event.
   * @return  The ID
   **/
  public int getID() {
    return id;
  }

  /**
   * Get the observability property of the event for each controller.
   * @return  Whether or not the event is observable
   **/
  public boolean[] isObservable() {
    return observable;
  }

  /**
   * Get the controllability property of the event for each controller.
   * @return  Whether or not the event is controllable
   **/
  public boolean[] isControllable() {
    return controllable;
  }

  /**
   * Get the event vector.
   * @return  The event vector
   **/
  public LabelVector getVector() {
    return vector;
  }

    /* OVERRIDDEN METHODS */

  /**
   * Indicates whether an object is "equal to" this event
   * 
   * @param obj the reference object with which to compare
   * @return {@code true} if this event is the same as the argument
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    else if (obj instanceof Event) {
      Event other = (Event) obj;
      return this.label.equals(other.label);
    }
    else return false;
  }

  @Override
  public int hashCode() {
    return this.label.hashCode();
  }

  /**
   * Returns string representation of this event
   * @return string representation of this event
   */
  @Override
  public String toString() {
    return "("
      + "\"" + label + "\",ID:"
      + id + ","
      + "Observable=" + Arrays.toString(observable) + ","
      + "Controllable=" + Arrays.toString(controllable)
      + ")";
  }

}