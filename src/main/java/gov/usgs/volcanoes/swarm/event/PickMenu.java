package gov.usgs.volcanoes.swarm.event;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.quakeml.Pick;
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
import javax.swing.KeyStroke;

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

  private Pick p;
  private JRadioButtonMenuItem pWeight0;
  private boolean pickChannelP = false;
  private Pick s;
  private JRadioButtonMenuItem sWeight0;
  private boolean pickChannelS = false;
  private boolean hidePhases = false;

  private Pick coda1;
  private Pick coda2;
  private boolean hideCoda = false;
  
  private PickSettings settings;
  
  /**
   * Constructor.
   */
  public PickMenu(WaveViewPanel wvp) {
    super("Pick Menu");
    this.wvp = wvp;
    sampleRate = wvp.getWave().getSamplingRate();
    settings = PickSettingsDialog.getInstance().getSettings();
    createMenu();
  }

  /**
   * Create right click menu for pick.
   */
  private void createMenu() {
    createPhaseMenu();
    createCodaMenu();
    this.addSeparator();
    createSettingsMenu();
    
  }
 
  /**
   * Create settings menu.
   */
  private void createSettingsMenu() {
    JMenuItem settingsMenu = new JMenuItem("Settings");
    settingsMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        PickSettingsDialog.getInstance().setVisible(true);
      }
    });
    this.add(settingsMenu);
  }
  
  /**
   * Create coda submenu.
   */
  private void createCodaMenu() {

    JMenu coda = new JMenu("Coda");

    JMenuItem c1MenuItem = new JMenuItem("Coda 1");
    c1MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("C1", null, j2k);
        coda1 = pick;
        wvp.repaint();
      }
    });
    coda.add(c1MenuItem);
    
    JMenuItem c2MenuItem = new JMenuItem("Coda 2");
    c2MenuItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        Pick pick = createPick("C2", null, j2k);
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
    phasePe.addActionListener(new PickActionListener("P", Pick.Onset.EMERGENT));
    phase.add(phasePe);

    JMenuItem phasePi = new JMenuItem("P (Impulsive)");
    phasePi.addActionListener(new PickActionListener("P", Pick.Onset.IMPULSIVE));
    phase.add(phasePi);

    // add P uncertainty menus
    phase.add(createUncertaintyMenu("P"));

    JMenuItem clearP = new JMenuItem("Clear P");
    clearP.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        p = null;
        pickChannelP = false;
        wvp.repaint();
        propagatePick("P", p, wvp);
        pWeight0.setSelected(true); // reset weight to 0
      }
    });
    phase.add(clearP);

    phase.addSeparator();

    // S 
    JMenuItem phaseSe = new JMenuItem("S (Emergent)");
    phaseSe.addActionListener(new PickActionListener("S", Pick.Onset.EMERGENT));
    phase.add(phaseSe);

    JMenuItem phaseSi = new JMenuItem("S (Impulsive)");
    phaseSi.addActionListener(new PickActionListener("S", Pick.Onset.IMPULSIVE));
    phase.add(phaseSi);

    // add S uncertainty menus
    phase.add(createUncertaintyMenu("S"));
    
    // clear S
    JMenuItem clearS = new JMenuItem("Clear S");
    clearS.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        s = null;
        pickChannelS = false;
        wvp.repaint();
        propagatePick("S", s, wvp);
        sWeight0.setSelected(true); // reset weight to 0
      }
    });
    phase.add(clearS);

    phase.addSeparator();

    // hide P & S
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
   * Create uncertainty menu.
   * @param phase P or S
   * @param upper true if creating upper uncertainty menu
   * @return menu
   */
  private JMenu createUncertaintyMenu(String phase) {
    String menuText = phase + " Uncertainty";
    JMenu menu = new JMenu(menuText);
    ButtonGroup bg = new ButtonGroup();
    for (int i = 0; i < settings.getNumWeight(); i++) {
      JRadioButtonMenuItem mi = new JRadioButtonMenuItem(Integer.toString(i));
      if (i == 0) { 
        mi.setSelected(true);
        if (phase.equals("P")) {
          pWeight0=mi;
        }
        if (phase.equals("S")) {
          sWeight0=mi;
        }
      }
      String key = phase + " " + i;
      mi.setAccelerator(KeyStroke.getKeyStroke(key));
      bg.add(mi);
      mi.addActionListener(new UncertaintyActionListener(phase, i));
      menu.add(mi);
    }   
    return menu;
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
    Pick pick = new Pick(phase, time, channel); // assign public id later if saving to quakeml file
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
      wvp.setMarks(Double.NaN, Double.NaN);
    }
    wvp.repaint();
  }

  /**
   * Get S-P Duration.
   * @return duration in seconds
   */
  public double getSpDuration() {
    if (p == null || s == null) {
      return Double.NaN;
    }
    double duration = (s.getTime() - p.getTime()) / 1000.0;
    return duration;
  }
  
  /**
   * Get S-P distance.
   * 
   * @return distance in km
   */
  public double getSpDistance() {
    if (p == null || s == null) {
      return Double.NaN;
    }
    double duration = (s.getTime() - p.getTime()) / 1000.0;
    double distance = SwarmConfig.getInstance().pVelocity * duration;
    return distance;  
  }
  
  /**
   * Pick action listener.
   * @author Diana Norgaard
   */
  class PickActionListener implements ActionListener {
    private String phase;
    private Pick.Onset onset;
    
    private PickActionListener(String phase, Pick.Onset onset) {
      this.phase = phase;
      this.onset = onset;
    }

    public void actionPerformed(ActionEvent e) {
      Pick pick = createPick(phase, onset, j2k);
      if (phase.equals("P")) {
        p = pick;
        pickChannelP = true;
        pWeight0.setSelected(true);
      }
      if (phase.equals("S")) {
        s = pick;
        pickChannelS = true;
        sWeight0.setSelected(true);
      }
      propagatePick(phase, pick, wvp);
      
      wvp.repaint();
    }
  }
  
  /**
   * Uncertainty action listener.
   * @author Diana Norgaard
   */
  class UncertaintyActionListener implements ActionListener {
    private String phase;
    private int weight;

    private UncertaintyActionListener(String phase, int weight) {
      this.phase = phase;
      this.weight = weight;
    }

    public void actionPerformed(ActionEvent e) {
      double uncertainty = settings.getWeightToTime(weight, sampleRate) / 1000.0;
      if (phase.equals("P") && p != null) {
        p.getTimeQuantity().setUncertainty(uncertainty);
      }
      if (phase.equals("S") && s != null) {
        s.getTimeQuantity().setUncertainty(uncertainty);
      }
      
      wvp.repaint();
    }
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
    pickChannelP = false;
  }

  /**
   * Set S pick.
   * @param s pick
   */
  public void setS(Pick s) {
    this.s = s;
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
    return hideCoda;
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

}
