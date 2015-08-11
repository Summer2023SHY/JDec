/**
 * ProgressBarPopup - This class is used to show and update a progress bar inside of a dialog box.
 *
 * @author Micah Stairs
 *
 * TABLE OF CONTENTS:
 *  -Instance Variables
 *  -Constructor
 *  -Methods
 **/

import java.awt.*;
import javax.swing.*;

public class ProgressBarPopup extends JDialog {

    /* INSTANCE VARIABLES */

  private JProgressBar progressBar;
  private long nTotalTasks;

    /* CONSTRUCTOR */

  /**
   * Construct a ProgressBarPopup object.
   * @param gui     A reference to the GUI, which is the owner of this dialog box
   * @param title   The title of the popup box
   * @param nTasks  The total number of tasks that need to be completed (which are all treated with an equal weight)
   **/
  public ProgressBarPopup(JFrame gui, final String title, long nTasks) {

    super(gui, true);

    progressBar = new JProgressBar(0, 100);
    progressBar.setValue(0);
    progressBar.setString("0%");
    progressBar.setStringPainted(true);

    nTotalTasks = nTasks;

    // Prevent division by 0
    if (nTotalTasks < 1)
      nTotalTasks = 1;


    EventQueue.invokeLater(new Runnable() {
      @Override public void run() {
        add(progressBar);
        setGUIproperties(title);
      }
    });

  }

    /* METHODS */

  /**
   * Update the progress bar.
   * @param nCompletedTasks The updated number of how many tasks have been completed
   **/
  public void updateProgressBar(long nCompletedTasks) {

    // Prevent the bar from exceeding 100% accidentally
    if (nCompletedTasks > nTotalTasks)
      nCompletedTasks = nTotalTasks;
    
    final int newValue = (int) (((double) nCompletedTasks * 100.0) / (double) nTotalTasks);

    if (newValue != progressBar.getValue())
      EventQueue.invokeLater(new Runnable() {
        @Override public void run() {
          progressBar.setString(newValue + "%");
          progressBar.setValue(newValue);
          progressBar.repaint();
        }
      });
      
  }

  /**
   * Set some default GUI Properties.
   * @param title The title of the pop-up box
   **/
  private void setGUIproperties(String title) {

      /* Pack things in nicely */

    pack();

      /* Sets screen location in the center of the screen (only works after calling pack) */

    setLocationRelativeTo(null);

      /* Update title */

    setTitle(title);

      /* Show screen */

    setVisible(true);

  }
    
}