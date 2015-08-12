/**
 * JDec - A Java application for Decentralized Control. This application has been design to build
 *        and manipulate various structures such as Automata, U-Structures, and Crushes.
 *        NOTE: There should only ever be one instance of this class running at one time.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Class Constants
 *  -Instance Variables
 *  -Main Method
 *  -Constructor
 *  -Setup Methods
 *  -Actions
 *  -Enabling/Disabling Components
 *  -Prompts
 *  -Helper Methods
 *  -Inner Classes
 **/

import com.apple.eawt.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.w3c.dom.*;
import org.xml.sax.*;

public class JDec extends JFrame implements ActionListener {

    /* CLASS CONSTANTS */

  private static final String GUI_DATA_FILE_NAME  = "gui.data";
  private static final File TEMPORARY_DIRECTORY   = new File("JDec_Temporary_Files");
  public static final int PREFERRED_DIALOG_WIDTH  = 500;
  public static final int PREFERRED_DIALOG_HEIGHT = 500;

    /* INSTANCE VARIABLES */

  // Tabs
  private JTabbedPane tabbedPane;
  private ArrayList<AutomatonTab> tabs = new ArrayList<AutomatonTab>();
  
  // Enabling/disabling components
  private java.util.List<Component> componentsWhichRequireTab              = new ArrayList<Component>();
  private java.util.List<Component> componentsWhichRequireAnyAutomaton     = new ArrayList<Component>();
  private java.util.List<Component> componentsWhichRequireBasicAutomaton   = new ArrayList<Component>();
  private java.util.List<Component> componentsWhichRequireUStructure       = new ArrayList<Component>();
  private java.util.List<Component> componentsWhichRequirePrunedUStructure = new ArrayList<Component>();
  private java.util.List<Component> componentsWhichRequireAnyUStructure    = new ArrayList<Component>();

  // Miscellaneous
  private File currentDirectory = null;
  private int temporaryFileIndex = 1;
  private JLabel noTabsMessage;

  // Tool-tip Text
  private static Document tooltipDocument;
  static {
    
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    factory.setIgnoringElementContentWhitespace(true);
    
    try {
      tooltipDocument = factory.newDocumentBuilder().parse(new File("res/tooltips.xml"));
    } catch (ParserConfigurationException | SAXException | IOException e) {
      tooltipDocument = null;
    }

  }

    /* MAIN METHOD */

  /**
   * Create an instance of this application, setting it to use the Mac's screen menu bar.
   * @param args  Any arguments are simply ignored
   **/  
  public static void main(String[] args) {
    
    // Use OSX's screen menu bar
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    
    // Set the icon
    Application application = Application.getApplication();
    Image image = Toolkit.getDefaultToolkit().getImage("icon.png");
    application.setDockIconImage(image);
    
    // Start the application  
    new JDec();
  
  }

    /* CONSTRUCTOR */

