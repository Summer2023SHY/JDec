import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.filechooser.*;

public class AutomataGUI extends JFrame implements ActionListener {

		/* Private instance variables */

	private JTextPane 	eventInput,
						stateInput,
						transitionInput;

    private Canvas canvas;
    private BufferedImage image = null;
    private int imageSize;

    private File currentDirectory = null;
    private File mostRecentInputFile = null;
    
    public static void main(String[] args) {
		new AutomataGUI();
    }

    public AutomataGUI() {

            /* Create input boxes */

        Container container = new Container();
        container.setLayout(new FlowLayout());
        container.add(createInputContainer());

            /* Create canvas */

        canvas = new Canvas();
        container.add(canvas);
        add(container);

            /* Calculate the size we want the image to be, and update the canvas */

        pack();
        imageSize = container.getHeight();
        repaint();

            /* Add menu */

        addMenu();

            /* Finish setting up */

    	setGUIproperties();
        loadCurrentDirectory();

    }

    /**
    *   Set some default GUI Properties.
    **/
    private void setGUIproperties() {

        // Pack things in nicely
        pack();
        
        // Ensure our application will be closed when the user presses the "X" */
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        // Sets screen location in the center of the screen (only works after calling pack)
        setLocationRelativeTo(null);

        // Update title
        setTitle("Automata Manipulator");

        // Show screen
        setVisible(true);

    }

