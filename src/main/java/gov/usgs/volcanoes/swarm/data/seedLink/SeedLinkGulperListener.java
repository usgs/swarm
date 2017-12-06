package gov.usgs.volcanoes.swarm.data.seedLink;

import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.swarm.data.Gulper;
import gov.usgs.volcanoes.swarm.data.GulperListener;

public class SeedLinkGulperListener implements GulperListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(SeedLinkGulperListener.class);
  private long lastRead;
  private Gulper gulper;
  private boolean alive = false;
  
  public SeedLinkGulperListener() {
    super();
    lastRead = System.currentTimeMillis();
  }

  public void gulperStarted() {
    LOGGER.debug("gulper started");
  }

  public void gulperStopped(boolean killed) {
    LOGGER.debug("gulper stopped");
  }

  public void gulperGulped(double t1, double t2, boolean success) {
    LOGGER.debug("gulper gulped");
    long now = System.currentTimeMillis();
    if (now - lastRead > 2 * SeedLinkSource.GULP_DELAY) {
      LOGGER.debug("killing gulper");
      if (gulper != null) {
        gulper.kill(this);
        alive = false;
      } else {
        throw new RuntimeException("Need to kill unknown gulper. This is a bug, please report it.");
      }
    }
  }

  public void read() {
    lastRead = System.currentTimeMillis();
  }

  public void setGulper(Gulper gulper) {
    this.gulper = gulper;
    alive = true;
  }

  public boolean isAlive() {
    return alive;
  }
}
