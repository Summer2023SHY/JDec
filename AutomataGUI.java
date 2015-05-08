import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AutomataGUI extends JFrame {

		/* Private instance variables */

	private JTextPane 	eventInput,
						stateInput,
						transitionsInput;
    
    public static void main(String[] args) {
		new AutomataGUI();
    }

    public AutomataGUI() {

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
				+ "<b><u>EXAMPLE</u></b>: '<i>EventName,T,F</i>' denotes an event called <b>EventName</b> that is <b>observable</b> but <b>not controllable</b>.</html>"
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
				"<html>1 state per line, formatted as <i>LABEL</i>,<i>MARKED</i>.<br>"
				+ "<b><u>EXAMPLE</u></b>: '<i>StateName,F</i>' denotes a state called <b>StateName</b> that is <b>unmarked</b>.</html>"
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
				"<html>1 state per line, formatted as <i>LABEL</i>,<i>MARKED</i>.<br>"
				+ "<b><u>EXAMPLE</u></b>: '<i>StateName,F</i>' denotes a state called <b>StateName</b> that is <b>unmarked</b>.</html>"
			),c);

		transitionsInput = new JTextPane();
		JScrollPane transitionInputScrollPane = new JScrollPane(transitionsInput);
		c.ipady = 400;
    	c.weightx = 0.5;
    	c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 3;
    	container.add(transitionInputScrollPane, c);

    	add(container);

    		/* Generate Automaton */

    	JButton generateAutomaton = new JButton("Generate");
    	generateAutomaton.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent e)
            {

            	Automaton a = new Automaton();

                for (String line : eventInput.getText().split("\n")) {
                	String[] splitLine = line.split(",");
                	if (splitLine.length == 3)
                		a.addEvent(splitLine[0], splitLine[1].equals("T"), splitLine[2].equals("T"));
                	else
                		System.out.println("ERROR: Could not parse '" + line + "'");
                }

            }

        });    
    	add(generateAutomaton, BorderLayout.SOUTH);

    		/* Finish GUI */

    	setGUIproperties();
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

}