package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.swarm.SwarmConfig;

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
   * Perform duration calculation.
   * 
   * @param startTime duration start time in j2k
   * @param endTime duration end time in j2k
   * @return duration string
   */
  public static String getDuration(double startTime, double endTime) {
    if (swarmConfig.durationEnabled) {
      double duration = Math.abs(startTime - endTime);
      double durationMagnitude = swarmConfig.getDurationMagnitude(duration);
      return String.format("Duration: %.2fs (Md: %.2f)", duration, durationMagnitude);
    } else {
      return null;
    }
  }
  
  /**
   * Perform coda duration calculation. More or less the same calculation as getDuration.
   * 
   * @param startTime duration start time in milliseconds since 1970
   * @param endTime duration end time in milliseconds since 1970
   * @return duration string
   */
  public static String getCodaDuration(long startTime, long endTime) {
    if (swarmConfig.durationEnabled) {
      double duration = Math.abs(startTime - endTime) / 1000.0;
      double durationMagnitude = swarmConfig.getDurationMagnitude(duration);
      String coda = String.format("Coda: %.2fs (Mc: %.2f)", duration, durationMagnitude);
      
      // get clipboard average
      WaveClipboardFrame cb = WaveClipboardFrame.getInstance();
      int count = 0;
      double sumDuration = 0;
      for (WaveViewPanel p : cb.getWaves()) {
        Pick c1 = p.getPickMenu().getCoda1();
        Pick c2 = p.getPickMenu().getCoda2();
        if (c1 != null && c2 != null) {
          sumDuration += Math.abs(c1.getTime() - c2.getTime()) / 1000.0;
          count++;
        }
      }
      if (count == 1) {
        return coda;
      }
      double avgDuration = sumDuration / count;
      double avgDurationMagnitude = swarmConfig.getDurationMagnitude(avgDuration);
      String avgCoda =
          String.format("Avg Coda: %.2fs (Mc: %.2f)", avgDuration, avgDurationMagnitude);

      // return final coda string
      return coda + ", " + avgCoda;
    } else {
      return null;
    }
  }
  
  /**
   * Return S-P duration and distance text for display.
   * 
   * @param pTime P phase pick time in milliseconds since 1970
   * @param sTime S phase pick time in milliseconds since 1970
   * @return S-P text
   */
  public static String getSpString(long pTime, long sTime) {
    double duration = (sTime - pTime) / 1000.0;
    double distance = SwarmConfig.getInstance().pVelocity * duration;
    return String.format("S-P: %.2fs (%.2fkm)", duration, distance);
  }
  
  /**
   * Convert time at cursor to String for display.
   * @param time in j2k
   * @return formatted string of time in local and GMT
   */
  public static String getTimeString(double time, TimeZone tz) {
    String utc = J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, time);
    double tzo = tz.getOffset(J2kSec.asEpoch(time)) / 1000;
    if (tzo != 0) {
      String tza = tz.getDisplayName(tz.inDaylightTime(J2kSec.asDate(time)), TimeZone.SHORT);
      String status = J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, time + tzo) + " (" + tza + "), "
          + utc + " (UTC)";
      return status;
    } else {
      return utc;
    }
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
