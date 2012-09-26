package gov.usgs.swarm.data;

import gov.usgs.swarm.ChannelInfo;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

public class WebServiceUtils
{
	/** Swarm web services property key prefix. */
	public final static String SWARM_WS_PROP_KEY_PREFIX = "SWARM_WS_";

	/** Default debug level. */
	private final static Level defaultDebugLevel = Level.FINEST;

	/** Date format. */
	private final static DateFormat dateFormat;

	/** Debug level. */
	private static Level debugLevel;

	static
	{
		dateFormat = createDateFormat();
		try
		{
			String s = getProperty(SWARM_WS_PROP_KEY_PREFIX + "DEBUG");
			if (s != null)
			{
				if (Boolean.valueOf(s))
				{
					debugLevel = defaultDebugLevel;
				}
				else
				{
					debugLevel = Level.parse(s);
				}
			}
		}
		catch (Exception ex)
		{
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
	public static String addChannel(List<String> channels, ChannelInfo ch,
			SeismicDataSource source)
	{
		final String formattedScnl = ch.getFormattedSCNL();
		if (!channels.contains(formattedScnl))
		{
			if (Swarm.config != null)
			{
				Metadata md = Swarm.config.getMetadata(formattedScnl, true);
				md.updateLongitude(ch.getLongitude());
				md.updateLatitude(ch.getLatitude());
				for (String g : ch.getGroups())
				{
					md.addGroup(g);
				}
				if (ch.getStation() != ch.getSiteName())
				{
					md.updateAlias(ch.getSiteName());
				}
				md.source = source;
			}
			channels.add(formattedScnl);
		}
		return formattedScnl;
	}

	/**
	 * Assign the channels.
	 * 
	 * @param channels the list of channels.
	 * @param source the seismic data source.
	 */
	public static void assignChannels(List<String> channels,
			SeismicDataSource source)
	{
		Collections.sort(channels);
		Swarm.config.assignMetadataSource(channels, source);
	}

	/**
	 * Create the date format.
	 * 
	 * @return the date format.
	 */
	public static DateFormat createDateFormat()
	{
		// YYYY-MM-DDThh:mm:ss[.FFFFFF] ex. 1997-01-31T12:04:32.123
		final SimpleDateFormat df = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSS");
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		return df;
	}

	/**
	 * Log a debug message.
	 * 
	 * @param level the message level.
	 * @param msg the message.
	 */
	public static void debug(Level level, String msg)
	{
		if (isDebug(level))
		{
			log(level, msg);
		}
	}

	/**
	 * Log a debug message.
	 * 
	 * @param msg the message.
	 */
	public static void debug(String msg)
	{
		debug(defaultDebugLevel, msg);
	}

	/**
	 * Log a fine message.
	 * 
	 * @param msg the message.
	 */
	public static void fine(String msg)
	{
		log(Level.FINE, msg);
	}

	/**
	 * Log a finer message.
	 * 
	 * @param msg the message.
	 */
	public static void finer(String msg)
	{
		log(Level.FINER, msg);
	}

	/**
	 * Log a finest message.
	 * 
	 * @param msg the message.
	 */
	public static void finest(String msg)
	{
		log(Level.FINEST, msg);
	}

	/**
	 * Get the text for the current date.
	 * 
	 * @return the text.
	 */
	public static String getDateText()
	{
		return getDateText(new Date());
	}

	/**
	 * Get the text for the specified date.
	 * 
	 * @param date the date.
	 * @return the text.
	 */
	public static String getDateText(Date date)
	{
		synchronized (dateFormat)
		{
			return dateFormat.format(date);
		}
	}

	/**
	 * Get the property for the specified key.
	 * 
	 * @param key the name of the system property.
	 * @return the property or null if none.
	 */
	public static String getProperty(String key)
	{
		return getProperty(key, (String) null);
	}

	/**
	 * Get the property for the specified key.
	 * 
	 * @param key the name of the system property.
	 * @param def a default value.
	 * @return the property or the default value if none.
	 */
	public static String getProperty(String key, String def)
	{
		String s = null;
		try
		{
			s = System.getProperty(key);
			if (s == null)
			{
				s = System.getenv(key);
			}
		}
		catch (Exception ex)
		{
		}
		if (s == null)
		{
			s = def;
		}
		return s;
	}

	/**
	 * Log an info message.
	 * 
	 * @param msg the message.
	 */
	public static void info(String msg)
	{
		log(Level.INFO, msg);
	}

	/**
	 * Determines if debug logging is enabled.
	 * 
	 * @return true if debug logging is enabled, false otherwise.
	 */
	public static boolean isDebug()
	{
		return isDebug(defaultDebugLevel);
	}

	/**
	 * Determines if debug logging is enabled.
	 * 
	 * @param level the message level.
	 * @return true if debug logging is enabled, false otherwise.
	 */
	public static boolean isDebug(Level level)
	{
		return debugLevel != null && level.intValue() >= debugLevel.intValue();
	}

	/**
	 * Log a message.
	 * 
	 * @param level the message level.
	 * @param msg the message.
	 */
	public static void log(Level level, String msg)
	{
		msg = "WebService: " + msg;
		if (Swarm.logger != null)
		{
			Swarm.logger.log(level, msg);
		}
		else
		{
			System.out.println(level + ": " + msg);
		}
	}

	/**
	 * Get the date for the text.
	 * 
	 * @param s the text.
	 * @return the date or null if none or error.
	 */
	public static Date parseDate(String s)
	{
		return parseDate(s, null);
	}

	/**
	 * Get the date for the text.
	 * 
	 * @param s the text.
	 * @param def the default date.
	 * @return the date or the default date if none or error.
	 */
	public static Date parseDate(String s, Date def)
	{
		if (s != null && s.length() > 0)
		{
			synchronized (dateFormat)
			{
				try
				{
					return dateFormat.parse(s);
				}
				catch (Exception ex)
				{
				}
			}
		}
		return def;
	}

	/**
	 * Sets if debug logging is enabled.
	 * 
	 * @param b true if debug logging is enabled, false otherwise.
	 */
	public static void setDebug(Level level)
	{
		debugLevel = level;
	}

	/**
	 * Log a warning message.
	 * 
	 * @param msg the message.
	 */
	public static void warning(String msg)
	{
		log(Level.WARNING, msg);
	}
}
