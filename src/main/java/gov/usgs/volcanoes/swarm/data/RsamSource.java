package gov.usgs.volcanoes.swarm.data;

import gov.usgs.volcanoes.core.data.RSAMData;

/**
 * Implemented by data sources which can provide RSAM.
 * 
 * @author Tom Parker
 */
public interface RsamSource {
  public abstract RSAMData getRsam(String station, double t1, double t2, int period);
}
