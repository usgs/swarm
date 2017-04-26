package gov.usgs.volcanoes.swarm.event;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Right click menu for picks.
 * 
 * @author Diana Norgaard
 */
public class PickMenu extends JPopupMenu {

  private static final long serialVersionUID = 8681764007165352268L;

  private WaveViewPanel wvp;
  private double j2k;

  private Pick p;
  private Pick s;
  private boolean hidePhases = false;

  /**
   * Constructor.
   */
  public PickMenu(WaveViewPanel wvp) {
    super("Pick Menu");
    this.wvp = wvp;
    createMenu();
  }

  /**
   * Create right click menu for pick.
   */
  private void createMenu() {
    createPhaseMenu();
    //TODO: duration
    //TODO: coda menu
  }
  
  private void createPhaseMenu() {
    JMenu phase = new JMenu("Phase");

    JMenuItem phasePe = new JMenuItem("P (Emergent)");
    phasePe.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("P", Pick.Onset.EMERGENT);
        p = pick;
        wvp.repaint();
      }
    });
    phase.add(phasePe);

    JMenuItem phasePi = new JMenuItem("P (Impulsive)");
    phasePi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("P", Pick.Onset.IMPULSIVE);
        p = pick;
        wvp.repaint();
      }
    });
    phase.add(phasePi);

    JMenuItem clearP = new JMenuItem("Clear P");
    clearP.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        p = null;
        wvp.repaint();
      }
    });
    phase.add(clearP);

    phase.addSeparator();

    JMenuItem phaseSe = new JMenuItem("S (Emergent)");
    phaseSe.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("S", Pick.Onset.EMERGENT);
        s = pick;
        wvp.repaint();
      }
    });
    phase.add(phaseSe);

    JMenuItem phaseSi = new JMenuItem("S (Impulsive)");
    phaseSi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("S", Pick.Onset.IMPULSIVE);
        s = pick;
        wvp.repaint();
      }
    });
    phase.add(phaseSi);

    JMenuItem clearS = new JMenuItem("Clear S");
    clearS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        s = null;
        wvp.repaint();
      }
    });
    phase.add(clearS);

    phase.addSeparator();

    JCheckBoxMenuItem hidePhaseMenu = new JCheckBoxMenuItem("Hide");
    hidePhaseMenu.setMnemonic(KeyEvent.VK_H);
    hidePhaseMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hidePhases = ((JCheckBoxMenuItem)e.getSource()).isSelected();
        wvp.repaint();
      }
    });
    phase.add(hidePhaseMenu);
    this.add(phase);
  }

  /**
   * Create pick object.
   * 
   * @param phase P or S
   * @param onset emergent or impulsive
   * @return pick object
   */
  private Pick createPick(String phase, Pick.Onset onset) {
    String publicId = SwarmConfig.getInstance().getQuakemlResourceId() + "/Pick/somerandom#";
    long time = J2kSec.asDate(j2k).getTime();
    String channel = wvp.getChannel();
    Pick pick = new Pick(publicId, time, channel);
    pick.setPhaseHint(phase);
    pick.setOnset(onset);
    Wave wave = wvp.getWave();
    int i = wave.getBufferIndexAtTime(j2k);
    int value = wave.buffer[i];
    int nextValue = wave.buffer[i + 1];
    if (nextValue > value) {
      pick.setPolarity(Pick.Polarity.POSITIVE);
    } else if (nextValue < value) {
      pick.setPolarity(Pick.Polarity.NEGATIVE);
    } else {
      pick.setPolarity(Pick.Polarity.UNDECIDABLE);
    }
    return pick;
  }

  public double getJ2k() {
    return j2k;
  }

  public void setJ2k(double j2k) {
    this.j2k = j2k;
  }

  public Pick getP() {
    return p;
  }

  public void setP(Pick p) {
    this.p = p;
  }

  public Pick getS() {
    return s;
  }

  public void setS(Pick s) {
    this.s = s;
  }

  public boolean isHidePhases() {
    return hidePhases;
  }

}
