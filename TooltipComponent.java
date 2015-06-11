/**
 * TooltipComponent - Used to add a tooltip with the specified text to the left of the given component.
 *
 * @author Micah Stairs
 **/

import java.awt.*;
import javax.swing.*;

public class TooltipComponent extends JPanel {

  /**
   * Construct a tooltip component with the specified text.
   * @param component   The component that we are adding a tool tip to
   * @param tooltipText The text to be displayed in the tool tip
   **/
  public TooltipComponent(Component component, String tooltipText) {

      /* Create tooltip */

    JButton openTooltip = new JButton("?");
    openTooltip.setFocusable(false);
    openTooltip.setToolTipText(tooltipText);

      /* Position components */

    setLayout(new BorderLayout());
    add(component, BorderLayout.CENTER);
    add(openTooltip, BorderLayout.WEST);

  }

 } // TooltipComponent class