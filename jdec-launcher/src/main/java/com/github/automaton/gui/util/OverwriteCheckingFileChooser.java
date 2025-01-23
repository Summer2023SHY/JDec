/*
 * Copyright (C) Sung Ho Yoon. All rights reserved.
 * Licensed under the MIT license. See LICENSE file in the project root for details.
 */

package com.github.automaton.gui.util;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * A file chooser that asks for overwrite confirmation to users.
 * 
 * @author Sung Ho Yoon
 * @since 2.1.0
 */
public class OverwriteCheckingFileChooser extends JFileChooser {

    /**
     * Called by the UI when the user hits the Approve button (labeled "Open" or "Save", by default).
     * Checks for additional confirmation if the file selection may result in an overwrite.
     */
    @Override
    public void approveSelection() {
        // Overwrite protection
        // Adapted from https://stackoverflow.com/a/3729157
        if (getDialogType() == SAVE_DIALOG && getSelectedFile().exists()) {
            int result = JOptionPane.showConfirmDialog(this, "Selected file exists, overwrite?",
                    "Existing file", JOptionPane.YES_NO_OPTION);
            switch (result) {
                case JOptionPane.YES_OPTION:
                    break;
                case JOptionPane.CANCEL_OPTION:
                    cancelSelection();
                case JOptionPane.NO_OPTION:
                case JOptionPane.CLOSED_OPTION:
                    return;
            }
        }
        super.approveSelection();
    }
}
