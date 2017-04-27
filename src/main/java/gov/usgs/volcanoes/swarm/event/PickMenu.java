package gov.usgs.volcanoes.swarm.event;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
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

  private WaveClipboardFrame clipboard = WaveClipboardFrame.getInstance();
  private WaveViewPanel wvp;
  private double j2k;

  private Pick p;
  private boolean pickComponentP = false;
  private Pick s;
  private boolean pickComponentS = false;
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
    createMarkMenu();
  }
  
  /**
   * Create mark submenu.
   */
  private void createMarkMenu() {
    JMenu markMenu = new JMenu("Marks");

    JMenuItem c1MenuItem = new JMenuItem("Clear");
    c1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        wvp.setMarks(Double.NaN, Double.NaN);
        wvp.repaint();
      }
    });
    markMenu.add(c1MenuItem);
    
    this.add(markMenu);
  }
  
  /**
   * Create coda submenu.
   */
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
   * Create phase submenu.
   */
  private void createPhaseMenu() {
    JMenu phase = new JMenu("Phase");

    JMenuItem phasePe = new JMenuItem("P (Emergent)");
    phasePe.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        p = createPick("P", Pick.Onset.EMERGENT);
        pickComponentP = true;
        wvp.repaint();
        clipboard.propagatePick("P", p, wvp);
      }
    });
    phase.add(phasePe);

    JMenuItem phasePi = new JMenuItem("P (Impulsive)");
    phasePi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        p = createPick("P", Pick.Onset.IMPULSIVE);
        pickComponentP = true;
        wvp.repaint();
        clipboard.propagatePick("P", p, wvp);
      }
    });
    phase.add(phasePi);

    JMenuItem clearP = new JMenuItem("Clear P");
    clearP.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        p = null;
        pickComponentP = false;
        wvp.repaint();
        clipboard.propagatePick("P", p, wvp);
      }
    });
    phase.add(clearP);

    phase.addSeparator();

    JMenuItem phaseSe = new JMenuItem("S (Emergent)");
    phaseSe.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        s = createPick("S", Pick.Onset.EMERGENT);
        pickComponentS = true;
        wvp.repaint();
        clipboard.propagatePick("S", s, wvp);
      }
    });
    phase.add(phaseSe);

    JMenuItem phaseSi = new JMenuItem("S (Impulsive)");
    phaseSi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        s = createPick("S", Pick.Onset.IMPULSIVE);
        pickComponentS = true;
        wvp.repaint();
        clipboard.propagatePick("S", s, wvp);
      }
    });
    phase.add(phaseSi);

    JMenuItem clearS = new JMenuItem("Clear S");
    clearS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        s = null;
        pickComponentS = false;
        wvp.repaint();
        clipboard.propagatePick("S", s, wvp);
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
   * Set P pick.
   * @param p pick
   */
  public void setP(Pick p) {
    this.p = p;
    pickComponentP = false;
  }

  /**
   * Set S pick.
   * @param s pick
   */
  public void setS(Pick s) {
    this.s = s;
    pickComponentS = false;
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

  /**
   * Determine if this is the component where P pick was selected.
   * @return true if this component is where user selected the P
   */
  public boolean isPickComponentP() {
    return pickComponentP;
  }

  /**
   * Determine if this is the component where S pick was selected.
   * @return true if this component is where user selected the S
   */
  public boolean isPickComponentS() {
    return pickComponentS;
  }

}
