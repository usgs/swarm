package gov.usgs.volcanoes.swarm.event;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

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

  public static String P = "P";
  public static String S = "S";
  public static String CODA1 = "C1";
  public static String CODA2 = "C2";
  
  private WaveClipboardFrame clipboard = WaveClipboardFrame.getInstance();
  private final WaveViewPanel wvp;
  private double sampleRate;
  private double j2k;

  protected HashMap<String, Pick> picks = new HashMap<String, Pick>();
  protected HashMap<String, Boolean> pickChannels = new HashMap<String, Boolean>();
  protected HashMap<String, JRadioButtonMenuItem[]> weightButtons =
      new HashMap<String, JRadioButtonMenuItem[]>();
  
  private JCheckBoxMenuItem hidePhasesMenu;
  private JCheckBoxMenuItem hideCodaMenu;
  private JCheckBoxMenuItem plotMenu;

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
    pickChannels.put(P, false);
    pickChannels.put(S, false);
    createMenu();
  }

  /**
   * Create right click menu for pick.
   */
  private void createMenu() {
    createPickMenu();
    createClearMenu();
    createHideMenu();
    
    // S-P plot  
    plotMenu = new JCheckBoxMenuItem("Plot");
    plotMenu.setSelected(true);
    plotMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        boolean plot = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        propagatePlot(plot);
      }
    });
    this.add(plotMenu);

  }
 
  /**
   * Pick menu.
   */
  private void createPickMenu() {
    JMenu pickMenu = new JMenu("Pick");
    this.add(pickMenu);

    for (final String phase : new String[] {P, S}) {
      JMenu phaseMenu = new JMenu(phase);
      pickMenu.add(phaseMenu);
      for (final Pick.Onset onset : Pick.Onset.values()) {
        if (onset == Pick.Onset.QUESTIONABLE) {
          continue;
        }
        JMenu onsetMenu = new JMenu(onset.toString());
        phaseMenu.add(onsetMenu);
        // create weight menu
        ButtonGroup bg = new ButtonGroup();
        JRadioButtonMenuItem[] mi = new JRadioButtonMenuItem[PickSettings.numWeight];
        for (int i = 0; i < mi.length; i++) {
          final int weight = i;
          mi[i] = new JRadioButtonMenuItem(Integer.toString(i));
          bg.add(mi[i]);
          if (i == 0) {
            mi[i].setSelected(true);
          }
          mi[i].addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              Pick pick = createPick(phase, onset, j2k, weight);
              propagatePick(phase, pick);
              setWeightButton(phase, onset, weight);
              pickChannels.put(phase, true);
              wvp.repaint();
              propagateUncertainty(phase, onset, weight);
            }
          });
          onsetMenu.add(mi[i]);
        }
        String key = phase + onset.toString().substring(0,1);
        weightButtons.put(key, mi);
      }

    }
    for (final String coda : new String[] {CODA1, CODA2}) {
      JMenuItem codaMenuItem = new JMenuItem(coda);
      codaMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          createPick(coda,null, j2k);
          wvp.repaint();
        }
      });
      pickMenu.add(codaMenuItem);
    }
  }
  
  /**
   * Clear menu.
   */
  private void createClearMenu() {
    JMenu clearMenu = new JMenu("Clear");
    this.add(clearMenu);
    
    for (final String pickType : new String[] {P, S, CODA1, CODA2}) {
      JMenuItem clearPick = new JMenuItem(pickType);
      clearPick.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          clearPick(pickType);
          wvp.repaint();
        }
      });
      clearMenu.add(clearPick);
    }
    
    JMenuItem clearAll = new JMenuItem("All");
    clearAll.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        for (final String pickType : new String[] {P, S, CODA1, CODA2}) {
          clearPick(pickType);
          wvp.repaint();
        }
      }
    });
    clearMenu.add(clearAll);
    
  }

  
  /**
   * Hide menu.
   */
  private void createHideMenu() {
    JMenu hideMenu = new JMenu("Hide");
    this.add(hideMenu);
    
    // hide P & S
    hidePhasesMenu = new JCheckBoxMenuItem("P & S");
    hidePhasesMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        wvp.repaint();
      }
    });
    hideMenu.add(hidePhasesMenu);
    

    hideCodaMenu = new JCheckBoxMenuItem("Coda");
    hideCodaMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        wvp.repaint();
      }
    });
    hideMenu.add(hideCodaMenu);
  }

  /**
   * Clear coda picks.
   */
  public void clearCoda() {
    picks.remove(CODA1);
    picks.remove(CODA2);
    wvp.repaint();    
  }

  /**
   * Create pick object with weight.
   * 
   * @param phase P or S
   * @param onset emergent or impulsive
   * @param j2ktime pick time in J2K
   * @weight weight 0 to 4
   */
  private Pick createPick(String phase, Pick.Onset onset, double j2kTime, int weight) {
    Pick pick = createPick(phase, onset, j2kTime);
    double uncertainty = settings.getWeightToTime(weight, sampleRate) / 1000.0;
    pick.getTimeQuantity().setUncertainty(uncertainty);
    picks.put(phase, pick);
    return pick;
  }
  
  /**
   * Create pick object without weight.
   * @param phase P or S
   * @param onset emergent or impulsive
   * @param j2ktime pick time in J2K
   */
  private Pick createPick(String phase, Pick.Onset onset, double j2kTime) {
    long time = J2kSec.asDate(j2kTime).getTime();
    String channel = wvp.getChannel();
    String publicId = EventDialog.QUAKEML_RESOURCE_ID + "/Pick/" + System.currentTimeMillis();
    Pick pick = new Pick(publicId, time, channel);
    pick.setPhaseHint(phase);
    pick.setOnset(onset);
    if (phase.equals(P) || phase.equals(S)) {      
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
    }
    picks.put(phase, pick);
    
    // propagate pick
    if (phase.equals(P) || phase.equals(S)) {
      propagatePick(phase, pick);
    }
    return pick;
  }

  /**
   * Propagate P or S pick to wave view panel of same station.
   * 
   * @param phase P or S
   * @param pick pick object
   */
  public void propagatePick(String phase, Pick pick) {
    for (WaveViewPanel otherWvp : clipboard.getWaves()) {
      if (wvp.getChannel().equals(otherWvp.getChannel())) {
        continue;
      }
      if (wvp.isSameStation(otherWvp)) {
        otherWvp.getPickMenu().setPick(phase, pick);
        otherWvp.repaint();
      }
    }
  }

  /**
   * Propagate plot setting to others pick menu of same station.
   * 
   * @param plot true to plot S-P for station
   */
  public void propagatePlot(boolean plot) {
    for (WaveViewPanel otherWvp : clipboard.getWaves()) {
      if (wvp.getChannel().equals(otherWvp.getChannel())) {
        continue;
      }
      if (wvp.isSameStation(otherWvp)) {
        otherWvp.getPickMenu().setPlot(plot);
        otherWvp.repaint();
      }
    }
  }
  
  /**
   * Propagate weight setting changes to other panels of same station.
   * @param phase P or S
   * @param onset Emergent or Impulsive
   * @param weight 0 to 4
   */
  public void propagateUncertainty(String phase, Pick.Onset onset, int weight) {
    for (WaveViewPanel otherWvp : clipboard.getWaves()) {
      if (wvp.getChannel().equals(otherWvp.getChannel())) {
        continue;
      }
      if (wvp.isSameStation(otherWvp)) {
        otherWvp.getPickMenu().setWeightButton(phase, onset, weight);
        otherWvp.repaint();
      }
    }
  }

  /**
   * Change duration markers selected in helicorder view wave panel to coda duration markers in
   * clipboard.
   */
  public void marksToCoda() {
    if (!Double.isNaN(wvp.getMark1())) {
      createPick(CODA1, null, wvp.getMark1());
    }
    if (!Double.isNaN(wvp.getMark2())) {
      createPick(CODA2, null, wvp.getMark2());
    }
    wvp.setMarks(Double.NaN, Double.NaN);
    wvp.repaint();
  }

  /**
   * Get coda duration.
   * @return duration in seconds
   */
  public double getCodaDuration() {
    Pick coda1 = picks.get(CODA1);
    Pick coda2 = picks.get(CODA2);
    if (coda1 == null && coda2 == null) {
      return Double.NaN;
    }
    Pick pickP = picks.get(P);
    if ((coda1 == null || coda2 == null) && pickP == null) {
      return Double.NaN;
    }
    if (coda1 != null && coda2 != null) {
      return Math.abs(coda1.getTime() - coda2.getTime()) / 1000d;
    }
    if (coda1 != null && coda1.getTime() > pickP.getTime()) {
      return (coda1.getTime() - pickP.getTime()) / 1000d;
    }
    if (coda2 != null && coda2.getTime() > pickP.getTime()) {
      return (coda2.getTime() - pickP.getTime()) / 1000d;
    }
    return Double.NaN;
  }
  
  /**
   * Get S-P Duration.
   * 
   * @return duration in seconds
   */
  public double getSpDuration() {
    Pick pickP = picks.get(P);
    Pick pickS = picks.get(S);
    if (pickP == null || pickS == null) {
      return Double.NaN;
    }
    double duration = (pickS.getTime() - pickP.getTime()) / 1000d;
    return duration;
  }

  /**
   * Get S-P distance based on pick times.
   * 
   * @return distance in km
   */
  public double getSpDistance() {
    Pick pickP = picks.get(P);
    Pick pickS = picks.get(S);
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
    Pick pickP = picks.get(P);
    Pick pickS = picks.get(S);
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
    Pick pickP = picks.get(P);
    Pick pickS = picks.get(S);
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
   * Get currently set time as J2K.
   * 
   * @return j2k time
   */
  public double getJ2k() {
    return j2k;
  }

  /**
   * Set currently selected time as J2K.
   * 
   * @param j2k time
   */
  public void setJ2k(double j2k) {
    this.j2k = j2k;
  }

  /**
   * Get pick.
   * @param pickType P, S, Coda1, Coda2
   * @return pick
   */
  public Pick getPick(String pickType) {
    return picks.get(pickType);
  }

  /**
   * Set pick. Assumes pick was not made on this channel.
   * 
   * @param pickType P, S, Coda1, Coda2
   * @param pick pick
   */
  public void setPick(String pickType, Pick pick) {
    setPick(pickType, pick, false);
  }
  
  /**
   * Set pick.
   * 
   * @param pickType P, S, Coda1, Coda2
   * @param pick pick
   * @param pickChannel true if pick was made on this channel.
   */
  public void setPick(String pickType, Pick pick, boolean pickChannel) {
    if (pick == null) {
      picks.remove(pickType);
    } else {
      picks.put(pickType, pick);
    }
    pickChannels.put(pickType, pickChannel);  
  }
  
  /**
   * Get enabled/disabled option for hiding phases.
   * 
   * @return true if hide phases option is enabled
   */
  public boolean isHidePhases() {
    return hidePhasesMenu.isSelected();
  }
  
  /**
   * Clear pick.
   * @param pickType pick type
   */
  private void clearPick(String pickType) {
    setPick(pickType, null);  
    if (pickType.equals(P) || pickType.equals(S)) {
      propagatePick(pickType, null);
      // reset weights to 0
      setWeightButton(pickType, Pick.Onset.EMERGENT, 0); // I done automatically
      propagateUncertainty(pickType, Pick.Onset.EMERGENT, 0);
      propagateUncertainty(pickType, Pick.Onset.IMPULSIVE, 0);
      repaint();
    }       
  }
  
  /**
   * Toggle weight for a given phase/onset combination.
   * @param phase P or S
   * @param onset Emergent or Impulsive 
   * @param weight 0 to 4
   */
  private void setWeightButton(String phase, Pick.Onset onset, int weight) {
    for (Pick.Onset o : Pick.Onset.values()) {
      if (o == Pick.Onset.QUESTIONABLE) {
        continue;
      }
      if (o == onset) {
        String key = phase + onset.toString().substring(0, 1);
        weightButtons.get(key)[weight].setSelected(true);
      } else {
        String key = phase + o.toString().substring(0, 1);
        weightButtons.get(key)[0].setSelected(true);
      }
    }
  }
  
  /**
   * Get enabled/disabled option for hiding coda.
   * 
   * @return true if hide coda option is enabled
   */
  public boolean isHideCoda() {
    return hideCodaMenu.isSelected();
  }

  /**
   * Determine if this is the channel where pick was made.
   * 
   * @return true if this channel is where user selected the pick
   */
  public boolean isPickChannel(String phase) {
    if (pickChannels.containsKey(phase)) {
      return pickChannels.get(phase);
    } else {
      return false;
    }
  }

  /**
   * Set pick channel flag.
   * @param phase P or S
   * @param pickChannel true if this is the channel pick was made on
   */
  public void setPickChannel(String phase, boolean pickChannel) {
    pickChannels.put(phase, pickChannel);
  }

  /**
   * Clear all picks.
   */
  public void clearAllPicks() {
    for (String pickType : new String[] {P, S, CODA1, CODA2}) {
      clearPick(pickType);
    }
  }

  /**
   * Is plot flag enabled or not.
   * 
   * @return true if plot flag enabled
   */
  public boolean isPlot() {
    return plotMenu.isSelected();
  }

  /**
   * Set plot flag.
   * 
   * @param plot true if enable plot
   */
  public void setPlot(boolean plot) {
    plotMenu.setSelected(plot);
  }
  
}
