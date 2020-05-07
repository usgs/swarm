package gov.usgs.volcanoes.swarm.data;

import java.util.HashMap;
import java.util.Map;

/**
 * Gulper List.
 * 
 * @author Dan Cervelli
 */
public class GulperList {
  public static final GulperList INSTANCE = new GulperList();

  private Map<String, Gulper> gulpers;

  private GulperList() {
    gulpers = new HashMap<String, Gulper>();
  }

  /**
   * Request gulper.
   * 
   * @param key gulper key
   * @param gl gulper listener
   * @param source seismic data source
   * @param ch channel string
   * @param t1 start time
   * @param t2 end time
   * @param size gulper size
   * @param delay gulper delay
   * @return
   */
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

  /**
   * Kill gulper.
   * 
   * @param key gulper key
   * @param gl gulper listener
   */
  public synchronized void killGulper(String key, GulperListener gl) {
    Gulper g = gulpers.get(key);
    if (g != null) {
      g.kill(gl);
    }
  }

  /**
   * Called from the gulper.
   * 
   * @param g gulper
   */
  public synchronized void removeGulper(Gulper g) {
    gulpers.remove(g.getKey());
  }
}
