/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility class for working with QuakeML files.
 * 
 * @author Tom Parker
 *
 */
public class QuakeMlUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuakeMlUtils.class);
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

  /**
   * Format date for QuakeML
   * @param millis milliseconds since 1/1/1970 00:00:00 GMT
   * @return time string in yyyy-MM-dd'T'HH:mm:ss.SSSX format
   */
  public static String formatDate(long millis){
    SimpleDateFormat dateF = new SimpleDateFormat(DATE_FORMAT);
    Date date = new Date(millis);
    return dateF.format(date);
  }
  
  /**
   * Parse QuakeML date/time.
   * @param timeString time string in yyyy-MM-dd'T'HH:mm:ss.SSSX format
   * @return milliseconds since 1/1/1970 00:00:00 GMT
   */
  public static long parseTime(String timeString) {
    String inString = timeString;
    timeString = timeString.replaceFirst("\\.(\\d)Z?", ".$100Z");
    timeString = timeString.replaceFirst("\\.(\\d{2})Z?$", ".$10Z");
    timeString = timeString.replaceFirst(":(\\d{2})Z?$", ":$1.000Z");

    long time = Long.MIN_VALUE;
    SimpleDateFormat dateF = new SimpleDateFormat(DATE_FORMAT);
    try {
      time = dateF.parse(timeString).getTime();
    } catch (ParseException e) {
      LOGGER.error("Cannot parse time String {}", inString);
      throw new RuntimeException("Cannot parse time string " + inString);
    }
    return time;
  }
}
