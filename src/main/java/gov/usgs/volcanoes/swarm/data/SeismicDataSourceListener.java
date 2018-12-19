package gov.usgs.volcanoes.swarm.data;

import java.util.EventListener;

/**
 * Seismic data source listener.
 *
 * @author Dan Cervelli
 */
public interface SeismicDataSourceListener extends EventListener {
  public void channelsUpdated();

  public void channelsProgress(String id, double progress);

  public void helicorderProgress(String channel, double progress);
}
