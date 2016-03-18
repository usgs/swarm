package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;

public class QuakeMlUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(QuakeMlUtils.class);
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

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
