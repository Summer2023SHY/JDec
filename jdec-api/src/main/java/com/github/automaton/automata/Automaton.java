/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.automata;

/* TABLE OF CONTENTS:
 *  -Class Constants
 *  -Class Variables
 *  -Instance Variables
 *  -Automaton Type Enum
 *  -Constructors
 *  -Automata Operations
 *  -Automata Operations Helper Methods
 *  -Image Generation
 *  -GUI Input Code Generation
 *  -Working with Files
 *  -Miscellaneous
 *  -Overridden Method
 *  -Mutator Methods
 *  -Accessor Methods
 */

import java.io.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Collectors;

import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.io.RandomAccessFileMode;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.*;

import com.github.automaton.automata.util.IDUtil;
import com.github.automaton.io.StateNotFoundException;
import com.github.automaton.io.graphviz.AutomatonDotConverter;
import com.github.automaton.io.input.AutomatonGuiInputGenerator;
import com.github.automaton.io.json.*;
import com.google.gson.*;
import com.google.gson.reflect.*;

import guru.nidi.graphviz.engine.Format;

/**
 * Class that is able to fully represent an automaton.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 * 
 * @revised 2.0
 **/
public class Automaton implements Cloneable {

    /* PUBLIC CLASS CONSTANTS */

    /** The default number of controllers in an automaton. */
    public static final int DEFAULT_NUMBER_OF_CONTROLLERS = 1;

    /** The maximum number of controllers in an automaton. */
    public static final int MAX_NUMBER_OF_CONTROLLERS = 10;

    /**
     * The label used to indicate a dump state.
     **/
    public static final String DUMP_STATE_LABEL = "Dump State";

    private static Logger logger = LogManager.getLogger();

    /* INSTANCE VARIABLES */

    // Events
    /**
     * List of events
     */
    protected List<Event> events = new ArrayList<Event>();
    /**
     * Mapping of labels that trigger events to their respective {@link Event}s.
     */
    protected transient Map<String, Event> eventsMap = new HashMap<String, Event>();

    // States
    /**
     * Mapping of IDs of states in this automaton to their respective
     * {@link State}s.
     * 
     * @since 2.0
     */
    protected Map<Long, State> states = new LinkedHashMap<>();

    // Special transitions
    private List<TransitionData> badTransitions;

    // Basic properties of the automaton
    /** The type of this automaton */
    protected Type type;
    /** Number of states in this automaton */
    protected long nStates = 0;
    /** Initial state of this automaton */
    protected long initialState = 0;
    /** Number of controllers */
    protected int nControllers;

    /**
     * GUI input generator.
     * 
     * @since 2.1.0
     */
    private transient AutomatonGuiInputGenerator<?> generator;

    /**
     * GraphViz DOT converter.
     * 
     * @since 2.1.0
     */
    private transient AutomatonDotConverter<?> dotConverter;

    /**
     * Internally used {@link Gson} object.
     * 
     * @since 2.0
     */
    protected transient Gson gson = new Gson();

    /* AUTOMATON TYPE ENUM */

    /**
     * Enum constant that represents the type of the {@link Automaton}
     * 
     * @author Micah Stairs
     * 
     * @since 1.0
     */
    public static enum Type {

        /** The basic automaton */
        AUTOMATON((byte) 0, Automaton.class),

        /** The U-Structure */
        U_STRUCTURE((byte) 1, UStructure.class),

        /** The pruned U-Structure */
        PRUNED_U_STRUCTURE((byte) 2, PrunedUStructure.class);

        // Private variables
        private final byte numericValue;
        private transient final Class<? extends Automaton> classType;

        /**
         * Construct a Type enum object.
         * 
         * @param numericValue The numeric value associated with this enum value (used
         *                     in data files)
         * @param classType    The associated class
         **/
        Type(byte numericValue, Class<? extends Automaton> classType) {
            this.numericValue = numericValue;
            this.classType = classType;
        }

        /**
         * Get the numeric value associated with this enumeration value.
         * 
         * @return The numeric value
         **/
        public byte getNumericValue() {
            return numericValue;
        }

        /**
         * Given a numeric value, get the associated automaton type.
         * 
         * @param value The numeric value
         * @return The automaton type (or {@code null}, if it could not be found)
         **/
        public static Type getType(byte value) {

            for (Type type : Type.values())
                if (type.numericValue == value)
                    return type;

            return null;

        }

        /**
         * Given a class, get the associated enumeration value.
         * 
         * @param classType The class
         * @return The automaton type (or {@code null}, if it could not be found)
         **/
        public static Type getType(Class<?> classType) {

            for (Type type : Type.values())
                if (type.classType == classType)
                    return type;

            return null;

        }

        /**
         * Given a header file of an automaton, get the associated enumeration value.
         * 
         * @param file The header file of the automaton
         * @return The automaton type (or {@code null}, if it could not be found)
         **/
        public static Type getType(File file) {

            try (RandomAccessFile raf = RandomAccessFileMode.READ_ONLY.create(file)) {

                return Automaton.Type.getType(raf.readByte());

            } catch (IOException e) {

                logger.catching(e);
                return null;

            }

        }

        /**
         * Returns the string representation of the type of Automaton this enum
         * constant represents.
         * 
         * @return the string representation of type
         */
        @Override
        public String toString() {
            switch (this) {

                case AUTOMATON:
                    return "Automaton";

                case U_STRUCTURE:
                    return "U-Structure";

                case PRUNED_U_STRUCTURE:
                    return "Pruned U-Structure";

            }

            return null;

        }

    }

    /* CONSTRUCTORS */

    /**
     * Constructs a new {@code Automaton} with the
     * {@link #DEFAULT_NUMBER_OF_CONTROLLERS default number of controllers}.
     * 
     * @revised 2.0
     */
    public Automaton() {
        this(DEFAULT_NUMBER_OF_CONTROLLERS);
    }

    /**
     * Constructs a new {@code Automaton} with the specified number of controllers.
     * 
     * @param nControllers the number of controllers that the new automaton has
     *                     (1 implies centralized control, >1 implies decentralized
     *                     control)
     * @throws IllegalArgumentException if argument is not positive
     * 
     * @since 2.0
     **/
    public Automaton(int nControllers) {
        if (nControllers <= 0) {
            throw new IllegalArgumentException("Invalid number of controllers: " + nControllers);
        }
        this.nControllers = nControllers;
        initializeLists();
        initializeVariables();
        type = Type.getType(this.getClass());
    }

    /**
     * Constructs a new {@code Automaton} that is represented by a JSON object
     * 
     * @param jsonObject a JSON object that represents an automaton
     * 
     * @see #buildAutomaton(JsonObject)
     * @since 2.0
     **/
    Automaton(JsonObject jsonObject) {

        Objects.requireNonNull(jsonObject);

        initializeLists();
        ;

        type = Type.getType(jsonObject.getAsJsonPrimitive("type").getAsByte());
        nStates = jsonObject.getAsJsonPrimitive("nStates").getAsLong();
        initialState = jsonObject.getAsJsonPrimitive("initialState").getAsLong();
        nControllers = jsonObject.getAsJsonPrimitive("nControllers").getAsInt();

        events = JsonUtils.readListPropertyFromJsonObject(jsonObject, "events", Event.class);
        for (Event e : events) {
            eventsMap.put(e.getLabel(), e);
        }
        states = new LinkedHashMap<>();
        for (State s : gson.fromJson(jsonObject.get("states"), new TypeToken<HashSet<State>>() {
        })) {
            states.put(s.getID(), s);
        }

        readSpecialTransitionsFromJsonObject(jsonObject);

        initializeVariables();

    }

