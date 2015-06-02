import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class RandomAutomatonPrompt extends JFrame {

    AutomataGUI gui;
    JSpinner nControllers, nEvents, nStates, minTransitions, maxTransitions, nBadTransitions;

    static int  nControllersDefault = 1,
                nEventsDefault = 4,
                nStatesDefault = 10,
                minTransitionsDefault = 0,
                maxTransitionsDefault = 2,
                nBadTransitionsDefault = 0;

    public RandomAutomatonPrompt(AutomataGUI gui) {

        this.gui = gui;

        addComponents();

        setGUIproperties();

    }

    /**
     * Add all of the components to the window.
     **/
    private void addComponents() {

        /* Setup */

        Container container = new Container();
        setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

            /* Number of controllers */

        JLabel nControllersLabel = new JLabel(" # Controllers:");
        c.gridx = 0;
        c.gridy = 0;
        add(nControllersLabel, c);

        nControllers = new JSpinner(new SpinnerNumberModel(nControllersDefault, 1, Automaton.MAX_NUMBER_OF_CONTROLLERS, 1));
        c.gridx = 1;
        c.gridy = 0;
        add(nControllers, c);

            /* Number of events */

        JLabel nEventsLabel = new JLabel(" # Events:");
        c.gridx = 0;
        c.gridy = 1;
        add(nEventsLabel, c);

        nEvents = new JSpinner(new SpinnerNumberModel(nEventsDefault, 0, Event.MAX_NUMBER_OF_EVENTS, 1));
        c.gridx = 1;
        c.gridy = 1;
        add(nEvents, c);

            /* Number of states */

        JLabel nStatesLabel = new JLabel(" # States:");
        c.gridx = 0;
        c.gridy = 2;
        add(nStatesLabel, c);

        nStates = new JSpinner(new SpinnerNumberModel(nStatesDefault, 0, Integer.MAX_VALUE, 1)); // The Automaton class can support more states, but SpinnerNumberModel cannot
        c.gridx = 1;
        c.gridy = 2;
        add(nStates, c);

            /* Number of transitions */

        JLabel minTransitionsLabel = new JLabel(" Min. # Transitions per State:");
        c.gridx = 0;
        c.gridy = 3;
        add(minTransitionsLabel, c);

        minTransitions = new JSpinner(new SpinnerNumberModel(minTransitionsDefault, 0, Automaton.MAX_TRANSITION_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 3;
        add(minTransitions, c);

        JLabel maxTransitionsLabel = new JLabel(" Max. # Transitions per State:");
        c.gridx = 0;
        c.gridy = 4;
        add(maxTransitionsLabel, c);

        maxTransitions = new JSpinner(new SpinnerNumberModel(maxTransitionsDefault, 0, Automaton.MAX_TRANSITION_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 4;
        add(maxTransitions, c);

        JLabel nBadTransitionsLabel = new JLabel(" # Bad Transitions:");
        c.gridx = 0;
        c.gridy = 5;
        add(nBadTransitionsLabel, c);

        nBadTransitions = new JSpinner(new SpinnerNumberModel(nBadTransitionsDefault, 0, Integer.MAX_VALUE, 1));
        c.gridx = 1;
        c.gridy = 5;
        add(nBadTransitions, c);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                RandomAutomatonPrompt.this.dispose();
            }
        });
        c.gridx = 0;
        c.gridy = 6;
        add(cancelButton, c);

        JButton generateButton = new JButton("Generate");
        generateButton.addActionListener(new ActionListener() {
     
            public void actionPerformed(ActionEvent e) {
                gui.generateRandomAutomaton(
                    "random",
                    nEventsDefault = (Integer) nEvents.getValue(),
                    nStatesDefault = (Integer) nStates.getValue(),
                    minTransitionsDefault = (Integer) minTransitions.getValue(),
                    maxTransitionsDefault = (Integer) maxTransitions.getValue(),
                    nControllersDefault = (Integer) nControllers.getValue(),
                    nBadTransitionsDefault = (Integer) nBadTransitions.getValue()
                );
                RandomAutomatonPrompt.this.dispose();
            }

        });
        c.gridx = 1;
        c.gridy = 6;
        add(generateButton, c);

    }

    /**
     * Set some default GUI Properties.
     **/
    private void setGUIproperties() {

            /* Pack things in nicely */

        pack();
        
            /* Ensure our popup box will be closed when the user presses the "X" */

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

            /* Sets screen location in the center of the screen (only works after calling pack) */

        setLocationRelativeTo(null);

            /* Update title */

        setTitle("Generate Random Automaton");

            /* Show screen */

        setVisible(true);

    }
    
}