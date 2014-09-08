package gov.usgs.swarm.data;

import gov.usgs.swarm.data.orfeus.BaseSLLog;
import gov.usgs.util.Log;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SeedLink client log that uses Java logging.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkLog extends BaseSLLog
{
	/** Log prefix text. */
	private static final String LOG_PREFIX;

	/** The logger. */
	public static final Logger logger;
	static
	{
		logger = Log.getLogger("gov.usgs.swarm.data.SeedLinkLog");
		logger.setLevel(Level.FINE);
		LOG_PREFIX = "SeedLink: ";
	}

	/**
	 * Create the SeedLink client log.
	 */
	public SeedLinkLog()
	{
		setErrPrefix(LOG_PREFIX);
		setLogPrefix(LOG_PREFIX);
	}

	/**
	 * 
	 * Logs a message in appropriate manner.
	 * 
	 * @param isError true if error message, false otherwise.
	 * @param verbosity verbosity level for this message.
	 * @param message message text.
	 */
	public void log(boolean isError, int verbosity, String message)
	{
		final Level level;
		if (isError) // error message
		{
			message = errPrefix + message;
			level = Level.INFO;
		}
		else
		{
			message = logPrefix + message;
			switch (verbosity)
			{
			case 0:
				level = Level.INFO;
				break;
			case 1:
				level = Level.FINE;
				break;
			case 2:
				level = Level.FINER;
				break;
			default:
				level = Level.FINEST;
				break;
			}
		}
		logger.log(level, message);
	}

	public void setLevel(Level newLevel)
	{
		logger.setLevel(newLevel);
	}
}