    /**
     * Builds an automaton from a JSON object.
     * 
     * @param jsonObj a JSON object that represents an automaton
     * @return a new automaton represented by the argument
     * 
     * @throws IllegalAutomatonJsonException if the value for {@code "type"} does
     *                                       not exist or cannot be represented as a
     *                                       {@code byte}
     * @throws AutomatonException            if the value for {@code "type"} is
     *                                       invalid
     * 
     * @since 2.0
     */
    public static Automaton buildAutomaton(JsonObject jsonObj) {
        Automaton.Type type;
        try {
            type = Automaton.Type.getType(jsonObj.getAsJsonPrimitive("type").getAsByte());
        } catch (ClassCastException | NumberFormatException e) {
            throw new IllegalAutomatonJsonException(
                    "Invalid value for 'type': " + Objects.toString(jsonObj.get("type")), e);
        }
        switch (type) {
            case AUTOMATON:
                return new Automaton(jsonObj);
            case U_STRUCTURE:
                return new UStructure(jsonObj);
            case PRUNED_U_STRUCTURE:
                return new PrunedUStructure(jsonObj);
            default:
                throw new AutomatonException("Invalid automaton type: " + Objects.toString(type));
        }
    }

    /**
     * Used to initialize all lists in order to prevent the possibility of
     * NullPointerExceptions.
     * 
     * @apiNote This method must be called at the beginning of the constructor of
     *          Automaton. This method is intended to
     *          be overridden by sub-classes, however, any sub-classes of Automaton
     *          do not need to explicitly call it.
     **/
    protected void initializeLists() {

        badTransitions = new ArrayList<TransitionData>();

    }

    /** AUTOMATA OPERATIONS **/

    /**
     * Create a new copy of this automaton that has all unreachable states and
     * transitions removed.
     * 
     * @return The accessible automaton
     * 
     * @since 2.0
     * 
     * @see AutomataOperations#accessible(Automaton, IntFunction)
     **/
    public Automaton accessible() {
        return AutomataOperations.accessible(this, Automaton::new);
    }

    /**
     * A helper method used to generate the accessible portion of this automaton.
     * 
     * @param <T>       The type of automaton
     * @param automaton The generic automaton object
     * @return The same automaton that was passed into the method, now containing
     *         the accessible part of this automaton
     * 
     * @throws UnsupportedOperationException always
     * 
     * @deprecated Use {@link AutomataOperations#accessible(Automaton, IntFunction)} instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    protected final <T extends Automaton> T accessibleHelper(T automaton) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new copy of this automaton that has all states removed which are
     * unable to reach a marked state.
     * 
     * @return The co-accessible automaton
     * 
     * @see AutomataOperations#coaccessible(Automaton, IntFunction)
     * 
     * @since 2.0
     **/
    public Automaton coaccessible() {
        return AutomataOperations.coaccessible(this, Automaton::new);
    }

    /**
     * A helper method used to generate the co-accessible portion of this automaton.
     * 
     * @param <T>               The type of automaton
     * @param automaton         The generic automaton object
     * @param invertedAutomaton The inverted automaton object
     * @return The same automaton that was passed into the method, now containing
     *         the co-accessible
     *         part of this automaton
     * 
     * @throws UnsupportedOperationException always
     * 
     * @deprecated Use {@link AutomataOperations#coaccessible(Automaton, IntFunction)} instead.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    protected final <T extends Automaton> T coaccessibleHelper(T automaton, T invertedAutomaton) {
        throw new UnsupportedOperationException();
    }

    /**
     * Create a new copy of this automaton that has the marking status of all states
     * toggled, and that has an added
     * 'dead' or 'dump' state where all undefined transitions lead.
     * 
     * @implNote This method should be overridden by subclasses, using the
     *           {@link #complementHelper(Automaton)} method.
     * @return The complement automaton
     * @throws OperationFailedException When there already exists a dump state,
     *                                  indicating that this
     *                                  operation has already been performed on this
     *                                  automaton
     * 
     * @since 2.0
     **/
    public Automaton complement() throws OperationFailedException {

        return AutomataOperations.complement(this, Automaton::new);
    }

    /**
     * A helper method used to generate complement of this automaton.
     * 
     * @param <T>       The type of automaton
     * @param automaton The generic automaton object
     * @return The same automaton that was passed into the method, now containing
     *         the complement of this automaton
     * @throws UnsupportedOperationException always
     * 
     * @deprecated Use {@link AutomataOperations#complement(Automaton, IntFunction)} instead.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    protected final <T extends Automaton> T complementHelper(T automaton) {
        throw new UnsupportedOperationException();
    }

    /**
     * Creates a new copy of this automaton that is trim (both accessible and
     * co-accessible).
     * 
     * @return The trim automaton
     * 
     * @throws NoInitialStateException if the initial state is not specified
     * 
     * @since 2.0
     **/
    public Automaton trim() {
        // Error checking
        if (getState(initialState) == null) {
            throw new NoInitialStateException("No starting state");
        }
        return accessible().coaccessible();
    }

    /**
     * Create a new version of this automaton which has all of the transitions going
     * the opposite direction.
     * 
     * @return The inverted automaton
     * 
     * @see AutomataOperations#invert(Automaton, IntFunction)
     * 
     * @revised 2.0
     **/
    public Automaton invert() {
        return AutomataOperations.invert(this, Automaton::new);
    }

