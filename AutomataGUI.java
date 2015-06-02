import javax.swing.*;
import javax.swing.event.*;
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

    private static JTabbedPane tabbedPane;

    private static ArrayList<AutomatonTab> tabs = new ArrayList<AutomatonTab>();

    private int imageSize = 600;

    private File currentDirectory = null;

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
        promptBeforeExit();

    }

    /**
     * Prompt the user to save files before exiting.
     **/
    private void promptBeforeExit() {
    
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { 

                    /* Check for unsaved information */

                boolean unSavedInformation = false;
                for (int i = 0; i < tabbedPane.getTabCount(); i++)
                    if (!tabs.get(i).isSaved())
                        unSavedInformation = true;
                
                if (!unSavedInformation)
                    System.exit(0);

                    /* Prompt user to save */

                String buttons[] = { "Yes", "No" };
                
                int promptResult = JOptionPane.showOptionDialog(
                    null,
                    "Are you sure you want to exit? Any unsaved information will be lost.",
                    "Unsaved Information",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    buttons,
                    buttons[1]
                );
                
                if (promptResult == JOptionPane.YES_OPTION)
                    System.exit(0);

            }
        });

    }

    /**
     * Create an empty tab.
     **/
    private void createTab() {

        int index = tabbedPane.getTabCount();

        AutomatonTab tab = new AutomatonTab(index);
        tabs.add(tab);

        tabbedPane.addTab("untitled", null, tab, "");
        tabbedPane.setSelectedIndex(index);

    }

    /**
     * Create a tab, and load in an automaton.
     * @param automatonFile The header file of the stored automaton
     * @param automaton     The automaton object
     **/
    private void createTab(File automatonFile, Automaton automaton) {

        // Create new tab
        createTab();

        int newIndex = tabbedPane.getTabCount() - 1;

        // Set tab values
        AutomatonTab tab = tabs.get(newIndex);
        tab.file = automatonFile;
        tab.updateTabTitle();
        tab.automaton = automaton;
        tab.automaton.generateInputForGUI();
        tab.controllerInput.setValue(tab.automaton.getNumberOfControllers());
        tab.eventInput.setText(tab.automaton.getEventInput());
        tab.stateInput.setText(tab.automaton.getStateInput());
        tab.transitionInput.setText(tab.automaton.getTransitionInput());

        generateImage();

    }

    /**
     * Close the current tab.
     **/
    private void closeCurrentTab() {

            /* Get index of the currently selected tab */

        int index = tabbedPane.getSelectedIndex();

            /* Remove tab */

        tabbedPane.remove(index);
        tabs.remove(index);

            /* Re-number tabs */

        for (int i = 0; i < tabs.size(); i++)
            tabs.get(i).index = i;
        
    }

    /**
     * Set some default GUI Properties.
     **/
    private void setGUIproperties() {

            /* Pack things in nicely */

        pack();
        
            /* Ensure our application will be closed when the user presses the "X" */

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // setResizable(false);

            /* Sets screen location in the center of the screen (only works after calling pack) */

        setLocationRelativeTo(null);

            /* Update title */

        setTitle("Automata Manipulator");

            /* Show screen */

        setVisible(true);

    }

    /**
     * Load the current directory from file (so that the current directory is maintained even after the program has been closed).
     **/
    private void loadCurrentDirectory() {

        try {

            Scanner sc = new Scanner(new File(GUI_DATA_FILE_NAME));

            if (sc.hasNextLine())
                currentDirectory = new File(sc.nextLine());

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Saved the current directory to file (so that the current directory is maintained even after the program has been closed).
     **/
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

    private void export() {

        int index = tabbedPane.getSelectedIndex();

        AutomatonTab tab = tabs.get(index);

        // Create automaton from input code
        Automaton automaton = AutomatonGenerator.generateFromGUICode(
                tab.eventInput.getText(),
                tab.stateInput.getText(),
                tab.transitionInput.getText(),
                (Integer) tabs.get(tabbedPane.getSelectedIndex()).controllerInput.getValue(),
                true,
                tab.file
            );
        tab.automaton = automaton;

        // Set the image blank if there were no states entered
        if (automaton == null)
            tab.canvas.setImage(null);

        // Try to create graph image
        else {

            String fileName = tab.file.getName();
            String destinationFileName = currentDirectory + "/" + fileName.substring(0, fileName.length() - 4) + ".svg";

            if (automaton.generateImage(imageSize, Automaton.OutputMode.SVG, destinationFileName))
                JOptionPane.showMessageDialog(null, "The image of the graph has been exported to '" + destinationFileName + "'.", "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            else
                JOptionPane.showMessageDialog(null, "The image of the graph could not be exported!", "Export Failed", JOptionPane.ERROR_MESSAGE);

        }

    }

    private void generateAutomatonButtonPressed() {

        int index = tabbedPane.getSelectedIndex();

        AutomatonTab tab = tabs.get(index);

        // Create automaton from input code
        Automaton automaton = AutomatonGenerator.generateFromGUICode(
                tab.eventInput.getText(),
                tab.stateInput.getText(),
                tab.transitionInput.getText(),
                (Integer) tabs.get(tabbedPane.getSelectedIndex()).controllerInput.getValue(),
                true,
                tab.file
            );
        tab.automaton = automaton;
        tab.setSaved(true);

        generateImage();

    }

    private void generateImage() {

        AutomatonTab tab = tabs.get(tabbedPane.getSelectedIndex());

        // Create destination file name
        String destinationFileName = "untitled.png";
        if (tab.file != null) {
            String fileName = tab.file.getName();
            destinationFileName = currentDirectory + "/" + fileName.substring(0, fileName.length() - 4) + ".png";
        }

        // Set the image blank if there were no states entered
        if (tab.automaton == null)
            tab.canvas.setImage(null);

        // Try to create graph image, displaying it on the screen
        else if (tab.automaton.generateImage(imageSize, Automaton.OutputMode.PNG, destinationFileName))
            tab.canvas.setImage(tab.automaton.loadImageFromFile(destinationFileName));

        // Display error message
        else
            JOptionPane.showMessageDialog(null, "Something went wrong while loading the generated image from file!", "Error", JOptionPane.ERROR_MESSAGE);

    }

    /**
     * Adds the menu system to the application.
     **/
    private void addMenu() {

        JMenuBar menuBar = new JMenuBar();
        JMenuItem menuItem;
        JMenu menu;

          /* File Menu */

        menu = new JMenu("File");
        menuBar.add(menu);

        menuItem = new JMenuItem("New Tab");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Open");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Save As...");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Refresh Tab");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Clear");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Close Tab");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Export as SVG");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Quit");
        menuItem.addActionListener(this);
        menu.add(menuItem);

          /* Operation Menu */

        menu = new JMenu("Operation");
        menuBar.add(menu);

        menuItem = new JMenuItem("Accessible");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Co-Accessible");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Trim");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Intersection");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Union");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menu.addSeparator();

        menuItem = new JMenuItem("Synchronized Composition");
        menuItem.addActionListener(this);
        menu.add(menuItem);

            /* Generate Menu */

        menu = new JMenu("Generate");
        menuBar.add(menu);

        menuItem = new JMenuItem("From GUI Code");
        menuItem.addActionListener(this);
        menu.add(menuItem);

        menuItem = new JMenuItem("Random Automaton");
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
        AutomatonTab tab = null;

        // Only get the tab if it actually exists
        if (index > -1)
            tab = tabs.get(index);

        // Execute the appropriate command
        switch (event.getActionCommand()) {

                /* FILE STUFF */

            case "Clear":

                // Clear input fields
                tab.eventInput.setText("");
                tab.stateInput.setText("");
                tab.transitionInput.setText("");

                // Set blank image
                tab.canvas.setImage(null);

                break;

            case "New Tab":

                createTab();
                break;

            case "Save As...":

                // Prompt user to save Automaton to the specified file
                if (saveFile("Choose .hdr File") != null) {
                    tab.setSaved(true);
                    tab.updateTabTitle();
                    generateAutomatonButtonPressed(); // This is what actually saves it to the new file
                }
                    
                break;

            case "Open":

                // Prompt user to select Automaton from file (stop if they did not pick a file)
                if (selectFile("Select Automaton", index) == null)
                    break;

                tab.canvas.setImage(null);
                tab.updateTabTitle();

            case "Refresh Tab":

                refresh(index);

                break;

            case "Export as SVG":

                export();
                break;

            case "Close Tab":

                closeCurrentTab();
                break;

            case "Quit":

                dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
                break;

                /* AUTOMATA OPERATIONS */

            case "Accessible":

                Automaton automaton = tab.automaton.accessible();
                if (automaton == null)
                    JOptionPane.showMessageDialog(null, "Please specify a starting state.", "Accessible Operation Failed", JOptionPane.ERROR_MESSAGE);
                else
                    createTab(new File("accessible.hdr"), automaton);
                break;

            case "Co-Accessible":

                createTab(new File("coaccessible.hdr"), tab.automaton.coaccessible());
                break;

            case "Trim":

                automaton = tab.automaton.trim();
                if (automaton == null)
                    JOptionPane.showMessageDialog(null, "Please specify a starting state.", "Trim Operation Failed", JOptionPane.ERROR_MESSAGE);
                else
                    createTab(new File("trim.hdr"), automaton);
                break;

            case "Intersection":

                // Allow user to pick other automaton
                Automaton otherAutomaton = tabs.get(pickAutomaton("Which automaton would you like to take the intersection with?", index)).automaton;

                // Create new tab with the intersection
                Automaton intersection = Automaton.intersection(tab.automaton, otherAutomaton);
                if (intersection == null)
                    JOptionPane.showMessageDialog(null, "Both automata must have the same number of controllers.", "Intersection Operation Failed", JOptionPane.ERROR_MESSAGE);
                else
                    createTab(new File("intersection.hdr"), intersection);
                
                break;

            case "Union":

                // Allow user to pick other automaton
                otherAutomaton = tabs.get(pickAutomaton("Which automaton would you like to take the union with?", index)).automaton;

                // Create new tab with the union
                Automaton union = Automaton.union(tab.automaton, otherAutomaton);
                if (union == null)
                    JOptionPane.showMessageDialog(null, "Both automata must have the same number of controllers.", "Union Operation Failed", JOptionPane.ERROR_MESSAGE);
                else
                    createTab(new File("union.hdr"), union);

                break;

            case "Synchronized Composition":

                // Create new tab with the U-structure generated by synchronized composition
                automaton = tab.automaton.synchronizedComposition();
                if (automaton == null)
                    JOptionPane.showMessageDialog(null, "Please ensure that you specified a starting state.", "Synchronized Composition Operation Failed", JOptionPane.ERROR_MESSAGE);
                else
                    createTab(new File("synchronizedComposition.hdr"), automaton);

                break;

            case "From GUI Code":

                generateAutomatonButtonPressed();
                break;

            case "Random Automaton":

                new RandomAutomatonPrompt(this);

                break;
            
        }

    }

    public void generateRandomAutomaton(String fileName, int nEvents, long nStates, int minTransitionsPerState, int maxTransitionsPerState, int nControllers, int nBadTransitions) {

        Automaton automaton = AutomatonGenerator.generateRandom(fileName, nEvents, nStates, minTransitionsPerState, maxTransitionsPerState, nControllers, nBadTransitions);
        createTab(new File(fileName + ".hdr"), automaton);

    }

    // Load automaton from file, filling the input fields with its data
    private void refresh(int index) {

        AutomatonTab tab = tabs.get(index);

        tab.automaton = new Automaton(tab.file, false);
        tab.automaton.generateInputForGUI();
        tab.controllerInput.setValue(tab.automaton.getNumberOfControllers());
        tab.eventInput.setText(tab.automaton.getEventInput());
        tab.stateInput.setText(tab.automaton.getStateInput());
        tab.transitionInput.setText(tab.automaton.getTransitionInput());

        generateImage();

        tab.setSaved(true);

    }

    /** 
     * Opens up a JFileChooser for the user to choose a file from their file system.
     * @param title The title to put in the file chooser dialog box
     * @param index The index of the tab we're selecting a file for
     * @return a file that the user selected on their computer, or null if they didn't choose anything
     **/
    private File selectFile (String title, int index) {

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
            tabs.get(index).file = fileChooser.getSelectedFile();
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

        tabs.get(tabbedPane.getSelectedIndex()).file = file;
        currentDirectory = file.getParentFile();
        saveCurrentDirectory();

        return file;
        
    }

    private int pickAutomaton(String str, int indexToSkip) {

            /* Create list of options */

        ArrayList<String> optionsList = new ArrayList<String>();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {

            AutomatonTab tab = tabs.get(i);
                
            // Skip automaton
            if (i == indexToSkip || tab.file == null)
                continue;

            // Add automaton to list of options
            optionsList.add(tab.file.getName());

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
            if (tabs.get(i).file != null && tabs.get(i).file.getName().equals(choice))
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

    /**
    * Private class to maintain all GUI information about a single automaton.
    **/
    private class AutomatonTab extends Container {

            /* Public instance variables */

        public JTextPane    eventInput = null,
                            stateInput = null,
                            transitionInput = null;

        public JSpinner controllerInput = null;

        public Canvas canvas = null;

        public File file = null;

        public Automaton automaton;

        private boolean saved = true;

        public int index = -1;

            /* Constructor */

        public AutomatonTab(int index) {

            super();
            this.index = index;

                /* Setup */

            setLayout(new FlowLayout());
            add(createInputContainer());


                /* Create canvas */

            canvas = new Canvas();
            add(canvas);
        }

        private Container createInputContainer() {

                /* Setup */

            Container container = new Container();
            container.setLayout(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.HORIZONTAL;

                /* Controller Input */

            JLabel controllerInputLabel = new JLabel("# Controllers:");
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 0;
            c.gridy = 0;
            container.add(controllerInputLabel, c);

            controllerInput = new JSpinner(new SpinnerNumberModel(1, 1, Automaton.MAX_NUMBER_OF_CONTROLLERS, 1));
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 1;
            c.gridy = 0;
            container.add(controllerInput, c);

                /* Event Input */

            JLabel eventInputInstructions = new JLabel("Enter events:");
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 0;
            c.gridy = 1;
            container.add(new TooltipComponent(
                    eventInputInstructions,
                    "<html>1 event per line, formatted as <i>LABEL[,OBSERVABLE,CONTROLLABLE]</i>.<br>"
                    + "<b><u>EXAMPLE</u></b>: '<i>EventName,T,F</i>' denotes an event called <b>EventName</b> "
                    + "that is <b>observable</b> but <b>not controllable</b> for 1 controller.<br>"
                    + "<b><u>EXAMPLE</u></b>: '<i>EventName,TT,FT</i>' denotes an event called <b>EventName</b> "
                    + "that is <b>observable</b> but <b>not controllable</b> for the first controller, and is "
                    + "<b>observable</b> and <b>controllable</b> for the second controller.<br><b><u>NOTE</u></b>: "
                    + "'<i>T</i>' and '<i>F</i>' are case in-sensitive. If the observable and controllable properties are "
                    + "omitted, then it is assumed that they are observable and controllable for all controllers.<br>"
                    + "It is not possible, however, to omit the properties for some controllers, but not all.</html>"
                ),c);

            eventInput = new JTextPane();
            eventInput.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            eventInput.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
            JScrollPane eventInputScrollPane = new JScrollPane(eventInput) {
                @Override public Dimension getPreferredSize() {
                    return new Dimension(100, 100);    
                }
            };
            watchForChanges(eventInput);
            c.ipady = 100;
            c.weightx = 0.5;
            c.weighty = 1.0;
            c.gridx = 0;
            c.gridy = 2;
            container.add(eventInputScrollPane, c);

                /* State Input */

            JLabel stateInputInstructions = new JLabel("Enter states:");
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 1;
            c.gridy = 1;
            container.add(new TooltipComponent(
                    stateInputInstructions,
                    "<html>1 state per line, formatted as <i>[@]LABEL[,MARKED]</i> (where the '@' symbol denotes that this is the initial state).<br>"
                    + "<b><u>EXAMPLE</u></b>: <i>'StateName,F'</i> denotes a state called <b>StateName</b> that is <b>unmarked</b>.<br>"
                    + "<b><u>EXAMPLE</u></b>: <i>'@StateName'</i> denotes a state called <b>StateName</b> that is the <b>initial state</b> and is "
                    + "<b>marked</b>.<br><b><u>NOTE</u></b>: '<i>T</i>' and '<i>F</i>' are case in-sensitive. If omitted, the default value is "
                    + "'<i>T</i>'. There is only allowed to be one initial state.</html>"
                ),c);

            stateInput = new JTextPane();
            stateInput.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            stateInput.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
            JScrollPane stateInputScrollPane = new JScrollPane(stateInput) {
                @Override public Dimension getPreferredSize() {
                    return new Dimension(100, 100);    
                }
            };
            watchForChanges(stateInput);
            c.ipady = 100;
            c.weightx = 0.5;
            c.weighty = 1.0;
            c.gridx = 1;
            c.gridy = 2;
            container.add(stateInputScrollPane, c);

                /* Transition Input */

            c.gridwidth = 2;

            JLabel transitionInputInstructions = new JLabel("Enter transitions:");
            c.ipady = 0;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.gridx = 0;
            c.gridy = 3;
            container.add(new TooltipComponent(
                    transitionInputInstructions,
                    "<html>1 transition per line, formatted as <i>INITIAL_STATE,EVENT,TARGET_STATE[:SPECIAL_PROPERTIES]</i>"
                    + ", which are used in the synchronized composition operation).<br>"
                    + "<b><u>EXAMPLE</u></b>: <i>'FirstState,Event,SecondState'</i> denotes a transition that goes from "
                    + "the state <b>'FirstState'</b> to the state <b>'SecondState'</b> by the event called <b>'Event'</b>.<br>"
                    + "<b><u>NOTE</u></b>: <i>SPECIAL_PROPERTIES</i> can be added to a transition by appending ':NAME_OF_PROPERTY'. "
                    + "Additional properties are separated by commas.<br><b><u>Names of special properties:</u></b>: <i>'BAD'</i>, "
                    + "<i>'UNCONDITIONAL_VIOLATION'</i>, AND <i>'CONDITIONAL_VIOLATION'</i>.</html>"
                ),c);

            transitionInput = new JTextPane();
            transitionInput.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            transitionInput.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
            JScrollPane transitionInputScrollPane = new JScrollPane(transitionInput) {
                @Override public Dimension getPreferredSize() {
                    return new Dimension(100, 100);    
                }
            };
            watchForChanges(transitionInput);
            c.ipady = 200;
            c.weightx = 0.5;
            c.weighty = 1.0;
            c.gridx = 0;
            c.gridy = 4;
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
            c.gridy = 5;
            container.add(generateAutomatonButton, c);

            return container;

        }

        private void updateTabTitle() {

            String title = "untitled";

            if (file != null)
                title = file.getName();

            if (!saved)
                title += "*";

            tabbedPane.setTitleAt(index, title);

        }

        private void watchForChanges(JTextPane textPane) {

            textPane.getDocument().addDocumentListener(new DocumentListener() {

                @Override public void changedUpdate(DocumentEvent e) {
                    setSaved(false);
                }

                @Override public void insertUpdate(DocumentEvent e) {
                    setSaved(false);
                }

                @Override public void removeUpdate(DocumentEvent e) {
                    setSaved(false);
                }

            });    
        }

        public void setSaved(boolean newSavedStatus) {

            if (newSavedStatus != saved) {
                saved = newSavedStatus;
                updateTabTitle();
            }

        }

        public boolean isSaved() {
            return saved;
        }

    } // AutomatonTab

}