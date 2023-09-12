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
 * Used to add a tool-tip with the specified text to the left of the given component.
 *
 * @author Micah Stairs
 * 
 * @since 1.0
 **/
public class TooltipComponent extends JPanel {

  /**
   * This value is injected into the HTML by replacing occurrences of 'TOOL_TIP_WIDTH' text with the number,
   * which cause the width of the tool-tip to adjust accordingly.
   **/
  private static final int TOOL_TIP_WIDTH = 450;

  /**
   * Construct a tool-tip component with the specified text.
   * @param component   The component that we are adding a tool-tip to
   * @param tooltipText The text to be displayed in the tool-tip (if {@code null}, then the '?' button will be hidden)
   **/
  public TooltipComponent(Component component, String tooltipText) {

    if (tooltipText != null && tooltipText.equals(""))
      tooltipText = null;

      /* Inject constants into the HTML */

    if (tooltipText != null)
      tooltipText = tooltipText.replaceAll("TOOL_TIP_WIDTH", String.valueOf(TOOL_TIP_WIDTH));

      /* Create tool-tip */

    JButton tooltip = new JButton("?");
    tooltip.setFocusable(false);
    tooltip.setToolTipText(tooltipText);

      /* Position components */

    setLayout(new BorderLayout());
    add(component, BorderLayout.CENTER);
    
    // Hide the '?' button if there is no text to display
    if (tooltipText != null)
      add(tooltip, BorderLayout.WEST);

  }

}