    /**
     * A helper method used to generate the inverse of this automaton.
     * 
     * @param <T>       The type of automaton
     * @param automaton The generic automaton object
     * @return The same automaton that was passed into the method, now containing
     *         the inverse of this automaton
     * 
     * @throws UnsupportedOperationException always
     * 
     * @deprecated Use {@link AutomataOperations#invert(Automaton, IntFunction)} instead.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    protected final <T extends Automaton> T invertHelper(T automaton) {
        throw new UnsupportedOperationException();
    }

    /**
     * Generate the intersection of the two specified automata.
     * 
     * @param first  The first automaton
     * @param second The second automaton
     * @return The intersection
     * @throws IncompatibleAutomataException If the number of controllers do not
     *                                       match, or the automata have
     *                                       incompatible events
     * 
     * @since 2.0
     * 
     * @deprecated Use
     *             {@link AutomataOperations#intersection(Automaton, Automaton)}.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public static Automaton intersection(Automaton first, Automaton second) throws IncompatibleAutomataException {
        return AutomataOperations.intersection(first, second);
    }

    /**
     * Generate the union of the two specified automata.
     * 
     * @param first  The first automaton
     * @param second The second automaton
     * @return The union
     * @throws IncompatibleAutomataException If the number of controllers do not
     *                                       match, or the automata have
     *                                       incompatible events
     * 
     * @since 2.0
     * 
     * @deprecated Use {@link AutomataOperations#union(Automaton, Automaton)}.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public static Automaton union(Automaton first, Automaton second) throws IncompatibleAutomataException {
        return AutomataOperations.union(first, second);
    }

    /**
     * Apply the synchronized composition algorithm to an automaton to produce the
     * U-Structure.
     * 
     * @return The U-Structure
     * @throws NoInitialStateException  if there was no starting state
     * @throws OperationFailedException if something else went wrong
     **/
    public UStructure synchronizedComposition() {

        // Error checking
        if (getState(initialState) == null) {
            throw new NoInitialStateException("No starting state");
        }

        /* Setup */

        Deque<StateVector> stack = new ArrayDeque<StateVector>();
        Set<StateVector> valuesInStack = new HashSet<StateVector>();
        UStructure uStructure = new UStructure(nControllers);

        /* Add initial state to the stack */

        { // The only reason this is inside a scope is so that variable names could be
          // re-used more cleanly
            List<State> listOfInitialStates = new ArrayList<State>();
            State startingState = getState(initialState);

            // Create list of initial IDs and build the label
            for (int i = 0; i <= nControllers; i++) {
                listOfInitialStates.add(startingState);
            }

            StateVector initialStateVector = new StateVector(listOfInitialStates, nStates);
            stack.push(initialStateVector);
            valuesInStack.add(initialStateVector);

            uStructure.addStateAt(initialStateVector, true);

        }

        /* Continue until the stack is empty */

        while (stack.size() > 0) {

            StateVector stateVector = stack.pop();
            valuesInStack.remove(stateVector);

            // Get list of IDs and states
            List<State> listOfStates = stateVector.getStates();
            List<Long> listOfIDs = new ArrayList<Long>();
            for (State s : listOfStates)
                listOfIDs.add(s.getID());

            // For each transition in the system automaton
            outer: for (Transition t1 : listOfStates.get(0).getTransitions()) {

                Event e = t1.getEvent();

                List<State> targetStates = new ArrayList<State>();
                State currTargetState = getState(t1.getTargetStateID());
                targetStates.add(currTargetState);

                List<String> combinedEvent = new ArrayList<>();
                combinedEvent.add(e.getLabel());

                // Determine observable and controllable properties for this event vector
                boolean[] observable = new boolean[nControllers];
                boolean[] controllable = new boolean[nControllers];

                // For each controller
                for (int i = 0; i < nControllers; i++) {

                    // Observable events by this controller
                    if (e.isObservable(i)) {

                        observable[i] = true;

                        // If the event is observable, but not possible at this current time, then we
                        // can skip this altogether
                        State target = null;
                        for (Transition t2 : listOfStates.get(i + 1).getTransitions())
                            if (t2.getEvent().equals(e)) {
                                target = getState(t2.getTargetStateID());
                            }
                        if (target == null)
                            continue outer;

                        combinedEvent.add(e.getLabel());
                        targetStates.add(target);

                        if (e.isControllable(i)) {

                            controllable[i] = true;

                        }

                        // Unobservable events by this controller
                    } else {
                        combinedEvent.add(Event.EPSILON);
                        targetStates.add(listOfStates.get(i + 1));
                    }

                } // for i

                LabelVector eventLabelVector = new LabelVector(combinedEvent);

                StateVector targetStateVector = new StateVector(targetStates, nStates);

                boolean isConditionalViolation = false, isUnconditionalViolation = false;

                /* Check control configurations */
                if (transitionExists(listOfIDs.get(0), e.getID(), targetStates.get(0).getID()) && BooleanUtils.or(e.isControllable())) {
                    if (badTransitions.contains(new TransitionData(listOfIDs.get(0), e.getID(), targetStates.get(0).getID())))
                        isUnconditionalViolation = true;
                    else
                        isConditionalViolation = true;
                }
                controlCheck: for (int i = 1; i < listOfIDs.size() && (isConditionalViolation || isUnconditionalViolation); i++) {
                    String eventLabel = eventLabelVector.getLabelAtIndex(i);
                    if ((listOfIDs.get(i) == targetStates.get(i).getID()) && (Objects.equals(eventLabel, Event.EPSILON)))
                        continue controlCheck;
                    else if (!transitionExists(listOfIDs.get(i), getEvent(eventLabel).getID(), targetStates.get(i).getID())) {
                        isUnconditionalViolation = isConditionalViolation = false;
                    }
                }

                // Add event
                uStructure.addEventIfNonExisting(eventLabelVector, observable, controllable);

                // Add state if it doesn't already exist
                if (!uStructure.stateExists(targetStateVector.getID())) {

                    // Add state
                    if (!uStructure.addStateAt(targetStateVector, false)) {
                        throw new OperationFailedException("Failed to add state");
                    }

                    // Only add the ID if it's not already waiting to be processed
                    if (!valuesInStack.contains(targetStateVector)) {
                        stack.push(targetStateVector);
                        valuesInStack.add(targetStateVector);
                    } else
                        logger.debug("Prevented adding of state since it was already in the stack.");
                    /*
                     * NOTE: Does this ever get printed to the console? Intuitively it should, but I
                     * have never seen it before. (from Micah Stairs)
                     */
                }

                // Add transition
                int eventID = uStructure.addTransition(stateVector, eventLabelVector, targetStateVector);

                inner: for (int i = 0; i < nControllers; i++) {
                    if ((isConditionalViolation || isUnconditionalViolation)
                            && !transitionExistsWithEvent(stateVector.getStateFor(i + 1).getID(),
                                    getEvent(combinedEvent.get(0)).getID())) {
                        isUnconditionalViolation = false;
                        isConditionalViolation = false;
                        break inner;
                    }
                }

                if (isUnconditionalViolation) {
                    uStructure.addUnconditionalViolation(stateVector.getID(), eventID, targetStateVector.getID());
                    stateVector.setDisablementOf(combinedEvent.get(0));
                }
                if (isConditionalViolation) {

                    uStructure.addConditionalViolation(stateVector.getID(), eventID, targetStateVector.getID());
                    stateVector.setEnablementOf(combinedEvent.get(0));
                }

            } // for

            // For each unobservable transition in the each of the controllers of the
            // automaton
            outer: for (int i = 0; i < nControllers; i++) {

                for (Transition t : listOfStates.get(i + 1).getTransitions()) {
                    if (!t.getEvent().isObservable(i)) {

                        List<State> targetStates = new ArrayList<State>();
                        List<String> combinedEvent = new ArrayList<>();

                        for (int j = 0; j <= nControllers; j++) {

                            // The current controller
                            if (j == i + 1) {
                                combinedEvent.add(t.getEvent().getLabel());
                                targetStates.add(getState(t.getTargetStateID()));
                            } else {
                                combinedEvent.add(Event.EPSILON);
                                targetStates.add(getState(listOfIDs.get(j)));
                            }

                        }

                        LabelVector eventLabelVector = new LabelVector(combinedEvent);
                        StateVector targetStateVector = new StateVector(targetStates, nStates);

                        // Add event
                        boolean[] observable = new boolean[nControllers];
                        boolean[] controllable = new boolean[nControllers];
                        controllable[i] = t.getEvent().isControllable(i);
                        uStructure.addEventIfNonExisting(eventLabelVector, observable, controllable);

                        // Add state if it doesn't already exist
                        if (!uStructure.stateExists(targetStateVector)) {

                            // Add state
                            if (!uStructure.addStateAt(targetStateVector, false)) {
                                throw new OperationFailedException("Failed to add state");
                            }

                            // Only add the ID if it's not already waiting to be processed
                            if (!valuesInStack.contains(targetStateVector)) {
                                stack.push(targetStateVector);
                                valuesInStack.add(targetStateVector);
                            } else
                                logger.debug("Prevented adding of state since it was already in the stack.");

                        }

                        // Add transition
                        int eventID = uStructure.addTransition(stateVector, eventLabelVector, targetStateVector);
                        if (eventID == 0)
                            logger.error("Failed to add transition.");
                    }
                }

            } // for

        } // while

        /* Re-number states (by removing empty ones) */

        uStructure.renumberStates();

        /* Return produced U-Structure */

        return uStructure;

    }

    /**
     * Test to see if this system is observable.
     * 
     * @implNote This is an expensive test.
     * @return Whether or not this system is observable
     **/
    public boolean testObservability() {
        return AutomataOperations.testObservability(this, false).getLeft();
    }

    /**
     * Test to see if this system is observable.
     * 
     * @param showInferenceLevel whether to store and return the calculated
     *                           inferencing level for each control decision
     * @return Whether or not this system is observable and the inferencing level
     *         that makes the system inference observable
     * 
     * @since 2.0
     * @see AutomataOperations#testObservability(Automaton, boolean)
     */
    public Pair<Boolean, OptionalInt> testObservability(boolean showInferenceLevel) {

        return AutomataOperations.testObservability(this, showInferenceLevel);

    }

    /**
     * Calculates the ambiguity levels for each control configuration.
     * 
     * @return the ambiguity levels for each control configuration, if the system is
     *         inference observable
     * 
     * @throws SystemNotObservableException if system is not inference observable
     * 
     * @since 2.1.0
     */
    public List<AmbiguityData> calculateAmbiguityLevels() {

        try {
            return AutomataOperations.calculateAmbiguityLevels(this);
        } catch (IllegalArgumentException e) {
            throw new SystemNotObservableException(e);
        }

    }

    /**
     * Test to see if this system is controllable.
     * 
     * @implNote This is a cheap test.
     * @return Whether or not this system is controllable
     **/
    public boolean testControllability() {

        for (TransitionData data : badTransitions) {

            Event event = getEvent(data.eventID);

            // Ensure that the event is controllable
            if (!BooleanUtils.or(event.isControllable()))
                // Otherwise this system is not controllable
                return false;

        }

        return true;

    }

