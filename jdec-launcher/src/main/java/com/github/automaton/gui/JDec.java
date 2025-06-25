/*
 * Copyright (C) Micah Stairs and Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.AbstractPanInteractor;
import org.apache.commons.collections4.properties.PropertiesFactory;
import org.apache.commons.io.*;
import org.apache.commons.lang3.*;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.*;
// import org.apache.logging.log4j.core.config.Configurator;
import org.w3c.dom.*;
import org.xml.sax.*;

import com.github.automaton.automata.*;
import com.github.automaton.gui.util.*;
import com.github.automaton.gui.util.bipartite.BipartiteGraphExport;
import com.github.automaton.gui.util.graphviz.GraphvizEngineInitializer;
import com.github.automaton.io.AutomatonIOAdapter;
import com.github.automaton.io.input.*;
import com.github.automaton.io.graphviz.AutomatonDotConverter;
import com.github.automaton.io.json.AutomatonJsonFileAdapter;
import com.github.automaton.io.legacy.*;
import com.google.gson.*;
import com.jthemedetecor.OsThemeDetector;

import guru.nidi.graphviz.engine.Format;

/**
 * A Java application for Decentralized Control. This application has been
 * design to build
 * and manipulate various structures such as Automata and U-Structures.
 *
 * @author Micah Stairs
 * @author Sung Ho Yoon
 * 
 * @since 1.0
 */
public class JDec extends JFrame {

    /* CLASS CONSTANTS */

    /** Preferred width for dialogs */
    public static final int PREFERRED_DIALOG_WIDTH = 500;
    /** Preferred height for dialogs */
    public static final int PREFERRED_DIALOG_HEIGHT = 500;

    /**
     * Automatically generated Git properties at build time
     * 
     * @since 2.0
     */
    private static final Properties gitProperties;
    static {
        try {
            gitProperties = PropertiesFactory.INSTANCE.load(JDec.class.getClassLoader(), "git.properties");
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    private static final String applicationTitle = String.format(
            "JDec (%s - %s%s) - A Java application for Decentralized Control",
            Objects.requireNonNullElse(JDec.class.getPackage().getImplementationVersion(), "dev"),
            gitProperties.getProperty("git.commit.id.abbrev"),
            Boolean.valueOf(gitProperties.getProperty("git.dirty")) ? "*" : StringUtils.EMPTY);
    private static final String GUI_DATA_FILE_NAME = "gui.data";

    /**
     * Whether drawing of automata via Graphviz is enabled
     * 
     * @since 1.3
     */
    private static final boolean DRAW_ENABLED = GraphvizEngineInitializer.setupGraphvizEngines();
    /**
     * Maximum number of states in an automaton to trigger automatic rendering
     */
    private static final int N_STATES_TO_AUTOMATICALLY_DRAW = 20;

    /** Logger */
    private static Logger logger = LogManager.getLogger();

    /**
     * Singleton instance.
     * 
     * @since 2.0
     */
    private static JDec instance;

    /* INSTANCE VARIABLES */

    // Tabs
    /** Tabbed pane */
    private JTabbedPane tabbedPane;
    /** Currently open tabs */
    private java.util.List<AutomatonTab> tabs = Collections.synchronizedList(new ArrayList<>());

    // Enabling/disabling components
    /** Components of JDec that require a tab */
    private java.util.List<Component> componentsWhichRequireTab = Collections.synchronizedList(new ArrayList<>());
    /** Components of JDec that require any automaton */
    private java.util.List<Component> componentsWhichRequireAnyAutomaton = Collections
            .synchronizedList(new ArrayList<>());
    /** Components of JDec that require a {@link Automaton basic automaton} */
    private java.util.List<Component> componentsWhichRequireBasicAutomaton = Collections
            .synchronizedList(new ArrayList<>());
    /** Components of JDec that require a {@link UStructure U structure} */
    private java.util.List<Component> componentsWhichRequireUStructure = Collections
            .synchronizedList(new ArrayList<>());
    /**
     * Components of JDec that require a {@link PrunedUStructure pruned U structure}
     */
    private java.util.List<Component> componentsWhichRequirePrunedUStructure = Collections
            .synchronizedList(new ArrayList<>());
    /** Components of JDec that require any U structure */
    private java.util.List<Component> componentsWhichRequireAnyUStructure = Collections
            .synchronizedList(new ArrayList<>());
    /**
     * Components of JDec that require a {@link SubsetConstruction}.
     * 
     * @since 2.1.0
     */
    private java.util.List<Component> componentsWhichRequireSubsetConstruction = Collections
            .synchronizedList(new ArrayList<>());

    // Miscellaneous
    /** The current directory */
    File currentDirectory = SystemUtils.getUserDir();
    /** Index for temporary files */
    private AtomicInteger temporaryFileIndex = new AtomicInteger(1);
    /** Special message to display when no tabs are open */
    private JLabel noTabsMessage;
    /**
     * Action handler for JDec.
     * 
     * @since 2.1.0
     */
    private final JDecActionHandler handler = new JDecActionHandler();

    // Synchronization
    /**
     * Tracks number of busy activities.
     * 
     * @since 2.0
     */
    private AtomicInteger nBusyActivities = new AtomicInteger();
    /**
     * Lock for image generation.
     * 
     * @since 2.0
     */
    private Lock imgGenerationLock = new ReentrantLock(true);
    /**
     * Lock for synchronized composition.
     * 
     * @since 2.0
     */
    private Lock syncCompositionLock = new ReentrantLock(true);

    /** Tool-tip Text */
    private static Document tooltipDocument;
    static {

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setIgnoringElementContentWhitespace(true);
            tooltipDocument = factory.newDocumentBuilder().parse(
                    getResourceURL("tooltips.xml").openStream());
        } catch (ParserConfigurationException | SAXException | IOException e) {
            logger.catching(e);
            tooltipDocument = null;
        } catch (FactoryConfigurationError fce) {
            logger.catching(fce);
            tooltipDocument = null;
        }

    }

    /** Directory for temporary files */
    private File TEMPORARY_DIRECTORY;
    {
        try {
            TEMPORARY_DIRECTORY = Files.createTempDirectory(null).toFile();
        } catch (IOException e) {
            logger.warn("Temporary directory could not be created.", e);
            TEMPORARY_DIRECTORY = new File(SystemUtils.getJavaIoTmpDir(), "JDec_Temporary_Files");
        }
    }

    /* MAIN METHOD */

    /**
     * Create an instance of this application.
     * <p>
     * When running on a Mac, the default behavior is to use the built-in
     * screen menu bar.
     * 
     * @param args Any arguments are simply ignored
     **/
    public static void main(String[] args) {

        // Configurator.setAllLevels(LogManager.ROOT_LOGGER_NAME, Level.DEBUG);
        final OsThemeDetector detector = OsThemeDetector.getDetector();

        if (SystemUtils.IS_OS_MAC) {
            // macOS-specific UI tinkering

            // Use system menu bar
            System.setProperty("apple.laf.useScreenMenuBar", "true");
            // Set application name
            System.setProperty("apple.awt.application.name", "JDec");
            // Use system theme for title bar
            System.setProperty("apple.awt.application.appearance", "system");
            // Associate cmd+Q with the our window handler
            System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
        }

        try {
            if (SystemUtils.IS_OS_MAC) {
                if (detector.isDark())
                    UIManager.setLookAndFeel("com.formdev.flatlaf.themes.FlatMacDarkLaf");
                else
                    UIManager.setLookAndFeel("com.formdev.flatlaf.themes.FlatMacLightLaf");
                UIManager.put("TabbedPane.tabAreaInsets", new Insets(0, 70, 0, 0));
            } else if (detector.isDark())
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
            else
                UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
            logger.catching(e);
        }

        // Start the application
        JDec jdec = instance();

        detector.registerListener(isDark -> {
            SwingUtilities.invokeLater(() -> {
                try {
                    if (SystemUtils.IS_OS_MAC) {
                        if (isDark)
                            UIManager.setLookAndFeel("com.formdev.flatlaf.themes.FlatMacDarkLaf");
                        else
                            UIManager.setLookAndFeel("com.formdev.flatlaf.themes.FlatMacLightLaf");
                        UIManager.put("TabbedPane.tabAreaInsets", new Insets(0, 70, 0, 0));
                    } else if (isDark)
                        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatDarkLaf");
                    else
                        UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
                    SwingUtilities.updateComponentTreeUI(jdec);
                } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
                    logger.catching(e);
                }
            });
        });

    }

    /* CONSTRUCTOR */

    /**
     * Returns the singleton instance of JDec.
     * 
     * @return the singleton instance of JDec
     * 
     * @since 2.0
     */
    public static JDec instance() {
        synchronized (JDec.class) {
            if (instance == null)
                instance = new JDec();
            return instance;
        }
    }

