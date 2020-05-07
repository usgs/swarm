package gov.usgs.volcanoes.swarm.time;

import javax.swing.event.EventListenerList;

/**
 * A class used to synchronize wave panel displays with at a point in time chosen on a helicorder.
 * 
 * @author Tom Parker
 */
public final class WaveViewTime {

  private static final EventListenerList timeListeners = new EventListenerList();

  private WaveViewTime() {}

  public static void addTimeListener(TimeListener tl) {
    timeListeners.add(TimeListener.class, tl);
  }

  public static void removeTimeListener(TimeListener tl) {
    timeListeners.remove(TimeListener.class, tl);
  }

  /**
   * Fire time changed.
   * 
   * @param j2k j2k seconds
   */
  public static void fireTimeChanged(double j2k) {
    Object[] ls = timeListeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == TimeListener.class) {
        ((TimeListener) ls[i + 1]).timeChanged(j2k);
      }
    }
  }

}
