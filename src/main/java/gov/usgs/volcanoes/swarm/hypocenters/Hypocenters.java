package gov.usgs.volcanoes.swarm.hypocenters;

import java.util.ArrayList;
import java.util.List;

public class Hypocenters {

  private final static List<Hypocenter> hypocenters;
  private final static List<HypocenterListListener> listeners;
  private final static HypocenterPlotter plotter;

  static {
    hypocenters = new ArrayList<Hypocenter>();
    listeners = new ArrayList<HypocenterListListener>();
    plotter = new HypocenterPlotter(hypocenters);
  }

  /** uninstantiable */
  private Hypocenters() {}

  public static void addListener(HypocenterListListener listener) {
    listeners.add(listener);
  }

  public static void add (Hypocenter hypocenter) {
    hypocenters.add(hypocenter);
  }

  public static Hypocenters getInstance() {
    return HypocentersHolder.hypocenters;
  }

  private static class HypocentersHolder {
    public static Hypocenters hypocenters = new Hypocenters();
  }

}
