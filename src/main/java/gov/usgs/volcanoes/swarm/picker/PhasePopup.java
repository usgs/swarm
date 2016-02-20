package gov.usgs.volcanoes.swarm.picker;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class PhasePopup extends JPopupMenu {
  private JMenu p;
  private JMenu s;
  private final String channel;
  private final long time;
  private final Event event;
  private Component parent;

  public PhasePopup(final Event event, final String channel, final long time) {
    this.channel = channel;
    this.time = time;
    this.event = event;

    // final Component parentFrame = SwingUtilities.getAncestorNamed("mainPanel", this.getParent());
    p = new JMenu("P");
    add(p);


    JMenuItem ip0 = new JMenuItem("iP0");
    ip0.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.P)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(0).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    p.add(ip0);

    JMenuItem ip1 = new JMenuItem("iP1");
    ip1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.P)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(1).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    p.add(ip1);

    JMenuItem ep2 = new JMenuItem("eP2");
    ep2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e).phaseType(Phase.PhaseType.P)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(2).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    p.add(ep2);

    JMenuItem ep3 = new JMenuItem("eP3");
    ep3.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e).phaseType(Phase.PhaseType.P)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(3).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    p.add(ep3);

    JMenuItem clearP = new JMenuItem("clear P");
    clearP.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        event.clearPhase(channel, Phase.PhaseType.P);
        parent.validate();
        parent.repaint();
      }
    });
    p.add(clearP);

    s = new JMenu("S");
    add(s);

    JMenuItem is0 = new JMenuItem("iS0");
    is0.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.S)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(0).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    s.add(is0);

    JMenuItem is1 = new JMenuItem("iS1");
    is1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.S)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(1).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    s.add(is1);

    JMenuItem es2 = new JMenuItem("eS2");
    es2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e).phaseType(Phase.PhaseType.S)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(2).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    s.add(es2);

    JMenuItem es3 = new JMenuItem("eS3");
    es3.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e).phaseType(Phase.PhaseType.S)
            .firstMotion(Phase.FirstMotion.UP).time(time).weight(3).build();
        event.setPhase(channel, phase);
        parent.validate();
        parent.repaint();
      }
    });
    s.add(es3);

    JMenuItem clearS = new JMenuItem("clear S");
    clearS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        event.clearPhase(channel, Phase.PhaseType.S);
        parent.validate();
        parent.repaint();
      }
    });
    s.add(clearS);

  }

  public void setParent(Component parent) {
    this.parent = parent;
  }
}
