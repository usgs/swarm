package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.volcanoes.core.data.Wave;
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
   * Convert time at cursor to String for display.
   * 
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
      waveInfo = String.format("[%s - %s (UTC), %d samples (%.2f s), %.2f samples/s, %d, %d, %.1f]",
          J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, wave.getStartTime()),
          J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, wave.getEndTime()), wave.numSamples(),
          wave.numSamples() / wave.getSamplingRate(), wave.getSamplingRate(), dataRange[0],
          dataRange[1], wave.rsam());
    } catch (NullPointerException e) {
      // do nothing
    }
    return waveInfo;
  }

}
