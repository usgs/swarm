package gov.usgs.volcanoes.swarm.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A gulper listener that ignores its gulper.
 * 
 * @author Tom Parker
 *
 */
public class NopGulperListener implements GulperListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(NopGulperListener.class);

  public void gulperStarted() {
    LOGGER.debug("Gulper started");
  }

  public void gulperStopped(boolean killed) {
    LOGGER.debug("Gulper stopped");
  }

  public void gulperGulped(double t1, double t2, boolean success) {
    LOGGER.debug("Gulper gulped");
  }

}
