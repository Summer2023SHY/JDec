import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class AutomataGUI extends JFrame {

	private JTextPane eventInput;
    
    public static void main(String[] args) {
		new AutomataGUI();
    }

    public AutomataGUI() {

    	eventInput = new JTextPane();
    	add(eventInput);

    	JButton generateAutomaton = new JButton("Generate");
    	generateAutomaton.addActionListener(new ActionListener() {
 
            public void actionPerformed(ActionEvent e)
            {
                System.out.println(eventInput.getText());
            }
        });    
    	add(generateAutomaton, BorderLayout.SOUTH);

    	setGUIproperties();
    }

    /**
    * Set some default GUI Properties
    */
    private void setGUIproperties() {

        // Pack things in nicely
        pack();
        
        // Ensure our application will be closed when the user presses the "X" */
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(800, 600);

        // Sets screen location in the center of the screen (only works after calling pack)
        setLocationRelativeTo(null);

        // Update Title
        setTitle("Automata Manipulator");

        // Show Screen
        setVisible(true);

    }

}