    /**
     * Generate the twin plant by combining this automaton w.r.t. G_{Sigma*}.
     * 
     * @implNote The technique used here is similar to how the complement works.
     *           This would not work
     *           in all cases, but G_{Sigma*} is a special case.
     * @return The twin plant
     **/
    public final Automaton generateTwinPlant() {

        Automaton automaton = new Automaton(getNumberOfControllers());

        /* Add events */

        automaton.addAllEvents(getEvents());

        /* Build twin plant */

        long dumpStateID = getNumberOfStates() + 1;
        boolean needToAddDumpState = false;

        // Add each state to the new automaton
        for (long s = 1; s <= getNumberOfStates(); s++) {

            State state = getState(s);

            long id = automaton.addState(state.getLabel(), !state.isMarked(), s == initialState);

            // Try to add transitions for each event
            for (Event e : events) {

                boolean foundMatch = false;

                // Search through each transition for the event
                for (Transition t : state.getTransitions())
                    if (t.getEvent().equals(e)) {
                        automaton.addTransition(id, e.getID(), t.getTargetStateID());
                        foundMatch = true;
                    }

                // Check to see if this event is controllable by at least one controller
                boolean controllable = false;
                for (boolean b : e.isControllable())
                    if (b) {
                        controllable = true;
                        break;
                    }

                // Add new transition leading to dump state if this event if undefined at this
                // state and is controllable
                if (!foundMatch && controllable) {
                    automaton.addTransition(id, e.getID(), dumpStateID);
                    automaton.markTransitionAsBad(id, e.getID(), dumpStateID);
                    needToAddDumpState = true;
                }

            }

        }

        /* Create dump state if it needs to be made */

        if (needToAddDumpState) {

            long id = automaton.addState(DUMP_STATE_LABEL, false, false);

            if (id != dumpStateID)
                logger.error("Dump state ID did not match expected ID.");

        }

        /* Add special transitions */

        copyOverSpecialTransitions(automaton);

        /* Return generated automaton */

        return automaton;

    }

    /**
     * Generate the twin plant by combining this automaton w.r.t. G_{Sigma*}.
     * 
     * @implNote The technique used here is similar to how the complement works.
     *           This would not work
     *           in all cases, but G_{Sigma*} is a special case.
     * @return The twin plant
     **/
    public final Automaton generateTwinPlant2() {

        Automaton automaton = new Automaton(getNumberOfControllers());

        /* Add events */

        automaton.addAllEvents(getEvents());

        /* Build twin plant */

        long dumpStateID = getNumberOfStates() + 1;
        boolean needToAddDumpState = false;

        List<Event> activeEvents = getActiveEvents();

        // Add each state to the new automaton
        for (long s = 1; s <= getNumberOfStates(); s++) {

            State state = getState(s);

            long id = automaton.addState(state.getLabel(), !state.isMarked(), s == initialState);

            // Try to add transitions for each event
            for (Event e : events) {

                boolean foundMatch = false;

                // Search through each transition for the event
                for (Transition t : state.getTransitions())
                    if (t.getEvent().equals(e)) {
                        automaton.addTransition(id, e.getID(), t.getTargetStateID());
                        foundMatch = true;
                    }

                // Check to see if this event is controllable by at least one controller
                boolean controllable = false;
                for (boolean b : e.isControllable())
                    if (b) {
                        controllable = true;
                        break;
                    }

                // Add new transition leading to dump state if this event if undefined at this
                // state and is controllable and active
                if (!foundMatch && controllable && activeEvents.contains(e)) {
                    automaton.addTransition(id, e.getID(), dumpStateID);
                    automaton.markTransitionAsBad(id, e.getID(), dumpStateID);
                    needToAddDumpState = true;
                }

            }

        }

        /* Create dump state if it needs to be made */

        if (needToAddDumpState) {

            long id = automaton.addState(DUMP_STATE_LABEL, false, false);

            if (id != dumpStateID)
                logger.error("Dump state ID did not match expected ID.");

        }

        /* Add special transitions */

        copyOverSpecialTransitions(automaton);

        /* Return generated automaton */

        return automaton;

    }

    /* AUTOMATA OPERATION HELPER METHODS */

    /**
     * Helper method to copy over all special transition data from this automaton to
     * another.
     * 
     * @implNote The data is only copied over if both of the states involved in the
     *           transition actually exist.
     * @apiNote This method is intended to be overridden.
     * @param <T>       type of automaton
     * @param automaton The automaton which is receiving the special transitions
     **/
    protected <T extends Automaton> void copyOverSpecialTransitions(T automaton) {

        for (TransitionData data : badTransitions)
            if (automaton.stateExists(data.initialStateID) && automaton.stateExists(data.targetStateID))
                automaton.markTransitionAsBad(data.initialStateID, data.eventID, data.targetStateID);

    }

    /**
     * Renumber all states so that all state IDs are continuous.
     * This must be done after operations such as
     * {@link #intersection(Automaton, Automaton) intersection} or
     * {@link #union(Automaton, Automaton) union}.
     */
    /* To make this method more efficient we could make the buffer larger. */
    protected final void renumberStates() {

        Map<Long, Long> mappingHashMap = new LinkedHashMap<>();

        long newID = 1;

        Map<Long, State> newStateMap = new HashMap<>();

        for (State s : getStates()) {
            long origID = s.getID();
            s.setID(newID);
            newStateMap.put(newID++, s);
            mappingHashMap.put(origID, s.getID());
        }

        states = newStateMap;

        /* Update transitions */

        for (State s : getStates()) {
            for (Transition t : s.getTransitions()) {
                t.setTargetStateID(mappingHashMap.get(t.getTargetStateID()));
            }
        }

        /* Update initial state */
        setInitialStateID(mappingHashMap.get(initialState));

        /* Update the special transitions */

        renumberStatesInAllTransitionData(mappingHashMap);

    }

    /**
     * Renumber the states in all applicable special transition data.
     * 
     * @apiNote This method is designed to be overridden when subclassing, in order
     *          to renumber the states in
     *          all applicable special transition data for this automaton type.
     * @param mappingHashMap The hash map containing the mapping information (old
     *                       state IDs to new state IDs)
     * 
     * @since 2.0
     **/
    protected void renumberStatesInAllTransitionData(Map<Long, Long> mappingHashMap) {

        renumberStatesInTransitionData(mappingHashMap, badTransitions);

    }

    /**
     * Helper method to renumber states in the specified list of special
     * transitions.
     * 
     * @param mappingHashMap The hash map containing the mapping information
     * @param list           The list of special transition data
     * 
     * @since 2.0
     **/
    protected final void renumberStatesInTransitionData(Map<Long, Long> mappingHashMap,
            List<? extends TransitionData> list) {

        for (TransitionData data : list) {

            // Update initialStateID
            data.initialStateID = mappingHashMap.get(data.initialStateID);

            // Update targetStateID
            data.targetStateID = mappingHashMap.get(data.targetStateID);

        }

    }

    /**
     * Given a list of IDs and a maximum possible ID, create a unique combined ID.
     * 
     * @implNote The order of the list matters. This method does not sort the list
     *           internally.
     * @param list  The list of IDs
     * @param maxID The largest possible value that could appear in the list
     *              (usually {@link #nStates})
     * @return The unique combined ID
     * @throws ArithmeticException if the ID combination result overflows a
     *                             {@code long}
     * 
     * @deprecated Use {@link IDUtil#combineIDs(List, long)}.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public static long combineIDs(List<Long> list, long maxID) {
        return IDUtil.combineIDs(list, maxID);

    }

    /**
     * Given a list of IDs and the largest possible value that could appear in the
     * list, create a unique
     * combined ID using a {@link BigInteger}.
     * 
     * @implNote The order of the list matters. This method does not sort the list
     *           internally.
     * @param list  The list of IDs
     * @param maxID The largest possible value that could appear in the list
     *              (usually {@link #nStates})
     * @return The unique combined ID
     * 
     * @deprecated Use {@link IDUtil#combineBigIDs(List, long)}.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public static BigInteger combineBigIDs(List<Long> list, long maxID) {
        return IDUtil.combineBigIDs(list, maxID);
    }

    /**
     * Given a combined ID, obtain the list of original IDs by reversing the
     * process.
     * 
     * @param combinedID The combined ID
     * @param maxID      The largest possible value to be used as an ID
     * @return The original list of IDs
     * 
     * @deprecated Use {@link IDUtil#separateIDs(long, long)}.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public static List<Long> separateIDs(long combinedID, long maxID) {

        return IDUtil.separateIDs(combinedID, maxID);

    }

    /**
     * Check to see if this automaton accepts the specified counter-example.
     * 
     * @param sequences The list of sequences of event labels which represent the
     *                  counter-example
     * @return {@code -1} if the automaton accepts the counter-example, or the
     *         number of steps it took to reject the counter-example
     **/
    public int acceptsCounterExample(List<List<String>> sequences) {

        int nSteps = 0;

        // Ensure that all sequences can be matched
        for (List<String> sequence : sequences) {

            State currentState = getState(getInitialStateID());

            outer: for (String label : sequence) {

                nSteps++;

                // Check all transitions for a match
                for (Transition t : currentState.getTransitions()) {
                    if (t.getEvent().getLabel().equals(label)) {
                        currentState = getState(t.getTargetStateID());
                        continue outer;
                    }
                }

                // If we got this far, then this automaton does not accept the counter-example
                return nSteps;

            }

        }

        return -1;

    }

