package gov.usgs.volcanoes.swarm.options;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.event.EventListenerList;

/**
 * A singleton ArrayList of internal frames which keeps a list of listeners.
 * 
 * @author Tom Parker
 * 
 */
public final class SwarmOptions {

  private static final EventListenerList optionsListeners = new EventListenerList();
  private static final ArrayList<JInternalFrame> internalFrames = new ArrayList<JInternalFrame>();

  private SwarmOptions() {}

  public static void optionsChanged() {
    for (SwarmOptionsListener listener : optionsListeners.getListeners(SwarmOptionsListener.class))
      listener.optionsChanged();
  }

  public static void addOptionsListener(SwarmOptionsListener tl) {
    optionsListeners.add(SwarmOptionsListener.class, tl);
  }

  public static void removeOptionsListener(SwarmOptionsListener tl) {
    optionsListeners.remove(SwarmOptionsListener.class, tl);
  }

  public static List<JInternalFrame> getFrames() {
    return internalFrames;
  }
}
