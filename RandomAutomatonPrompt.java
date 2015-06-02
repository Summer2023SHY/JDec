import java.awt.*;
import javax.swing.*;

public class RandomAutomatonPrompt extends JFrame {

    public RandomAutomatonPrompt() {

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

        JSpinner nControllers = new JSpinner(new SpinnerNumberModel(1, 1, Automaton.MAX_NUMBER_OF_CONTROLLERS, 1));
        c.gridx = 1;
        c.gridy = 0;
        add(nControllers, c);

            /* Number of events */

        JLabel nEventsLabel = new JLabel(" # Events:");
        c.gridx = 0;
        c.gridy = 1;
        add(nEventsLabel, c);

        JSpinner nEvents = new JSpinner(new SpinnerNumberModel(0, 0, Event.MAX_NUMBER_OF_EVENTS, 1));
        c.gridx = 1;
        c.gridy = 1;
        add(nEvents, c);

            /* Number of states */

        JLabel nStatesLabel = new JLabel(" # States:");
        c.gridx = 0;
        c.gridy = 2;
        add(nStatesLabel, c);

        JSpinner nStates = new JSpinner(new SpinnerNumberModel(0, 0, Automaton.MAX_STATE_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 2;
        add(nStates, c);

            /* Number of transitions */

        JLabel minTransitionsLabel = new JLabel(" Min. # transitions per state:");
        c.gridx = 0;
        c.gridy = 3;
        add(minTransitionsLabel, c);

        JSpinner minTransitions = new JSpinner(new SpinnerNumberModel(0, 0, Automaton.MAX_TRANSITION_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 3;
        add(minTransitions, c);

        JLabel maxTransitionsLabel = new JLabel(" Max. # transitions per state:");
        c.gridx = 0;
        c.gridy = 4;
        add(maxTransitionsLabel, c);

        JSpinner maxTransitions = new JSpinner(new SpinnerNumberModel(0, 0, Automaton.MAX_TRANSITION_CAPACITY, 1));
        c.gridx = 1;
        c.gridy = 4;
        add(maxTransitions, c);

        JButton cancelButton = new JButton("Cancel");
        c.gridx = 0;
        c.gridy = 5;
        add(cancelButton, c);

        JButton generateButton = new JButton("Generate");
        c.gridx = 1;
        c.gridy = 5;
        add(generateButton, c);

    }

    /**
     * Set some default GUI Properties.
     **/
    private void setGUIproperties() {

            /* Pack things in nicely */

        pack();
        
            /* Ensure our application will be closed when the user presses the "X" */

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

            /* Sets screen location in the center of the screen (only works after calling pack) */

        setLocationRelativeTo(null);

            /* Update title */

        setTitle("Generate Random Automata");

            /* Show screen */

        setVisible(true);

    }
    
}