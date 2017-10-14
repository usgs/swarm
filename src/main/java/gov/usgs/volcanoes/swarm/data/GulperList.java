package gov.usgs.volcanoes.swarm.data;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Dan Cervelli
 */
public class GulperList {
  public final static GulperList INSTANCE = new GulperList();

  private Map<String, Gulper> gulpers;

  private GulperList() {
    gulpers = new HashMap<String, Gulper>();
  }

  public synchronized Gulper requestGulper(String key, GulperListener gl, SeismicDataSource source,
      String ch, double t1, double t2, int size, int delay) {
    Gulper g = gulpers.get(key);
    if (g != null) {
      g.addListener(gl);
      g.update(t1, t2);
    } else {
      if (t2 - t1 < size) {
        source.getWave(ch, t1, t2);
      } else {
        g = source.createGulper(this, key, ch, t1, t2, size, delay);
        g.addListener(gl);
        g.update(t1, t2);
        g.start();
        gulpers.put(key, g);
      }
    }
    return g;
  }

  public synchronized void killGulper(String key, GulperListener gl) {
    Gulper g = gulpers.get(key);
    if (g != null)
      g.kill(gl);
  }

  /**
   * Called from the gulper.
   * @param g
   */
  public synchronized void removeGulper(Gulper g) {
    gulpers.remove(g.getKey());
  }
}