    /* IMAGE GENERATION */

    /**
     * Output this automaton in a format that is readable by GraphViz, then export
     * as requested.
     * 
     * @param outputFileName The location to put the generated output
     * @return Whether or not the output was successfully generated
     * @throws NullPointerException if argument is {@code null}
     * 
     * @deprecated Use {@link AutomatonDotConverter#generateImage(String)} instead.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public final boolean generateImage(String outputFileName) {
        return getDotConverter().generateImage(outputFileName);
    }

    /**
     * Exports this automaton in a Graphviz-exportable format
     * 
     * @param outputFileName name of the exported file
     * @param format         file format to export with
     * @return the exported file
     * @throws IllegalArgumentException if {@code outputFileName} has a file
     *                                  extension that is not consistent with
     *                                  {@code format}
     * @throws NullPointerException     if any argument is {@code null}
     * @throws IOException              If I/O error occurs
     * @since 1.1
     * 
     * @deprecated Use {@link AutomatonDotConverter#export(String, Format)} instead.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public final File export(String outputFileName, Format format) throws IOException {
        return getDotConverter().export(outputFileName, format);
    }

    /**
     * Exports this automaton to a file
     * 
     * @param file the destination file to export this automaton to
     * @return the exported file
     * 
     * @throws NullPointerException     if argument is {@code null}
     * @throws IllegalArgumentException if argument is using an unsupported
     *                                  file extension
     * @throws IOException              if an I/O error occurs
     * 
     * @since 2.0
     * 
     * @deprecated Use {@link AutomatonDotConverter#export(File)} instead.
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public final File export(File file) throws IOException {
        return getDotConverter().export(file);
    }

    /**
     * Returns the DOT converter for this automaton.
     * 
     * @return the DOT converter for this automaton
     * 
     * @since 2.1.0
     */
    public final AutomatonDotConverter<? extends Automaton> getDotConverter() {
        if (this.dotConverter == null)
            this.dotConverter = AutomatonDotConverter.createConverter(this);
        return this.dotConverter;
    }

    /**
     * Generates all GUI input code.
     * 
     * @deprecated Use {@link AutomatonGuiInputGenerator} instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public void generateInputForGUI() {
        getGuiInputGenerator().refresh();
    }

    /**
     * Returns the GUI input generator for this automaton.
     * 
     * @return the GUI input generator for this automaton
     * 
     * @since 2.1.0
     */
    public final AutomatonGuiInputGenerator<? extends Automaton> getGuiInputGenerator() {
        if (this.generator == null)
            this.generator = AutomatonGuiInputGenerator.createGuiInputGenerator(this);
        return this.generator;
    }

    /**
     * Returns the event GUI input code.
     * 
     * @return the event GUI input code
     * 
     * @deprecated Use {@link AutomatonGuiInputGenerator#getEventInput()} instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public final String getEventInput() {
        return getGuiInputGenerator().getEventInput();
    }

    /**
     * Returns the state GUI input code.
     * 
     * @return the state GUI input code
     * 
     * @deprecated Use {@link AutomatonGuiInputGenerator#getStateInput()} instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public final String getStateInput() {
        return getGuiInputGenerator().getStateInput();
    }

    /**
     * Returns the transition GUI input code.
     * 
     * @return the transition GUI input code
     * 
     * @deprecated Use {@link AutomatonGuiInputGenerator#getTransitionInput()}
     *             instead.
     */
    @Deprecated(since = "2.1.0", forRemoval = true)
    public final String getTransitionInput() {
        return getGuiInputGenerator().getTransitionInput();
    }

    /**
     * Creates and returns a (deep) copy of this automaton.
     * 
     * @return a copy of this automaton
     * 
     * @since 2.0
     **/
    @Override
    public Automaton clone() {
        return new Automaton(this.toJsonObject());
    }

    /**
     * Returns a JSON representation of this automaton
     * 
     * @return a JSON representation of this automaton
     * 
     * @since 2.0
     */
    public JsonObject toJsonObject() {
        JsonObject jsonObj = new JsonObject();
        jsonObj.addProperty("nStates", nStates);
        jsonObj.addProperty("initialState", initialState);
        jsonObj.addProperty("nControllers", nControllers);

        jsonObj.addProperty("type", type.numericValue);
        JsonUtils.addListPropertyToJsonObject(jsonObj, "events", events, Event.class);
        jsonObj.add("states", gson.toJsonTree(getStates(), TypeUtils.parameterize(Collection.class, State.class)));

        addSpecialTransitionsToJsonObject(jsonObj);

        return jsonObj;
    }

    /**
     * Exports special transitions to the given JSON object.
     * 
     * @param jsonObj the JSON object to export to
     * 
     * @since 2.0
     */
    protected void addSpecialTransitionsToJsonObject(JsonObject jsonObj) {
        addTransitionDataToJsonObject(jsonObj, "badTransitions", badTransitions);
    }

    /**
     * Exports a list of transition data to the given JSON object.
     * 
     * @param jsonObj the JSON object to export to
     * @param name    the name to use for the JSON object
     * @param list    the list of transition data to export
     * 
     * @since 2.0
     */
    protected void addTransitionDataToJsonObject(JsonObject jsonObj, String name, List<TransitionData> list) {
        JsonUtils.addListPropertyToJsonObject(jsonObj, name, list, TransitionData.class);
    }

    /**
     * Reads special transitions from the given JSON object
     * 
     * @param jsonObj the JSON object to import from
     */
    protected void readSpecialTransitionsFromJsonObject(JsonObject jsonObj) {
        badTransitions = readTransitionDataFromJsonObject(jsonObj, "badTransitions");
    }

    /**
     * Exports a list of transition data to the given JSON object.
     * 
     * @param jsonObj the JSON object to import from
     * @param name    the name of the property in the JSON object that stores
     *                transition data
     * 
     * @return the list of transition data that is imported from the specified JSON
     *         object
     * 
     * @since 2.0
     */
    protected List<TransitionData> readTransitionDataFromJsonObject(JsonObject jsonObj, String name) {
        return JsonUtils.readListPropertyFromJsonObject(jsonObj, name, TransitionData.class);
    }

    /* MISCELLANEOUS */

    /**
     * Initialize the variables, ensuring that they all lay within the proper
     * ranges.
     **/
    private void initializeVariables() {

        /*
         * The number of controllers should be greater than 0, but it should not exceed
         * the maximum
         */

        if (nControllers < 1)
            nControllers = 1;
        if (nControllers > MAX_NUMBER_OF_CONTROLLERS)
            nControllers = MAX_NUMBER_OF_CONTROLLERS;
    }

    /* OVERRIDDEN METHOD */

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    /* MUTATOR METHODS */

    /**
     * Adds a transition based the label of the event (instead the ID).
     * 
     * @param startingStateID The ID of the state where the transition originates
     *                        from
     * @param eventLabel      The label of the event that triggers the transition
     * @param targetStateID   The ID of the state where the transition leads to
     * @return The ID of the event label (returns 0 if the addition was
     *         unsuccessful)
     **/
    public int addTransition(long startingStateID, String eventLabel, long targetStateID) {

        for (Event e : events)
            if (eventLabel.equals(e.getLabel())) {
                if (!addTransition(startingStateID, e.getID(), targetStateID))
                    return 0;
                else
                    return e.getID();
            }

        return 0;

    }