    private void loadCurrentDirectory() {

        try {

            Scanner sc = new Scanner(new File("gui.data"));

            if (sc.hasNextLine())
                currentDirectory = new File(sc.nextLine());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void saveCurrentDirectory() {

        if (currentDirectory != null) {

            try {

                PrintWriter writer = new PrintWriter(new FileWriter("gui.data", false));
                writer.println(currentDirectory.getPath());
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

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
                "<html>1 event per line, formatted as <i>LABEL,[OBSERVABLE],[CONTROLLABLE].<br>"
                + "<b><u>EXAMPLE</u></b>: '<i>EventName,True,False</i>' denotes an event called <b>EventName</b> "
                + "that is <b>observable</b> but <b>not controllable</b>.<br>"
                + "<b><u>NOTE</u></b>: '<i>True</i>' and '<i>False</i>' can be abbreviated as '<i>T</i>' and '<i>F</i>', "
                + "respectively. If omitted, the default value is '<i>True</i>'.</html>"
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
                "<html>1 state per line, formatted as <i>LABEL,[MARKED]</i>.<br>"
                + "<b><u>EXAMPLE</u></b>: <i>'StateName,False'</i> denotes a state called <b>StateName</b> that is <b>unmarked</b>.<br>"
                + "<b><u>NOTE</u></b>: <i>'True'</i> and <i>'False'</i> can be abbreviated as <i>'T'</i> and <i>'F'</i>, respectively. "
                + "If omitted, the default value is '<i>True</i>'.</html>"
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
                
                // Remove any temporary files
                Automaton.deleteTemporaryFiles();

                // Create automaton from input code
                Automaton automaton = generateAutomaton(eventInput.getText(), stateInput.getText(), transitionInput.getText());

                // Set the image blank if there were no states entered
                if (automaton == null)
                    canvas.setBlankImage();

                // Try to create graph image, displaying it on the screen
                else if (automaton.outputDOT(imageSize)) {
                    image = automaton.loadImageFromFile();
                    canvas.repaint();
                    // pack();
                }

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

    /* In order to use TestAutomata.java to run some test routines on it, this method was made public and had some parameters added */
    public static Automaton generateAutomaton(String eventInputText, String stateInputText, String transitionInputText) {

            /* Setup */
        
        Automaton automaton = new Automaton();
        HashMap<String, Integer> eventMapping = new HashMap<String, Integer>(); // Maps the events's labels to the events's ID
        HashMap<String, Long> stateMapping = new HashMap<String, Long>(); // Maps the state's labels to the state's ID

            /* States */
        
        for (String line : stateInputText.split("\n")) {
            
            String[] splitLine = line.split(",");

            if (splitLine.length >= 1 && splitLine[0].length() > 0) {
                long id = automaton.addState(splitLine[0], splitLine.length < 2 || isTrue(splitLine[1]));

                if (id == 0)
                    System.out.println("ERROR: Could not store '" + line + "' as a state.");
                else
                    stateMapping.put(splitLine[0], id);

            }
            else if (line.length() > 0)
                System.out.println("ERROR: Could not parse '" + line + "' as a state.");
        }

            /* The image will be blank if there are no states */

        if (stateMapping.isEmpty())
            return null;
        
            /* Events */

        for (String line : eventInputText.split("\n")) {
            
            String[] splitLine = line.split(",");

            if (splitLine.length >= 1 && splitLine[0].length() > 0) {
                int id = automaton.addEvent(splitLine[0], splitLine.length < 2 || isTrue(splitLine[1]), splitLine.length < 3 || isTrue(splitLine[2]));

                 if (id == 0)
                    System.out.println("ERROR: Could not store '" + line + "' as an event.");
                else
                     eventMapping.put(splitLine[0], id);
            }
            else if (line.length() > 0)
                System.out.println("ERROR: Could not parse '" + line + "' as an event.");

        }

            /* Transitions */

        for (String line : transitionInputText.split("\n")) {
            String[] splitLine = line.split(",");
            if (splitLine.length == 3) {
                Long initialStateID = stateMapping.get(splitLine[0]);
                Integer eventID = eventMapping.get(splitLine[1]);
                Long targetStateID = stateMapping.get(splitLine[2]);
                if (initialStateID == null || initialStateID == 0  || eventID == null || eventID == 0 || targetStateID == null || targetStateID == 0)
                    System.out.println("ERROR: Could not store '" + line + "' as a transition.");
                else
                    automaton.addTransition(initialStateID, eventID, targetStateID);
            }
            else if (line.length() > 0)
                System.out.println("ERROR: Could not parse '" + line + "' as a transition.");
        }

        return automaton;
    }

    private static boolean isTrue(String str) {
        return str.equals("T") || str.equals("True");
    }

    /* Adds the menu system to the application */
    private void addMenu() {

        JMenuBar menuBar = new JMenuBar();
        JMenuItem menuItem;
        JMenu menu;

          /* File Menu */

        menu = new JMenu("File");
        menuBar.add(menu);

        menuItem = new JMenuItem("Open");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Refresh");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Close");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        this.setJMenuBar(menuBar);

    }

    /**
    *   This method handles all of the actions triggered when the user interacts with the main menu.
    *   @param event - The triggered event
    **/
    public void actionPerformed(ActionEvent event) {

        switch (event.getActionCommand()) {

            case "Open":

                // Prompt user to select Automaton from file (stop if they did not pick a file)
                if (selectFile("Select Automaton") == null)
                    break;

            case "Refresh":

                // Load Automaton from file, filling the input fields with its data
                Automaton automaton = new Automaton(mostRecentInputFile);
                eventInput.setText(automaton.getEventInput());
                stateInput.setText(automaton.getStateInput());
                transitionInput.setText(automaton.getTransitionInput());
                automaton.generateInputForGUI();

                break;

            case "Close":

                // Clear input fields
                eventInput.setText("");
                stateInput.setText("");
                transitionInput.setText("");

                // Set blank image
                canvas.setBlankImage();

                break;
        }

    }

    /** 
     *  Opens up a JFileChooser for the user to choose a file from their file system.
     *  @param title - The title to put in the file chooser dialog box.
     *  @return a file that the user selected on their computer, or null if they didn't choose anything
     */
    private File selectFile (String title) {

            /* Set up the file chooser */

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);

            /* Filter .BMP files */

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Automaton files", "hdr");
        fileChooser.setFileFilter(filter);

            /* Begin at the most recently accessed directory */

        if (currentDirectory != null)
            fileChooser.setCurrentDirectory(currentDirectory);

            /* Prompt user to select a file */

        fileChooser.showOpenDialog(null);

            /* Update last file opened and update current directory */

        if (fileChooser.getSelectedFile() != null) {
            mostRecentInputFile = fileChooser.getSelectedFile();
            currentDirectory = mostRecentInputFile.getParentFile();
            saveCurrentDirectory();
        }

        return fileChooser.getSelectedFile();
        
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

        public Canvas () {

            setVisible(true);

        }

        public void setBlankImage() {

            image = null;
            this.repaint();
            // pack();

        }

        /**
        * Returns the dimensions that the canvas should be, taking into consideration
        * the image size and padding.
        * @return preferred dimension
        */
        @Override public Dimension getPreferredSize() {

            return image == null  ? new Dimension(imageSize, imageSize)
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
                g.fillRect(0, 0, imageSize, imageSize);

                /* Draw image */

            } else {

                g.drawImage(image, 0, 0, null);

            }

        }

     } // Canvas Class

}