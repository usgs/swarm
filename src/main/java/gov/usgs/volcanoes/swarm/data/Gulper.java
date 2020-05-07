package gov.usgs.volcanoes.swarm.data;

import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gulper.
 * 
 * @author Dan Cervelli
 */
public class Gulper implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(Gulper.class);

  private final SeismicDataSource gulpSource;
  private final GulperList gulperList;
  private final String channel;
  private double lastTime;
  private double goalTime;
  private Thread thread;
  private final String key;
  private final Set<GulperListener> listeners;

  private final int gulpSize;
  private final int gulpDelay;

  /**
   * Create the gulper. This does not call methods to support subclassing.
   *
   * @param gl the gulper list.
   * @param k the key.
   * @param ch the channel.
   * @param t1 the start time.
   * @param t2 the end time.
   * @param size the gulper size.
   * @param delay the gulper delay.
   */
  public Gulper(final GulperList gl, final String k, final SeismicDataSource source,
      final String ch, final double t1, final double t2, final int size, final int delay) {
    gulpSize = size;
    gulpDelay = delay;
    gulperList = gl;
    gulpSource = source;
    key = k;
    listeners = new HashSet<GulperListener>();
    channel = ch;
    lastTime = t2;

    final double now = J2kSec.now();
    if (lastTime > now) {
      lastTime = now;
    }
  }

  public synchronized void addListener(final GulperListener gl) {
    listeners.add(gl);
  }

  public synchronized void removeListener(final GulperListener gl) {
    listeners.remove(gl);
  }

  public String getChannel() {
    return channel;
  }

  public String getKey() {
    return key;
  }

  /**
   * Kill this gulper.
   */
  protected void kill() {
    // use local copy to ensure it isn't changed elsewhere,
    // no need to lock since OK to have multiple calls to Thread.interrupt
    final Thread t = thread;
    if (t != null) {
      thread = null;
      t.interrupt();
    }
  }

  /**
   * Kill gulper.
   * 
   * @param gl gulper listener
   */
  public void kill(final GulperListener gl) {
    removeListener(gl);
    if (listeners.size() == 0) {
      kill();
    }
  }

  /**
   * Determine if this gulper has been killed.
   *
   * @return true if the gulper has been killed or was never started.
   */
  public boolean isKilled() {
    return thread == null;
  }


  /**
   * Start gulper.
   */
  public void start() {
    thread = new Thread(this);
    thread.start();
    LOGGER.debug("gulper started for {}", channel);
  }

  /**
   * Update gulper.
   * 
   * @param t1 start time
   * @param t2 end time
   */
  public void update(final double t1, final double t2) {
    final CachedDataSource cache = CachedDataSource.getInstance();
    if (t2 < lastTime) {
      lastTime = t2;
    }
    goalTime = t1;

    while (cache.inHelicorderCache(channel, lastTime - gulpSize, lastTime) && lastTime > goalTime
        && !isKilled()) {
      lastTime -= gulpSize;
      lastTime += 10;
    }
  }

  protected synchronized void fireStarted() {
    for (final GulperListener listener : listeners) {
      listener.gulperStarted();
    }
  }

  protected synchronized void fireGulped(final double t1, final double t2, final Wave w) {
    fireGulped(t1, t2, w != null && !isKilled());
  }

  protected synchronized void fireGulped(final double t1, final double t2, final boolean success) {
    for (final GulperListener listener : listeners) {
      listener.gulperGulped(t1, t2, success);
    }
  }

  protected synchronized void fireStopped() {
    final boolean killed = isKilled();
    for (final GulperListener listener : listeners) {
      listener.gulperStopped(killed);
    }
  }

  /**
   * Thread's run method.
   * 
   * @see java.lang.Runnable#run()
   */
  public void run() {
    fireStarted();
    runLoop();
    gulpSource.close();
    if (isKilled()) {
      LOGGER.debug("gulper killed");
    } else {
      LOGGER.debug("gulper finished");
    }
    gulperList.removeGulper(this);
    fireStopped();
  }

  /**
   * This is the run loop which a subclass may override.
   */
  protected void runLoop() {
    while (lastTime > goalTime && !isKilled()) {
      try {
        final double t1 = lastTime - gulpSize;
        final double t2 = lastTime;
        final Wave w = gulpSource.getWave(channel, t1, t2);
        fireGulped(t1, t2, w);
        update(goalTime, lastTime - gulpSize + 10);
      } catch (final Throwable e) {
        System.err.println("Exception during gulp:");
        e.printStackTrace();
      }
      delay();
    }
  }

  protected void delay() {
    if (!isKilled()) {
      try {
        Thread.sleep(gulpDelay);
      } catch (final InterruptedException ignore) {
        //
      }
    }
  }

  @Override
  public String toString() {
    return channel;
  }
}
