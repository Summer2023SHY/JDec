import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.filechooser.*;

public class AutomataGUI extends JFrame implements ActionListener {

        /** Class Constants **/

    private static final String GUI_DATA_FILE_NAME = "gui.data";

		/** Private instance variables **/

    private JTabbedPane tabbedPane;

	private ArrayList<JTextPane>   eventInput = new ArrayList<JTextPane>(),
	                               stateInput = new ArrayList<JTextPane>(),
	                               transitionInput = new ArrayList<JTextPane>();

    private ArrayList<Canvas> canvas = new ArrayList<Canvas>();
    private int imageSize = 587;

    private File currentDirectory = null;
    private ArrayList<File> automataFile = new ArrayList<File>();
    private ArrayList<Automaton> automata = new ArrayList<Automaton>();

        /** MAIN METHOD **/
    
    public static void main(String[] args) {
		new AutomataGUI();
    }

        /** CONSTRUCTOR **/

    public AutomataGUI() {

            /* Clear temporary files */

        Automaton.clearTemporaryFiles();

            /* Create tabbed pane and add a tab to it */

        tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);
        createTab();
        add(tabbedPane);

            /* Add menu */

        addMenu();

            /* Finish setting up */

    	setGUIproperties();
        loadCurrentDirectory();

    }

    private void createTab() {

            /* Setup */

        eventInput.add(null);
        stateInput.add(null);
        transitionInput.add(null);
        canvas.add(null);
        automataFile.add(null);
        automata.add(null);

        int index = tabbedPane.getTabCount();

            /* Create input boxes */

        Container container = new Container();
        container.setLayout(new FlowLayout());
        container.add(createInputContainer(index));

            /* Create canvas */

        canvas.set(index, new Canvas());
        container.add(canvas.get(index));

            /* Add to tabs */

        tabbedPane.addTab("untitled", null, container, "");
        tabbedPane.setSelectedIndex(index);

    }

    private void closeCurrentTab() {

            /* Get index of the currently selected tab */

        int index = tabbedPane.getSelectedIndex();

            /* Remove elements from each of the ArrayLists corresponding with this tab */

        eventInput.remove(index);
        stateInput.remove(index);
        transitionInput.remove(index);
        canvas.remove(index);
        automataFile.remove(index);
        automata.remove(index);

            /* Remove tab */

        tabbedPane.remove(index);
    }

    /**
     *  Set some default GUI Properties.
     **/
    private void setGUIproperties() {

        // Pack things in nicely
        pack();
        
        // Ensure our application will be closed when the user presses the "X" */
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // setResizable(false);

        // Sets screen location in the center of the screen (only works after calling pack)
        setLocationRelativeTo(null);

        // Update title
        setTitle("Automata Manipulator");

        // Show screen
        setVisible(true);

    }

    private void loadCurrentDirectory() {

        try {

            Scanner sc = new Scanner(new File(GUI_DATA_FILE_NAME));

            if (sc.hasNextLine())
                currentDirectory = new File(sc.nextLine());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void saveCurrentDirectory() {

        if (currentDirectory != null) {

            try {

                PrintWriter writer = new PrintWriter(new FileWriter(GUI_DATA_FILE_NAME, false));
                writer.println(currentDirectory.getPath());
                writer.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    private Container createInputContainer(int index) {

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
                "<html>1 event per line, formatted as <i>LABEL[,OBSERVABLE[,CONTROLLABLE]]</i>.<br>"
                + "<b><u>EXAMPLE</u></b>: '<i>EventName,True,False</i>' denotes an event called <b>EventName</b> "
                + "that is <b>observable</b> but <b>not controllable</b>.<br>"
                + "<b><u>NOTE</u></b>: '<i>True</i>' and '<i>False</i>' can be abbreviated as '<i>T</i>' and '<i>F</i>', "
                + "respectively. If omitted, the default value is '<i>True</i>'.</html>"
            ),c);

        eventInput.set(index, new JTextPane());
        eventInput.get(index).setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        eventInput.get(index).setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        JScrollPane eventInputScrollPane = new JScrollPane(eventInput.get(index)) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(100, 100);    
            }
        };
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
                "<html>1 state per line, formatted as <i>[*]LABEL[,MARKED]</i> (where the asterisk denotes that this is the initial state).<br>"
                + "<b><u>EXAMPLE</u></b>: <i>'StateName,False'</i> denotes a state called <b>StateName</b> that is <b>unmarked</b>.<br>"
                + "<b><u>EXAMPLE</u></b>: <i>'*StateName'</i> denotes a state called <b>StateName</b> that is the <b>initial state</b> and is <b>marked</b>.<br>"
                + "<b><u>NOTES</u></b>: <i>'True'</i> and <i>'False'</i> can be abbreviated as <i>'T'</i> and <i>'F'</i>, respectively. "
                + "If omitted, the default value is '<i>True</i>'. There is only allowed to be one initial state.</html>"
            ),c);

        stateInput.set(index, new JTextPane());
        stateInput.get(index).setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        stateInput.get(index).setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        JScrollPane stateInputScrollPane = new JScrollPane(stateInput.get(index)) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(100, 100);    
            }
        };
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

        transitionInput.set(index, new JTextPane());
        transitionInput.get(index).setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        transitionInput.get(index).setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        JScrollPane transitionInputScrollPane = new JScrollPane(transitionInput.get(index)) {
            @Override public Dimension getPreferredSize() {
                return new Dimension(100, 100);    
            }
        };
        c.ipady = 200;
        c.weightx = 0.5;
        c.weighty = 1.0;
        c.gridx = 0;
        c.gridy = 3;
        container.add(transitionInputScrollPane, c);

            /* Generate Automaton (NOTE: It is assumed that Automatons that are typed in by hand will not be extremely large) */

        JButton generateAutomatonButton = new JButton("Generate Automaton");
        generateAutomatonButton.setFocusable(false);
        generateAutomatonButton.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent e) {
                generateAutomatonButtonPressed();
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

    private void generateAutomatonButtonPressed() {

        int index = tabbedPane.getSelectedIndex();

        // Create automaton from input code
        Automaton automaton = generateAutomaton(
                eventInput.get(index).getText(),
                stateInput.get(index).getText(),
                transitionInput.get(index).getText(),
                true,
                automataFile.get(index)
            );
        automata.set(index, automaton);

        // Set the image blank if there were no states entered
        if (automaton == null)
            canvas.get(index).setImage(null);

        // Try to create graph image, displaying it on the screen
        else if (automaton.generateImage(imageSize))
            canvas.get(index).setImage(automaton.loadImageFromFile());

    }

    /* In order to use TestAutomata.java to run some test routines on it, this method was made public and had some parameters added */
    public static Automaton generateAutomaton(String eventInputText, String stateInputText, String transitionInputText, boolean verbose, File headerFile) {

            /* Setup */
        
        Automaton automaton = new Automaton(headerFile, true);
        HashMap<String, Integer> eventMapping = new HashMap<String, Integer>(); // Maps the events's labels to the events's ID
        HashMap<String, Long> stateMapping = new HashMap<String, Long>(); // Maps the state's labels to the state's ID

            /* States */
        
        for (String line : stateInputText.split("\n")) {
            
            String[] splitLine = line.split(",");
            String label = splitLine[0];

            // Check to see if this is a duplicate state label
            if (stateMapping.get(label) != null) {
                if (verbose)
                    System.out.println("ERROR: Could not store '" + line + "' as a state, since there is already a state with this label.");
                continue;
            }

            // Try to add the state
            if (splitLine.length >= 1 && label.length() > 0) {

                boolean isInitialState = (label.charAt(0) == '*');

                // Ensure the user didn't only have an asterisk as the name of the label (since the asterisk gets removed, we are left with an empty string)
                if (isInitialState && label.length() == 1) {
                    if (verbose)
                        System.out.println("ERROR: Could not parse '" + line + "' as a state (state name must be at least 1 character long).");
                    continue;

                }

                // Remove '*' from the label name
                if (isInitialState)
                    label = label.substring(1);

                // Check for invalid label
                if (!isValidLabel(label)) {
                    System.out.println("ERROR: Invalid label.");
                    continue;
                }

                long id = automaton.addState(label, splitLine.length < 2 || isTrue(splitLine[1]), isInitialState);

                // Error checking
                if (id == 0) {
                    if (verbose)
                        System.out.println("ERROR: Could not store '" + line + "' as a state.");
                    continue;
                }
                
                // Add state
                stateMapping.put(label, id);

            } else if (line.length() > 0 && verbose)
                System.out.println("ERROR: Could not parse '" + line + "' as a state.");
        }

            /* The image will be blank if there are no states */

        if (stateMapping.isEmpty())
            return null;
        
            /* Events */

        for (String line : eventInputText.split("\n")) {
            
            String[] splitLine = line.split(",");
            String label = splitLine[0];

            // Check to see if this is a duplicate event label
            if (eventMapping.get(label) != null) {
                if (verbose)
                    System.out.println("ERROR: Could not store '" + line + "' as an event, since there is already an event with this label.");
                continue;
            }

            // Try to add the event
            if (splitLine.length >= 1 && label.length() > 0) {

                int id = automaton.addEvent(label, splitLine.length < 2 || isTrue(splitLine[1]), splitLine.length < 3 || isTrue(splitLine[2]));

                // Check for invalid label
                if (!isValidLabel(label)) {
                    System.out.println("ERROR: Invalid label.");
                    continue;
                }

                // Error checking
                 if (id == 0) {
                    if (verbose)
                        System.out.println("ERROR: Could not store '" + line + "' as an event.");
                    continue;
                 }
                    
                
                // Add event
                eventMapping.put(label, id);
            }
            else if (line.length() > 0 && verbose)
                System.out.println("ERROR: Could not parse '" + line + "' as an event.");

        }

            /* Transitions */

        for (String line : transitionInputText.split("\n")) {
            
            String[] splitLine = line.split(",");

            // Ensure that all 3 parameters are present
            if (splitLine.length == 3) {

                // Get ID's of initial state, event, and target state
                Long initialStateID = stateMapping.get(splitLine[0]);
                Integer eventID = eventMapping.get(splitLine[1]);
                Long targetStateID = stateMapping.get(splitLine[2]);

                // Prevent crashing by checking to see if any of the values are null (indicates that they've entered a state or event that doesn't exist)
                if (initialStateID == null || eventID == null || targetStateID == null) {
                    if (verbose)
                        System.out.println("ERROR: Could not store '" + line + "' as a transition.");
                }
                
                // Add transition
                else
                    automaton.addTransition(initialStateID, eventID, targetStateID);
            
            } else if (line.length() > 0 && verbose)
                System.out.println("ERROR: Could not parse '" + line + "' as a transition.");
        }

        return automaton;
    }

    private static boolean isTrue(String str) {
        return str.equals("T") || str.equals("True");
    }

    /**
     * Label must consist of only letters or the underscore characters (other characters, including digits cause complications with GraphViz).
     * @param label The label to validate
     * @return whether or not the label is valid
     **/
    private static boolean isValidLabel(String label) {

        for (int i = 0; i < label.length(); i++)
            if (!Character.isLetter(label.charAt(i)) && label.charAt(i) != '_')
                return false;

        return true;

    }

    /**
     *  Adds the menu system to the application.
     **/
    private void addMenu() {

        JMenuBar menuBar = new JMenuBar();
        JMenuItem menuItem;
        JMenu menu;

          /* File Menu */

        menu = new JMenu("File");
        menuBar.add(menu);

        menuItem = new JMenuItem("Clear");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("New Tab");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Open");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Save As...");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Refresh");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Close");
        menuItem.addActionListener(this);
        menu.add(menuItem);

          /* Operation Menu */

        menu = new JMenu("Operation");
        menuBar.add(menu);

        menuItem = new JMenuItem("Accessible");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Intersection");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Union");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        this.setJMenuBar(menuBar);

    }

    /**
     * This method handles all of the actions triggered when the user interacts with the main menu.
     * @param event - The triggered event
     **/
    public void actionPerformed(ActionEvent event) {

        int index = tabbedPane.getSelectedIndex();

        switch (event.getActionCommand()) {

                /* File Stuff */

            case "Clear":

                // Clear input fields
                eventInput.get(index).setText("");
                stateInput.get(index).setText("");
                transitionInput.get(index).setText("");

                // Set blank image
                canvas.get(index).setImage(null);

                break;

            case "New Tab":

                createTab();
                break;

            case "Save As...":

                // Prompt user to save Automaton to the specified file
                saveFile("Choose .hdr File");
                tabbedPane.setTitleAt(index, automataFile.get(index).getName());
                generateAutomatonButtonPressed(); // This actually saves it to the new file
                    
                break;

            case "Open":

                // Prompt user to select Automaton from file (stop if they did not pick a file)
                if (selectFile("Select Automaton") == null)
                    break;

                tabbedPane.setTitleAt(index, automataFile.get(index).getName());

            case "Refresh":

                refresh(index);

                break;

            case "Close":

                closeCurrentTab();
                break;

                /* Automata Operations */

            case "Accessible":

                // Create new tab
                createTab();
                int newIndex = tabbedPane.getTabCount() - 1;

                // Set tab values
                automataFile.set(newIndex, new File("accessible.hdr"));
                tabbedPane.setTitleAt(newIndex, automataFile.get(newIndex).getName());
                automata.set(newIndex, automata.get(index).accessible());
                automata.get(newIndex).generateInputForGUI();
                eventInput.get(newIndex).setText(automata.get(newIndex).getEventInput());
                stateInput.get(newIndex).setText(automata.get(newIndex).getStateInput());
                transitionInput.get(newIndex).setText(automata.get(newIndex).getTransitionInput());

                break;

            case "Intersection":

                // Allow user to pick other automaton
                Automaton otherAutomaton = automata.get(pickAutomaton("Which automaton would you like to take the intersection with?", index));

                // Create new tab
                createTab();
                newIndex = tabbedPane.getTabCount() - 1;

                // Set tab values
                automataFile.set(newIndex, new File("intersection.hdr"));
                tabbedPane.setTitleAt(newIndex, automataFile.get(newIndex).getName());
                automata.set(newIndex, Automaton.intersection(automata.get(index), otherAutomaton));
                automata.get(newIndex).generateInputForGUI();
                eventInput.get(newIndex).setText(automata.get(newIndex).getEventInput());
                stateInput.get(newIndex).setText(automata.get(newIndex).getStateInput());
                transitionInput.get(newIndex).setText(automata.get(newIndex).getTransitionInput());

                break;

            case "Union":

                // Allow user to pick other automaton
                otherAutomaton = automata.get(pickAutomaton("Which automaton would you like to take the union with?", index));

                // Create new tab
                createTab();
                newIndex = tabbedPane.getTabCount() - 1;

                // Set tab values
                automataFile.set(newIndex, new File("union.hdr"));
                tabbedPane.setTitleAt(newIndex, automataFile.get(newIndex).getName());
                automata.set(newIndex, Automaton.union(automata.get(index), otherAutomaton));
                automata.get(newIndex).generateInputForGUI();
                eventInput.get(newIndex).setText(automata.get(newIndex).getEventInput());
                stateInput.get(newIndex).setText(automata.get(newIndex).getStateInput());
                transitionInput.get(newIndex).setText(automata.get(newIndex).getTransitionInput());

                break;
            
        }

    }

    // Load automaton from file, filling the input fields with its data
    private void refresh(int index) {

        automata.set(index, new Automaton(automataFile.get(index), false));
        automata.get(index).generateInputForGUI();
        eventInput.get(index).setText(automata.get(index).getEventInput());
        stateInput.get(index).setText(automata.get(index).getStateInput());
        transitionInput.get(index).setText(automata.get(index).getTransitionInput());

    }

    /** 
     *  Opens up a JFileChooser for the user to choose a file from their file system.
     *  @param title - The title to put in the file chooser dialog box
     *  @return a file that the user selected on their computer, or null if they didn't choose anything
     **/
    private File selectFile (String title) {

            /* Set up the file chooser */

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);

            /* Filter .hdr files */

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Automaton files", "hdr");
        fileChooser.setFileFilter(filter);

            /* Begin at the most recently accessed directory */

        if (currentDirectory != null)
            fileChooser.setCurrentDirectory(currentDirectory);

            /* Prompt user to select a file */

        fileChooser.showOpenDialog(null);

            /* Update last file opened and update current directory */

        if (fileChooser.getSelectedFile() != null) {
            automataFile.set(tabbedPane.getSelectedIndex(), fileChooser.getSelectedFile());
            currentDirectory = fileChooser.getSelectedFile().getParentFile();
            saveCurrentDirectory();
        }

        return fileChooser.getSelectedFile();
        
    }

    /**
     *  Prompts the user to name and specify the filename they wish to save the data.
     *  @return - A File object to which data can be saved
     **/
    private File saveFile(String title) {

            /* Set up the file chooser */

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);

            /* Filter .hdr files */

        FileNameExtensionFilter filter = new FileNameExtensionFilter("Automaton files", "hdr");
        fileChooser.setFileFilter(filter);

            /* Begin at the most recently accessed directory */
        
        if (currentDirectory != null)
            fileChooser.setCurrentDirectory(currentDirectory);

            /* Prompt user to select a filename */

        fileChooser.showSaveDialog(null);

            /* User pressed cancel, so there was no file */

        if (fileChooser.getSelectedFile() == null)
            return null;

            /* Add .hdr extension if the user didn't put it there */

        String name = fileChooser.getSelectedFile().getName();

        // Remove anything after the period
        if (name.indexOf(".") != -1)
            name = name.substring(0, name.indexOf("."));

        File file = new File(fileChooser.getSelectedFile().getParentFile() + "/" + name + ".hdr");

            /* Update last file opened and update current directory */

        automataFile.set(tabbedPane.getSelectedIndex(), file);
        currentDirectory = file.getParentFile();
        saveCurrentDirectory();

        return file;
        
    }

    private int pickAutomaton(String str, int indexToSkip) {

            /* Create list of options */

        ArrayList<String> optionsList = new ArrayList<String>();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                
            // Skip automaton
            if (i == indexToSkip || automataFile.get(i) == null)
                continue;

            // Add automaton to list of options
            optionsList.add(automataFile.get(i).getName());

        }

        String[] options = optionsList.toArray(new String[optionsList.size()]);
        
            /* Display prompt to user */
        
        String choice = (String) JOptionPane.showInputDialog(
                null,
                str,
                "Choose Automaton",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
            );

            /* Return index of chosen automaton */

       for (int i = 0; i < tabbedPane.getTabCount(); i++)
            if (automataFile.get(i) != null && automataFile.get(i).getName().equals(choice))
                return i;

        return -1;

    }

    /**
     * Private class to add a tooltip with the specified text to the left of the given component.
     **/
    private class TooltipComponent extends JPanel {

	    public TooltipComponent(Component someComponent, String tooltipText) {

	    	// Create tooltip
	        JButton openTooltip = new JButton("?");
            openTooltip.setFocusable(false);
	        openTooltip.setToolTipText(tooltipText);

	        // Position components
	       	setLayout(new BorderLayout());
	        add(someComponent, BorderLayout.CENTER);
	        add(openTooltip, BorderLayout.WEST);

	    }

	} // TooltipComponent class

    /**
     * Private class to maintain a canvas on which a BufferedImage can be drawn.
     **/
    private class Canvas extends JPanel {

            /* Private instance variable */

        private BufferedImage image;

            /* Constructor */

        public Canvas () {

            setVisible(true);

        }

        /**
         *  Update the image in the canvas.
         **/
        public void setImage(BufferedImage image) {

            this.image = image;
            this.repaint();

        }

        /**
         *  Returns the dimensions that the canvas should be.
         *  @return the preferred dimension
         **/
        @Override public Dimension getPreferredSize() {

            return new Dimension(imageSize, imageSize);
        
        }

        /**
         *  Updates the canvas, drawing the image (or blank canvas) in the center.
         *  @param g - Graphics object
         **/
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

     } // Canvas class

}