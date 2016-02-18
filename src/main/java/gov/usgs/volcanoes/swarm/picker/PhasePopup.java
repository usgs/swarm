package gov.usgs.volcanoes.swarm.picker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import gov.usgs.volcanoes.swarm.picker.Phase.Builder;

public class PhasePopup extends JPopupMenu {
  private JMenu p;
  private JMenu s;
  private final String channel;
  private final long time;
  private final Event event;

  public PhasePopup(final Event event, final String channel, final long time) {
    this.channel = channel;
    this.time = time;
    this.event = event;

    p = new JMenu("P");
    add(p);

    JMenuItem ip0 = new JMenuItem("iP0");
    ip0.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.P)
            .time(time).weight(0).build();
        event.setPhase(channel, phase);
      }
    });
    p.add(ip0);

    JMenuItem ip1 = new JMenuItem("iP1");
    ip1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.P)
            .time(time).weight(1).build();
        event.setPhase(channel, phase);
      }
    });
    p.add(ip1);

    JMenuItem ep2 = new JMenuItem("eP2");
    ep2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e).phaseType(Phase.PhaseType.P)
            .time(time).weight(2).build();
        event.setPhase(channel, phase);
      }
    });
    p.add(ep2);

    JMenuItem ep3 = new JMenuItem("eP3");
    ep3.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e).phaseType(Phase.PhaseType.P)
            .time(time).weight(3).build();
        event.setPhase(channel, phase);
      }
    });
    p.add(ep3);

    JMenuItem clearP = new JMenuItem("clear P");
    clearP.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        event.clearPhase(channel, Phase.PhaseType.P);
      }
    });
    p.add(clearP);

    s = new JMenu("S");
    add(s);

    JMenuItem is0 = new JMenuItem("iS0");
    is0.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.S)
            .time(time).weight(0).build();
        event.setPhase(channel, phase);
      }
    });
    s.add(is0);

    JMenuItem is1 = new JMenuItem("iS1");
    is1.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.i).phaseType(Phase.PhaseType.S)
            .time(time).weight(1).build();
        event.setPhase(channel, phase);
      }
    });
    s.add(is1);

    JMenuItem es2 = new JMenuItem("eS2");
    es2.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e).phaseType(Phase.PhaseType.S)
            .time(time).weight(2).build();
        event.setPhase(channel, phase);
      }
    });
    s.add(es2);

    JMenuItem es3 = new JMenuItem("eS3");
    es3.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Phase phase = new Phase.Builder().onset(Phase.Onset.e)
            .phaseType(Phase.PhaseType.S)
            .time(time).weight(3).build();
        event.setPhase(channel, phase);
      }
    });
    s.add(es3);
    
    JMenuItem clearS = new JMenuItem("clear S");
    clearS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        event.clearPhase(channel, Phase.PhaseType.S);
      }
    });
    s.add(clearS);

  }
}
