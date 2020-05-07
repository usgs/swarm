package gov.usgs.volcanoes.swarm.data.fdsnws;

import gov.usgs.volcanoes.swarm.ChannelInfo;
import gov.usgs.volcanoes.swarm.ChannelUtil;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

/**
 * Web service utility methods.
 *
 * @author Kevin Frechette (ISTI)
 */
public class WebServiceUtils {

  /** Swarm web services property key prefix. */
  public static final String SWARM_WS_PROP_KEY_PREFIX = "SWARM_WS_";

  /** Empty location code. */
  public static final String EMPTY_LOC_CODE = "--";

  /** Default debug level. */
  private static final Level defaultDebugLevel = Level.FINEST;

  /** Date format. */
  private static final DateFormat dateFormat;

  /** Debug level. */
  private static Level debugLevel;

  static {
    dateFormat = createDateFormat();
    try {
      final String s = getProperty(SWARM_WS_PROP_KEY_PREFIX + "DEBUG");
      if (s != null) {
        if (Boolean.valueOf(s)) {
          debugLevel = defaultDebugLevel;
        } else {
          debugLevel = Level.parse(s);
        }
      }
    } catch (final Exception ex) {
      //
    }
  }

  /**
   * Add the channel.
   * 
   * @param channels the list of channels.
   * @param ch the channel information.
   * @param source the seismic data source.
   * @return the channel information text.
   */
  public static String addChannel(final List<String> channels, final ChannelInfo ch,
      final SeismicDataSource source) {
    return ChannelUtil.addChannel(channels, ch, source);
  }

  /**
   * Assign the channels.
   * 
   * @param channels the list of channels.
   * @param source the seismic data source.
   */
  public static void assignChannels(final List<String> channels, final SeismicDataSource source) {
    ChannelUtil.assignChannels(channels, source);
  }

  /**
   * Create the date format.
   * 
   * @return the date format.
   */
  public static DateFormat createDateFormat() {
    // YYYY-MM-DDThh:mm:ss[.FFFFFF] ex. 1997-01-31T12:04:32.123
    final SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    df.setTimeZone(TimeZone.getTimeZone("GMT"));
    return df;
  }

  /**
   * Get the text for the current date.
   * 
   * @return the text.
   */
  public static String getDateText() {
    return getDateText(new Date());
  }

  /**
   * Get the text for the specified date.
   * 
   * @param date the date.
   * @return the text.
   */
  public static String getDateText(final Date date) {
    synchronized (dateFormat) {
      return dateFormat.format(date);
    }
  }

  /**
   * Get the property for the specified key.
   * 
   * @param key the name of the system property.
   * @return the property or null if none.
   */
  public static String getProperty(final String key) {
    return getProperty(key, (String) null);
  }

  /**
   * Get the property for the specified key.
   * 
   * @param key the name of the system property.
   * @param def a default value.
   * @return the property or the default value if none.
   */
  public static String getProperty(final String key, final String def) {
    String s = null;
    try {
      s = System.getProperty(key);
      if (s == null) {
        s = System.getenv(key);
      }
    } catch (final Exception ex) {
      //
    }
    if (s == null) {
      s = def;
    }
    return s;
  }

  /**
   * Determines if debug logging is enabled.
   * 
   * @return true if debug logging is enabled, false otherwise.
   */
  public static boolean isDebug() {
    return isDebug(defaultDebugLevel);
  }

  /**
   * Determines if debug logging is enabled.
   * 
   * @param level the message level.
   * @return true if debug logging is enabled, false otherwise.
   */
  public static boolean isDebug(final Level level) {
    return debugLevel != null && level.intValue() >= debugLevel.intValue();
  }

  /**
   * Get the date for the text.
   * 
   * @param s the text.
   * @return the date or null if none or error.
   */
  public static Date parseDate(final String s) {
    return parseDate(s, null);
  }

  /**
   * Get the date for the text.
   * 
   * @param s the text.
   * @param def the default date.
   * @return the date or the default date if none or error.
   */
  public static Date parseDate(final String s, final Date def) {
    if (s != null && s.length() > 0) {
      synchronized (dateFormat) {
        try {
          return dateFormat.parse(s);
        } catch (final Exception ex) {
          //
        }
      }
    }
    return def;
  }
}