  /**
   * Construct and display the GUI.
   **/
  public JDec() {

      /* Clear temporary files */

    Automaton.clearTemporaryFiles();

      /* Create message to dislay when there are no tabs */

    noTabsMessage = new JLabel("You do not have any tabs open.");
    noTabsMessage.setHorizontalAlignment(JLabel.CENTER);
    noTabsMessage.setVerticalAlignment(JLabel.CENTER);

      /* Create tabbed pane and add a tab to it */

    tabbedPane = new JTabbedPane();
    tabbedPane.setFocusable(false);
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        updateComponentsWhichRequireAutomaton();  
      }
    });
    createTab(true, Automaton.Type.AUTOMATON);
    add(tabbedPane);

      /* Add menu */

    addMenu();
    updateComponentsWhichRequireAutomaton();

      /* Finish setting up */

    setGUIproperties();
    loadCurrentDirectory();
    TEMPORARY_DIRECTORY.mkdirs();
    promptBeforeExit();
    cleanupBeforeProgramQuits();

  }

    /* SETUP METHODS */

  /**
   * Adds the menu system to the application.
   **/
  private void addMenu() {

    JMenuBar menuBar = new JMenuBar();

    // File menu
    menuBar.add(createMenu("File",
      "New Tab->New Automaton,New U-Structure,New Pruned U-Structure,New Crush",
      "Open",
      "Save As...[TAB]",
      "Refresh Tab[TAB]",
      null,
      "Clear[TAB]",
      "Close Tab[TAB]",
      null,
      "View in Browser[AUTOMATON]",
      null,
      "Quit"
    ));

    // Standard operations menu
    menuBar.add(createMenu("Standard Operations",
      "Accessible[AUTOMATON]",
      "Co-Accessible[BASIC_AUTOMATON]",
      "Trim[BASIC_AUTOMATON]",
      "Complement[AUTOMATON]",
      null,
      "Intersection[BASIC_AUTOMATON]",
      "Union[BASIC_AUTOMATON]"
    ));

    // Special operations menu
    menuBar.add(createMenu("Special Operations",
      "Synchronized Composition[BASIC_AUTOMATON]",
      null,
      "Add Communications[U_STRUCTURE]",
      "Feasible Protocols->Generate All[U_STRUCTURE],Make Protocol Feasible[U_STRUCTURE],Find Smallest[U_STRUCTURE]",
      "Crush[ANY_U_STRUCTURE]"
    ));

    // Quantitative communication menu
    menuBar.add(createMenu("Quantitative Communication",
      "Nash[U_STRUCTURE]"
    ));

    // Properties menu
    menuBar.add(createMenu("Properties",
      "Test Observability[BASIC_AUTOMATON]",
      "Test Controllability[BASIC_AUTOMATON]",
      "Shapley Values[ANY_U_STRUCTURE]"
    ));
    
    // Generate menu
    menuBar.add(createMenu("Generate",
      "Random Automaton"
    ));

    this.setJMenuBar(menuBar);

  }

  /**
   * Helper method to help make the code look cleaner for menu creation.
   * @param menuTitle The title of the menu
   * @param strings   The list of menu items (where null is recognized as a separator)
   * @return          The created menu
   **/
  private JMenu createMenu(String menuTitle, String... strings) {

    JMenu menu = new JMenu(menuTitle);

    for (String str : strings) {

      // Add separator
      if (str == null)
        menu.addSeparator();

      // Add sub-menu with its menu items
      else if (str.contains("->")) {

        String[] parts = str.split("->");
        JMenu subMenu = new JMenu(parts[0]);

        for (String str2 : parts[1].split(","))
          addMenuItem(subMenu, str2);

        subMenu.addActionListener(this);
        menu.add(subMenu);

      // Add menu item
      } else
        addMenuItem(menu, str);

    }

    return menu;

  }

  /**
   * Simple helper method to add a menu item to a menu, and to store the item in the proper lists (if applicable).
   * @param menu  The menu in which the menu item is being added
   * @param str   The title of the menu item (special identifiers such as '[TAB]' indicate which lists the item gets stored in)
   **/
  private void addMenuItem(JMenu menu, String str) {

    // Check to see if this menu item requires a tab
    boolean requiresTab = str.contains("[TAB]");
    if (requiresTab)
      str = str.replace("[TAB]", "");

    // Check to see if this menu item requires any type of automaton
    boolean requiresAnyAutomaton = str.contains("[AUTOMATON]");
    if (requiresAnyAutomaton)
      str = str.replace("[AUTOMATON]", "");

    // Check to see if this menu item requires a basic automaton (so not a U-Structure)
    boolean requiresBasicAutomaton = str.contains("[BASIC_AUTOMATON]");
    if (requiresBasicAutomaton)
      str = str.replace("[BASIC_AUTOMATON]", "");

    // Check to see if this menu item requires a U-Structure
    boolean requiresUStructure = str.contains("[U_STRUCTURE]");
    if (requiresUStructure)
      str = str.replace("[U_STRUCTURE]", "");

    // Check to see if this menu item requires a pruned U-Structure
    boolean requiresPrunedUStructure = str.contains("[PRUNED_U_STRUCTURE]");
    if (requiresPrunedUStructure)
      str = str.replace("[PRUNED_U_STRUCTURE]", "");

    // Check to see if this menu item requires a U-Structure or a pruned U-Structure
    boolean requiresAnyUStructure = str.contains("[ANY_U_STRUCTURE]");
    if (requiresAnyUStructure)
      str = str.replace("[ANY_U_STRUCTURE]", "");

    // Create menu item object
    JMenuItem menuItem = new JMenuItem(str);
    menuItem.addActionListener(this);

    int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    // Add the appropriate accelerator
    switch (str) {

      case "Open":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKey));
        break;

      case "Close Tab":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutKey));
        break;

      case "Save As...":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey));
        break;

      case "View in Browser":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutKey));
        break;

      case "Synchronized Composition":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, shortcutKey));
        break;

      case "Add Communications":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey | InputEvent.SHIFT_DOWN_MASK));
        break;

      case "Nash":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKey));
        break;

      case "Random Automaton":
        menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutKey));
        break;

    }


    // Add menu items into the appropriate lists
    if (requiresTab)
      componentsWhichRequireTab.add(menuItem);
    if (requiresAnyAutomaton)
      componentsWhichRequireAnyAutomaton.add(menuItem);
    if (requiresBasicAutomaton)
      componentsWhichRequireBasicAutomaton.add(menuItem);
    if (requiresUStructure)
      componentsWhichRequireUStructure.add(menuItem);
    if (requiresPrunedUStructure)
      componentsWhichRequirePrunedUStructure.add(menuItem);
    if (requiresAnyUStructure)
      componentsWhichRequireAnyUStructure.add(menuItem);
    
    // Add the item to the menu
    menu.add(menuItem);

  }

  /**
   * Make a new thread that monitors when the program quits, deleting all temporary files.
   **/
  private static void cleanupBeforeProgramQuits() {
  
    // The code within this will execute when the program exits for good
    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() { 
      @Override public void run() {

        for (String file : TEMPORARY_DIRECTORY.list())
          new File(TEMPORARY_DIRECTORY, file).delete();

        TEMPORARY_DIRECTORY.delete();

      }
    }));
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
          if (tabs.get(i).hasUnsavedInformation())
            unSavedInformation = true;
        
        if (!unSavedInformation)
          System.exit(0);

          /* Prompt user to save */

        if (askForConfirmation("Unsaved Information", "Are you sure you want to exit? Any unsaved information will be lost."))
          System.exit(0);

      }
    });

  }

  /**
   * Set some default GUI Properties.
   **/
  private void setGUIproperties() {

    // Make the application use the entire screen by default
    setExtendedState(JFrame.MAXIMIZED_BOTH);
    
    // Ensure our application will be closed when the user presses the "X"
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    // Update title
    setTitle("JDec v1.0 - A Java application for Decentralized Control");

    // Show screen
    setVisible(true);

    // Make tooltips appear instantly when the user hovers over them
    ToolTipManager.sharedInstance().setInitialDelay(0);

    // Makes tooltips stay longer (default is only 4 seconds)
    ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

  }

    /* ACTIONS */

  /**
   * This method handles all of the actions triggered when the user interacts with the main menu.
   * @param event The triggered event
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

      case "New Automaton":

        createTab(true, Automaton.Type.AUTOMATON);
        break;

      case "New U-Structure":

        createTab(true, Automaton.Type.U_STRUCTURE);
        break;

      case "New Pruned U-Structure":

        createTab(true, Automaton.Type.PRUNED_U_STRUCTURE);
        break;

      case "New Crush":

        createTab(true, Automaton.Type.CRUSH);
        break;

      case "Save As...":

        // Prompt user to save Automaton to the specified file
        if (saveFile("Choose .hdr File") != null) {
          tab.updateTabTitle();
          if (tab.automaton != null)
            tab.automaton = tab.automaton.duplicate(tab.headerFile, tab.bodyFile);
        }
          
        break;

      case "Open":

        // Prompt user to select Automaton from file (stop if they did not pick a file), placing it in a new tab
        if (selectFile("Select Automaton", -1) == null)
          break;

        index = tabbedPane.getSelectedIndex(); // Index has changed since a new tab was created
        tab = tabs.get(index);
        tab.updateTabTitle();
    
      case "Refresh Tab":

        refresh(index);
        break;

      case "View in Browser":

        viewInBrowser();
        break;

      case "Close Tab":

        closeCurrentTab();
        break;

      case "Quit":

        dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
        break;

        /* AUTOMATA OPERATIONS */

      case "Accessible":

        String fileName = getTemporaryFileName();
        File headerFile = new File(fileName + ".hdr");
        File bodyFile = new File(fileName + ".bdy");
        Automaton automaton = tab.automaton.accessible(headerFile, bodyFile);

        // Create new tab for the accessible automaton
        if (automaton == null) {
          temporaryFileIndex--; // We did not need this temporary file after all, so we can re-use it
          displayErrorMessage("Accessible Operation Failed", "Please specify a starting state.");
        } else
          createTab(automaton);
        break;

      case "Co-Accessible":

        fileName = getTemporaryFileName();
        headerFile = new File(fileName + ".hdr");
        bodyFile = new File(fileName + ".bdy");

        // Create new tab for the co-accessible automaton
        createTab(tab.automaton.coaccessible(headerFile, bodyFile));
        break;

      case "Trim":

        fileName = getTemporaryFileName();
        headerFile = new File(fileName + ".hdr");
        bodyFile = new File(fileName + ".bdy");
        
        automaton = tab.automaton.trim(headerFile, bodyFile);

        // Create new tab for the trim automaton
        if (automaton == null) {
          temporaryFileIndex--; // We did not need this temporary file after all, so we can re-use it
          displayErrorMessage("Trim Operation Failed", "Please specify a starting state.");
        } else
          createTab(automaton);
        break;

      case "Complement":

        fileName = getTemporaryFileName();
        headerFile = new File(fileName + ".hdr");
        bodyFile = new File(fileName + ".bdy");
        try {
          // Create new tab with the complement
          createTab(tab.automaton.complement(headerFile, bodyFile));
        } catch(OperationFailedException e) {
          temporaryFileIndex--; // We did not need this temporary file after all, so we can re-use it
          displayErrorMessage("Complement Operation Failed", "There already exists a dump state, so the complement could not be taken again.");
        }
        break;

      case "Intersection":

        // Allow user to pick other automaton
        int otherIndex = pickAutomaton("Which automaton would you like to take the intersection with?", index);
        if (otherIndex == -1)
          break;
        Automaton otherAutomaton = tabs.get(otherIndex).automaton;

        // Set up files
        fileName = getTemporaryFileName();
        headerFile = new File(fileName + ".hdr");
        bodyFile = new File(fileName + ".bdy");

        try {
          // Create new tab with the intersection
          createTab(Automaton.intersection(tab.automaton, otherAutomaton, headerFile, bodyFile));
        } catch(IncompatibleAutomataException e) {
          temporaryFileIndex--; // We did not need this temporary file after all, so we can re-use it
          displayErrorMessage("Intersection Operation Failed", "Please ensure that both automata have the same number of controllers and that there are no incompatible events (meaning that events share the same name but have different properties).");
        }
        
        break;

      case "Union":

        // Allow user to pick other automaton
        otherIndex = pickAutomaton("Which automaton would you like to take the union with?", index);
        if (otherIndex == -1)
          break;
        otherAutomaton = tabs.get(otherIndex).automaton;

        fileName = getTemporaryFileName();
        headerFile = new File(fileName + ".hdr");
        bodyFile = new File(fileName + ".bdy");

        try {
          // Create new tab with the union
          createTab(Automaton.union(tab.automaton, otherAutomaton, headerFile, bodyFile));
        } catch(IncompatibleAutomataException e) {
          temporaryFileIndex--; // We did not need this temporary file after all, so we can re-use it
          displayErrorMessage("Union Operation Failed", "Please ensure that both automata have the same number of controllers and that there are no incompatible events (meaning that events share the same name but have different properties).");
        }

        break;

      case "Synchronized Composition":

        fileName = getTemporaryFileName();
        headerFile = new File(fileName + ".hdr");
        bodyFile = new File(fileName + ".bdy");

        // Create new tab with the U-structure generated by synchronized composition
        automaton = tab.automaton.synchronizedComposition(headerFile, bodyFile);
        if (automaton == null) {
          temporaryFileIndex--; // We did not need this temporary file after all, so we can re-use it
          displayErrorMessage("Synchronized Composition Operation Failed", "Please ensure that you specified a starting state.");
        } else
          createTab(automaton);

        break;

      case "Crush":

        UStructure uStructure = ((UStructure) tab.automaton);

        if (tab.type == Automaton.Type.U_STRUCTURE) {
          if (uStructure.getSizeOfPotentialAndNashCommunications() > 0) {
            displayErrorMessage("Crush Operation Aborted", "You must choose a communication protocol before taking the Crush.");
            break;
          }
        }


        if (uStructure.hasViolations()) {
          displayErrorMessage("Crush Operation Aborted", "This structure contains one or more violations.");
          break;
        }

        new NashInfoForCrushPrompt(this, tab, "Cost and Probability Values", "Specify costs and probabilities for each communication.");

        break;

      case "Add Communications": 

        uStructure = ((UStructure) tab.automaton);

        // // Display error message if there was not enough controllers
        // if (uStructure.getNumberOfControllers() == 1)
        //   displayErrorMessage("Not Enough Controllers", "There must be more than 1 controller in order for a communication to take place."); 

        // Display warning message, and abort the operation if requested
        if (uStructure.getSizeOfPotentialAndNashCommunications() > 0)
          if (!askForConfirmation("Communications Already Exist", "This U-Structure appears to already have had communications added. Are you sure you want to proceed? WARNING: This may result in duplicate communications."))  
            break;

        fileName = getTemporaryFileName();
        headerFile = new File(fileName + ".hdr");
        bodyFile = new File(fileName + ".bdy");

        // Create a copy of the current automaton with all communications added and potential communications marked
        createTab(uStructure.addCommunications(headerFile, bodyFile));

        break;

      case "Generate All":

        uStructure = ((UStructure) tab.automaton);

        if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
          displayErrorMessage("Operation Failed", "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
        else
          new GenerateFeasibleProtocolsPrompt(this, uStructure, "Generate All Feasible Protocols", " Specify whether or not a controller is allowed to send to or receive from a certain controller: ");
        break;

      case "Make Protocol Feasible":

        uStructure = ((UStructure) tab.automaton);

        if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
          displayErrorMessage("Operation Failed", "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
        else
          new MakeProtocolFeasiblePrompt(this, uStructure);
        break;

      case "Find Smallest":

        uStructure = ((UStructure) tab.automaton);

        if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
          displayErrorMessage("Operation Failed", "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
        else
          new FeasibleProtocolOutput(this, uStructure, uStructure.generateSmallestFeasibleProtocols(uStructure.getPotentialAndNashCommunications()), "Smallest Feasible Protocols", " Protocol(s) with the fewest number of communications: ");
        break;

      case "Nash":

        uStructure = ((UStructure) tab.automaton);

        if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
          displayErrorMessage("Operation Failed", "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
        else
          new NashInfoForNashEquilibriaPrompt(this, tab, "Cost and Probability Values", "Specify costs and probabilities for each communication.");
        break;

      case "Test Observability":

        if (tab.automaton.testObservability())
          displayMessage("Passed Test", "The system is observable.", JOptionPane.INFORMATION_MESSAGE);
        else
          displayMessage("Failed Test", "The system is not observable.", JOptionPane.INFORMATION_MESSAGE);
        break;

      case "Test Controllability":

        if (tab.automaton.testControllability())
          displayMessage("Passed Test", "The system is controllable.", JOptionPane.INFORMATION_MESSAGE);
        else
          displayMessage("Failed Test", "The system is not controllable.", JOptionPane.INFORMATION_MESSAGE);
        break;

      case "Shapley Values":

        uStructure = (UStructure) tab.automaton;
        uStructure.findShapleyValues();

        break;

      case "Random Automaton":

        new RandomAutomatonPrompt(this);
        break;
      
    }

    updateComponentsWhichRequireAutomaton();

  }

  /**
   * Create an empty tab.
   * @param assignTemporaryFiles  Whether or not temporary files should be assigned to the tab
   **/
  private void createTab(boolean assignTemporaryFiles, Automaton.Type type) {

      /* Add tab */

    int index = tabbedPane.getTabCount();

    AutomatonTab tab = new AutomatonTab(index, type);
    tabs.add(tab);

    tabbedPane.addTab(null, null, tab, "");
    tabbedPane.setSelectedIndex(index);

    if (assignTemporaryFiles) {
      String fileName = getTemporaryFileName();
      tab.headerFile = new File(fileName + ".hdr");
      tab.bodyFile = new File(fileName + ".bdy");
      tab.updateTabTitle();
    }

      /* Re-activate appropriate components if this is the first tab */

    if (tabs.size() == 1)
      for (Component component : componentsWhichRequireTab)
        component.setEnabled(true);

      /* Refresh components which require a specific type of automaton */
    
    updateComponentsWhichRequireAutomaton();

      /* Remove message indicating no tabs are open, if applicable */

    if (tabbedPane.getTabCount() == 1) {
      remove(noTabsMessage);
      add(tabbedPane);
      repaint();
    }

  }

  /**
   * Create a tab, and load in an automaton.
   * @param automaton   The automaton object
   **/
  public void createTab(Automaton automaton) {

      /* Create new tab */

    createTab(false, Automaton.Type.getType(automaton.getClass()));
    int newIndex = tabbedPane.getTabCount() - 1;

      /* Set tab values */

    AutomatonTab tab = tabs.get(newIndex);
    tab.headerFile   = automaton.getHeaderFile();
    tab.bodyFile     = automaton.getBodyFile();
    tab.automaton    = automaton;
    tab.refreshGUI();
    tab.setSaved(true);

      /* Generate an image (unless it's quite large) */

    if (tab.automaton.getNumberOfStates() <= 100) {
      generateImage();
      tab.generateImageButton.setEnabled(false);
    } else
      tab.generateImageButton.setEnabled(true);

  }

  /**
   * Close the current tab, displaying a warning message if the current tab is unsaved.
   **/
  private void closeCurrentTab() {

      /* Get index of the currently selected tab */

    int index = tabbedPane.getSelectedIndex();

      /* Check for unsaved information */

    AutomatonTab tab = tabs.get(index);
    if (tab.hasUnsavedInformation()) {

      // Create message to display in pop-up
      String message = "Are you sure you want to close this tab? ";
      if (tab.usingTemporaryFiles())
        message += "This automaton is only being saved temporarily. To save this automaton\npermanently, ensure that you have generated the automaton, then select 'Save As...' from the 'File' menu.";
      else
        message += "Any un-generated GUI input code will be lost.";

      // Confirm that the user wants to proceed
      if (!askForConfirmation("Unsaved Information", message))
        return;

    }

      /* Remove tab */

    tabbedPane.remove(index);
    tabs.remove(index);

      /* Re-number tabs */

    for (int i = 0; i < tabs.size(); i++)
      tabs.get(i).index = i;

      /* De-activate appropriate components if there are no tabs left */

    if (tabs.size() == 0)
      for (Component component : componentsWhichRequireTab)
        component.setEnabled(false);

      /* Show message indicating no tabs are open, if applicable */

    if (tabbedPane.getTabCount() == 0) {
      remove(tabbedPane);
      add(noTabsMessage);
    }
    
    repaint();

  }


  /**
   * View the .SVG image in the user's default browser, if possible.
   **/
  private void viewInBrowser() {

    int index = tabbedPane.getSelectedIndex();
    AutomatonTab tab = tabs.get(index);
    String fileName = currentDirectory + "/" + removeExtension(tab.headerFile.getName()) + ".svg";

    boolean successful = false;

    // Try to load image from file
    try {
      if (Desktop.isDesktopSupported()) {
        Desktop.getDesktop().browse(new URI("file://" + fileName));
        successful = true;
      }
    } catch (IOException | URISyntaxException e) { }
    
    // Display the proper error message
    if (!successful) {
      if (new File(fileName).exists())
        displayErrorMessage("Unable To Open", "The .SVG file could not be opened in your browser. You can find the file here: '" + fileName + "'.");
      else
        displayErrorMessage("File Not Found", "The .SVG file could not be found. Please ensure that you have generated the image.");
    }

  }

  /**
   * Generate an automaton using the entered GUI input code.
   **/
  private void generateAutomatonButtonPressed() {

    // Get the current tab
    int index = tabbedPane.getSelectedIndex();
    AutomatonTab tab = tabs.get(index);

    // Create automaton from input code
    switch (tab.type) {

      case AUTOMATON:

        int nControllers = (Integer) tabs.get(tabbedPane.getSelectedIndex()).controllerInput.getValue();
        tab.automaton = AutomatonGenerator.generateFromGUICode(
          new Automaton(tab.headerFile, tab.bodyFile, nControllers),
          tab.eventInput.getText(),
          tab.stateInput.getText(),
          tab.transitionInput.getText(),
          tab.eventInput,
          tab.stateInput,
          tab.transitionInput,
          this
        );
        break;

      case U_STRUCTURE:

        int nControllersBeforeUStructure = (Integer) tabs.get(tabbedPane.getSelectedIndex()).controllerInput.getValue();
        tab.automaton = AutomatonGenerator.generateFromGUICode(
          new UStructure(tab.headerFile, tab.bodyFile, nControllersBeforeUStructure),
          tab.eventInput.getText(),
          tab.stateInput.getText(),
          tab.transitionInput.getText(),
          tab.eventInput,
          tab.stateInput,
          tab.transitionInput,
          this
        );
        break;

      case PRUNED_U_STRUCTURE:

        nControllersBeforeUStructure = (Integer) tabs.get(tabbedPane.getSelectedIndex()).controllerInput.getValue();
        tab.automaton = AutomatonGenerator.generateFromGUICode(
          new PrunedUStructure(tab.headerFile, tab.bodyFile, nControllersBeforeUStructure),
          tab.eventInput.getText(),
          tab.stateInput.getText(),
          tab.transitionInput.getText(),
          tab.eventInput,
          tab.stateInput,
          tab.transitionInput,
          this
        );
        break;

      case CRUSH:

        nControllersBeforeUStructure = (Integer) tabs.get(tabbedPane.getSelectedIndex()).controllerInput.getValue();
        tab.automaton = AutomatonGenerator.generateFromGUICode(
          new Crush(tab.headerFile, tab.bodyFile, nControllersBeforeUStructure),
          tab.eventInput.getText(),
          tab.stateInput.getText(),
          tab.transitionInput.getText(),
          tab.eventInput,
          tab.stateInput,
          tab.transitionInput,
          this
        );
        break;

      default:

        // NOTE: The following error should never appear to the user, and it indicates a bug in the program
        displayErrorMessage("Crucial Error", "Unable to generate automaton from GUI input code due to unrecognized automaton type.");
        return;

    }

    // Abort if the automaton was unable to be generated (due to errors parsing the input code)
    if (tab.automaton == null)
      return;

    tab.setSaved(true);

    // Generate an image (unless it's quite large)
    if (tab.automaton.getNumberOfStates() <= 100) {
      generateImage();
      tab.generateImageButton.setEnabled(false);
    } else
      tab.generateImageButton.setEnabled(true);

    // Refresh GUI
    updateComponentsWhichRequireAutomaton(); 

  }

  /**
   * Generate an image of the graph, saving both .PNG and .SVG to file, and displaying the .PNG
   * on the screen.
   **/
  private void generateImage() {

    // Get the current tab
    AutomatonTab tab = tabs.get(tabbedPane.getSelectedIndex());

    // Create destination file name (excluding extension)
    String destinationFileName = currentDirectory + "/" + removeExtension(tab.headerFile.getName());

    try {

      // Set the image blank if there were no states entered
      if (tab.automaton == null)
        tab.canvas.setImage(null);

      // Try to create graph image, displaying it on the screen
      else if (tab.automaton.generateImage(destinationFileName)) {
        tab.canvas.setImage(tab.automaton.loadImageFromFile(destinationFileName + ".png"));
        tab.svgFile = new File(destinationFileName + ".svg");
      }

      // Display error message
      else
        displayErrorMessage("Error", "Something went wrong while trying to generate and display the image. NOTE: It may be the case that you do not have X11 installed.");
    
    } catch (SegmentationFaultException e) {
      displayErrorMessage("GraphViz Failed", "GraphViz encountered a segmentation fault, so the .PNG image was unable to be generated.\nFortunately, the .SVG image was likely able to be generated. Click 'View Image in Browser' to see it.");

    } catch (MissingDependencyException e) {
      displayErrorMessage("Missing Dependency", "Please ensure that GraphViz is installed, with its directory added to the PATH environment variable.");
    
    } catch (MissingOrCorruptBodyFileException e) {
      displayErrorMessage("Corrupt or Missing File", "Please ensure that the .bdy file associated with this automaton is not corrupt or missing.");
    }

  }

  /**
   * Generate a random automaton with the specified properties.
   * @param nEvents                 The number of events to be generated in the automaton
   * @param nStates                 The number of states to be generated in the automaton
   * @param minTransitionsPerState  The minimum number of outgoing transitions per state
   * @param maxTransitionsPerState  The maximum number of outgoing transitions per state
   * @param nControllers            The number of controllers in the automaton
   * @param nBadTransitions         The number of bad transition in the automaton
   * @param progressBar             The progress bar to be updated during the generation process
   **/
  public void generateRandomAutomaton(int nEvents, long nStates, int minTransitionsPerState, int maxTransitionsPerState, int nControllers, int nBadTransitions, JProgressBar progressBar) {

    // Get a temporary file name
    String fileName = getTemporaryFileName();

    // Generate random automaton
    Automaton automaton = AutomatonGenerator.generateRandom(
      new File(fileName + ".hdr"),
      new File(fileName + ".bdy"),
      nEvents,
      nStates,
      minTransitionsPerState,
      maxTransitionsPerState,
      nControllers,
      nBadTransitions,
      progressBar
    );

    // Place the generated automaton in a new tab
    createTab(automaton);

  }

  /**
   * Load automaton from file, filling the input fields with its data.
   * NOTE: A loading bar is displayed to keep track of the progress.
   * @param index The tab's index
   **/
  private void refresh(final int index) {
    
    // This process is started in a new thread so that the progress bar can be refreshed
    new Thread() {
      @Override public void run() {

      final ProgressBarPopup progressBarPopup = new ProgressBarPopup(JDec.this, "Loading...", 3);
      AutomatonTab tab = tabs.get(index);
      
      // Instantiate automaton
      switch (Automaton.Type.getType(tab.headerFile)) {

        case AUTOMATON:
          tab.automaton = new Automaton(tab.headerFile, tab.bodyFile, false);
          break;

        case U_STRUCTURE:
          tab.automaton = new UStructure(tab.headerFile, tab.bodyFile);
          break;

        case PRUNED_U_STRUCTURE:
          tab.automaton = new PrunedUStructure(tab.headerFile, tab.bodyFile);
          break;

        case CRUSH:
          tab.automaton = new Crush(tab.headerFile, tab.bodyFile);
          break;

        default:
          displayErrorMessage("Corrupt File", "Please ensure that the .hdr file associated with this automaton is not corrupt.");
          return;

      }

      progressBarPopup.updateProgressBar(1);

      tab.refreshGUI();

      progressBarPopup.updateProgressBar(2);

      // Generate an image (unless it's quite large)
      if (tab.automaton.getNumberOfStates() <= 100) {
        generateImage();
        tab.generateImageButton.setEnabled(false);
      } else
        tab.generateImageButton.setEnabled(true);

      tab.setSaved(true);

      progressBarPopup.updateProgressBar(3);

      EventQueue.invokeLater(new Runnable() {
          @Override public void run() {
            progressBarPopup.dispose();
          }
        });

      // Refresh components which require a specific type of automaton
      updateComponentsWhichRequireAutomaton();

    }}.start();

  }

    /* ENABLING/DISABLING COMPONENTS */

  /**
   * Update the components in the menu bar that require a specific type of automaton, by
   * enabling/disabling them as appropriate.
   **/
  public void updateComponentsWhichRequireAutomaton() {
    updateComponentsWhichRequireAnyAutomaton();
    updateComponentsWhichRequireBasicAutomaton();
    updateComponentsWhichRequireUStructure();
    updateComponentsWhichRequirePrunedUStructure();
    updateComponentsWhichRequireAnyUStructure();
  }

  /**
   * Enable/disable components that require any type of automaton (including U-Structure, Crush, etc.).
   **/
  private void updateComponentsWhichRequireAnyAutomaton() {

    int index = tabbedPane.getSelectedIndex();

    // Determine whether the components should be enabled or disabled
    boolean enabled = (
      index >= 0
      && tabs.get(index).automaton != null
    );
    
    // Enabled/disable all components in the list
    for (Component component : componentsWhichRequireAnyAutomaton)
      component.setEnabled(enabled);

  }

  /**
   * Enable/disable components that require a basic automaton.
   **/
  private void updateComponentsWhichRequireBasicAutomaton() {

    int index = tabbedPane.getSelectedIndex();

    // Determine whether the components should be enabled or disabled
    boolean enabled = (
      index >= 0
      && tabs.get(index).type == Automaton.Type.AUTOMATON
      && tabs.get(index).automaton != null
    );
    
    // Enabled/disable all components in the list
    for (Component component : componentsWhichRequireBasicAutomaton)
      component.setEnabled(enabled);

  }

  /**
   * Enable/disable components that require a U-Structure.
   **/
  private void updateComponentsWhichRequireUStructure() {

    int index = tabbedPane.getSelectedIndex();

    // Determine whether the components should be enabled or disabled
    boolean enabled = (
      index >= 0
      && tabs.get(index).type == Automaton.Type.U_STRUCTURE
      && tabs.get(index).automaton != null
    );
    
    // Enabled/disable all components in the list
    for (Component component : componentsWhichRequireUStructure)
      component.setEnabled(enabled);

  }

  /**
   * Enable/disable components that require a pruned U-Structure.
   **/
  private void updateComponentsWhichRequirePrunedUStructure() {

    int index = tabbedPane.getSelectedIndex();

    // Determine whether the components should be enabled or disabled
    boolean enabled = (
      index >= 0
      && tabs.get(index).type == Automaton.Type.PRUNED_U_STRUCTURE
      && tabs.get(index).automaton != null
    );
    
    // Enabled/disable all components in the list
    for (Component component : componentsWhichRequirePrunedUStructure)
      component.setEnabled(enabled);

  }

    /**
   * Enable/disable components that require a U-Structure or a pruned U-Structure.
   **/
  private void updateComponentsWhichRequireAnyUStructure() {

    int index = tabbedPane.getSelectedIndex();

    // Determine whether the components should be enabled or disabled
    boolean enabled = (
      index >= 0
      && (tabs.get(index).type == Automaton.Type.PRUNED_U_STRUCTURE || tabs.get(index).type == Automaton.Type.U_STRUCTURE)
      && tabs.get(index).automaton != null
    );
    
    // Enabled/disable all components in the list
    for (Component component : componentsWhichRequireAnyUStructure)
      component.setEnabled(enabled);

  }

    /* PROMPTS */

  /** 
   * Opens up a JFileChooser for the user to choose a file from their file system.
   * @param title   The title to put in the file chooser dialog box
   * @param index   The index of the tab we're selecting a file for (-1 indicates that a new tab will be created for it)
   * @return        The file, or null if the user did not choose anything
   **/
  private File selectFile(String title, int index) {

      /* Set up the file chooser */

    JFileChooser fileChooser = new JFileChooser() {
      @Override protected JDialog createDialog(Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(JDec.this);
        dialog.setModal(true);
        return dialog;
      }
    };

    fileChooser.setDialogTitle(title);

      /* Filter .hdr files */

    FileNameExtensionFilter filter = new FileNameExtensionFilter("Automaton files", "hdr");
    fileChooser.setFileFilter(filter);

      /* Begin at the most recently accessed directory */

    if (currentDirectory != null)
      fileChooser.setCurrentDirectory(currentDirectory);

      /* Prompt user to select a file */

    if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
      return null;

      /* Update files in the tab and update current directory */

    if (fileChooser.getSelectedFile() != null) {

      // Check to see if that file is already open
      for (AutomatonTab tab : tabs)
        if (tab.headerFile.equals(fileChooser.getSelectedFile())) {
          displayErrorMessage("File Already Open", "The specified file is already open in another tab.");
          return null;
        }

      // Get files
      File headerFile = fileChooser.getSelectedFile();
      File bodyFile = new File(headerFile.getParentFile() + "/" + removeExtension(headerFile.getName()) + ".bdy");
     
      // Create new tab (if requested)
      if (index == -1) {
        createTab(false, Automaton.Type.getType(headerFile));
        index = tabbedPane.getSelectedIndex();
      }
      AutomatonTab tab = tabs.get(index);
      
      // Update files
      tab.headerFile = headerFile;
      tab.bodyFile = bodyFile;

      // Update current directory
      currentDirectory = fileChooser.getSelectedFile().getParentFile();
      saveCurrentDirectory();

    }

    return fileChooser.getSelectedFile();
    
  }

  /**
   * Prompts the user to name and specify the filename that they wish to save the data to.
   * @param title The title to give the window
   * @return      The .hdr file to save the data to
   **/
  private File saveFile(String title) {

      /* Set up the file chooser */

    JFileChooser fileChooser = new JFileChooser() {
      @Override protected JDialog createDialog(Component parent) throws HeadlessException {
        JDialog dialog = super.createDialog(JDec.this);
        dialog.setModal(true);
        return dialog;
      }
    };

    fileChooser.setDialogTitle(title);

      /* Filter .hdr files */

    FileNameExtensionFilter filter = new FileNameExtensionFilter("Automaton files", "hdr");
    fileChooser.setFileFilter(filter);

      /* Begin at the most recently accessed directory */
    
    if (currentDirectory != null)
      fileChooser.setCurrentDirectory(currentDirectory);

      /* Prompt user to select a filename */

    int result = fileChooser.showSaveDialog(null);

      /* No file was selected */

    if (result != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null)
      return null;

      /* Add .hdr extension if the user didn't put it there */

    String name = fileChooser.getSelectedFile().getName();

    // Remove anything after the period
    if (name.indexOf(".") != -1)
      name = name.substring(0, name.indexOf("."));

    String prefix   = fileChooser.getSelectedFile().getParentFile() + "/" + name;
    File headerFile = new File(prefix + ".hdr");
    File bodyFile   = new File(prefix + ".bdy");
    File svgFile    = new File(prefix + ".svg");

    AutomatonTab currentTab = tabs.get(tabbedPane.getSelectedIndex());

      /* Check to see if that file is already open */

    for (AutomatonTab tab : tabs)
      if (tab.headerFile.equals(headerFile) && currentTab.index != tab.index) {
        displayErrorMessage("File Is Open", "The specified file is open in another tab. Please choose a different filename.");
        return null;
      }

      /* If it exists, copy the .SVG file to the new location */

    try {

      // Copy the file if it exists
      if (currentTab.svgFile != null && currentTab.svgFile.exists()) {
        Files.copy(currentTab.svgFile.toPath(), svgFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        currentTab.svgFile = svgFile;
      }

    } catch (IOException e) {

      // Allow the user to re-generate the image if there was nothing to copy
      currentTab.svgFile = null;
      currentTab.generateImageButton.setEnabled(true);

    }

      /* Update last file opened and update current directory */

    currentTab.headerFile = headerFile;
    currentTab.bodyFile   = bodyFile;

    currentDirectory = headerFile.getParentFile();
    saveCurrentDirectory();

    return headerFile;
    
  }

  /**
   * Allow the user to select an automaton that is currently open.
   * @param str         The message to display
   * @param indexToSkip The index of the automaton which should be omitted from the options
   * @return            The index of the selected automaton (or -1 if there was not an automaton selected)
   **/
  private int pickAutomaton(String str, int indexToSkip) {

      /* Create list of options */

    ArrayList<String> optionsList = new ArrayList<String>();

    for (int i = 0; i < tabbedPane.getTabCount(); i++) {

      AutomatonTab tab = tabs.get(i);
        
      // Skip automaton
      if (i == indexToSkip || tab.automaton == null)
        continue;

      // Add automaton to list of options
      optionsList.add(removeExtension(tab.headerFile.getAbsolutePath()));

    }

    String[] options = optionsList.toArray(new String[optionsList.size()]);

      /* Show error message if there is no second automaton to pick from */

    if (options.length == 0) {
      displayErrorMessage("Operation Failed", "This operation requires two generated automata.");
      return -1;
    }
    
      /* Display prompt to user */
    
    String choice = (String) JOptionPane.showInputDialog(
        this,
        str,
        "Choose Automaton",
        JOptionPane.PLAIN_MESSAGE,
        null,
        options,
        options[0]
      );

      /* Return index of chosen automaton */

    for (int i = 0; i < tabbedPane.getTabCount(); i++)
      if (tabs.get(i).headerFile != null && removeExtension(tabs.get(i).headerFile.getAbsolutePath()).equals(choice))
        return i;

    return -1;

  }

  /**
   * Allow the user to select a controller in the current automaton.
   * @param str   The message to display
   * @return      The index of the selected controller (or -1 if there was not a controller selected)
   **/
  public int pickController(String str) {

    UStructure uStructure = (UStructure) tabs.get(tabbedPane.getSelectedIndex()).automaton;

      /* Create list of options */

    ArrayList<String> optionsList = new ArrayList<String>();
    for (int i = 1; i <= uStructure.getNumberOfControllers(); i++)
      optionsList.add(String.valueOf(i));
    String[] options = optionsList.toArray(new String[optionsList.size()]);

      /* Display prompt to user */
    
    String choice = (String) JOptionPane.showInputDialog(
        this,
        str,
        "Choose Controller",
        JOptionPane.PLAIN_MESSAGE,
        null,
        options,
        options[0]
      );

      /* Return index of chosen controller */

    for (int i = 1; i <= options.length; i++)
      if (choice != null && choice.equals(String.valueOf(i)))
        return i;

    return -1;

  }

  /**
   * Given a title and a message, ask the user for confirmation, returning the result.
   * @param title   The title to display on the dialog box
   * @param message The message to display in the dialog box
   * @return        True if the user selected "Yes", false if the user selected "No"
   **/
  private boolean askForConfirmation(String title, String message) {

    String buttons[] = { "Yes", "No" };
    
    int promptResult = JOptionPane.showOptionDialog(
      this,
      message,
      title,
      JOptionPane.DEFAULT_OPTION,
      JOptionPane.WARNING_MESSAGE,
      null,
      buttons,
      buttons[1]
    );

    return promptResult == 0;
  
  }

    /* HELPER METHODS */

  /**
   * Get the tooltip text for the specified input box and automaton type.
   * @param inputBox      A string representing the relevant input box
   *                      NOTE: This is the same as the tag used in the XML file
   * @param automatonType The enum value associated with the automaton type
   * @return              The HTML formatted tool-tip text, or null if it could not be found
   **/
  private static String getTooltipText(String inputBox, Automaton.Type automatonType) {

    try {

      // Find the specified element
      Node node1 = tooltipDocument.getElementsByTagName("INPUT").item(0);
      Element element1 = (Element) node1;
      Node node2 = element1.getElementsByTagName(inputBox).item(0);
      Element element2 = (Element) node2;
      Node node3 = element2.getElementsByTagName(automatonType.name()).item(0);
      Element element3 = (Element) node3;

      // Generate a string of this element and its descendents, including tags
      StringWriter buffer = new StringWriter();
      Transformer xform = TransformerFactory.newInstance().newTransformer();
      xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
      xform.transform(new DOMSource(element3), new StreamResult(buffer));

      // Remove the outer tag, trim it, then return it
      String startTag = "<" + automatonType.name() + ">";
      String endTag = "</" + automatonType.name() + ">";
      return buffer.toString().replace(startTag, "").replace(endTag, "").trim();

    } catch (NullPointerException | TransformerException e) { }

    return null;

  }

  /**
   * Display an error message in a modal dialog which will stay on top of the GUI at all times.
   * @param title   The title to display in the dialog box
   * @param message The message to display in the dialog box
   **/
  public void displayErrorMessage(String title, String message) {
    
    displayMessage(title, message, JOptionPane.ERROR_MESSAGE);

  }

  /**
   * Display a message of the specified type in a modal dialog which will stay on top of the GUI
   * at all times.
   * @param title       The title to display in the dialog box
   * @param message     The message to display in the dialog box
   * @param messageType The type of message (using JOptionPane constants)
   **/
  public void displayMessage(String title, String message, int messageType) {
    
    JOptionPane op = new JOptionPane(message, messageType);
    JDialog dialog = op.createDialog(title);
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);

  }

  /**
   * Removes the last 4 characters of the string, which is used to trim either '.hdr' or '.bdy' off the end.
   * @param str The string to be trimmed
   * @return    The trimmed string
   **/
  private String removeExtension(String str) {
    return str.substring(0, str.length() - 4);  
  }

  /**
   * Get a temporary filename (prefixed by 'untitled') which is stored in the temporary directory.
   * @return  The temporary filename, which does not contain an extension (since it will be used for
   *          both the .bdy and .hdr files)
   **/
  public String getTemporaryFileName() {
    return TEMPORARY_DIRECTORY.getAbsolutePath() + "/untitled" + temporaryFileIndex++;
  }

  /**
   * Load the current directory from file (so that the current directory is maintained even after the
   * program has been closed).
   **/
  private void loadCurrentDirectory() {

    try {

      Scanner sc = new Scanner(new File(GUI_DATA_FILE_NAME));

      if (sc.hasNextLine())
        currentDirectory = new File(sc.nextLine());

    } catch (FileNotFoundException e) {
      // Simply ignore the error, since it just means that there was no pre-existing file found
    }

  }

  /**
   * Saves the current directory to file (so that the current directory is maintained even after the
   * program has been closed).
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

    /* INNER CLASSES */

  /**
   * Private class to maintain a canvas on which a BufferedImage can be drawn.
   **/
  private class Canvas extends JPanel {

    private BufferedImage image;

    /**
     * Construct and display a canvas, initially with a grey background.
     **/
    public Canvas() {

      setBackground(Color.LIGHT_GRAY);
      setVisible(true);

    }

    /**
     * Update the image in the canvas.
     * @param image The new image to be displayed in the canvas (null indicates no image)
     **/
    public void setImage(BufferedImage image) {

      this.image = image;
      this.repaint();

    }

    /**
     * Updates the canvas, drawing the image (or blank canvas) in the center.
     * @param graphics Graphics object
     **/
    @Override protected void paintComponent(Graphics graphics) {

      super.paintComponent(graphics);

        /* Draw image */
      
      if (image != null) {

        double ratio = Math.max((double) image.getWidth() / (double) getWidth(), (double) image.getHeight() / (double) getHeight());
        int width  = (int) (image.getWidth()  / ratio);
        int height = (int) (image.getHeight() / ratio);
        int horizontalPadding = Math.max(0, (getWidth()  - width)  / 2);
        int verticalPadding   = Math.max(0, (getHeight() - height) / 2);
        graphics.drawImage(image, horizontalPadding, verticalPadding, width, height, null);

      }

    }

  } // Canvas class

  /**
   * Class used to maintain a tab inside the JDec object.
   * NOTE: Since this is an inner class (and since there are a lot of instance variables), I chose to
   * keep most variables public, as opposed to have a multitude of getters and setters.
   **/
  class AutomatonTab extends Container {

      /* Instance variables */

    // GUI elements
    public JSplitPane splitPane;
    public JTextPane eventInput, stateInput, transitionInput;
    public JSpinner controllerInput;
    public JButton generateAutomatonButton, generateImageButton, viewImageInBrowserButton;
    public Canvas canvas = null;

    // Automaton properties
    public Automaton automaton;
    public File headerFile, bodyFile, svgFile;
    public Automaton.Type type;

    // Tab properties
    public int index;
    private boolean saved = true;

      /* Constructor */

    /**
     * Construct an AutomatonTab, given it's index, and the type of automaton it will contain.
     * @param index The index of this tab
     * @param type  The type of the automaton this tab will hold
     **/
    public AutomatonTab(int index, Automaton.Type type) {

      this.index = index;
      this.type = type;

      setLayout(new BorderLayout());

      // Container upperContainer = createSelectAutomatonTypeContainer();
      add(new JLabel("Automaton type: " + type.toString(), SwingConstants.CENTER), BorderLayout.NORTH);

      // Container lowerContainer = new Container();
      // lowerContainer.setLayout(new GridLayout(1, 2));
      // lowerContainer.add(createInputContainer(type));
      // lowerContainer.add(canvas = new Canvas());
      // add(lowerContainer, BorderLayout.CENTER);

      // Create containers
      Container inputContainer = createInputContainer(type);
      canvas = new Canvas();

      // Create a split pane with the two scroll panes in it
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, inputContainer, canvas);
      splitPane.setOneTouchExpandable(true);
      splitPane.setContinuousLayout(true);
      add(splitPane, BorderLayout.CENTER);

    }

    /**
     * Build the container that holds all of the input boxes.
     **/
    private Container createInputContainer(Automaton.Type type) {

        /* Setup */

      Container container = new Container();
      container.setLayout(new GridBagLayout());
      GridBagConstraints c = new GridBagConstraints();
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridwidth = 1;

        /* Controller Input */

      // Controller input label
      JLabel controllerInputLabel = new JLabel( (type == Automaton.Type.AUTOMATON ? "# Controllers:" : "# Controllers Before U-Structure:") );
      c.ipady = 0; c.weightx = 0.5; c.weighty = 0.0; c.gridx = 0; c.gridy = 0;
      container.add(controllerInputLabel, c);

      // Controller input spinner
      controllerInput = new JSpinner(new SpinnerNumberModel(1, 1, Automaton.MAX_NUMBER_OF_CONTROLLERS, 1));
      controllerInput.addChangeListener(new ChangeListener() {
        @Override public void stateChanged(ChangeEvent e) {
          setSaved(false);
        }
      });
      c.ipady = 0; c.weightx = 0.5; c.weighty = 0.0; c.gridx = 1; c.gridy = 0;
      container.add(controllerInput, c);

        /* Event Input */

      // Event input label
      c.ipady = 0; c.weightx = 0.5; c.weighty = 0.0; c.gridx = 0; c.gridy = 1;
      container.add(new TooltipComponent(new JLabel("Enter events:"), getTooltipText("EVENT_INPUT", type)), c);
      
      // Event input box
      eventInput = createTextPaneWithTraversal();
      JScrollPane eventInputScrollPane = new JScrollPane(eventInput) {
        @Override public Dimension getPreferredSize() {
          return new Dimension(200, 200);  
        }
      };
      watchForChanges(eventInput);
      c.ipady = 100; c.weightx = 0.5; c.weighty = 1.0; c.gridx = 0; c.gridy = 2;
      container.add(eventInputScrollPane, c);

        /* State Input */

      // State input label
      c.ipady = 0; c.weightx = 0.5; c.weighty = 0.0; c.gridx = 1; c.gridy = 1;
      container.add(new TooltipComponent(new JLabel("Enter states:"), getTooltipText("STATE_INPUT", type)), c);
      
      // State input box
      stateInput = createTextPaneWithTraversal();
      JScrollPane stateInputScrollPane = new JScrollPane(stateInput) {
        @Override public Dimension getPreferredSize() {
          return new Dimension(200, 200);  
        }
      };
      watchForChanges(stateInput);
      c.ipady = 100; c.weightx = 0.5; c.weighty = 1.0; c.gridx = 1; c.gridy = 2;
      container.add(stateInputScrollPane, c);

        /* Transition Input */

      c.gridwidth = 2;

      // Transition input label
      c.ipady = 0; c.weightx = 1.0; c.weighty = 0.0; c.gridx = 0; c.gridy = 3;
      container.add(new TooltipComponent(new JLabel("Enter transitions:"), getTooltipText("TRANSITION_INPUT", type)), c);
      
      // Transition input box
      transitionInput = createTextPaneWithTraversal();
      JScrollPane transitionInputScrollPane = new JScrollPane(transitionInput) {
        @Override public Dimension getPreferredSize() {
          return new Dimension(200, 200);  
        }
      };
      watchForChanges(transitionInput);
      c.ipady = 200; c.weightx = 0.5; c.weighty = 1.0; c.gridx = 0; c.gridy = 4;
      container.add(transitionInputScrollPane, c);

        /* Generate Automaton Button */

      generateAutomatonButton = new JButton("Generate Automaton From GUI Code");
      generateAutomatonButton.setFocusable(false);
      generateAutomatonButton.setToolTipText(
        "<html>Create a new automaton from the above input, saving it to file. For small automata, an image of the graph is automatically generated.<br>"
          + "<b><u>NOTE</u></b>: The generated automaton is saved to file, not the GUI input code itself. "
          + "The means that your automaton is not saved until you have generated it.</html>"
      );
      generateAutomatonButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          generateAutomatonButtonPressed();
        }
      });
      c.ipady = 0; c.weightx = 0.5; c.weighty = 1.0; c.gridx = 0; c.gridy = 5;
      container.add(generateAutomatonButton, c);

        /* Generate Image Button */

      generateImageButton = new JButton("Generate Image");
      generateImageButton.setFocusable(false);
      generateImageButton.setToolTipText(
        "<html>Given the generated automaton, produce an image of the graph, displaying it to the right.<br>"
        + "<b><u>NOTE</u></b>: This process can take a long time for large automata.</html>"
      );
      generateImageButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          generateImage();
          generateImageButton.setEnabled(false);
        }
      });
      c.ipady = 0; c.weightx = 0.5; c.weighty = 1.0; c.gridx = 0; c.gridy = 6;
      container.add(generateImageButton, c);

        /* View Image in Browser Button */

      viewImageInBrowserButton = new JButton("View Image in Browser");
      viewImageInBrowserButton.setFocusable(false);
      viewImageInBrowserButton.setToolTipText("<html>View an enlarged version of the generated image in your default browser.</html>");
      viewImageInBrowserButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          viewInBrowser();
        }
      });
      c.ipady = 0; c.weightx = 0.5; c.weighty = 1.0; c.gridx = 0; c.gridy = 7;
      container.add(viewImageInBrowserButton, c);

      return container;

    }

    /**
     * Create a textpane that allows the user to use traversal keys to navigate in between panes.
     * NOTE: These traversal keys are likely 'Tab' to go forward and 'Shift + Tab' to go backward.
     * @return  The instantiated textpane with traversal keys added
     **/
    private JTextPane createTextPaneWithTraversal() {

      JTextPane pane = new JTextPane();
      pane.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
      pane.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

      return pane;

    }

    /**
     * Add a DocumentListener to the specified text pane, which will set this tab's saved status
     * to false whenever the input in the text pane changes.
     * @param textPane  The text pane to monitor for changes
     **/
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

    /**
     * Update this tab's title, based on the filename and the saved status.
     **/
    private void updateTabTitle() {

      String title = removeExtension(headerFile.getName());

      // Temporary files are always considered unsaved, since the directory is wiped upon closing of the program
      if (!saved || usingTemporaryFiles())
        title += "*";

      tabbedPane.setTitleAt(index, title);

    }

    /**
     * Update the saved status, making the necessary updates to the tab's GUI.
     **/
    public void setSaved(boolean newSavedStatus) {

      if (newSavedStatus != saved) {
        saved = newSavedStatus;
        updateTabTitle();
      }

      generateAutomatonButton.setEnabled(!saved);

    }

    /**
     * Check to see if this tab is saved.
     * @return  Whether or not this tab is presently saved
     **/
    public boolean isSaved() {
      return saved;
    }

    /**
     * Refresh the GUI by re-generating the GUI input code.
     * NOTE: This method is quite expensive, as it requires the entire automaton to be read and then
     *       turned in a form representable by strings.
     **/
    public void refreshGUI() {

      // System.out.println("Starting refresh...");
      // long s = System.currentTimeMillis();
      automaton.generateInputForGUI();

      controllerInput.setValue(automaton.getNumberOfControllers());
      eventInput.setText(automaton.getEventInput());
      stateInput.setText(automaton.getStateInput());
      transitionInput.setText(automaton.getTransitionInput());
      // System.out.println("Finished in " + (System.currentTimeMillis() - s) + "ms.");

    }

    /**
     * Check to see if this tab has any unsaved information (which includes information subject to
     * loss due to temporary files). The only un-generated code this won't account for is if there
     * was input code, then it was all cleared.
     * @return  Whether or not this tab has any unsaved information
     **/
    public boolean hasUnsavedInformation() {

      // If there is nothing in the input boxes, then obviously there is no unsaved information
      if (eventInput.getText().equals("") && stateInput.getText().equals("") && transitionInput.getText().equals(""))
        return false;

      // If there is ungenerated GUI input code, then there is unsaved information
      if (!saved)
        return true;

      // Temporary files are considered "unsaved"
      if (usingTemporaryFiles())
        return true;

      // Otherwise, there is no unsaved information
      return false;

    }

    /**
     * Check to see if this tab is using temporary files.
     * NOTE: This method assumes that both the .hdr and .bdy file are found in the same directory.
     * @return  Whether or not the tab is using temporary files to store the automaton
     **/
    public boolean usingTemporaryFiles() {

      return headerFile.getParentFile().getAbsolutePath().equals(TEMPORARY_DIRECTORY.getAbsolutePath());

    }

  } // AutomatonTab

}