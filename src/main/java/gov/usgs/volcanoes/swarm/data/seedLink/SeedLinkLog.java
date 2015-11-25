package gov.usgs.volcanoes.swarm.data.seedLink;

import java.util.logging.Level;
import java.util.logging.Logger;

import gov.usgs.volcanoes.swarm.data.seedLink.orfeus.BaseSLLog;

/**
 * SeedLink client log that uses Java logging.
 *
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkLog extends BaseSLLog {
  /** Log prefix text. */
  private static final String LOG_PREFIX;

  /** The logger. */
  public static final Logger logger;

  static {

    logger = Logger.getLogger("gov.usgs.volcanoes.swarm.data.seedLink.SeedLinkLog");
    logger.setLevel(Level.FINE);
    LOG_PREFIX = "SeedLink: ";
  }

  /**
   * Create the SeedLink client log.
   */
  public SeedLinkLog() {
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
  @Override
  public void log(final boolean isError, final int verbosity, String message) {
    final Level level;
    if (isError) // error message
    {
      message = errPrefix + message;
      level = Level.INFO;
    } else {
      message = logPrefix + message;
      switch (verbosity) {
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

  public void setLevel(final Level newLevel) {
    logger.setLevel(newLevel);
  }
}
