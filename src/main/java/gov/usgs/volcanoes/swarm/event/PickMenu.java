package gov.usgs.volcanoes.swarm.event;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

  private Pick coda1;
  private Pick coda2;
  private boolean hideCoda = false;
  
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
    createCodaMenu();
  }
  
  private void createCodaMenu() {

    JMenu coda = new JMenu("Coda");

    JMenuItem c1MenuItem = new JMenuItem("Coda 1");
    c1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("C1", null);
        coda1 = pick;
        wvp.repaint();
      }
    });
    coda.add(c1MenuItem);
    
    JMenuItem c2MenuItem = new JMenuItem("Coda 2");
    c2MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("C2", null);
        coda2 = pick;
        wvp.repaint();
      }
    });
    coda.add(c2MenuItem);
    
    coda.addSeparator();

    JCheckBoxMenuItem clearCodaMenu = new JCheckBoxMenuItem("Clear Coda");
    clearCodaMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        coda1 = null;
        coda2 = null;
        wvp.repaint();
      }
    });
    coda.add(clearCodaMenu);

    JCheckBoxMenuItem hideCodaMenu = new JCheckBoxMenuItem("Hide Coda");
    hideCodaMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hideCoda = ((JCheckBoxMenuItem)e.getSource()).isSelected();
        wvp.repaint();
      }
    });
    coda.add(hideCodaMenu);
    
    this.add(coda);
  }
  
  /**
   * Create submen under Phase menu.
   */
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

    JCheckBoxMenuItem hidePhaseMenu = new JCheckBoxMenuItem("Hide Phases");
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
    long time = J2kSec.asDate(j2k).getTime();
    String channel = wvp.getChannel();
    Pick pick = new Pick("", time, channel); // assign public id later if saving to quakeml file
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
  
  /**
   * Get currently set time as J2K.
   * @return j2k time
   */
  public double getJ2k() {
    return j2k;
  }

  /**
   * Set currently selected time as J2K.
   * @param j2k time
   */
  public void setJ2k(double j2k) {
    this.j2k = j2k;
  }

  /**
   * Get P pick.
   * @return pick
   */
  public Pick getP() {
    return p;
  }

  /**
   * Get S pick. 
   * @return pick
   */
  public Pick getS() {
    return s;
  }

  /**
   * Get enabled/disabled option for hiding phases.
   * @return true if hide phases option is enabled
   */
  public boolean isHidePhases() {
    return hidePhases;
  }

  /**
   * Get one side of coda window as pick.
   * @return pick
   */
  public Pick getCoda1() {
    return coda1;
  }

  /**
   * Get other side of coda window as pick.
   * @return pick
   */
  public Pick getCoda2() {
    return coda2;
  }

  /**
   * Get enabled/disabled option for hiding coda.
   * @return true if hide coda option is enabled
   */
  public boolean isHideCoda() {
    return hideCoda;
  }

}
