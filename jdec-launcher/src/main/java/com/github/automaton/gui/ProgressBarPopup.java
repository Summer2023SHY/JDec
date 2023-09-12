package com.github.automaton.gui;

/* 
 * Copyright (C) 2016 Micah Stairs
 * Copyright (C) 2023 Sung Ho Yoon
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.awt.*;
import javax.swing.*;

/**
 * Used to show and update a progress bar inside of a dialog box.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 */
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
      @Override
      public void run() {
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

    // Pack things in nicely
    pack();

    // Make it so that the user cannot close this dialog box
    // NOTE: If we did not do this, then we would have to make sure that the thread governing
    //       the process that is being executed is killed (which is not managed by this class)
    setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

    // Ensure that the dialog cannot be resized
    setResizable(false);

    // Sets screen location in the center of the screen (only works after calling pack)
    setLocationRelativeTo(null);

    // Update title
    setTitle(title);

    // Show screen
    setVisible(true);

  }
    
}