    /**
     * Adds a transition based on the label of the event (instead the ID).
     * 
     * @param startingState The state where the transition originates from
     * @param eventLabel    The label of the event that triggers the transition
     * @param targetState   The state where the transition leads to
     * @return The ID of the event label (returns 0 if the addition was
     *         unsuccessful)
     * 
     * @since 1.3
     **/
    public int addTransition(State startingState, String eventLabel, State targetState) {

        for (Event e : events)
            if (eventLabel.equals(e.getLabel())) {
                if (!addTransition(startingState.getID(), e.getID(), targetState.getID()))
                    return 0;
                else
                    return e.getID();
            }

        return 0;

    }

    /**
     * Adds a transition based on the label of the event (instead the ID).
     * 
     * @param startingState The state where the transition originates from
     * @param labelVector   The label vector of the event that triggers the
     *                      transition
     * @param targetState   The state where the transition leads to
     * @return The ID of the event label (returns 0 if the addition was
     *         unsuccessful)
     * 
     * @since 1.3
     **/
    public int addTransition(State startingState, LabelVector labelVector, State targetState) {

        return addTransition(startingState, labelVector.toString(), targetState);

    }

    /**
     * Adds a transition based on the specified IDs (which means that the states and
     * event must already exist).
     * 
     * @param startingStateID The ID of the state where the transition originates
     *                        from
     * @param eventID         The ID of the event that triggers the transition
     * @param targetStateID   The ID of the state where the transition leads to
     * @return Whether or not the addition was successful
     **/
    public boolean addTransition(long startingStateID, int eventID, long targetStateID) {

        /* Get starting state from ID */

        State startingState = getState(startingStateID);

        if (startingState == null) {
            logger.error("Could not add transition.", new StateNotFoundException(targetStateID));
            return false;
        }

        /* Add transition to this automaton */

        Event event = getEvent(eventID);
        if (!startingState.addTransition(new Transition(event, targetStateID))) {
            logger.error("Transition already exists.");
            return false;
        }

        return true;

    }

    /**
     * Checks whether a transition already exists.
     * 
     * @param startingStateID The ID of the state where the transition originates
     *                        from
     * @param eventID         The ID of the event that triggers the transition
     * @param targetStateID   The ID of the state where the transition leads to
     * @return Whether or not the matching transition exists
     * 
     * @since 2.0
     */
    public boolean containsTransition(long startingStateID, int eventID, long targetStateID) {
        State startingState = getState(startingStateID);
        Event event = getEvent(eventID);
        if (startingState == null)
            return false;
        else if (event == null)
            return false;
        return startingState.getTransitions().contains(new Transition(event, targetStateID));
    }

    /**
     * Checks whether a transition already exists.
     * 
     * @param startingState The state where the transition originates from
     * @param event         The event that triggers the transition
     * @param targetStateID The ID of the state where the transition leads to
     * @return Whether or not the matching transition exists
     * 
     * @since 2.0
     */
    public boolean containsTransition(State startingState, Event event, long targetStateID) {
        if (startingState == null)
            return false;
        else if (event == null)
            return false;
        else if (!startingState.equals(getState(startingState.getID()))) {
            throw new IllegalArgumentException("State information inconsistent");
        }
        return getState(startingState.getID()).getTransitions().contains(new Transition(event, targetStateID));
    }

    /**
     * Removes the specified transition.
     * 
     * @param startingStateID The ID of the state where the transition originates
     *                        from
     * @param eventID         The ID of the event that triggers the transition
     * @param targetStateID   The ID of the state where the transition leads to
     * @return Whether or not the removal was successful
     **/
    public boolean removeTransition(long startingStateID, int eventID, long targetStateID) {

        /* Get starting state from ID */

        State startingState = getState(startingStateID);

        if (startingState == null) {
            logger.error("Could not remove transition.", new StateNotFoundException(targetStateID));
            return false;
        }

        /* Remove transition and update the file */

        Event event = getEvent(eventID);
        startingState.removeTransition(new Transition(event, targetStateID));

        /*
         * Remove transition from list of special transitions (if it appears anywhere in
         * them)
         */

        removeTransitionData(new TransitionData(startingStateID, eventID, targetStateID));

        return true;

    }

    /**
     * Remove any special transition information attached to a particular
     * transition.
     * 
     * @apiNote This method is intended to be overridden.
     * @param data The transition data associated with the special transitions to be
     *             removed
     **/
    protected void removeTransitionData(TransitionData data) {

        badTransitions.remove(data);

    }

    /**
     * Add the specified state to the automaton with an empty transition list.
     * 
     * @param label          The "name" of the new state
     * @param marked         Whether or not the states is marked
     * @param isInitialState Whether or not this is the initial state
     * @return The ID of the added state; or {@code 0} if the addition was
     *         unsuccessful
     **/
    public long addState(String label, boolean marked, boolean isInitialState) {
        return addState(label, marked, null, isInitialState);
    }

    /**
     * Add the specified state to the automaton.
     * 
     * @param label          The "name" of the new state
     * @param marked         Whether or not the states is marked
     * @param transitions    The list of transitions (if {@code null}, then an empty
     *                       list is made)
     * @param isInitialState Whether or not this is the initial state
     * @return The ID of the added state; or {@code 0} if the addition was
     *         unsuccessful
     **/
    public long addState(String label, boolean marked, List<Transition> transitions, boolean isInitialState) {

        if (transitions == null)
            transitions = new ArrayList<Transition>();

        long id = ++nStates;

        /* Add new state to this automaton */

        State state = new State(label, id, marked, transitions);
        states.put(id, state);

        /* Change initial state */

        if (isInitialState)
            initialState = id;

        return id;
    }

    /**
     * Add the specified state to the automaton.
     * 
     * @implNote This method assumes that no state already exists with the specified
     *           ID.
     * @implNote The method {@link #renumberStates()} should be called some time
     *           after using this method has been called to make the state IDs
     *           consecutive.
     * @param label          The "name" of the new state
     * @param marked         Whether or not the states is marked
     * @param transitions    The list of transitions (if {@code null}, then a new
     *                       list is made)
     * @param isInitialState Whether or not this is the initial state
     * @param id             The index where the state should be added at
     * @return Whether or not the addition was successful (returns {@code false} if
     *         a state already existed there)
     **/
    public boolean addStateAt(String label, boolean marked, List<Transition> transitions, boolean isInitialState,
            long id) {

        return addStateAt(
                new State(label, id, marked, Objects.requireNonNullElse(transitions, new ArrayList<Transition>())),
                isInitialState);
    }

    /**
     * Add the specified state to the automaton.
     * 
     * @param label          The "name" of the new state
     * @param marked         Whether or not the states is marked
     * @param transitions    The list of transitions (if {@code null}, then a new
     *                       list is made)
     * @param isInitialState Whether or not this is the initial state
     * @param id             The index where the state should be added at
     * @param enablement     Whether or not this is an enablement state
     * @param disablement    Whether or not this is a disablement state
     * @return Whether or not the addition was successful (returns {@code false} if
     *         a state already existed there)
     * 
     * @since 2.0
     **/
    @Deprecated(since = "2.1.0", forRemoval = true)
    public boolean addStateAt(String label, boolean marked, List<Transition> transitions, boolean isInitialState,
            long id,
            boolean enablement, boolean disablement) {

        throw new UnsupportedOperationException();
    }

