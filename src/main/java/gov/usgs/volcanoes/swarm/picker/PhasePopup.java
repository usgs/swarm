package gov.usgs.volcanoes.swarm.picker;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

public class PhasePopup extends JPopupMenu {
  private JMenuItem p1;

  public PhasePopup() {
    p1 = new JMenuItem("P1");
    add(p1);
  }

}
