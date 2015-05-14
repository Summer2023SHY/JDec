import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.filechooser.*;

public class AutomataGUI extends JFrame implements ActionListener {

		/* Private instance variables */

    private JTabbedPane tabbedPane;

	private ArrayList<JTextPane>   eventInput = new ArrayList<JTextPane>(),
	                               stateInput = new ArrayList<JTextPane>(),
	                               transitionInput = new ArrayList<JTextPane>();

    private ArrayList<Canvas> canvas = new ArrayList<Canvas>();
    private int imageSize = 587;

    private File currentDirectory = null;
    private ArrayList<File> automataFile = new ArrayList<File>();
    
    public static void main(String[] args) {
		new AutomataGUI();
    }

    public AutomataGUI() {

            /* Create tabbed pane and add a tab to it */

        tabbedPane = new JTabbedPane();
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

        // Set the image blank if there were no states entered
        if (automaton == null)
            canvas.get(index).setImage(null);

        // Try to create graph image, displaying it on the screen
        else if (automaton.outputDOT(imageSize))
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

            // Try to add the state
            } else if (splitLine.length >= 1 && label.length() > 0) {

                boolean isInitialState = (label.charAt(0) == '*');

                // Ensure the user didn't only have an asterisk as the name of the label (since the asterisk gets removed, we are left with an empty string)
                if (isInitialState && label.length() == 1) {
                    
                    if (verbose)
                        System.out.println("ERROR: Could not parse '" + line + "' as a state (state name must be at least 1 character long).");

                } else {

                    // Remove '*' from the label name
                    if (isInitialState)
                        label = label.substring(1);

                    long id = automaton.addState(label, splitLine.length < 2 || isTrue(splitLine[1]), isInitialState);

                    // Error checking
                    if (id == 0) {
                        if (verbose)
                            System.out.println("ERROR: Could not store '" + line + "' as a state.");
                    }
                    
                    // Add state
                    else
                        stateMapping.put(label, id);

                }

            }
            else if (line.length() > 0 && verbose)
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

            // Try to add the event
            } else if (splitLine.length >= 1 && label.length() > 0) {
                int id = automaton.addEvent(label, splitLine.length < 2 || isTrue(splitLine[1]), splitLine.length < 3 || isTrue(splitLine[2]));

                // Error checking
                 if (id == 0) {
                    if (verbose)
                        System.out.println("ERROR: Could not store '" + line + "' as an event.");
                 }
                    
                
                // Add event
                else
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

    /* Adds the menu system to the application */
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

        menuItem = new JMenuItem("New");
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

        this.setJMenuBar(menuBar);

    }

    /**
    *   This method handles all of the actions triggered when the user interacts with the main menu.
    *   @param event - The triggered event
    **/
    public void actionPerformed(ActionEvent event) {

        int index = tabbedPane.getSelectedIndex();

        switch (event.getActionCommand()) {

            case "Clear":

                // Clear input fields
                eventInput.get(index).setText("");
                stateInput.get(index).setText("");
                transitionInput.get(index).setText("");

                // Set blank image
                canvas.get(index).setImage(null);

                break;

            case "New":

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

                // Load Automaton from file, filling the input fields with its data
                Automaton automaton = new Automaton(automataFile.get(tabbedPane.getSelectedIndex()), false);
                automaton.generateInputForGUI();
                eventInput.get(tabbedPane.getSelectedIndex()).setText(automaton.getEventInput());
                stateInput.get(tabbedPane.getSelectedIndex()).setText(automaton.getStateInput());
                transitionInput.get(tabbedPane.getSelectedIndex()).setText(automaton.getTransitionInput());

                break;

            case "Close":

                closeCurrentTab();
                break;
            
        }

    }

    /** 
     *  Opens up a JFileChooser for the user to choose a file from their file system.
     *  @param title - The title to put in the file chooser dialog box
     *  @return a file that the user selected on their computer, or null if they didn't choose anything
     */
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
     */
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

        private BufferedImage image;

            /* Class Constants */

        public Canvas () {

            setVisible(true);

        }

        public void setImage(BufferedImage image) {

            this.image = image;
            this.repaint();
            // pack();

        }

        /**
        * Returns the dimensions that the canvas should be.
        * @return preferred dimension
        */
        @Override public Dimension getPreferredSize() {

            return new Dimension(imageSize, imageSize);

            // return image == null  ? new Dimension(imageSize, imageSize)
            //                     : new Dimension(image.getWidth(), image.getHeight());
        
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