    /**
     * Add the specified state to the automaton.
     * 
     * @implNote The method {@link #renumberStates()} should be called some time
     *           after using
     *           this method to make the state IDs consecutive.
     * @param label          The "name" of the new state
     * @param marked         Whether or not the states is marked
     * @param transitions    The list of transitions (if {@code null}, then a new
     *                       list is made)
     * @param isInitialState Whether or not this is the initial state
     * @param id             The index where the state should be added at
     * @param enablementEvents     Whether or not this is an enablement state
     * @param disablementEvents    Whether or not this is a disablement state
     * @return Whether or not the addition was successful (returns {@code false} if
     *         a state already existed there)
     * 
     * @since 2.0
     **/
    public boolean addStateAt(String label, boolean marked, List<Transition> transitions, boolean isInitialState,
            long id,
            Set<String> enablementEvents, Set<String> disablementEvents) {

        if (stateExists(id))
            return false;

        return addStateAt(
                new State(label, id, marked, Objects.requireNonNullElse(transitions, new ArrayList<Transition>()),
                    enablementEvents, disablementEvents),
                isInitialState);
    }

    /**
     * Add the specified state to this automaton.
     * 
     * @param state          a state to add to this automaton
     * @param isInitialState Whether or not this is the initial state
     * @return Whether or not the addition was successful (returns {@code false} if
     *         a state already existed there)
     * 
     * @see #addStateAt(String, boolean, List, boolean, long)
     * @since 1.3
     */
    public boolean addStateAt(State state, boolean isInitialState) {

        /* Add new state to this automaton */

        if (states.containsKey(state.getID())) {
            logger.error("State with matching ID already exists");
            return false;
        }

        nStates++;

        states.put(state.getID(), state);

        /* Update initial state */

        if (isInitialState)
            initialState = state.getID();

        return true;
    }

    /**
     * Add the specified event to the set.
     * 
     * @implNote It is assumed that the new event is not already a member of the set
     *           (it is not checked for here for efficiency purposes).
     * @param label        The "name" of the new event
     * @param observable   Whether or not the event is observable
     * @param controllable Whether or not the event is controllable
     * @return The ID of the added event (0 indicates failure)
     **/
    public int addEvent(String label, boolean[] observable, boolean[] controllable) {
        int id = events.size() + 1;
        return addEvent(new Event(label, id, observable, controllable));
    }

    /**
     * Add the specified event to the set.
     * 
     * @implNote It is assumed that the new event is not already a member of the set
     *           (it is not checked for here for efficiency purposes).
     * @param labelVector  The label vector that represents the new event
     * @param observable   Whether or not the event is observable
     * @param controllable Whether or not the event is controllable
     * @return The ID of the added event (0 indicates failure)
     * 
     * @since 1.3
     **/
    public int addEvent(LabelVector labelVector, boolean[] observable, boolean[] controllable) {
        int id = events.size() + 1;
        return addEvent(new Event(labelVector, id, observable, controllable));
    }

    /**
     * Helper method for adding a new event
     * 
     * @param event the event to add
     * @return the ID of the added event, or {@code 0} if addition failed
     * 
     * @see #addEvent(String, boolean[], boolean[])
     * @see #addEvent(LabelVector, boolean[], boolean[])
     * @since 1.3
     */
    private int addEvent(Event event) {

        /* Add the event */

        if (!events.add(event)) {
            logger.error("Could not add event to list.");
            return 0;
        }

        eventsMap.put(event.getLabel(), event);

        return event.getID();
    }

    /**
     * Add the specified event to the set if it does not already exist.
     * 
     * @param label        The "name" of the new event
     * @param observable   Whether or not the event is observable
     * @param controllable Whether or not the event is controllable
     * @return The ID of the added event (negative ID indicates that the event
     *         already existed)
     *         or {@code 0}, which indicates failure (occurring when maximum number
     *         of events has been reached)
     **/
    public int addEventIfNonExisting(String label, boolean[] observable, boolean[] controllable) {

        Event event = getEvent(label);

        if (event == null)
            return addEvent(label, observable, controllable);
        else
            return -event.getID();

    }

    /**
     * Add the specified event to the set if it does not already exist.
     * 
     * @param labelVector  The label vector that represents the new event
     * @param observable   Whether or not the event is observable
     * @param controllable Whether or not the event is controllable
     * @return The ID of the added event (negative ID indicates that the event
     *         already existed)
     *         or {@code 0}, which indicates failure (occurring when maximum number
     *         of events has been reached)
     * 
     * @since 1.3
     **/
    public int addEventIfNonExisting(LabelVector labelVector, boolean[] observable, boolean[] controllable) {

        Event event = getEvent(labelVector);

        if (event == null)
            return addEvent(labelVector, observable, controllable);
        else
            return -event.getID();

    }

    /**
     * Add the entire list of events to `the automaton.
     * 
     * @param newEvents The list of events to add
     **/
    protected void addAllEvents(List<Event> newEvents) {

        for (Event e : newEvents)
            addEvent(e.getLabel(), ArrayUtils.clone(e.isObservable()), ArrayUtils.clone(e.isControllable()));

    }

    /**
     * Add the entire list of events to the automaton (ensuring that no duplicates
     * are added).
     * 
     * @param newEvents The list of events to add
     * @throws IncompatibleAutomataException If one of the events to be added is
     *                                       incompatible with an existing event
     **/
    protected void addEventsWithErrorChecking(List<Event> newEvents) throws IncompatibleAutomataException {

        for (Event event1 : newEvents) {

            Event event2 = getEvent(event1.getLabel());

            if (event2 == null)
                addEvent(event1.getLabel(), ArrayUtils.clone(event1.isObservable()),
                        ArrayUtils.clone(event1.isControllable()));
            else if (!Arrays.equals(event1.isObservable(), event2.isObservable())
                    || !Arrays.equals(event1.isControllable(), event2.isControllable()))
                throw new IncompatibleAutomataException();

        }

    }

    /**
     * Mark the specified as being "bad", which is used in synchronized composition.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     **/
    public void markTransitionAsBad(long initialStateID, int eventID, long targetStateID) {

        badTransitions.add(new TransitionData(initialStateID, eventID, targetStateID));

    }

    /**
     * Set the initial state to the state with the specified ID.
     * 
     * @param id The ID of the new initial state
     **/
    public void setInitialStateID(long id) {
        initialState = id;
    }

    /* ACCESSOR METHODS */

    /**
     * Check to see if a transition exists.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     * @return Whether or not the transition exists
     **/
    public boolean transitionExists(long initialStateID, int eventID, long targetStateID) {

        Event event = getEvent(eventID);

        if (event == null)
            return false;

        Transition transition = new Transition(event, targetStateID);
        State s = getState(initialStateID);

        for (Transition t : s.getTransitions())
            if (t.equals(transition))
                return true;

        return false;

    }

    /**
     * Check to see if the specified state already has a transition with a
     * particular event.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @return Whether or not the transition exists
     **/
    public boolean transitionExistsWithEvent(long initialStateID, int eventID) {

        if (getEvent(eventID) == null)
            return false;

        for (Transition t : getState(initialStateID).getTransitions())
            if (t.getEvent().getID() == eventID)
                return true;

        return false;

    }

    /**
     * Check to see if a transition is bad.
     * 
     * @param initialStateID The initial state
     * @param eventID        The event triggering the transition
     * @param targetStateID  The target state
     * @return Whether or not the transition is bad
     **/
    public boolean isBadTransition(long initialStateID, int eventID, long targetStateID) {

        TransitionData transitionData = new TransitionData(initialStateID, eventID, targetStateID);

        for (TransitionData t : badTransitions)
            if (t.equals(transitionData))
                return true;

        return false;

    }

    /**
     * Check to see if a transition is bad.
     * 
     * @param initialState The initial state
     * @param event        The event triggering the transition
     * @param targetState  The target state
     * @return Whether or not the transition is bad
     * 
     * @since 1.3
     **/
    public boolean isBadTransition(State initialState, Event event, State targetState) {

        return isBadTransition(initialState.getID(), event.getID(), targetState.getID());

    }

    /**
     * Check to see if a state exists.
     * 
     * @implNote This is a light-weight method which can be used instead of calling
     *           "{@code getState(id) != null}").
     *           It does not load all of the state information, but only checks the
     *           first byte to see if it exists or not.
     * @param id The unique identifier corresponding to the state we are looking for
     * @return {@code true} if the state with the matching ID exists
     **/
    public boolean stateExists(long id) {
        return states.containsKey(id);
    }

