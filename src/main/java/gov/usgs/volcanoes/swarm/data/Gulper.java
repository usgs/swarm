package gov.usgs.volcanoes.swarm.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;

/**
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

  private static Logger logger;

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
    if (lastTime > now)
      lastTime = now;
  }

  /**
   * Create the gulper and start it.
   *
   * @param gl the gulper list.
   * @param k the key.
   * @param glnr the gulper listener.
   * @param source the seismic data source.
   * @param ch the channel.
   * @param t1 the start time.
   * @param t2 the end time.
   * @param size the gulper size.
   * @param delay the gulper delay.
   * @deprecated use
   *             {@link #Gulper(GulperList, String, SeismicDataSource, String, double, double, int, int)}
   *             that does not call methods to support subclassing.
   */
  @Deprecated
  public Gulper(final GulperList gl, final String k, final GulperListener glnr,
      final SeismicDataSource source, final String ch, final double t1, final double t2,
      final int size, final int delay) {
    this(gl, k, source, ch, t1, t2, size, delay);
    addListener(glnr);
    update(t1, t2);
    start();
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
   * Determine if this gulper has been killed.
   *
   * @return true if the gulper has been killed or was never started.
   */
  public boolean isKilled() {
    return thread == null;
  }

  public void kill(final GulperListener gl) {
    removeListener(gl);
    if (listeners.size() == 0) {
      kill();
    }
  }

  public void start() {
    thread = new Thread(this);
    thread.start();
    LOGGER.debug("gulper started for {}", channel);
  }

  public void update(final double t1, final double t2) {
    final CachedDataSource cache = CachedDataSource.getInstance();
    if (t2 < lastTime)
      lastTime = t2;
    goalTime = t1;

    while (cache.inHelicorderCache(channel, lastTime - gulpSize, lastTime) && lastTime > goalTime
        && !isKilled()) {
      lastTime -= gulpSize;
      lastTime += 10;
    }
  }

  protected synchronized void fireStarted() {
    for (final GulperListener listener : listeners)
      listener.gulperStarted();
  }

  protected synchronized void fireGulped(final double t1, final double t2, final Wave w) {
    fireGulped(t1, t2, w != null && !isKilled());
  }

  protected synchronized void fireGulped(final double t1, final double t2, final boolean success) {
    for (final GulperListener listener : listeners)
      listener.gulperGulped(t1, t2, success);
  }

  protected synchronized void fireStopped() {
    final boolean killed = isKilled();
    for (final GulperListener listener : listeners)
      listener.gulperStopped(killed);
  }

  @Override
  public void run() {
    fireStarted();
    runLoop();
    gulpSource.close();
    if (isKilled())
      LOGGER.debug("gulper killed");
    else
      LOGGER.debug("gulper finished");
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
    if (!isKilled())
      try {
        Thread.sleep(gulpDelay);
      } catch (final InterruptedException ignore) {
      }
  }

  @Override
  public String toString() {
    return channel;
  }
}
