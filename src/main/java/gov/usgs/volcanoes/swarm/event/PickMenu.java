package gov.usgs.volcanoes.swarm.event;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.quakeml.Pick.Onset;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 * Right click menu for picks.
 * 
 * @author Diana Norgaard
 */
public class PickMenu extends JPopupMenu {

  private static final long serialVersionUID = 8681764007165352268L;

  private WaveClipboardFrame clipboard = WaveClipboardFrame.getInstance();
  private WaveViewPanel wvp;
  private double sampleRate;
  private double j2k;

  private Pick pickP;
  private JRadioButtonMenuItem pWeight0;
  private boolean pickChannelP = false;
  private Pick pickS;
  private JRadioButtonMenuItem sWeight0;
  private boolean pickChannelS = false;
  private boolean hidePhases = false;

  private Pick coda1;
  private Pick coda2;
  private JCheckBoxMenuItem hideCodaMenu;
  
  private PickSettings settings;
  
  /**
   * Constructor.
   */
  public PickMenu(WaveViewPanel wvp) {
    super("Pick Menu");
    this.wvp = wvp;
    if (wvp.getWave() != null) {
      sampleRate = wvp.getWave().getSamplingRate();
    }
    settings = PickSettingsDialog.getInstance().getSettings();
    createMenu();
  }

  /**
   * Create right click menu for pick.
   */
  private void createMenu() {
    createPhaseMenu();
    createCodaMenu();
  }
  
  /**
   * Create emergent P pick.
   */
  public void createEmergentP() {
    pickP = createPick("P", Onset.EMERGENT, j2k);
    pickChannelP = true;
    propagatePick("P", pickP, wvp);
    wvp.repaint();
    
  }
  
  /**
   * Create impulsive P pick.
   */
  public void createImpulsiveP() {
    pickP = createPick("P", Onset.IMPULSIVE, j2k);
    pickChannelP = true;
    propagatePick("P", pickP, wvp);
    wvp.repaint();
    
  }
  
  /**
   * Create emergent S pick.
   */
  public void createEmergentS() {
    pickS = createPick("S", Onset.EMERGENT, j2k);  
    pickChannelS = true; 
    propagatePick("S", pickS, wvp);
    wvp.repaint();
    
  }

  /**
   * Create impulsive S pick.
   */
  public void createImpulsiveS() {
    pickS = createPick("S", Onset.IMPULSIVE, j2k); 
    pickChannelS = true; 
    propagatePick("S", pickS, wvp);
    wvp.repaint();   
  }
  
  /**
   * Clear P pick.
   */
  public void clearP() {
    pickP = null;
    pickChannelP = false;
    propagatePick("P", pickP, wvp);
    pWeight0.setSelected(true); // reset weight to 0
    wvp.repaint();
  }
  
  /**
   * Clear S pick.
   */
  public void clearS() {
    pickS = null;
    pickChannelS = false;
    wvp.repaint();
    propagatePick("S", pickS, wvp);
    sWeight0.setSelected(true); // reset weight to 0
  }
  
