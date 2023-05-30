package com.github.automaton.gui;

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
   * @param tooltipText The text to be displayed in the tool-tip (if null, then the '?' button will be hidden)
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