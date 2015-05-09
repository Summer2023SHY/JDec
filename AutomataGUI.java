import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.*;

public class AutomataGUI extends JFrame {

		/* Private instance variables */

	private JTextPane 	eventInput,
						stateInput,
						transitionInput;

    private Canvas canvas;
    private BufferedImage image = null;
    
    public static void main(String[] args) {
		new AutomataGUI();
    }

    public AutomataGUI() {

        Container container = new Container();
        container.setLayout(new FlowLayout());

        container.add(createInputContainer());

        canvas = new Canvas();
        container.add(canvas);

        add(container);

    	setGUIproperties();
    }

    private Container createInputContainer() {

            /* Setup */

        Container container = new Container();
        container.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;

            /* Event Input */

        JLabel eventInputInstructions = new JLabel("Enter events:");
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 0;
        container.add(new TooltipComponent(
                eventInputInstructions,
                "<html>1 event per line, formatted as <i>LABEL</i>,<i>OBSERVABLE</i>,<i>CONTROLLABLE</i>.<br>"
                + "<b><u>EXAMPLE</u></b>: '<i>EventName,True,False</i>' denotes an event called <b>EventName</b> "
                + "that is <b>observable</b> but <b>not controllable</b>.<br>"
                + "<b><u>NOTE</u></b>: '<i>True</i>' and '<i>False</i>' can be abbreviated as '<i>T</i>' and '<i>F</i>', respectively.</html>"
            ),c);

        eventInput = new JTextPane();
        JScrollPane eventInputScrollPane = new JScrollPane(eventInput);   
        c.ipady = 100;
        c.weightx = 0.5;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 1;
        container.add(eventInputScrollPane, c);

            /* State Input */

        JLabel stateInputInstructions = new JLabel("Enter states:");
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 0.0;
        c.gridx = 1;
        c.gridy = 0;
        container.add(new TooltipComponent(
                stateInputInstructions,
                "<html>1 state per line, formatted as <i>LABEL,MARKED</i>.<br>"
                + "<b><u>EXAMPLE</u></b>: <i>'StateName,False'</i> denotes a state called <b>StateName</b> that is <b>unmarked</b>.<br>"
                + "<b><u>NOTE</u></b>: <i>'True'</i> and <i>'False'</i> can be abbreviated as <i>'T'</i> and <i>'F'</i>, respectively.</html>"
            ),c);

        stateInput = new JTextPane();
        JScrollPane stateInputScrollPane = new JScrollPane(stateInput);
        c.ipady = 100;
        c.weightx = 0.5;
        c.weighty = 1.0;
        c.gridx = 1;
        c.gridy = 1;
        container.add(stateInputScrollPane, c);

            /* Transition Input */

        c.gridwidth = 2;

        JLabel transitionInputInstructions = new JLabel("Enter transitions:");
        c.ipady = 0;
        c.weightx = 1.0;
        c.weighty = 0.0;
        c.gridx = 0;
        c.gridy = 2;
        container.add(new TooltipComponent(
                transitionInputInstructions,
                "<html>1 transition per line, formatted as <i>INITIAL_STATE,EVENT,TARGET_STATE</i>.<br>"
                + "<b><u>EXAMPLE</u></b>: <i>'FirstState,Event,SecondState'</i> denotes a transition that goes from "
                + "the state <b>'FirstState'</b> to the state <b>'SecondState'</b> by the event called <b>'Event'</b>.</html>"
            ),c);

        transitionInput = new JTextPane();
        JScrollPane transitionInputScrollPane = new JScrollPane(transitionInput);
        c.ipady = 200;
        c.weightx = 0.5;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 3;
        container.add(transitionInputScrollPane, c);

            /* Generate Automaton (NOTE: It is assumed that Automatons that are typed in by hand will not be extremely large) */

        JButton generateAutomatonButton = new JButton("Generate Automaton");
        generateAutomatonButton.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent e) {
                generateAutomaton();
            }

        });
        c.ipady = 0;
        c.weightx = 0.5;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 4;
        container.add(generateAutomatonButton, c);

        return container;

    }

    /**
     * Set some default GUI Properties
     **/
    private void setGUIproperties() {

        // Pack things in nicely
        pack();
        
        // Ensure our application will be closed when the user presses the "X" */
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(800, 600);

        // Sets screen location in the center of the screen (only works after calling pack)
        setLocationRelativeTo(null);

        // Update title
        setTitle("Automata Manipulator");

        // Show screen
        setVisible(true);

    }

    private void generateAutomaton() {

        // Setup
        Automaton automaton = new Automaton();
        HashMap<String, Integer> eventMapping = new HashMap<String, Integer>(); // Maps the events's labels to the events's ID
        HashMap<String, Long> stateMapping = new HashMap<String, Long>(); // Maps the state's labels to the state's ID
        
        // Events
        for (String line : eventInput.getText().split("\n")) {
            
            String[] splitLine = line.split(",");

            if (splitLine.length == 3) {
                int id = automaton.addEvent(splitLine[0], isTrue(splitLine[1]), isTrue(splitLine[2]));
                eventMapping.put(splitLine[0], id);
            }
            else if (line.length() > 0)
                System.out.println("ERROR: Could not parse '" + line + "' as an event.");

        }

        // States
        for (String line : stateInput.getText().split("\n")) {
            
            String[] splitLine = line.split(",");

            if (splitLine.length == 2) {
                long id = automaton.addState(splitLine[0], isTrue(splitLine[1]));
                stateMapping.put(splitLine[0], id);
            }
            else if (line.length() > 0)
                System.out.println("ERROR: Could not parse '" + line + "' as a state.");
        }

        // Transitions (TO-DO: CATCH NULLPOINTEREXCEPTIONS)
        for (String line : transitionInput.getText().split("\n")) {
            String[] splitLine = line.split(",");
            if (splitLine.length == 3) {
                long initialStateID = stateMapping.get(splitLine[0]);
                int eventID = eventMapping.get(splitLine[1]);
                long targetStateID = stateMapping.get(splitLine[2]);
                automaton.addTransition(initialStateID, eventID, targetStateID);
            }
            else if (line.length() > 0)
                System.out.println("ERROR: Could not parse '" + line + "' as a transition.");
        }

        automaton.outputDOT();
        image = automaton.loadImageFromFile();
        repaint();
    }

    private boolean isTrue(String str) {
        return str.equals("T") || str.equals("True");
    }

    /**
     * Private class to add a tooltip with the specified text to the left of the given component.
     **/
    private class TooltipComponent extends JPanel {

	    public TooltipComponent(Component someComponent, String tooltipText) {

	    	// Create tooltip
	        JButton openTooltip = new JButton("?");
	        openTooltip.setToolTipText(tooltipText);

	        // Position components
	       	setLayout(new BorderLayout());
	        add(someComponent, BorderLayout.CENTER);
	        add(openTooltip, BorderLayout.WEST);

	    }

	}

    private class Canvas extends JPanel {

            /* Class Constants */
        
        private final int DEFAULT_HEIGHT  = 100;
        private final int DEFAULT_WIDTH   = 100;

        public Canvas () {

            setVisible(true);

        }

        /**
        * Returns the dimensions that the canvas should be, taking into consideration
        * the image size and padding.
        * @return preferred dimension
        */
        @Override public Dimension getPreferredSize() {


            return image == null  ? new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT)
                                : new Dimension(image.getWidth(), image.getHeight());
        
        }

        /**
        * Updates the canvas, drawing the image (or blank canvas) in the center,
        * with pre-defined padding around it.
        * @param g - Graphics object
        */
        @Override protected void paintComponent(Graphics g) {

            super.paintComponent(g);

                /* Draw blank canvas */
            
            if (image == null) {

                g.setColor(Color.LIGHT_GRAY);
                g.fillRect(0, 0, DEFAULT_WIDTH, DEFAULT_HEIGHT);

                /* Draw image */

            } else
                g.drawImage(image, 0, 0, null);

        }

     } // Canvas Class

}