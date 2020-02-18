package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;

import java.util.HashMap;
import java.util.Map;

public final class SwarmMultiMonitors {
  /**
   * A singleton class to keep track of a list of Multimonitors.
   * 
   * @author Tom Parker
   * 
   */
  private static final Map<String, MultiMonitor> monitors = new HashMap<String, MultiMonitor>();

  private SwarmMultiMonitors() {}

  public static void removeMonitor(MultiMonitor mm) {
    monitors.remove(mm.getDataSource().getName());
    SwarmInternalFrames.remove(mm);
    mm = null;
  }

  public static MultiMonitor getMonitor(SeismicDataSource source) {
    MultiMonitor monitor = monitors.get(source.getName());
    if (monitor == null) {
      monitor = new MultiMonitor(source);
      monitors.put(source.getName(), monitor);
      SwarmInternalFrames.add(monitor);
    }
    return monitor;
  }

  public static void monitorChannelSelected(SeismicDataSource source, String channel) {
    MultiMonitor monitor = getMonitor(source);
    monitor.setVisible(true);
    monitor.addChannel(channel);
  }
}
