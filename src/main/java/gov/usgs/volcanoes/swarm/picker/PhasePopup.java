package gov.usgs.volcanoes.swarm.picker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import gov.usgs.volcanoes.swarm.picker.Phase.Builder;

public class PhasePopup extends JPopupMenu {
  private JMenuItem ip0;
  private JMenu p;
  private JMenu s;
  private final String channel;
  private final long time;

  public PhasePopup(String channel, long time) {
    this.channel = channel;
    this.time = time;

    ip0 = new JMenuItem("iP0");
    ip0.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().channel(PhasePopup.this.channel).onset(Phase.Onset.I)
            .duration(1000).maxAmplitude(100).phaseType(Phase.PhaseType.P)
            .time(PhasePopup.this.time).weight(0).build();
      }
    });
    add(ip0);

    p = new JMenu("P");
    add(p);
    addOnset(p);

    s = new JMenu("S");
    add(s);
    addOnset(s);
  }

  private static void addOnset(JMenu menu) {
    menu.add(new JMenuItem("i"));
    menu.add(new JMenuItem("e"));
  }

  private static void addFirstMotion(JMenu menu) {
    menu.add(new JMenuItem("i"));
    menu.add(new JMenuItem("e"));
  }

}