  /**
   * Create phase submenu.
   */
  private void createPhaseMenu() {
    JMenu phaseP = new JMenu("P");

    JMenuItem phasePe = new JMenuItem("Emergent");
    phasePe.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createEmergentP();
      }     
    });
    phaseP.add(phasePe);

    JMenuItem phasePi = new JMenuItem("Impulsive");
    phasePi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createImpulsiveP();
      }     
    });
    phaseP.add(phasePi);

    JMenuItem clearP = new JMenuItem("Clear");
    clearP.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearP();
      }
    });
    phaseP.add(clearP);

    this.add(phaseP);
    // add P uncertainty menus
    this.add(createUncertaintyMenu("P"));

    // S 
    JMenu phaseS = new JMenu("S");
    JMenuItem phaseSe = new JMenuItem("Emergent");
    phaseSe.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createEmergentS();
      }      
    });
    phaseS.add(phaseSe);

    JMenuItem phaseSi = new JMenuItem("Impulsive");
    phaseSi.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createImpulsiveS();
      }      
    });
    phaseS.add(phaseSi);

    
    // clear S
    JMenuItem clearS = new JMenuItem("Clear");
    clearS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearS();
      }
    });
    phaseS.add(clearS);
    this.add(phaseS);
    // add S uncertainty menus
    this.add(createUncertaintyMenu("S"));

    // hide P & S
    JCheckBoxMenuItem hidePhaseMenu = new JCheckBoxMenuItem("Hide P & S");
    hidePhaseMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        hidePhases = ((JCheckBoxMenuItem)e.getSource()).isSelected();
        wvp.repaint();
      }
    });
    this.add(hidePhaseMenu);
    this.addSeparator();
  }

  /**
   * Create uncertainty menu.
   * @param phase P or S
   * @param upper true if creating upper uncertainty menu
   * @return menu
   */
  private JMenu createUncertaintyMenu(final String phase) {
    JMenu menu = new JMenu(phase + " Uncertainty");
    ButtonGroup bg = new ButtonGroup();
    for (int i = 0; i < PickSettings.numWeight; i++) {
      final int weight = i;
      JRadioButtonMenuItem mi = new JRadioButtonMenuItem(Integer.toString(i));
      if (i == 0) { 
        mi.setSelected(true);
        if (phase.equals("P")) {
          pWeight0 = mi;
        }
        if (phase.equals("S")) {
          sWeight0 = mi;
        }
      }
      bg.add(mi);
      //mi.addActionListener(new UncertaintyActionListener(phase, i));
      mi.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setUncertainty(phase, weight);
        }      
      });
      menu.add(mi);
    }   
    return menu;
  }

  /**
   * Create one coda pick.
   */
  public void createCoda1() {
    Pick pick = createPick("C1", null, j2k);
    coda1 = pick;
    wvp.repaint();   
  }
  
  /**
   * Create another coda pick.
   */
  public void createCoda2() {
    Pick pick = createPick("C2", null, j2k);
    coda2 = pick;
    wvp.repaint();   
  }
  
  /**
   * Clear coda picks.
   */
  public void clearCoda() {
    coda1 = null;
    coda2 = null;
    wvp.repaint();    
  }
  
  /**
   * Create coda submenu.
   */
  private void createCodaMenu() {

    JMenu coda = new JMenu("Coda");
    coda.setMnemonic(Character.CONTROL);

    JMenuItem c1MenuItem = new JMenuItem("Coda 1");
    c1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createCoda1();
      }
    });
    coda.add(c1MenuItem);
    
    JMenuItem c2MenuItem = new JMenuItem("Coda 2");
    c2MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        createCoda2();
      }
    });
    coda.add(c2MenuItem);
    
    coda.addSeparator();

    JCheckBoxMenuItem clearCodaMenu = new JCheckBoxMenuItem("Clear");
    clearCodaMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        clearCoda();
      }
    });
    coda.add(clearCodaMenu);

    hideCodaMenu = new JCheckBoxMenuItem("Hide");
    hideCodaMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        wvp.repaint();
      }
    });
    coda.add(hideCodaMenu);
    
    this.add(coda);
  }
  
  /**
   * Create pick object.
   * 
   * @param phase P or S
   * @param onset emergent or impulsive
   * @return pick object
   */
  private Pick createPick(String phase, Pick.Onset onset, double j2kTime) {
    long time = J2kSec.asDate(j2kTime).getTime();
    String channel = wvp.getChannel();
    String publicId = EventDialog.QUAKEML_RESOURCE_ID + "/Pick/" + System.currentTimeMillis();
    Pick pick = new Pick(publicId, time, channel); 
    pick.setPhaseHint(phase);
    pick.setOnset(onset);
    // use default weight of 0 to set uncertainty
    if (phase.equals("P") || phase.equals("S")) {
      long millis = settings.getWeightToTime(0, sampleRate);
      double uncertainty = millis / 1000.0;
      pick.getTimeQuantity().setUncertainty(uncertainty);
    }
    // determine polarity
    Wave wave = wvp.getWave();
    int i = wave.getBufferIndexAtTime(j2kTime);
    try {
      int value = wave.buffer[i];
      int nextValue = wave.buffer[i + 1];
      if (nextValue > value) {
        pick.setPolarity(Pick.Polarity.POSITIVE);
      } else if (nextValue < value) {
        pick.setPolarity(Pick.Polarity.NEGATIVE);
      } else {
        pick.setPolarity(Pick.Polarity.UNDECIDABLE);
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      pick.setPolarity(Pick.Polarity.UNDECIDABLE);
    }
    return pick;
  }

  /**
   * Propagate P or S pick to wave view panel of same station.
   * 
   * @param phase P or S
   * @param pick pick object
   */
  public void propagatePick(String phase, Pick pick, WaveViewPanel pickWave) {
    for (WaveViewPanel otherWvp : clipboard.getWaves()) {
      if (wvp.getChannel().equals(otherWvp.getChannel())) {
        continue;
      }
      if (pickWave.isSameStation(otherWvp)) {
        if (phase.equals("P")) {
          otherWvp.getPickMenu().setP(pick);
        }
        if (phase.equals("S")) {
          otherWvp.getPickMenu().setS(pick);
        }
        otherWvp.repaint();
      }
    }
  }
  
  /**
   * Change duration markers selected in helicorder view wave panel 
   * to coda duration markers in clipboard.
   */
  public void marksToCoda() {
    if (!Double.isNaN(wvp.getMark1())) {
      Pick pick = createPick("C1", null, wvp.getMark1());
      coda1 = pick;
    }
    if (!Double.isNaN(wvp.getMark2())) {
      Pick pick = createPick("C2", null, wvp.getMark2());
      coda2 = pick;
    }
    wvp.setMarks(Double.NaN, Double.NaN);
    wvp.repaint();
  }

  /**
   * Get S-P Duration.
   * @return duration in seconds
   */
  public double getSpDuration() {
    if (pickP == null || pickS == null) {
      return Double.NaN;
    }
    double duration = 1000d * (pickS.getTime() - pickP.getTime());
    return duration;
  }
  
  /**
   * Get S-P distance based on pick times.
   * 
   * @return distance in km
   */
  public double getSpDistance() {
    if (pickP == null || pickS == null) {
      return Double.NaN;
    }
    return getDistance(pickP.getTime(), pickS.getTime()); 
  }
  
  /**
   * Get minimum S-P distance based on pick times and uncertainty.
   * 
   * @return distance in km
   */
  public double getSpMinDistance() {
    if (pickP == null || pickS == null) {
      return Double.NaN;
    }
    double stime = pickS.getTime() - 1000d * pickS.getTimeQuantity().getUncertainty();
    double ptime = pickP.getTime() + 1000d * pickP.getTimeQuantity().getUncertainty();
    return getDistance(ptime, stime);
  }

  /**
   * Get maximum S-P distance based on pick times and uncertainty.
   * 
   * @return distance in km
   */
  public double getSpMaxDistance() {
    if (pickP == null || pickS == null) {
      return Double.NaN;
    }
    double stime = pickS.getTime() + 1000d * pickS.getTimeQuantity().getUncertainty();
    double ptime = pickP.getTime() - 1000d * pickP.getTimeQuantity().getUncertainty();
    return getDistance(ptime, stime);
  }

  /**
   * Get S-P distance.
   * 
   * @param ptime P pick time in milliseconds
   * @param stime S pick time in milliseconds
   * @return distance in km
   */
  private double getDistance(double ptime, double stime) {
    if (ptime > stime) {
      return Double.NaN;
    }
    double duration = (stime - ptime) / 1000d;
    double distance = SwarmConfig.getInstance().pVelocity * duration;
    return distance;
  }
  
  /**
   * Set phase pick uncertainty.
   * @param phase P or S
   * @param weight 0 to 4
   */
  public void setUncertainty(String phase, int weight) {
    double uncertainty = settings.getWeightToTime(weight, sampleRate) / 1000.0;
    if (phase.equals("P") && pickP != null) {
      pickP.getTimeQuantity().setUncertainty(uncertainty);
    }
    if (phase.equals("S") && pickS != null) {
      pickS.getTimeQuantity().setUncertainty(uncertainty);
    }
    wvp.repaint();   
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
    return pickP;
  }

  /**
   * Get S pick. 
   * @return pick
   */
  public Pick getS() {
    return pickS;
  }

  /**
   * Set P pick.
   * @param p pick
   */
  public void setP(Pick p) {
    this.pickP = p;
    pickChannelP = false;
  }

  /**
   * Set S pick.
   * @param s pick
   */
  public void setS(Pick s) {
    this.pickS = s;
    pickChannelS = false;
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
    return hideCodaMenu.isSelected();
  }

  /**
   * Determine if this is the channel where P pick was selected.
   * @return true if this channel is where user selected the P
   */
  public boolean isPickChannelP() {
    return pickChannelP;
  }

  /**
   * Determine if this is the channel where S pick was selected.
   * @return true if this channel is where user selected the S
   */
  public boolean isPickChannelS() {
    return pickChannelS;
  }

  public void setPickChannelP(boolean pickChannelP) {
    this.pickChannelP = pickChannelP;
  }

  public void setPickChannelS(boolean pickChannelS) {
    this.pickChannelS = pickChannelS;
  }

}
