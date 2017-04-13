package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.swarm.SwarmConfig;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.swing.JTextArea;

/**
 * Holds and displays information about wave at selected time.
 * 
 * @author Diana Norgaard
 *
 */
public class StatusTextArea extends JTextArea {

  private static final long serialVersionUID = -4045063168343152079L;
  private static SwarmConfig swarmConfig;
  public static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
  
  /**
   * Default Constructor.
   */
  public StatusTextArea(String text) {
    super(text);
    setEditable(false);
    setLineWrap(true);
    swarmConfig = SwarmConfig.getInstance();    
  }  

  /**
   * Perform coda duration calculation.
   * 
   * @param startMark coda duration start time in j2k
   * @param endMark coda duration end time in j2k
   * @return coda string
   */
  public static String getCoda(double startMark, double endMark) {
    if (swarmConfig.durationEnabled) {
      double duration = Math.abs(startMark - endMark);
      double durationMagnitude = swarmConfig.getDurationMagnitude(duration);
      return String.format("Duration: %.2fs (Md: %.2f)", duration, durationMagnitude);
    } else {
      return null;
    }
  }
  
  /**
   * Convert time at cursor to String for display.
   * @param time in j2k
   * @return formatted string of time in local and GMT
   */
  public static String getTimeString(double time, TimeZone tz) {
    String text = dateFormat.format(J2kSec.asDate(time));
    double tzo = tz.getOffset(J2kSec.asEpoch(time));
    if (tzo != 0) {
      String tza = tz.getDisplayName(tz.inDaylightTime(J2kSec.asDate(time)), TimeZone.SHORT);
      text = "Time at cursor: " + dateFormat.format(J2kSec.asDate(time + tzo / 1000)) + " (" + tza
          + "), " + text + " (UTC)";
    }
    return text;
  }
  
  /**
   * Get wave information text for display.
   * 
   * @param wave wave to get information for
   * @return wave information text
   */
  public static String getWaveInfo(Wave wave) {
    int[] dataRange = wave.getDataRange();
    String waveInfo = null;
    try {
      waveInfo = String.format("[%s - %s (UTC), %d samples (%.2f s), %d samples/s, %d, %d]",
          J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, wave.getStartTime()),
          J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, wave.getEndTime()), wave.numSamples(),
          wave.numSamples() / wave.getSamplingRate(), (int) wave.getSamplingRate(), dataRange[0],
          dataRange[1]);
    } catch (NullPointerException e) {
      // do nothing
    }
    return waveInfo;
  }
}
