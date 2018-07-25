package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.util.HashMap;

/**
 * Store pick data from pick menu.
 * 
 * @author Diana Norgaard
 *
 */
public class PickData {

  public static String P = "P";
  public static String S = "S";
  public static String CODA1 = "C1";
  public static String CODA2 = "C2";
  
  protected HashMap<String, Pick> picks = new HashMap<String, Pick>();
  protected HashMap<String, Boolean> pickChannels = new HashMap<String, Boolean>();
  protected HashMap<String, Integer> weight = new HashMap<String, Integer>();
      
  protected boolean hidePhases = false;
  protected boolean hideCoda = false;
  protected boolean plot = true;
  
  /**
   * Default constructor.
   */
  public PickData() {
    pickChannels.put(P, false);
    pickChannels.put(S, false);
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
    double vp = SwarmConfig.getInstance().pVelocity;
    double vs = vp/SwarmConfig.getInstance().velocityRatio;
    double distance = duration * (vp*vs)/(vp-vs);
    return distance;
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
   * Get number of picks for channel.
   * @return number of picks made
   */
  public int getPickCount() {
    return picks.size();
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
   * Clear pick.
   * @param phase pick type
   */
  public void clearPick(String phase, WaveViewPanel wvp) {
    setPick(phase, null);  
    if (phase.equals(P) || phase.equals(S)) {
      propagatePick(phase, null, wvp);
      setWeight(phase, 0);
    }       
  }
  
  /**
   * Clear all picks.
   */
  public void clearAllPicks(WaveViewPanel wvp) {
    for (String pickType : new String[] {P, S, CODA1, CODA2}) {
      clearPick(pickType, wvp);
    }
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
   * Create pick object.
   * 
   * @param phase P or S
   * @param onset emergent or impulsive
   * @param polarity positive, negative, unknown
   * @param j2kTime pick time in J2K
   * @param wvp wave view panel pick was made on
   * @weight weight 0 to 4
   */
  public Pick createPick(String phase, Pick.Onset onset, Pick.Polarity polarity, double j2kTime,
      WaveViewPanel wvp, int weight) {
    long time = J2kSec.asDate(j2kTime).getTime();
    String channel = wvp.getChannel();
    String publicId = EventDialog.QUAKEML_RESOURCE_ID + "/Pick/" + System.currentTimeMillis();
    Pick pick = new Pick(publicId, time, channel);
    pick.setPhaseHint(phase);
    pick.setOnset(onset);
    pick.setPolarity(polarity);
    /*    if (phase.equals(P) || phase.equals(S)) {      
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
    }*/
    picks.put(phase, pick);
    
    // propagate pick
    if (phase.equals(P) || phase.equals(S)) {
      propagatePick(phase, pick, wvp);
      double uncertainty = PickSettingsDialog.getInstance().getSettings().getWeightToTime(weight,
          wvp.getWave().getSamplingRate()) / 1000.0;
      pick.getTimeQuantity().setUncertainty(uncertainty);
    }
    picks.put(phase, pick);
    return pick;
  }
  
  
  /**
   * Propagate P or S pick to wave view panel of same station.
   * 
   * @param phase P or S
   * @param pick pick object
   * @param wvp wave panel associated with this pick data
   */
  public void propagatePick(String phase, Pick pick, WaveViewPanel wvp) {
    for (WaveViewPanel otherWvp : WaveClipboardFrame.getInstance().getWaves()) {
      if (wvp.getChannel().equals(otherWvp.getChannel())) {
        continue;
      }
      if (wvp.isSameStation(otherWvp)) {
        otherWvp.getPickData().setPick(phase, pick);
        otherWvp.repaint();
      }
    }
  }

  /**
   * Propagate plot setting to others pick menu of same station.
   * 
   * @param plot true to plot S-P for station
   * @param wvp wave panel associated with this pick data
   */
  public void propagatePlot(boolean plot, WaveViewPanel wvp) {
    for (WaveViewPanel otherWvp : WaveClipboardFrame.getInstance().getWaves()) {
      if (wvp.getChannel().equals(otherWvp.getChannel())) {
        continue;
      }
      if (wvp.isSameStation(otherWvp)) {
        otherWvp.getPickData().setPlot(plot);
        otherWvp.repaint();
      }
    }
  }
  
  /**
   * Propagate weight setting changes to other panels of same station.
   * @param phase P or S
   * @param weight 0 to 4
   * @param wvp wave panel associated with this pick data
   */
  public void propagateUncertainty(String phase, int weight, WaveViewPanel wvp) {
    for (WaveViewPanel otherWvp : WaveClipboardFrame.getInstance().getWaves()) {
      if (wvp.getChannel().equals(otherWvp.getChannel())) {
        continue;
      }
      if (wvp.isSameStation(otherWvp)) {
        otherWvp.getPickData().setWeight(phase, weight);
        otherWvp.repaint();
      }
    }
  }
  
  /**
   * Get picks.
   * @return the picks
   */
  public HashMap<String, Pick> getPicks() {
    return picks;
  }


  /**
   * Set picks.
   * @param picks the picks to set
   */
  public void setPicks(HashMap<String, Pick> picks) {
    this.picks = picks;
  }

  /**
   * Get pick channels.
   * @return the pickChannels
   */
  public HashMap<String, Boolean> getPickChannels() {
    return pickChannels;
  }


  /**
   * Set pick channels.
   * @param pickChannels the pickChannels to set
   */
  public void setPickChannels(HashMap<String, Boolean> pickChannels) {
    this.pickChannels = pickChannels;
  }
  
  /**
   * Set weight for a pick.
   */
  public void setWeight(String phase, int weight) {
    this.weight.put(phase, weight);
  }

  /**
   * Get hide phases flag.
   * @return the hidePhases
   */
  public boolean isHidePhases() {
    return hidePhases;
  }


  /**
   * Set hide phases flag.
   * @param hidePhases the hidePhases to set
   */
  public void setHidePhases(boolean hidePhases) {
    this.hidePhases = hidePhases;
  }


  /**
   * Get hide coda flag.
   * @return the hideCoda
   */
  public boolean isHideCoda() {
    return hideCoda;
  }


  /**
   * Set hide coda flag.
   * @param hideCoda the hideCoda to set
   */
  public void setHideCoda(boolean hideCoda) {
    this.hideCoda = hideCoda;
  }


  /**
   * Get plot option.
   * @return the plot
   */
  public boolean isPlot() {
    return plot;
  }


  /**
   * Set plot option.
   * @param plot the plot to set
   */
  public void setPlot(boolean plot) {
    this.plot = plot;
  }

}