    /**
     * Construct and display the GUI.
     **/
    private JDec() {

        try {
            URL iconUrl = getResourceURL("icon.jpg");
            ImageIcon icon = new ImageIcon(iconUrl);
            if (SystemUtils.IS_OS_MAC) {
                Taskbar.getTaskbar().setIconImage(icon.getImage());
            } else
                setIconImage(icon.getImage());
        } catch (IOException ioe) {
        }

        setMinimumSize(new Dimension(1280, 720));

        /* Create message to display when there are no tabs */

        noTabsMessage = new JLabel("You do not have any tabs open.");
        noTabsMessage.setHorizontalAlignment(JLabel.CENTER);
        noTabsMessage.setVerticalAlignment(JLabel.CENTER);

        /* Create tabbed pane and add a tab to it */

        tabbedPane = new JTabbedPane();
        tabbedPane.setFocusable(false);
        tabbedPane.addChangeListener(e -> updateComponentsWhichRequireAutomaton());
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
                "New Tab->New Automaton,New U-Structure,New Pruned U-Structure",
                "Open",
                "Save[TAB]",
                "Save As...[TAB]",
                "Export...[TAB]",
                "Refresh Tab[TAB]",
                null,
                "Clear[TAB]",
                "Close Tab[TAB]",
                null,
                "Quit[NOT_MACOS]"));

        // View menu
        menuBar.add(createMenu("View",
                "Previous Tab[TAB]",
                "Next Tab[TAB]",
                null,
                "View in Browser[ANY_AUTOMATON]",
                null,
                "Show event-specific view[U_STRUCTURE]"));

        // Standard operations menu
        menuBar.add(createMenu("Standard Operations",
                "Accessible[ANY_AUTOMATON]",
                "Co-Accessible[BASIC_AUTOMATON]",
                "Trim[ANY_AUTOMATON]",
                "Complement[BASIC_AUTOMATON]",
                "Generate Twin Plant[BASIC_AUTOMATON]",
                null,
                "Intersection[BASIC_AUTOMATON]",
                "Union[BASIC_AUTOMATON]"));

        // Special operations menu
        menuBar.add(createMenu("Special Operations",
                "Synchronized Composition[BASIC_AUTOMATON]",
                null,
                "Subset Construction[U_STRUCTURE]",
                "Relabel States[U_STRUCTURE]",
                "Build Automaton Representation[SUBSET_CONSTRUCTION]",
                "Add Communications[U_STRUCTURE]",
                "Feasible Protocols->Generate All[U_STRUCTURE],Make Protocol Feasible[U_STRUCTURE],Find Smallest[U_STRUCTURE],Find First[U_STRUCTURE]",
                null));

        // Properties menu
        menuBar.add(createMenu("Properties",
                "Show control configurations[U_STRUCTURE]",
                null,
                "Test Inference Observability[BASIC_AUTOMATON]",
                "Generate local control decisions[BASIC_AUTOMATON]",
                "Test Controllability[BASIC_AUTOMATON]",
                "Output Bipartite Graphs[BASIC_AUTOMATON]",
                "Output Bipartite Graph Image[BASIC_AUTOMATON]",
                null,
                "Test Incremental Observability[BASIC_AUTOMATON]"));

        // Generate menu
        menuBar.add(createMenu("Generate",
                "Random Automaton"));

        // Information menu
        menuBar.add(createMenu("About",
                "Third-party License",
                "Open GitHub Repository",
                "View License"));

        this.setJMenuBar(menuBar);

    }

    /**
     * Helper method to help make the code look cleaner for menu creation.
     * 
     * @param menuTitle The title of the menu
     * @param strings   The list of menu items (where {@code null} is recognized as
     *                  a separator)
     * @return The created menu
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

                subMenu.addActionListener(handler);
                menu.add(subMenu);

                // Add menu item
            } else
                addMenuItem(menu, str);

        }

        return menu;

    }

    /**
     * Simple helper method to add a menu item to a menu, and to store the item in
     * the proper lists (if applicable).
     * 
     * @param menu The menu in which the menu item is being added
     * @param str  The title of the menu item (special identifiers such as '[TAB]'
     *             indicate which lists the item gets stored in)
     **/
    private void addMenuItem(JMenu menu, String str) {

        // Check to see if this menu item requires a tab
        boolean requiresTab = str.contains("[TAB]");
        if (requiresTab)
            str = str.replace("[TAB]", StringUtils.EMPTY);

        // Check to see if this menu item requires any type of automaton
        boolean requiresAnyAutomaton = str.contains("[ANY_AUTOMATON]");
        if (requiresAnyAutomaton)
            str = str.replace("[ANY_AUTOMATON]", StringUtils.EMPTY);

        // Check to see if this menu item requires a basic automaton (so not a
        // U-Structure)
        boolean requiresBasicAutomaton = str.contains("[BASIC_AUTOMATON]");
        if (requiresBasicAutomaton)
            str = str.replace("[BASIC_AUTOMATON]", StringUtils.EMPTY);

        // Check to see if this menu item requires a U-Structure
        boolean requiresUStructure = str.contains("[U_STRUCTURE]");
        if (requiresUStructure)
            str = str.replace("[U_STRUCTURE]", StringUtils.EMPTY);

        // Check to see if this menu item requires a pruned U-Structure
        boolean requiresPrunedUStructure = str.contains("[PRUNED_U_STRUCTURE]");
        if (requiresPrunedUStructure)
            str = str.replace("[PRUNED_U_STRUCTURE]", StringUtils.EMPTY);

        // Check to see if this menu item requires a U-Structure or a pruned U-Structure
        boolean requiresAnyUStructure = str.contains("[ANY_U_STRUCTURE]");
        if (requiresAnyUStructure)
            str = str.replace("[ANY_U_STRUCTURE]", StringUtils.EMPTY);

        // Check to see if this menu item requires a U-Structure or a pruned U-Structure
        boolean requiresSubsetConstruction = str.contains("[SUBSET_CONSTRUCTION]");
        if (requiresSubsetConstruction)
            str = str.replace("[SUBSET_CONSTRUCTION]", StringUtils.EMPTY);

        // Check to see if this menu item is not displayed in macOS
        boolean requiresNotMacOS = str.contains("[NOT_MACOS]");
        if (requiresNotMacOS && SystemUtils.IS_OS_MAC)
            return;
        else
            str = str.replace("[NOT_MACOS]", StringUtils.EMPTY);

        // Create menu item object
        JMenuItem menuItem = new JMenuItem(str);
        menuItem.addActionListener(handler);

        int shortcutKey = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Add the appropriate accelerator
        switch (str) {

            case "New Automaton":
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, shortcutKey));
                break;

            case "Open":
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, shortcutKey));
                break;

            case "Close Tab":
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, shortcutKey));
                break;

            case "Save":
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey));
                break;

            case "Save As...":
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0));
                break;

            case "Previous Tab":
                menuItem.setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, shortcutKey | InputEvent.ALT_DOWN_MASK));
                break;

            case "Next Tab":
                menuItem.setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, shortcutKey | InputEvent.ALT_DOWN_MASK));
                break;

            case "View in Browser":
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, shortcutKey));
                break;

            case "Synchronized Composition":
                menuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, shortcutKey));
                break;

            case "Subset Construction":
                menuItem.setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_R, shortcutKey | InputEvent.SHIFT_DOWN_MASK));
                break;

            case "Relabel States":
                menuItem.setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_S, shortcutKey | InputEvent.SHIFT_DOWN_MASK));
                break;

            case "Add Communications":
                menuItem.setAccelerator(
                        KeyStroke.getKeyStroke(KeyEvent.VK_C, shortcutKey | InputEvent.SHIFT_DOWN_MASK));
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
        if (requiresAnyUStructure)
            componentsWhichRequireAnyUStructure.add(menuItem);
        if (requiresSubsetConstruction)
            componentsWhichRequireSubsetConstruction.add(menuItem);

        // Add the item to the menu
        menu.add(menuItem);

    }

    /**
     * Make a new thread that monitors when the program quits, deleting all
     * temporary files.
     **/
    private void cleanupBeforeProgramQuits() {

        // The code within this will execute when the program exits for good
        Runtime.getRuntime().addShutdownHook(
                new Thread(() -> {

                    for (String file : TEMPORARY_DIRECTORY.list())
                        new File(TEMPORARY_DIRECTORY, file).delete();

                    TEMPORARY_DIRECTORY.delete();

                }));
    }

    /**
     * Prompt the user to save files before exiting.
     **/
    private void promptBeforeExit() {

        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {

                synchronized (JDec.this) {
                    /* Check for unsaved information */
                    boolean tabInUse = false;
                    boolean unSavedInformation = false;
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (tabs.get(i).hasUnsavedInformation())
                            unSavedInformation = true;
                        if (tabs.get(i).nUsingThreads.get() > 0)
                            tabInUse = true;
                    }

                    if (!unSavedInformation && !tabInUse)
                        System.exit(0);

                    /* Prompt user to save */

                    if (askForConfirmation("Unsaved Information",
                            "Are you sure you want to exit? Any unsaved information will be lost."))
                        System.exit(0);
                }
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
        if (SystemUtils.IS_OS_MAC) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            if (SystemUtils.isJavaVersionAtLeast(JavaVersion.JAVA_17))
                getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            else
                setTitle(null);
        } else
            setTitle(applicationTitle);

        // Show screen
        setVisible(true);

        // Make tooltips appear instantly when the user hovers over them
        ToolTipManager.sharedInstance().setInitialDelay(0);

        // Makes tooltips stay longer (default is only 4 seconds)
        ToolTipManager.sharedInstance().setDismissDelay(Integer.MAX_VALUE);

    }

    /* ACTIONS */

    /**
     * Create an empty tab.
     * 
     * @param assignTemporaryFiles Whether or not temporary files should be assigned
     *                             to the tab
     **/
    private synchronized void createTab(boolean assignTemporaryFiles, Automaton.Type type) {

        /* Add tab */

        int index = tabbedPane.getTabCount();

        AutomatonTab tab = new AutomatonTab(index, type);
        tabs.add(tab);

        tabbedPane.addTab(null, null, tab, StringUtils.EMPTY);
        tabbedPane.setSelectedIndex(index);

        try {
            if (assignTemporaryFiles) {
                String fileName = getTemporaryFileName();
                File tempFile = new File(
                        fileName + FilenameUtils.EXTENSION_SEPARATOR + AutomatonJsonFileAdapter.EXTENSION);
                FileUtils.touch(tempFile);
                tab.ioAdapter = new AutomatonJsonFileAdapter(tempFile, false);
                tab.updateTabTitle();
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(logger.throwing(ioe));
        }

        /* Re-activate appropriate components if this is the first tab */

        if (tabs.size() == 1) {
            synchronized (componentsWhichRequireTab) {
                for (Component component : componentsWhichRequireTab)
                    component.setEnabled(true);
            }
        }

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
     * 
     * @param automaton The automaton object
     * 
     * @revised 2.0
     **/
    public synchronized void createTab(Automaton automaton) {
        AutomatonJsonFileAdapter jsonIOAdapter;
        try {
            jsonIOAdapter = AutomatonJsonFileAdapter.wrap(automaton, new File(
                    getTemporaryFileName() + FilenameUtils.EXTENSION_SEPARATOR + AutomatonJsonFileAdapter.EXTENSION));
        } catch (IOException ioe) {
            throw new UncheckedIOException(logger.throwing(ioe));
        }

        createJsonTab(jsonIOAdapter);
        int newIndex = tabbedPane.getTabCount() - 1;

        /* Set tab values */

        AutomatonTab tab = tabs.get(newIndex);

        tab.ioAdapter = jsonIOAdapter;
        tab.automaton = tab.ioAdapter.getAutomaton();
        tab.refreshGUI();
        // tab.setSaved(true);
    }

    /**
     * Create a tab, and load in an automaton from {@code .hdr}/{@code .bdy} file
     * pair.
     * 
     * @param binaryAutomatonAdapter a wrapper for {@code .hdr}/{@code .bdy} file
     *                               pair
     * 
     * @since 2.0
     **/
    public synchronized void createLegacyTab(AutomatonBinaryFileAdapter binaryAutomatonAdapter) {

        /* Create new tab */

        createTab(false, Automaton.Type.getType(binaryAutomatonAdapter.getAutomaton().getClass()));
        int newIndex = tabbedPane.getTabCount() - 1;

        /* Set tab values */

        AutomatonTab tab = tabs.get(newIndex);

        tab.ioAdapter = binaryAutomatonAdapter;
        tab.automaton = binaryAutomatonAdapter.getAutomaton();
        tab.refreshGUI();
        tab.setSaved(true);

        /* Generate an image (unless it's quite large) */

        if (!DRAW_ENABLED) {
            tab.generateImageButton.setEnabled(false);
        } else if (tab.automaton.getNumberOfStates() <= N_STATES_TO_AUTOMATICALLY_DRAW) {
            generateImage();
            tab.generateImageButton.setEnabled(false);
        } else
            tab.generateImageButton.setEnabled(true);

    }

    /**
     * Create a tab, and load in an automaton from a JSON object
     * 
     * @param jsonAutomatonAdapter a wrapper for JSON object
     **/
    public synchronized void createJsonTab(AutomatonJsonFileAdapter jsonAutomatonAdapter) {

        /* Create new tab */

        createTab(false, Automaton.Type.getType(jsonAutomatonAdapter.getAutomaton().getClass()));
        int newIndex = tabbedPane.getTabCount() - 1;

        /* Set tab values */

        AutomatonTab tab = tabs.get(newIndex);

        tab.ioAdapter = jsonAutomatonAdapter;
        tab.automaton = jsonAutomatonAdapter.getAutomaton();
        tab.refreshGUI();
        tab.setSaved(true);

        /* Generate an image (unless it's quite large) */

        if (!DRAW_ENABLED) {
            tab.generateImageButton.setEnabled(false);
        } else if (tab.automaton.getNumberOfStates() <= N_STATES_TO_AUTOMATICALLY_DRAW) {
            generateImage();
            tab.generateImageButton.setEnabled(false);
        } else
            tab.generateImageButton.setEnabled(true);

    }

    /**
     * Returns the currently selected tab.
     * 
     * @return the currently selected tab
     * 
     * @throws NoSuchElementException if there is no open tab
     * 
     * @since 2.1.0
     */
    synchronized AutomatonTab getCurrentTab() {
        if (tabs.isEmpty())
            throw new NoSuchElementException();
        return tabs.get(tabbedPane.getSelectedIndex());
    }

    /**
     * Returns the currently open tabs.
     * 
     * @return the currently open tabs
     * 
     * @since 2.1.0
     */
    synchronized java.util.List<AutomatonTab> getTabs() {
        return tabs;
    }

    /**
     * Close the current tab, displaying a warning message if the current tab is
     * unsaved.
     **/
    private synchronized void closeCurrentTab() {

        /* Get index of the currently selected tab */

        int index = tabbedPane.getSelectedIndex();

        /* Check for unsaved information */

        AutomatonTab tab = tabs.get(index);
        if (tab.nUsingThreads.get() > 0)
            return;
        if (tab.hasUnsavedInformation()) {

            // Create message to display in pop-up
            String message = "Are you sure you want to close this tab? ";
            if (tab.usingTemporaryFiles())
                message += "This automaton is only being saved temporarily. To save this automaton permanently, ensure that you have generated the automaton, then select 'Save As...' from the 'File' menu.";
            else
                message += "Any un-generated GUI input code will be lost.";

            // Confirm that the user wants to proceed
            if (!askForConfirmation("Unsaved Information", message))
                return;

        }

        if (tab.ioAdapter instanceof AutomatonBinaryFileAdapter adapter) {
            try {
                adapter.close();
            } catch (IOException ioe) {
                throw new UncheckedIOException(logger.throwing(ioe));
            }
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
        File imageFile = new File(FilenameUtils.removeExtension(tab.ioAdapter.getFile().getAbsolutePath()) + ".svg");

        boolean successful = false;

        // Try to load image from file
        try {
            if (Desktop.isDesktopSupported()) {
                if (SystemUtils.IS_OS_WINDOWS) {
                    Desktop.getDesktop().open(imageFile);
                } else {
                    Desktop.getDesktop().browse(imageFile.toURI());
                }
                successful = true;

            }
        } catch (IOException e) {
            logger.catching(e);
        }

        // Display the proper error message
        if (!successful) {
            if (imageFile.exists())
                displayErrorMessage("Unable To Open",
                        "The .SVG file could not be opened in your browser. You can find the file here: '" + imageFile.getAbsolutePath()
                                + "'.");
            else
                displayErrorMessage("File Not Found",
                        "The .SVG file could not be found. Please ensure that you have generated the image.");
        }

    }

    /**
     * Generate an automaton using the entered GUI input code.
     **/
    private void generateAutomatonButtonPressed() {

        // Get the current tab
        final AutomatonTab tab = getCurrentTab();
        /*
         * if (tab.automaton != null) {
         * try {
         * tab.automaton.close();
         * } catch (IOException ioe) {
         * throw new UncheckedIOException(ioe);
         * }
         * }
         */

        // Create automaton from input code
        switch (tab.type) {

            case AUTOMATON:

                int nControllers = (Integer) tab.controllerInput.getValue();
                tab.automaton = AutomatonGenerator.generateFromGUICode(
                        new Automaton(nControllers),
                        tab.eventInput.getText(),
                        tab.stateInput.getText(),
                        tab.transitionInput.getText(),
                        tab.eventInput,
                        tab.stateInput,
                        tab.transitionInput,
                        this);
                break;

            case U_STRUCTURE:

                nControllers = (Integer) tab.controllerInput.getValue();
                tab.automaton = AutomatonGenerator.generateFromGUICode(
                        new UStructure(nControllers),
                        tab.eventInput.getText(),
                        tab.stateInput.getText(),
                        tab.transitionInput.getText(),
                        tab.eventInput,
                        tab.stateInput,
                        tab.transitionInput,
                        this);
                break;

            case PRUNED_U_STRUCTURE:

                nControllers = (Integer) tab.controllerInput.getValue();
                tab.automaton = AutomatonGenerator.generateFromGUICode(
                        new PrunedUStructure(nControllers),
                        tab.eventInput.getText(),
                        tab.stateInput.getText(),
                        tab.transitionInput.getText(),
                        tab.eventInput,
                        tab.stateInput,
                        tab.transitionInput,
                        this);
                break;

            default:

                // NOTE: The following error should never appear to the user, and it indicates a
                // bug in the program
                displayErrorMessage("Crucial Error",
                        "Unable to generate automaton from GUI input code due to unrecognized automaton type.");
                return;

        }

        // Abort if the automaton was unable to be generated (due to errors parsing the
        // input code)
        if (tab.automaton == null)
            return;

        // tab.setSaved(true);

        // Generate an image (unless it's quite large)
        if (tab.automaton.getNumberOfStates() <= N_STATES_TO_AUTOMATICALLY_DRAW) {
            generateImage();
            tab.generateImageButton.setEnabled(false);
        } else {
            tab.generateImageButton.setEnabled(true);
            tab.canvas.setURI(null);
        }

        tab.generateAutomatonButton.setEnabled(false);

        // Refresh GUI
        updateComponentsWhichRequireAutomaton();

    }

    /**
     * Generates a graph representation of the automaton as an SVG file and
     * displays it on the screen.
     * 
     * @see AutomatonDotConverter#generateImage(String)
     * 
     * @revised 2.0
     **/
    private void generateImage() {

        // Get the current tab
        AutomatonTab tab = getCurrentTab();
        tab.generateImageButton.setEnabled(false);

        Thread imgGenerationThread = new Thread(() -> {

            tab.nUsingThreads.incrementAndGet();
            tab.generateImageButton.setText("Waiting to generate image");

            imgGenerationLock.lock();

            tab.generateImageButton.setText("Generating image");

            // Create destination file name (excluding extension)
            String destinationFileName = FilenameUtils.removeExtension(tab.ioAdapter.getFile().getAbsolutePath());

            try {

                // Set the image blank if there were no states entered
                if (tab.automaton == null)
                    tab.canvas.loadSVGDocument(null);

                // Try to create graph image, displaying it on the screen
                else if (tab.automaton.getDotConverter().generateImage(destinationFileName)) {
                    tab.svgFile = new File(destinationFileName + ".svg");
                    tab.canvas.setSVGDocument(ImageLoader.loadSVGFromFile((tab.svgFile)));
                }

                // Display error message
                else
                    displayErrorMessage("Error",
                            "Something went wrong while trying to generate and display the image. NOTE: It may be the case that you do not have X11 installed.");

            } catch (IOException e) {
                logger.catching(e);
                displayErrorMessage("I/O Error", "An I/O error occurred.");
                tab.generateImageButton.setEnabled(true);
            } catch (RuntimeException re) {
                displayException(re);
                tab.generateImageButton.setEnabled(true);
            }

            tab.nUsingThreads.decrementAndGet();
            imgGenerationLock.unlock();
            tabbedPane.setSelectedComponent(tab);
            tab.generateImageButton.setText("Generate image");

        }, FilenameUtils.removeExtension(tab.ioAdapter.getFile().getName()) + " - Image generation");

        imgGenerationThread.start();

    }

    /**
     * Generate a random automaton with the specified properties.
     * 
     * @param prompt                 The dialog box which started this process
     * @param nEvents                The number of events to be generated in the
     *                               automaton
     * @param nStates                The number of states to be generated in the
     *                               automaton
     * @param minTransitionsPerState The minimum number of outgoing transitions per
     *                               state
     * @param maxTransitionsPerState The maximum number of outgoing transitions per
     *                               state
     * @param nControllers           The number of controllers in the automaton
     * @param nBadTransitions        The number of bad transition in the automaton
     * @param progressIndicator      The progress indicator to be updated during the
     *                               generation process
     **/
    public void generateRandomAutomaton(RandomAutomatonPrompt prompt,
            int nEvents,
            int nStates,
            int minTransitionsPerState,
            int maxTransitionsPerState,
            int nControllers,
            int nBadTransitions,
            JLabel progressIndicator) {

        // Generate random automaton
        Automaton automaton = RandomAutomatonGenerator.generateRandom(
                prompt,
                nEvents,
                nStates,
                minTransitionsPerState,
                maxTransitionsPerState,
                nControllers,
                nBadTransitions,
                progressIndicator);

        // Place the generated automaton in a new tab as long as the process was not
        // aborted
        if (automaton != null)
            createTab(automaton);

    }

    /**
     * Load automaton from file, filling the input fields with its data.
     * NOTE: A loading bar is displayed to keep track of the progress.
     * 
     * @param index The tab's index
     **/
    private synchronized void refresh(final int index) {

        setBusyCursor(true);

        // This process is started in a new thread so that the progress bar can be
        // refreshed
        new Thread(() -> {

            AutomatonTab tab = tabs.get(index);
            Automaton.Type type = Automaton.Type.getType(tab.automaton.getClass());
            if (type == null) {
                displayErrorMessage("Missing File", "The header file for this automaton could not be found.");
                return;
            }

            // final ProgressBarPopup progressBarPopup = new ProgressBarPopup(JDec.this,
            // "Loading...", 3);

            // Instantiate automaton
            /*
             * switch (type) {
             * 
             * case AUTOMATON:
             * tab.automaton = new Automaton(tab.headerFile, false);
             * break;
             * 
             * case U_STRUCTURE:
             * tab.automaton = new UStructure(tab.headerFile, tab.bodyFile);
             * break;
             * 
             * case PRUNED_U_STRUCTURE:
             * tab.automaton = new PrunedUStructure(tab.headerFile, tab.bodyFile);
             * break;
             * 
             * default:
             * displayErrorMessage("Unrecognized Type",
             * "This version of JDec does not support this type of automaton.");
             * return;
             * 
             * }
             */

            synchronized (tab) {

                tab.refreshGUI();

                // Generate an image (unless it's quite large)
                if (tab.automaton.getNumberOfStates() <= N_STATES_TO_AUTOMATICALLY_DRAW) {
                    generateImage();
                    tab.generateImageButton.setEnabled(false);
                } else
                    tab.generateImageButton.setEnabled(true);

                // tab.setSaved(true);
            }

            EventQueue.invokeLater(() -> {
                JDec.this.setBusyCursor(false);
            });

            // Refresh components which require a specific type of automaton
            updateComponentsWhichRequireAutomaton();

        }).start();

    }

    /* ENABLING/DISABLING COMPONENTS */

    /**
     * Update the components in the menu bar that require a specific type of
     * automaton, by
     * enabling/disabling them as appropriate.
     **/
    public synchronized void updateComponentsWhichRequireAutomaton() {
        updateComponentsWhichRequireAnyAutomaton();
        updateComponentsWhichRequireBasicAutomaton();
        updateComponentsWhichRequireUStructure();
        updateComponentsWhichRequirePrunedUStructure();
        updateComponentsWhichRequireAnyUStructure();
        updateComponentsWhichRequireSubsetConstruction();
    }

    /**
     * Enable/disable components that require any type of automaton (e.g.,
     * U-Structure).
     **/
    private void updateComponentsWhichRequireAnyAutomaton() {

        int index = tabbedPane.getSelectedIndex();

        // Determine whether the components should be enabled or disabled
        boolean enabled = (index >= 0
                && tabs.get(index).automaton != null);

        // Enabled/disable all components in the list
        synchronized (componentsWhichRequireAnyAutomaton) {
            for (Component component : componentsWhichRequireAnyAutomaton)
                component.setEnabled(enabled);
        }

    }

    /**
     * Enable/disable components that require a basic automaton.
     **/
    private void updateComponentsWhichRequireBasicAutomaton() {

        int index = tabbedPane.getSelectedIndex();

        // Determine whether the components should be enabled or disabled
        boolean enabled = (index >= 0
                && tabs.get(index).type == Automaton.Type.AUTOMATON
                && tabs.get(index).automaton != null);

        // Enabled/disable all components in the list
        synchronized (componentsWhichRequireBasicAutomaton) {
            for (Component component : componentsWhichRequireBasicAutomaton)
                component.setEnabled(enabled);
        }

    }

    /**
     * Enable/disable components that require a U-Structure.
     **/
    private void updateComponentsWhichRequireUStructure() {

        int index = tabbedPane.getSelectedIndex();

        // Determine whether the components should be enabled or disabled
        boolean enabled = (index >= 0
                && tabs.get(index).type == Automaton.Type.U_STRUCTURE
                && tabs.get(index).automaton != null);

        // Enabled/disable all components in the list
        synchronized (componentsWhichRequireUStructure) {
            for (Component component : componentsWhichRequireUStructure)
                component.setEnabled(enabled);
        }

    }

    /**
     * Enable/disable components that require a pruned U-Structure.
     **/
    private void updateComponentsWhichRequirePrunedUStructure() {

        int index = tabbedPane.getSelectedIndex();

        // Determine whether the components should be enabled or disabled
        boolean enabled = (index >= 0
                && tabs.get(index).type == Automaton.Type.PRUNED_U_STRUCTURE
                && tabs.get(index).automaton != null);

        // Enabled/disable all components in the list
        synchronized (componentsWhichRequirePrunedUStructure) {
            for (Component component : componentsWhichRequirePrunedUStructure)
                component.setEnabled(enabled);
        }

    }

    /**
     * Enable/disable components that require a U-Structure or a pruned U-Structure.
     **/
    private void updateComponentsWhichRequireAnyUStructure() {

        int index = tabbedPane.getSelectedIndex();

        // Determine whether the components should be enabled or disabled
        boolean enabled = (index >= 0
                && (tabs.get(index).type == Automaton.Type.PRUNED_U_STRUCTURE
                        || tabs.get(index).type == Automaton.Type.U_STRUCTURE)
                && tabs.get(index).automaton != null);

        // Enabled/disable all components in the list
        synchronized (componentsWhichRequireAnyUStructure) {
            for (Component component : componentsWhichRequireAnyUStructure)
                component.setEnabled(enabled);
        }

    }

    /**
     * Enable/disable components that require a subset construction.
     * 
     * @since 2.1.0
     **/
    private void updateComponentsWhichRequireSubsetConstruction() {

        int index = tabbedPane.getSelectedIndex();

        // Determine whether the components should be enabled or disabled
        boolean enabled = (index >= 0
                && tabs.get(index).type == Automaton.Type.SUBSET_CONSTRUCTION
                && tabs.get(index).automaton != null);

        // Enabled/disable all components in the list
        synchronized (componentsWhichRequireSubsetConstruction) {
            for (Component component : componentsWhichRequireSubsetConstruction)
                component.setEnabled(enabled);
        }

    }

    /* PROMPTS */

    /**
     * Opens up a JFileChooser for the user to choose a file from their file system.
     * 
     * @param title The title to put in the file chooser dialog box
     * @param index The index of the tab we're selecting a file for (-1 indicates
     *              that a new tab will be created for it)
     * @return The file, or null if the user did not choose anything
     **/
    private File selectFile(String title, int index) {

        /* Set up the file chooser */

        JFileChooser fileChooser = new JFileChooser() {
            @Override
            protected JDialog createDialog(Component parent) {
                JDialog dialog = super.createDialog(JDec.this);
                dialog.setModal(true);
                return dialog;
            }
        };

        fileChooser.setDialogTitle(title);

        /* Filter .hdr files */

        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter binaryFilter = new FileNameExtensionFilter("Automaton files",
                HeaderAccessFile.EXTENSION);
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON files",
                AutomatonJsonFileAdapter.EXTENSION);
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.addChoosableFileFilter(binaryFilter);

        /* Begin at the most recently accessed directory */

        if (currentDirectory != null)
            fileChooser.setCurrentDirectory(currentDirectory);

        /* Prompt user to select a file */

        if (fileChooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION)
            return null;

        switch (FilenameUtils.getExtension(fileChooser.getSelectedFile().getName())) {
            case HeaderAccessFile.EXTENSION:
                return loadBinaryAutomatonFile(fileChooser.getSelectedFile(), index);
            case AutomatonJsonFileAdapter.EXTENSION:
                return loadJsonAutomatonFile(fileChooser.getSelectedFile(), index);
            default:
                throw logger.throwing(new UnsupportedOperationException("Unsupported file extension"));
        }

    }

    private File loadBinaryAutomatonFile(File selectedFile, int index) {
        /* Update files in the tab and update current directory */

        if (selectedFile != null) {

            // Check to see if that file is already open
            for (AutomatonTab tab : tabs)
                if (tab.ioAdapter.getFile().equals(selectedFile)) {
                    displayErrorMessage("File Already Open", "The specified file is already open in another tab.");
                    return null;
                }

            // Get files
            File headerFile = selectedFile;
            File bodyFile = new File(FilenameUtils.removeExtension(headerFile.getAbsolutePath())
                    + FilenameUtils.EXTENSION_SEPARATOR + BodyAccessFile.EXTENSION);

            // Create new tab (if requested)
            if (index == -1) {
                createTab(false, Automaton.Type.getType(selectedFile));
                index = tabbedPane.getSelectedIndex();
            }
            AutomatonTab tab = tabs.get(index);

            // Update files
            try {
                tab.ioAdapter = new AutomatonBinaryFileAdapter(headerFile, bodyFile);
            } catch (IOException ioe) {
                throw new UncheckedIOException(logger.throwing(ioe));
            }
            tab.automaton = tab.ioAdapter.getAutomaton();

            // Update current directory
            currentDirectory = selectedFile.getParentFile();
            saveCurrentDirectory();

        }

        return selectedFile;
    }

    private File loadJsonAutomatonFile(File selectedFile, int index) {
        /* Update files in the tab and update current directory */

        if (selectedFile != null) {

            // Check to see if that file is already open
            for (AutomatonTab tab : tabs)
                if (tab.ioAdapter.getFile().equals(selectedFile)) {
                    displayErrorMessage("File Already Open", "The specified file is already open in another tab.");
                    return null;
                }

            // Get files
            AutomatonJsonFileAdapter jsonAdapter;

            try {
                jsonAdapter = new AutomatonJsonFileAdapter(selectedFile);
            } catch (IOException ioe) {
                throw new UncheckedIOException(logger.throwing(ioe));
            }

            // Create new tab (if requested)
            if (index == -1) {
                createTab(false, jsonAdapter.getAutomaton().getType());
                index = tabbedPane.getSelectedIndex();
            }
            AutomatonTab tab = tabs.get(index);

            // Update files
            tab.ioAdapter = jsonAdapter;
            tab.automaton = tab.ioAdapter.getAutomaton();

            // Update current directory
            currentDirectory = selectedFile.getParentFile();
            saveCurrentDirectory();

        }

        return selectedFile;
    }

    /**
     * Prompts the user to name and specify the filename that they wish to save the
     * data to.
     * 
     * @param title The title to give the window
     * @return The data file to save the data to
     **/
    private File saveFile(String title) {

        /* Set up the file chooser */

        JFileChooser fileChooser = new OverwriteCheckingFileChooser() {
            @Override
            protected JDialog createDialog(Component parent) {
                JDialog dialog = super.createDialog(JDec.this);
                dialog.setModal(true);
                return dialog;
            }
        };

        fileChooser.setDialogTitle(title);

        /* Filter files */

        fileChooser.setAcceptAllFileFilterUsed(false);
        FileNameExtensionFilter binaryFilter = new FileNameExtensionFilter("Automaton files",
                HeaderAccessFile.EXTENSION);
        FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON files",
                AutomatonJsonFileAdapter.EXTENSION);
        fileChooser.addChoosableFileFilter(jsonFilter);
        fileChooser.addChoosableFileFilter(binaryFilter);

        /* Begin at the most recently accessed directory */

        if (currentDirectory != null)
            fileChooser.setCurrentDirectory(currentDirectory);

        /* Prompt user to select a filename */

        int result = fileChooser.showSaveDialog(null);

        /* No file was selected */

        if (result != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null)
            return null;

        FileNameExtensionFilter usedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();

        if (!FilenameUtils.isExtension(fileChooser.getSelectedFile().getName(), usedFilter.getExtensions())) {
            fileChooser.setSelectedFile(new File(
                    fileChooser.getSelectedFile().getAbsolutePath()
                            + FilenameUtils.EXTENSION_SEPARATOR
                            + usedFilter.getExtensions()[0]));
        }

        switch (FilenameUtils.getExtension(fileChooser.getSelectedFile().getName())) {
            case HeaderAccessFile.EXTENSION:
                return saveBinaryFile(fileChooser.getSelectedFile());
            case AutomatonJsonFileAdapter.EXTENSION:
                return saveJsonFile(fileChooser.getSelectedFile());
            default:
                throw logger.throwing(new UnsupportedOperationException("Unsupported file extension"));
        }

    }

    /**
     * Saves the automaton stored in the current tab to a {@code .hdr} /
     * {@code .bdy} file pair.
     * 
     * @param selectedFile The {@code .hdr} selected by the user
     * @return The {@code .hdr} file that the data is saved in
     * 
     * @since 2.0
     **/
    private File saveBinaryFile(File selectedFile) {

        String prefix = FilenameUtils.removeExtension(selectedFile.getAbsolutePath());
        File headerFile = new File(prefix + FilenameUtils.EXTENSION_SEPARATOR + HeaderAccessFile.EXTENSION);
        File bodyFile = new File(prefix + FilenameUtils.EXTENSION_SEPARATOR + BodyAccessFile.EXTENSION);
        File svgFile = new File(prefix + FilenameUtils.EXTENSION_SEPARATOR + "svg");

        AutomatonTab currentTab = getCurrentTab();

        /* Check to see if that file is already open */

        for (AutomatonTab tab : tabs)
            if (tab.ioAdapter.getFile().equals(headerFile) && currentTab.index != tab.index) {
                displayErrorMessage("File Is Open",
                        "The specified file is open in another tab. Please choose a different filename.");
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

        try {
            if (currentTab.ioAdapter instanceof AutomatonBinaryFileAdapter adapter)
                adapter.close();
            currentTab.ioAdapter = AutomatonBinaryFileAdapter.wrap(currentTab.automaton, headerFile, bodyFile);
        } catch (IOException ioe) {
            throw new UncheckedIOException(logger.throwing(ioe));
        }

        currentDirectory = headerFile.getParentFile();
        saveCurrentDirectory();

        return headerFile;
    }

    /**
     * Saves the automaton stored in the current tab to a {@code .json} file.
     * 
     * @param selectedFile The {@code .json} selected by the user
     * @return The {@code .json} file that the data is saved in
     * 
     * @since 2.0
     **/
    private File saveJsonFile(File selectedFile) {

        File jsonFile = selectedFile;
        String prefix = FilenameUtils.removeExtension(selectedFile.getAbsolutePath());
        File svgFile = new File(prefix + ".svg");

        AutomatonTab currentTab = getCurrentTab();

        /* Check to see if that file is already open */

        for (AutomatonTab tab : tabs)
            if (tab.ioAdapter.getFile().equals(jsonFile) && currentTab.index != tab.index) {
                displayErrorMessage("File Is Open",
                        "The specified file is open in another tab. Please choose a different filename.");
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

        try {
            if (currentTab.ioAdapter instanceof AutomatonBinaryFileAdapter adapter)
                adapter.close();
            currentTab.ioAdapter = AutomatonJsonFileAdapter.wrap(currentTab.automaton, jsonFile);
        } catch (IOException ioe) {
            throw new UncheckedIOException(logger.throwing(ioe));
        }

        currentDirectory = jsonFile.getParentFile();
        saveCurrentDirectory();

        return jsonFile;
    }

    /**
     * Allow the user to select an automaton that is currently open.
     * 
     * @param str         The message to display
     * @param indexToSkip The index of the automaton which should be omitted from
     *                    the options
     * @return The index of the selected automaton (or -1 if there was not an
     *         automaton selected)
     **/
    private int pickAutomaton(String str, int indexToSkip) {

        /* Create list of options */

        java.util.List<String> optionsList = new ArrayList<>();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {

            AutomatonTab tab = tabs.get(i);

            // Skip automaton
            if (i == indexToSkip || tab.automaton == null)
                continue;

            // Add automaton to list of options
            optionsList.add(FilenameUtils.removeExtension(tab.ioAdapter.getFile().getAbsolutePath()));

        }

        String[] options = optionsList.toArray(String[]::new);

        /* Show error message if there is no second automaton to pick from */

        if (options.length == 0) {
            displayErrorMessage("Operation Aborted", "This operation requires two generated automata.");
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
                options[0]);

        /* Return index of chosen automaton */

        for (int i = 0; i < tabbedPane.getTabCount(); i++)
            if (tabs.get(i).ioAdapter.getFile() != null
                    && FilenameUtils.removeExtension(tabs.get(i).ioAdapter.getFile().getAbsolutePath()).equals(choice))
                return i;

        return -1;

    }

    /**
     * Allow the user to select a controller in the current automaton.
     * 
     * @param str                 The message to display
     * @param include0thComponent Whether or not the 0th component should be
     *                            included as an option
     * @return The index of the selected controller (or {@code -1}
     *         if there was not a controller selected)
     **/
    public int pickController(String str, boolean include0thComponent) {

        /* Create list of options */

        java.util.List<Integer> optionsList = new ArrayList<>();
        for (int i = (include0thComponent ? 0 : 1); i <= getCurrentTab().automaton.getNumberOfControllers(); i++)
            optionsList.add(i);
        Integer[] options = optionsList.toArray(Integer[]::new);

        /* Display prompt to user */

        Integer choice = (Integer) JOptionPane.showInputDialog(
                this,
                str,
                "Choose Controller",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]);

        /* Return index of chosen controller */

        return choice == null ? -1 : choice;

    }

    /**
     * Given a title and a message, ask the user for confirmation, returning the
     * result.
     * 
     * @param title   The title to display on the dialog box
     * @param message The message to display in the dialog box
     * @return {@code true} if the user selected "Yes", or {@code false} otherwise
     **/
    private boolean askForConfirmation(String title, String message) {

        int promptResult = JOptionPane.showConfirmDialog(
                this,
                message,
                title,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        return promptResult == JOptionPane.YES_OPTION;

    }

    /* HELPER METHODS */

    /**
     * Changes the cursor to reflect the application's status.
     * 
     * @param busy Whether or not the cursor should appear to be busy
     **/
    public synchronized void setBusyCursor(boolean busy) {

        if (busy)
            nBusyActivities.incrementAndGet();
        else
            nBusyActivities.decrementAndGet();
        if (nBusyActivities.get() > 0)
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else
            setCursor(Cursor.getDefaultCursor());

    }

    /**
     * Gets a URL to the specified local resource.
     * 
     * @param fileName the filename of the local resource
     * @return a URL pointing to the specified resource
     * 
     * @throws IOException if the specified resource does not exist
     * 
     * @since 1.1
     * @revised 2.0
     */
    private static URL getResourceURL(String fileName) throws IOException {
        return IOUtils.resourceToURL(fileName, JDec.class.getClassLoader());
    }

    /**
     * Get the tooltip text for the specified input box and automaton type.
     * 
     * @param inputBox      A string representing the relevant input box
     *                      <p>
     *                      NOTE: This is the same as the tag used in the XML file
     * @param automatonType The enum value associated with the automaton type
     * @return The HTML formatted tool-tip text, or {@code null} if it could not be
     *         found
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

            // Generate a string of this element and its descendants, including tags
            StringWriter buffer = new StringWriter();
            Transformer xform = TransformerFactory.newInstance().newTransformer();
            xform.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xform.transform(new DOMSource(element3), new StreamResult(buffer));

            // Remove the outer tag, trim it, then return it
            String startTag = "<" + automatonType.name() + ">";
            String endTag = "</" + automatonType.name() + ">";
            return buffer.toString().replace(startTag, StringUtils.EMPTY).replace(endTag, StringUtils.EMPTY).trim();

        } catch (NullPointerException | TransformerException e) {
        }

        return null;

    }

    /**
     * Display an exception in a modal dialog which will stay on top of the GUI at
     * all times.
     * 
     * @param exception Exception to display
     * 
     * @since 1.2
     * 
     * @revised 2.0
     **/
    public void displayException(Throwable exception) {
        logger.catching(exception);
        displayMessage("Exception: " + exception.getClass().getName(), exception.getMessage(),
                JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Display an error message in a modal dialog which will stay on top of the GUI
     * at all times.
     * 
     * @param title   The title to display in the dialog box
     * @param message The message to display in the dialog box
     **/
    public void displayErrorMessage(String title, String message) {
        logger.error(title + ": " + message);
        displayMessage(title, message, JOptionPane.ERROR_MESSAGE);

    }

    /**
     * Display a message of the specified type in a modal dialog which will stay on
     * top of the GUI
     * at all times.
     * 
     * @param title       The title to display in the dialog box
     * @param message     The message to display in the dialog box
     * @param messageType The type of message (using {@link JOptionPane} constants)
     **/
    public void displayMessage(String title, String message, int messageType) {

        JOptionPane op = new JOptionPane(message, messageType);
        JDialog dialog = op.createDialog(this, title);
        dialog.setAlwaysOnTop(true);
        dialog.setVisible(true);

    }

    /**
     * Get a temporary filename (prefixed by 'untitled') which is stored in the
     * temporary directory.
     * 
     * @return The temporary filename, which does not contain an extension
     * 
     * @revised 2.0
     **/
    public String getTemporaryFileName() {
        return TEMPORARY_DIRECTORY.getAbsolutePath() + File.separator + "untitled"
                + temporaryFileIndex.getAndIncrement();
    }

    /**
     * Load the current directory from file (so that the current directory is
     * maintained even after the program has been closed).
     **/
    private void loadCurrentDirectory() {

        try (Scanner sc = new Scanner(new File(GUI_DATA_FILE_NAME))) {

            if (sc.hasNextLine())
                currentDirectory = new File(sc.nextLine());

        } catch (FileNotFoundException e) {
            // Simply ignore the error, since it just means that there was no pre-existing
            // file found
        }

    }

    /**
     * Saves the current directory to file (so that the current directory is
     * maintained even after the program has been closed).
     **/
    private void saveCurrentDirectory() {

        if (currentDirectory != null) {

            try (PrintWriter writer = new PrintWriter(new FileWriter(GUI_DATA_FILE_NAME, false))) {

                writer.println(currentDirectory.getPath());

            } catch (IOException e) {
                logger.catching(e);
            }

        }

    }

    /* INNER CLASSES */

    /**
     * Class used to maintain a tab inside the JDec object.
     * NOTE: Since this is an inner class (and since there are a lot of instance
     * variables), I chose to
     * keep most variables public, as opposed to have a multitude of getters and
     * setters.
     **/
    class AutomatonTab extends Container {

        /* Instance variables */

        // GUI elements
        public JSplitPane splitPane;
        public JTextPane eventInput, stateInput, transitionInput;
        public JSpinner controllerInput;
        public JButton generateAutomatonButton, generateImageButton, viewImageInBrowserButton, exploreAutomatonButton,
                showAutomatonInfoButton;
        public JSVGCanvas canvas = null;

        // Automaton properties
        public AutomatonIOAdapter ioAdapter;
        public Automaton automaton;
        public File svgFile;
        public Automaton.Type type;

        // Tab properties
        public int index;
        private AtomicBoolean saved = new AtomicBoolean(true);
        /**
         * Number of threads using this tab.
         * 
         * @since 2.0
         */
        private AtomicInteger nUsingThreads = new AtomicInteger();

        /* Constructor */

        /**
         * Construct an AutomatonTab, given it's index, and the type of automaton it
         * will contain.
         * 
         * @param index The index of this tab
         * @param type  The type of the automaton this tab will hold
         **/
        @SuppressWarnings("unchecked")
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
            canvas = new JSVGCanvas();

            // Use mouse for translation
            canvas.getInteractors().add(new AbstractPanInteractor() {
                @Override
                public boolean startInteraction(InputEvent ie) {
                    int mods = ie.getModifiersEx();
                    return ie.getID() == MouseEvent.MOUSE_PRESSED &&
                            (mods & InputEvent.BUTTON1_DOWN_MASK) != 0;
                }
            });

            canvas.addMouseWheelListener(e -> {
                Action action = null;
                if (e.isControlDown()) {
                    if (e.getWheelRotation() < 0)
                        action = canvas.getActionMap().get(JSVGCanvas.ZOOM_IN_ACTION);
                    else if (e.getWheelRotation() > 0)
                        action = canvas.getActionMap().get(JSVGCanvas.ZOOM_OUT_ACTION);
                } else {
                    if (e.getWheelRotation() < 0) {
                        action = canvas.getActionMap().get(JSVGCanvas.FAST_SCROLL_UP_ACTION);
                    } else if (e.getWheelRotation() > 0) {
                        action = canvas.getActionMap().get(JSVGCanvas.FAST_SCROLL_DOWN_ACTION);
                    }
                }
                if (action != null)
                    action.actionPerformed(null);
            });

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
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(8, 8, 8, 8);
            c.gridwidth = 1;

            /* Controller Input */

            // Controller input label
            JLabel controllerInputLabel = new JLabel("# Controllers:");
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 0;
            c.gridy = 0;
            container.add(controllerInputLabel, c);

            // Controller input spinner
            controllerInput = new JSpinner(new SpinnerNumberModel(1, 1, Automaton.MAX_NUMBER_OF_CONTROLLERS, 1));
            controllerInput.addChangeListener(e -> setSaved(false));
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 1;
            c.gridy = 0;
            container.add(controllerInput, c);

            /* Event Input */

            // Event input label
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 0;
            c.gridy = 1;
            container.add(new TooltipComponent(new JLabel("Enter events:"), getTooltipText("EVENT_INPUT", type)), c);

            // Event input box
            eventInput = createTextPaneWithTraversal();
            JScrollPane eventInputScrollPane = new JScrollPane(eventInput) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, 200);
                }
            };
            if (type == Automaton.Type.SUBSET_CONSTRUCTION) {
                eventInput.setEditable(false);
            }
            watchForChanges(eventInput);
            c.ipady = 100;
            c.weightx = 0.5;
            c.weighty = 1.0;
            c.gridx = 0;
            c.gridy = 2;
            container.add(eventInputScrollPane, c);

            /* State Input */

            // State input label
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.0;
            c.gridx = 1;
            c.gridy = 1;
            container.add(new TooltipComponent(new JLabel("Enter states:"), getTooltipText("STATE_INPUT", type)), c);

            // State input box
            stateInput = createTextPaneWithTraversal();
            JScrollPane stateInputScrollPane = new JScrollPane(stateInput) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, 200);
                }
            };
            if (type == Automaton.Type.SUBSET_CONSTRUCTION) {
                stateInput.setEditable(false);
            }
            watchForChanges(stateInput);
            c.ipady = 100;
            c.weightx = 0.5;
            c.weighty = 1.0;
            c.gridx = 1;
            c.gridy = 2;
            container.add(stateInputScrollPane, c);

            /* Transition Input */

            c.gridwidth = 2;

            // Transition input label
            c.ipady = 0;
            c.weightx = 1.0;
            c.weighty = 0.0;
            c.gridx = 0;
            c.gridy = 3;
            container.add(
                    new TooltipComponent(new JLabel("Enter transitions:"), getTooltipText("TRANSITION_INPUT", type)),
                    c);

            // Transition input box
            transitionInput = createTextPaneWithTraversal();
            JScrollPane transitionInputScrollPane = new JScrollPane(transitionInput) {
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(200, 200);
                }
            };
            if (type == Automaton.Type.SUBSET_CONSTRUCTION) {
                transitionInput.setEditable(false);
            }
            watchForChanges(transitionInput);
            c.ipady = 200;
            c.weightx = 0.5;
            c.weighty = 1.0;
            c.gridx = 0;
            c.gridy = 4;
            container.add(transitionInputScrollPane, c);

            /* Generate Automaton Button */

            generateAutomatonButton = new JButton("Generate Automaton From Code");
            generateAutomatonButton.setFocusable(false);
            generateAutomatonButton.setToolTipText(
                    """
                            <html>Create a new automaton from the above input, saving it to file. For small automata, an image of the graph is
                            automatically generated.<br>
                            <b><u>NOTE</u></b>: The generated automaton is saved to file, not the code itself.
                            This means that your automaton is not saved until you have generated it.
                            </html>""");
            generateAutomatonButton.addActionListener(e -> generateAutomatonButtonPressed());
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 5;
            container.add(generateAutomatonButton, c);

            /* Generate Image Button */

            generateImageButton = new JButton("Generate Image");
            generateImageButton.setFocusable(false);
            generateImageButton.setToolTipText(
                    """
                            <html>Given the generated automaton, produce an image of the graph, displaying it to the right.<br>
                            <b><u>NOTE</u></b>: This process can take a long time for large automata.
                            </html>""");
            generateImageButton.addActionListener(e -> generateImage());
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 6;
            container.add(generateImageButton, c);

            /* View Image in Browser Button */

            viewImageInBrowserButton = new JButton("View Image in Browser");
            viewImageInBrowserButton.setFocusable(false);
            viewImageInBrowserButton.setToolTipText(
                    "<html>View an enlarged version of the generated image in your default browser.</html>");
            viewImageInBrowserButton.addActionListener(e -> viewInBrowser());
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 7;
            container.add(viewImageInBrowserButton, c);

            /* Show Information Button */

            showAutomatonInfoButton = new JButton("Show Information");
            showAutomatonInfoButton.setFocusable(false);
            showAutomatonInfoButton.setToolTipText("<html>Show basic information about this automaton.</html>");
            showAutomatonInfoButton.addActionListener(e -> {
                AutomatonTab tab = getCurrentTab();
                try {
                    new AutomatonInfoOutput(JDec.this, "Automaton Info", tab.automaton);
                } catch (RuntimeException re) {
                    displayException(re);
                }
            });
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 8;
            container.add(showAutomatonInfoButton, c);

            /* Explore Automaton Button */

            exploreAutomatonButton = new JButton("Explore");
            exploreAutomatonButton.setFocusable(false);
            exploreAutomatonButton
                    .setToolTipText("<html>Interactively crawl through the structure, state by state.</html>");
            exploreAutomatonButton.addActionListener(e -> {
                AutomatonTab tab = getCurrentTab();
                try {
                    new AutomataExplorer(JDec.this, tab.automaton, "Explore");
                } catch (RuntimeException re) {
                    displayException(re);
                }
            });
            c.ipady = 0;
            c.weightx = 0.5;
            c.weighty = 0.1;
            c.gridx = 0;
            c.gridy = 9;
            container.add(exploreAutomatonButton, c);

            return container;

        }

        /**
         * Create a textpane that allows the user to use traversal keys to navigate in
         * between panes.
         * NOTE: These traversal keys are likely 'Tab' to go forward and 'Shift + Tab'
         * to go backward.
         * 
         * @return The instantiated textpane with traversal keys added
         **/
        private JTextPane createTextPaneWithTraversal() {

            JTextPane pane = new JTextPane();

            // String[] words =
            // {"about","after","again","against","alone","along","another","around",
            // "because","before","below","between","Hello","heritage","happiness","goodbye","cruel","car","war","will",
            // "world","wall"};

            // final Completer completer = new TrieCompleter(Arrays.asList(words));
            // final AutoTyper typer = new AutoTyper(pane, completer);
            // final Keymap keymap = JTextComponent.addKeymap(null, pane.getKeymap());
            // pane.setKeymap(keymap);
            // // keymap.addActionForKeyStroke(KeyStroke.getKeyStroke("TAB"),
            // typer.completeAction);
            // keymap.addActionForKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,
            // InputEvent.CTRL_DOWN_MASK), typer.completeAction);

            pane.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
            pane.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);

            return pane;

        }

        /**
         * Add a DocumentListener to the specified text pane, which will set this tab's
         * saved status
         * to false whenever the input in the text pane changes.
         * 
         * @param textPane The text pane to monitor for changes
         **/
        private void watchForChanges(JTextPane textPane) {

            textPane.getDocument().addDocumentListener(new DocumentListener() {

                @Override
                public void changedUpdate(DocumentEvent e) {
                    setSaved(false);
                }

                @Override
                public void insertUpdate(DocumentEvent e) {
                    setSaved(false);
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    setSaved(false);
                }

            });
        }

        /**
         * Update this tab's title, based on the filename and the saved status.
         **/
        private synchronized void updateTabTitle() {

            String title = FilenameUtils.getBaseName(ioAdapter.getFile().getName());

            // Temporary files are always considered unsaved, since the directory is wiped
            // upon closing of the program
            if (!isSaved() || usingTemporaryFiles())
                title += "*";

            tabbedPane.setTitleAt(index, title);

        }

        /**
         * Update the saved status, making the necessary updates to the tab's GUI.
         * 
         * @param newSavedStatus the new saved status
         **/
        public synchronized void setSaved(boolean newSavedStatus) {

            if (newSavedStatus != isSaved()) {
                saved.set(newSavedStatus);
                updateTabTitle();
            }

            generateAutomatonButton.setEnabled(!isSaved());

        }

        /**
         * Check to see if this tab is saved.
         * 
         * @return Whether or not this tab is presently saved
         **/
        public synchronized boolean isSaved() {
            return saved.get();
        }

        /**
         * Refresh the GUI by re-generating the GUI input code.
         * <p>
         * NOTE: This method is quite expensive, as it requires the entire
         * automaton to be read and then turned in a form representable by
         * strings.
         **/
        public synchronized void refreshGUI() {

            logger.info("Starting refresh...");
            StopWatch stopWatch = StopWatch.createStarted();

            final boolean prevSavedStatus = isSaved();

            SwingUtilities.invokeLater(() -> {
                AutomatonGuiInputGenerator<?> generator = automaton.getGuiInputGenerator();
                generator.refresh();
                controllerInput.setValue(automaton.getNumberOfControllers());
                eventInput.setText(generator.getEventInput());
                stateInput.setText(generator.getStateInput());
                transitionInput.setText(generator.getTransitionInput());
                if (prevSavedStatus)
                    setSaved(prevSavedStatus);
                logger.debug("Finished in " + stopWatch.getDuration().toMillis() + "ms.");
            });

        }

        /**
         * Check to see if this tab has any unsaved information (which includes
         * information subject to
         * loss due to temporary files). The only un-generated code this won't account
         * for is if there
         * was input code, then it was all cleared.
         * 
         * @return Whether or not this tab has any unsaved information
         **/
        public synchronized boolean hasUnsavedInformation() {

            // If there is nothing in the input boxes, then obviously there is no unsaved
            // information
            if (eventInput.getText().isEmpty() && stateInput.getText().isEmpty() && transitionInput.getText().isEmpty())
                return false;

            // If there is ungenerated GUI input code, then there is unsaved information
            if (!isSaved())
                return true;

            // Temporary files are considered "unsaved"
            if (usingTemporaryFiles())
                return true;

            // Otherwise, there is no unsaved information
            return false;

        }

        /**
         * Check to see if this tab is using temporary files.
         * 
         * @return Whether or not the tab is using temporary files to store the
         *         automaton
         **/
        public synchronized boolean usingTemporaryFiles() {

            return ioAdapter.getFile().getParentFile().getAbsolutePath().equals(TEMPORARY_DIRECTORY.getAbsolutePath());

        }

    } // AutomatonTab

    /**
     * Action handler for components of JDec.
     * 
     * @since 2.1.0
     */
    private class JDecActionHandler implements ActionListener {
        /**
         * This method handles all of the actions triggered when the user interacts with
         * the main menu.
         * 
         * @param event The triggered event
         **/
        @Override
        public void actionPerformed(ActionEvent event) {

            int index = tabbedPane.getSelectedIndex();
            // Only get the tab if it actually exists
            AutomatonTab tab = index >= 0 ? tabs.get(index) : null;

            // Execute the appropriate command
            switch (event.getActionCommand()) {

                /* FILE STUFF */

                case "Clear":

                    clearTab(tab);
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

                case "Save":
                    if (!tab.usingTemporaryFiles()) {
                        try {
                            generateAutomatonButtonPressed();
                            tab.ioAdapter.setAutomaton(tab.automaton);
                            tab.ioAdapter.save();
                            tab.setSaved(true);
                        } catch (IOException | RuntimeException e) {
                            displayException(e);
                        }
                        break;
                    }
                case "Save As...":

                    // Prompt user to save Automaton to the specified file
                    if (saveFile("Choose file") != null) {
                        tab.updateTabTitle();
                        if (tab.automaton != null)
                            tab.automaton = tab.ioAdapter.getAutomaton();
                        tab.setSaved(true);
                    }

                    break;

                case "Export...":

                {
                    JFileChooser fileChooser = new OverwriteCheckingFileChooser() {
                        @Override
                        protected JDialog createDialog(Component parent) {
                            JDialog dialog = super.createDialog(JDec.this);
                            dialog.setModal(true);
                            return dialog;
                        }
                    };
                    fileChooser.setAcceptAllFileFilterUsed(false);
                    fileChooser.setDialogTitle("Export");
                    java.util.List.of(Format.PNG, Format.SVG, Format.DOT).forEach(f -> {
                        fileChooser.addChoosableFileFilter(new FileNameExtensionFilter(f.name() + " file", f.fileExtension));
                    });
                    if (currentDirectory != null)
                        fileChooser.setCurrentDirectory(currentDirectory);

                    /* Prompt user to select a filename */

                    int result = fileChooser.showSaveDialog(null);

                    /* No file was selected */

                    if (result != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null)
                        return;

                    FileNameExtensionFilter usedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();

                    if (!FilenameUtils.isExtension(fileChooser.getSelectedFile().getName(),
                            usedFilter.getExtensions())) {
                        fileChooser.setSelectedFile(new File(
                                fileChooser.getSelectedFile().getAbsolutePath()
                                        + FilenameUtils.EXTENSION_SEPARATOR
                                        + usedFilter.getExtensions()[0]));
                    }

                    try {
                        tab.automaton.getDotConverter().export(fileChooser.getSelectedFile());
                    } catch (IOException ioe) {
                        logger.catching(ioe);
                    }
                }

                    break;

                case "Open":

                    // Prompt user to select Automaton from file (stop if they did not pick a file),
                    // placing it in a new tab
                    if (selectFile("Select Automaton", -1) == null)
                        break;

                    index = tabbedPane.getSelectedIndex(); // Index has changed since a new tab was created
                    tab = tabs.get(index);
                    tab.setSaved(true);
                    tab.updateTabTitle();

                case "Refresh Tab":

                    refresh(index);
                    break;

                case "Close Tab":

                    closeCurrentTab();
                    break;

                case "Quit":

                    dispatchEvent(new WindowEvent(JDec.this, WindowEvent.WINDOW_CLOSING));
                    break;

                /* VIEW */

                case "Previous Tab":

                    if (--index < 0)
                        index += tabbedPane.getTabCount();

                    tabbedPane.setSelectedIndex(index);
                    break;

                case "Next Tab":

                    if (++index == tabbedPane.getTabCount())
                        index = 0;

                    tabbedPane.setSelectedIndex(index);
                    break;

                case "View in Browser":

                    viewInBrowser();
                    break;

                case "Show event-specific view":
                    new EventSpecificView((UStructure) tab.automaton);
                    break;

                /* AUTOMATA OPERATIONS */

                case "Accessible":

                    Automaton automaton = tab.automaton.accessible();

                    // Create new tab for the accessible automaton
                    if (automaton == null) {
                        temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after all, so we
                                                              // can re-use it
                        displayErrorMessage("Operation Failed", "Please specify a starting state.");
                    } else
                        createTab(automaton);
                    break;

                case "Co-Accessible":

                    // Create new tab for the co-accessible automaton
                    createTab(tab.automaton.coaccessible());
                    break;

                case "Trim":

                    automaton = tab.automaton.trim();

                    // Create new tab for the trim automaton
                    if (automaton == null) {
                        temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after all, so we
                                                              // can re-use it
                        displayErrorMessage("Operation Failed", "Please specify a starting state.");
                    } else
                        createTab(automaton);
                    break;

                case "Complement":

                    try {
                        // Create new tab with the complement
                        createTab(tab.automaton.complement());
                    } catch (OperationFailedException e) {
                        logger.catching(e);
                        temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after all, so we
                                                              // can re-use it
                        displayErrorMessage("Operation Failed",
                                "There already exists a dump state, so the complement could not be taken again.");
                    }
                    break;

                case "Generate Twin Plant":

                    // Create new tab with the twin plant
                    createTab(AutomataOperations.generateTwinPlant(tab.automaton));
                    break;

                case "Intersection": {
                    // Allow user to pick other automaton
                    int otherIndex = pickAutomaton("Which automaton would you like to take the intersection with?",
                            index);
                    if (otherIndex == -1)
                        break;
                    Automaton otherAutomaton = tabs.get(otherIndex).automaton;

                    try {
                        // Create new tab with the intersection
                        createTab(AutomataOperations.intersection(tab.automaton, otherAutomaton));
                    } catch (IncompatibleAutomataException e) {
                        logger.catching(e);
                        temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after all, so we
                                                              // can re-use it
                        displayErrorMessage("Operation Failed",
                                "Please ensure that both automata have the same number of controllers and that there are no incompatible events (meaning that events share the same name but have different properties).");
                    }
                }

                    break;

                case "Union": {
                    // Allow user to pick other automaton
                    int otherIndex = pickAutomaton("Which automaton would you like to take the union with?", index);
                    if (otherIndex == -1)
                        break;
                    Automaton otherAutomaton = tabs.get(otherIndex).automaton;

                    try {
                        // Create new tab with the union
                        createTab(AutomataOperations.union(tab.automaton, otherAutomaton));
                    } catch (IncompatibleAutomataException e) {
                        logger.catching(e);
                        temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after all, so we
                                                              // can re-use it
                        displayErrorMessage("Operation Failed",
                                "Please ensure that both automata have the same number of controllers and that there are no incompatible events (meaning that events share the same name but have different properties).");
                    }
                }

                    break;

                case "Synchronized Composition":

                    buildSynchronizedProduct(tab);
                    break;

                case "Subset Construction":

                    buildSubsetConstruction(tab);
                    break;

                case "Relabel States":

                    relabelStates(tab);
                    break;

                case "Build Automaton Representation": {
                    SubsetConstruction subsetConstruction = (SubsetConstruction) tab.automaton;
                    int controller = pickController("Select the controller to build automaton representation with.",
                            true);
                    if (controller < 0)
                        return;
                    tab.nUsingThreads.incrementAndGet();
                    try {
                        createTab(subsetConstruction.buildAutomatonRepresentationOf(controller));
                        setBusyCursor(false);
                    } catch (RuntimeException e) {
                        temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after
                                                              // all, so we can re-use it
                        setBusyCursor(false);
                        displayException(e);
                    } /*
                       * catch (OperationFailedException e) {
                       * temporaryFileIndex.decrementAndGet(); // We did not need this temporary file
                       * after all, so we can re-use it
                       * setBusyCursor(false);
                       * displayErrorMessage("Operation Failed", "Failed to add state.");
                       * }
                       */
                    tab.nUsingThreads.decrementAndGet();
                    updateComponentsWhichRequireAutomaton();
                }
                    break;

                case "Add Communications": {

                    UStructure uStructure = ((UStructure) tab.automaton);

                    // Display error message if there was not enough controllers
                    if (uStructure.getNumberOfControllers() == 1) {
                        displayErrorMessage("Not Enough Controllers",
                                "There must be more than 1 controller in order for a communication to take place.");
                        break;
                    }

                    // Display warning message, and abort the operation if requested
                    if (uStructure.getSizeOfPotentialAndNashCommunications() > 0)
                        if (!askForConfirmation("Communications Already Exist",
                                """
                                        This U-Structure appears to already have had communications added. Are you sure you want to proceed?
                                        WARNING: This may result in duplicate communications."""))
                            break;

                    setBusyCursor(true);

                    // Create a copy of the current automaton with all communications added and
                    // potential communications marked
                    UStructure uStructureWithCommunications = UStructureOperations.addCommunications(uStructure);
                    createTab(uStructureWithCommunications);

                    setBusyCursor(false);

                    if (TransitionData
                            .containsSelfLoop(uStructureWithCommunications.getPotentialCommunications()))
                        displayMessage("Communication Self-Loop",
                                "Please be advised that at least one of the communications added is a self-loop.",
                                JOptionPane.WARNING_MESSAGE);
                }

                    break;

                case "Generate All": {

                    UStructure uStructure = ((UStructure) tab.automaton);

                    if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
                        displayErrorMessage("Operation Failed",
                                "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
                    else
                        new GenerateFeasibleProtocolsPrompt(JDec.this, uStructure, "Generate All Feasible Protocols",
                                " Specify whether or not a controller is allowed to send to or receive from a certain controller: ");
                }
                    break;

                case "Make Protocol Feasible": {

                    UStructure uStructure = ((UStructure) tab.automaton);

                    if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
                        displayErrorMessage("Operation Failed",
                                "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
                    else
                        new MakeProtocolFeasiblePrompt(JDec.this, uStructure);
                }
                    break;

                case "Find Smallest": {

                    UStructure uStructure = ((UStructure) tab.automaton);

                    if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
                        displayErrorMessage("Operation Aborted",
                                "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
                    else {
                        setBusyCursor(true);
                        java.util.List<Set<CommunicationData>> smallestFeasibleProtocols = uStructure
                                .generateSmallestFeasibleProtocols(uStructure.getPotentialAndNashCommunications());
                        setBusyCursor(false);
                        new FeasibleProtocolOutput(JDec.this, uStructure, smallestFeasibleProtocols,
                                "Smallest Feasible Protocols",
                                " Protocol(s) with the fewest number of communications: ");
                    }
                }
                    break;

                case "Find First": {

                    UStructure uStructure = ((UStructure) tab.automaton);

                    if (uStructure.getSizeOfPotentialAndNashCommunications() == 0)
                        displayErrorMessage("Operation Aborted",
                                "The U-Structure needs to have at least 1 potential communication. Please ensure that you have added communications to it.");
                    else {
                        setBusyCursor(true);
                        Set<CommunicationData> feasibleProtocol = uStructure
                                .generateFeasibleProtocol(uStructure.getPotentialAndNashCommunications());
                        setBusyCursor(false);
                        new FeasibleProtocolOutput(JDec.this, uStructure, Collections.singletonList(feasibleProtocol),
                                "Feasible Protocol", " The first protocol found: ");
                    }
                }
                    break;

                case "Show control configurations":
                    new ControlConfigDisplay(JDec.this);
                    break;

                case "Test Inference Observability":

                    testInferenceObservability(tab);
                    break;

                case "Generate local control decisions": {
                    AutomatonTab currTab = tab;
                    String[] frameworks = {"Enable by default (EBD)", "Disable by default (DBD)"};
                    var choice = (String) JOptionPane.showInputDialog(
                        JDec.this, "Select control framework", "Select control framework", JOptionPane.PLAIN_MESSAGE,
                        null, frameworks, frameworks[0]
                    );
                    if (choice == null) return;
                    boolean enablementSelected = Objects.equals(frameworks[0], choice);
                    Thread observabilityThread = new Thread(
                            () -> {
                                JLabel label = new JLabel("Running observability test", SwingConstants.CENTER);
                                currTab.add(label, BorderLayout.SOUTH);
                                setBusyCursor(true);
                                currTab.nUsingThreads.incrementAndGet();
                                var ambList = AutomataOperations.generateLocalControlDecisions(currTab.automaton, enablementSelected);
                                currTab.nUsingThreads.decrementAndGet();
                                tabbedPane.setSelectedComponent(currTab);
                                setBusyCursor(false);
                                currTab.remove(label);
                                if (ambList.isEmpty()) {
                                    displayMessage("Local Control Decisions", "There is no local control solution for this system in the " + choice + " framework.",
                                    JOptionPane.INFORMATION_MESSAGE);
                                    return;
                                }
                                new AmbiguityLevelOutput(JDec.this, "Local Control Decisions", ambList);
                            },
                            FilenameUtils.removeExtension(currTab.ioAdapter.getFile().getName())
                                    + " - Observability Test");
                    observabilityThread.start();
                }
                    break;

                case "Test Controllability":

                    if (tab.automaton.testControllability())
                        displayMessage("Passed Test", "The system is controllable.", JOptionPane.INFORMATION_MESSAGE);
                    else
                        displayMessage("Failed Test", "The system is not controllable.",
                                JOptionPane.INFORMATION_MESSAGE);
                    break;

                case "Output Bipartite Graphs": {

                    /* Set up the file chooser */

                    JFileChooser fileChooser = new OverwriteCheckingFileChooser() {
                        @Override
                        protected JDialog createDialog(Component parent) {
                            JDialog dialog = super.createDialog(JDec.this);
                            dialog.setModal(true);
                            return dialog;
                        }
                    };

                    fileChooser.setDialogTitle("Output bipartite graph");

                    /* Filter files */

                    fileChooser.setAcceptAllFileFilterUsed(false);
                    FileNameExtensionFilter jsonFilter = new FileNameExtensionFilter("JSON files",
                            AutomatonJsonFileAdapter.EXTENSION);
                    fileChooser.addChoosableFileFilter(jsonFilter);

                    /* Begin at the most recently accessed directory */

                    if (currentDirectory != null)
                        fileChooser.setCurrentDirectory(currentDirectory);

                    /* Prompt user to select a filename */

                    int result = fileChooser.showSaveDialog(null);

                    /* No file was selected */

                    if (result != JFileChooser.APPROVE_OPTION || fileChooser.getSelectedFile() == null)
                        return;

                    FileNameExtensionFilter usedFilter = (FileNameExtensionFilter) fileChooser.getFileFilter();

                    if (!FilenameUtils.isExtension(fileChooser.getSelectedFile().getName(),
                            usedFilter.getExtensions())) {
                        fileChooser.setSelectedFile(new File(
                                fileChooser.getSelectedFile().getAbsolutePath()
                                        + FilenameUtils.EXTENSION_SEPARATOR
                                        + usedFilter.getExtensions()[0]));
                    }

                    File dest = fileChooser.getSelectedFile();

                    JsonObject graphJsonObject = BipartiteGraphExport.generateBipartiteGraphJson(tab.automaton);
                    dest.delete();
                    try (Writer writer = IOUtils.buffer(new FileWriter(dest))) {
                        new Gson().toJson(graphJsonObject, writer);
                    } catch (IOException ioe) {
                        throw new UncheckedIOException(ioe);
                    }
                }
                    break;

                case "Output Bipartite Graph Image": {

                        new BipartiteGraphView(tab.automaton);
                    }
                        break;

                case "Test Incremental Observability": {
                    IncrementalObsAutomataSelectionPrompt prompt = new IncrementalObsAutomataSelectionPrompt(JDec.this);
                    prompt.setVisible(true);
                    if (!prompt.userSelected())
                        break;
                    Set<Automaton> plants = prompt.getPlants(), specs = prompt.getSpecs();
                    if (plants.isEmpty() || specs.isEmpty())
                        displayMessage("Invalid selection", "Please try again.",
                                JOptionPane.WARNING_MESSAGE);
                    else if (AutomataOperations.testIncrementalObservability(plants, specs))
                        displayMessage("Passed Test", "The system is inference observable.",
                                JOptionPane.INFORMATION_MESSAGE);
                    else
                        displayMessage("Failed Test", "The system is not inference observable.",
                                JOptionPane.INFORMATION_MESSAGE);
                }
                    break;

                case "Random Automaton":

                    new RandomAutomatonPrompt(JDec.this);
                    break;

                case "Third-party License":
                    try {
                        InputStream thirdPartyInfo = getResourceURL("META-INF/jdec-assembly-THIRD-PARTY.txt")
                                .openStream();
                        TextPopup popup = new TextPopup(JDec.this, "Third-party License Notice");
                        thirdPartyInfo.transferTo(popup.getOutputStream());
                    } catch (UncheckedIOException ioe) {
                        displayMessage("Third-party License Notice", "Third-party license information not found!",
                                JOptionPane.WARNING_MESSAGE);
                    } catch (IOException ioe) {
                        displayException(ioe);
                    }
                    break;
                case "Open GitHub Repository":
                    try {
                        Desktop.getDesktop().browse(new URI("https://github.com/Summer2023SHY/JDec"));
                    } catch (IOException | URISyntaxException e) {
                        displayException(e);
                    }
                    break;
                case "View License":
                    try {
                        Desktop.getDesktop()
                                .browse(new URI("https://github.com/Summer2023SHY/JDec/blob/main/LICENSE"));
                    } catch (IOException | URISyntaxException e) {
                        displayException(e);
                    }
                    break;
            }

            updateComponentsWhichRequireAutomaton();

        }

        /**
         * Clears the content of the specified tab.
         * 
         * @param tab a tab to clear
         */
        private void clearTab(final AutomatonTab tab) {
            // Clear input fields
            tab.eventInput.setText(StringUtils.EMPTY);
            tab.stateInput.setText(StringUtils.EMPTY);
            tab.transitionInput.setText(StringUtils.EMPTY);

            // Set blank image
            tab.canvas.setURI(null);
        }

        /**
         * Builds the synchronized product of the automaton stored in the specified tab.
         * 
         * @param tab a tab
         * 
         * @see Automaton#synchronizedComposition()
         */
        private void buildSynchronizedProduct(final AutomatonTab tab) {
            // Check for unmarked states and display warning.
            if (tab.automaton.hasUnmarkedState())
                displayMessage("Unmarked States",
                        "There are 1 or more states that are unmarked. Since it is assumed that the system is prefix-closed, those states will be considered marked.",
                        JOptionPane.WARNING_MESSAGE);

            // Create new tab with the U-structure generated by synchronized composition
            setBusyCursor(true);

            Thread synchronizedCompositionThread = new Thread(
                    () -> {
                        tab.nUsingThreads.incrementAndGet();
                        syncCompositionLock.lock();
                        try {
                            UStructure uStructure = AutomataOperations.synchronizedComposition(tab.automaton);
                            createTab(uStructure);
                            setBusyCursor(false);
                        } catch (NoInitialStateException e) {
                            logger.catching(e);
                            temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after
                                                                  // all, so we can re-use it
                            setBusyCursor(false);
                            displayErrorMessage("Operation Failed",
                                    "Please ensure that you have specified a starting state (using an '@' symbol).");
                        } catch (OperationFailedException e) {
                            logger.catching(e);
                            temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after
                                                                  // all, so we can re-use it
                            setBusyCursor(false);
                            displayErrorMessage("Operation Failed", "Failed to add state.");
                        }
                        tab.nUsingThreads.decrementAndGet();
                        syncCompositionLock.unlock();
                        updateComponentsWhichRequireAutomaton();
                    },
                    FilenameUtils.removeExtension(tab.ioAdapter.getFile().getName())
                            + " - Synchronized composition");
            synchronizedCompositionThread.start();

        }

        /**
         * Builds a subset construction of the U-Structure stored in the specified tab.
         * 
         * @param tab a tab
         * 
         * @see UStructure#subsetConstruction(int)
         */
        private void buildSubsetConstruction(final AutomatonTab tab) {
            UStructure uStructure = (UStructure) tab.automaton;

            // Create new tab with the U-structure generated by synchronized composition
            setBusyCursor(true);
            try {
                int controller = pickController("Select the controller to execute subset construction with.",
                        true);
                if (controller == -1) {
                    temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after all, so
                                                          // we can re-use it
                    setBusyCursor(false);
                    return;
                }
                Automaton automaton = UStructureOperations.relabelConfigurationStates(uStructure).subsetConstruction(controller);
                createTab(automaton);
                setBusyCursor(false);
            } catch (RuntimeException e) {
                temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after all, so we
                                                      // can re-use it
                setBusyCursor(false);
                displayException(e);
            } /*
               * catch (OperationFailedException e) {
               * temporaryFileIndex.decrementAndGet(); // We did not need this temporary file
               * after all, so we can re-use it
               * setBusyCursor(false);
               * displayErrorMessage("Operation Failed", "Failed to add state.");
               * }
               */
        }

        /**
         * Relabels states in the U-structure stored in the specified tab, adding
         * duplicate states as needed.
         * 
         * @param tab a tab
         */
        private void relabelStates(final AutomatonTab tab) {

            UStructure uStructure = (UStructure) tab.automaton;

            // Create new tab with the U-structure generated by synchronized composition
            setBusyCursor(true);
            new Thread(
                    () -> {
                        tab.nUsingThreads.incrementAndGet();
                        try {
                            createTab(UStructureOperations.relabelConfigurationStates(uStructure));
                            setBusyCursor(false);
                        } catch (RuntimeException e) {
                            temporaryFileIndex.decrementAndGet(); // We did not need this temporary file after
                                                                  // all,
                                                                  // so we can re-use it
                            setBusyCursor(false);
                            displayException(e);
                        } /*
                           * catch (OperationFailedException e) {
                           * temporaryFileIndex.decrementAndGet(); // We did not need this temporary file
                           * after all, so we can re-use it
                           * setBusyCursor(false);
                           * displayErrorMessage("Operation Failed", "Failed to add state.");
                           * }
                           */
                        tab.nUsingThreads.decrementAndGet();
                        updateComponentsWhichRequireAutomaton();
                    },
                    FilenameUtils.removeExtension(tab.ioAdapter.getFile().getName()) + " - State relabeling")
                    .start();

        }

        /**
         * Tests whether the automaton stored in the specified tab is inference
         * observable.
         * 
         * @param tab a tab
         * 
         * @see Automaton#testObservability(boolean)
         */
        private void testInferenceObservability(final AutomatonTab tab) {
            final int ambLevelDisplayResponse = JOptionPane.showConfirmDialog(JDec.this,
                    "Do you want the calculated inference level displayed?", "Display inference level?",
                    JOptionPane.YES_NO_OPTION);
            if (ambLevelDisplayResponse == JOptionPane.CLOSED_OPTION) {
                return;
            }
            Thread observabilityThread = new Thread(
                    () -> {
                        JLabel label = new JLabel("Running observability test", SwingConstants.CENTER);
                        tab.add(label, BorderLayout.SOUTH);
                        try {
                            final boolean displayAmbLevel = ambLevelDisplayResponse == JOptionPane.YES_OPTION;
                            setBusyCursor(true);
                            tab.nUsingThreads.incrementAndGet();
                            Pair<Boolean, OptionalInt> observability = tab.automaton
                                    .testObservability(displayAmbLevel);
                            tab.nUsingThreads.decrementAndGet();
                            tabbedPane.setSelectedComponent(tab);
                            setBusyCursor(false);
                            tab.remove(label);
                            if (observability.getLeft())
                                if (displayAmbLevel) {
                                    // new AmbiguityLevelOutput(JDec.this, "Passed Test", observability);
                                    displayMessage("Passed Test",
                                            "The system is inference observable with "
                                                    + observability.getRight().getAsInt()
                                                    + (observability.getRight().getAsInt() == 1 ? " level"
                                                            : " levels")
                                                    + " of inferencing",
                                            JOptionPane.INFORMATION_MESSAGE);
                                } else
                                    displayMessage("Passed Test", "The system is inference observable.",
                                            JOptionPane.INFORMATION_MESSAGE);
                            else
                                displayMessage("Failed Test", "The system is not inference observable.",
                                        JOptionPane.INFORMATION_MESSAGE);
                        } catch (Exception e) {
                            setBusyCursor(false);
                            tab.remove(label);
                            JDec.this.displayException(e);
                        }
                    },
                    FilenameUtils.removeExtension(tab.ioAdapter.getFile().getName())
                            + " - Observability Test");
            observabilityThread.start();
        }

    }

}
