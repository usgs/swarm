package gov.usgs.volcanoes.swarm.picker;

import java.io.IOException;

public interface EventLocator {
  public void locate(Event event) throws IOException;
}