    /**
     * Check to see if a state exists.
     * 
     * @implNote This is a light-weight method which can be used instead of calling
     *           "{@code getState(id) != null}").
     *           It does not load all of the state information, but only checks the
     *           first byte to see if it exists or not.
     * @param state The state we are looking for
     * @return {@code true} if the state exists
     * 
     * @since 1.3
     **/
    public boolean stateExists(State state) {
        return states.containsValue(state);
    }

    /**
     * Given the ID number of a state, get the state information.
     * 
     * @param id The unique identifier corresponding to the requested state
     * @return The requested state
     **/
    public State getState(long id) {
        return states.get(id);
    }

    /**
     * Given the label of a state, get the state information.
     * 
     * @param label The label associated with the state
     * @return The requested state
     * 
     * @since 2.0
     */
    public State getState(String label) {
        return getStates().parallelStream().filter(s -> Objects.equals(s.getLabel(), label)).findAny().orElse(null);
    }

    /**
     * Given the label of a state, get the ID of the state.
     * 
     * @implNote This method is extremely expensive. It should only be used when
     *           absolutely necessary.
     * @param label The unique label corresponding to the requested state
     * @return The corresponding state ID (or {@code null}, if it was not found)
     **/
    public Long getStateID(String label) {
        State s = getState(label);
        return s == null ? null : s.getID();
    }

    /**
     * Returns the collection of states stored in this automaton.
     * The collection returned by this method is
     * {@link Collections#unmodifiableCollection(Collection) unmodifiable}.
     * 
     * @return collection of states stored in this automaton
     * 
     * @since 2.0
     */
    public final Collection<State> getStates() {
        return Collections.unmodifiableCollection(states.values());
    }

    /**
     * Check to see whether or not this automaton has any unmarked states.
     * 
     * @return Whether or not this automaton has at least one unmarked state
     **/
    public boolean hasUnmarkedState() {

        return getStates().stream().anyMatch(Predicate.not(State::isMarked));

    }

    /**
     * Given the ID number of an event, get the event information.
     * 
     * @implNote Using this method to check for a non-existent event is inefficient,
     *           since it checks each
     *           event one by one if it wasn't able to locate the event directly.
     *           This behaviour is required
     *           since there are cases where the event list is incomplete (for
     *           example,
     *           in {@link PrunedUStructure#removeInactiveEvents()}).
     * @param id The unique identifier corresponding to the requested event
     * @return The requested event (or {@code null} if it does not exist)
     **/
    public Event getEvent(int id) {

        // Try to get the event by directly indexing it, and return it if it exists
        try {
            Event event = getEvents().get(id - 1);
            if (event.getID() == id)
                return event;
        } catch (IndexOutOfBoundsException e) {
        }

        // Search each event one by one, looking for it
        // NOTE: This is necessary in removeInactiveEvents(), since events are in the
        // process of being re-numbered
        for (Event event : getEvents())
            if (event.getID() == id)
                return event;

        // Return null if it did not exist
        return null;

    }

    /**
     * Given the label of an event, get the event information.
     * 
     * @param label The unique label corresponding to the requested event
     * @return The requested event (or {@code null} if it does not exist)
     **/
    public Event getEvent(String label) {
        return eventsMap.get(label);
    }

    /**
     * Given the label vector of an event, get the event information.
     * 
     * @param labelVector The unique label vector corresponding to the requested
     *                    event
     * @return The requested event (or {@code null} if it does not exist)
     * 
     * @since 1.3
     **/
    public Event getEvent(LabelVector labelVector) {
        return eventsMap.get(labelVector.toString());
    }

    /**
     * Return the list of all events (ordered by ID, where event with an ID of 1 is
     * in position 0).
     * 
     * @return The list of all events
     **/
    public List<Event> getEvents() {
        return events;
    }

    /**
     * Get the number of events that are currently in this automaton.
     * 
     * @return Number of events
     **/
    public int getNumberOfEvents() {
        return getEvents().size();
    }

    /**
     * Return the list of all inactive events.
     * 
     * @implNote This is an expensive operation.
     * @return The list of all inactive events
     **/
    public List<Event> getInactiveEvents() {

        List<Event> inactiveEvents = new ArrayList<Event>(events);

        for (State state : getStates())
            for (Transition t : state.getTransitions())
                inactiveEvents.remove(t.getEvent());

        return inactiveEvents;
    }

    /**
     * Return the list of all active events.
     * 
     * @implNote This is an expensive operation.
     * @return The list of all active events
     **/
    public List<Event> getActiveEvents() {

        List<Event> activeEvents = new ArrayList<Event>(events);

        for (Event e : getInactiveEvents())
            activeEvents.remove(e);

        return activeEvents;
    }

    /**
     * Get the number of states that are currently in this automaton.
     * 
     * @return Number of states
     **/
    public long getNumberOfStates() {
        return nStates;
    }

    /**
     * Get the number of transitions that are currently in this automaton.
     * 
     * @implNote This is an expensive method.
     * @return Number of transitions
     **/
    public long getNumberOfTransitions() {

        return getStates().parallelStream().mapToInt(State::getNumberOfTransitions).sum();

    }

    /**
     * Generates and returns the list of all transitions in this automaton.
     * 
     * @return the list of all transitions in this automaton
     * @since 2.0
     */
    List<TransitionData> getAllTransitions() {
        return getStates().parallelStream().<TransitionData>mapMulti((state, consumer) -> {
            for (Transition t : state.getTransitions()) {
                consumer.accept(new TransitionData(state.getID(), t.getEvent().getID(), t.getTargetStateID()));
            }
        }).collect(Collectors.toList());
    }

    /**
     * Get the ID of the state where the automaton begins (the entry point).
     * 
     * @return The ID of the initial state (0 indicates that no initial state was
     *         specified)
     **/
    public long getInitialStateID() {
        return initialState;
    }

    /**
     * Get the number of controllers in the automaton (>1 indicates decentralized
     * control).
     * 
     * @return The number of controllers
     **/
    public int getNumberOfControllers() {
        return nControllers;
    }

    /**
     * Get the enum value associated with this automaton type.
     * 
     * @return The automaton type
     **/
    public final Type getType() {
        return type;
    }

    /**
     * Get the list of bad transitions.
     * 
     * @return The bad transitions
     **/
    public List<TransitionData> getBadTransitions() {
        return badTransitions;
    }

    /**
     * Check to see if the specified list contains at least 1 transition that is a
     * self-loop.
     * 
     * @param list The list of transitions
     * @return Whether or not the list contains a self-loop
     * 
     * @see TransitionData#containsSelfLoop(List)
     **/
    public boolean hasSelfLoop(List<? extends TransitionData> list) {

        return TransitionData.containsSelfLoop(list);

    }

    /**
     * Check to see if this automaton is deterministic.
     * 
     * @implNote This method has been added purely as a testing mechanism.
     * @return Whether or not this automaton is deterministic
     **/
    public boolean isDeterministic() {

        for (State s : getStates()) {

            List<Transition> transitions = s.getTransitions();
            Set<Integer> eventIDs = new HashSet<Integer>();

            for (Transition t : transitions)
                eventIDs.add(t.getEvent().getID());

            if (eventIDs.size() != transitions.size())
                return false;

        }

        return true;

    }

    /**
     * Check to see if all states and transitions in this automaton actually exist.
     * 
     * @implNote Does not check special transition data.
     * @implNote This method has been added purely as a testing mechanism.
     * @return Whether or not this automaton is deterministic
     **/
    public boolean isValid() {

        for (long s = 1; s <= getNumberOfStates(); s++) {

            State state = getState(s);

            if (state == null)
                return false;

            for (Transition t : state.getTransitions()) {
                if (getEvent(t.getEvent().getID()) == null)
                    return false;
                if (!stateExists(t.getTargetStateID()))
                    return false;
            }

        }

        return true;

    }

    /**
     * Mutate this automaton by adding self-loops to all states for all inactive
     * events.
     * 
     * @implNote This is being used for Liu's thesis implementation.
     **/
    public void addSelfLoopsForInactiveEvents() {

        List<Event> inactiveEvents = getInactiveEvents();

        for (State s : getStates())
            for (Event e : inactiveEvents)
                addTransition(s.getID(), e.getID(), s.getID());

    }

}
