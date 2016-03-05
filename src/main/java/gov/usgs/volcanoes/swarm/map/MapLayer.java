package gov.usgs.volcanoes.swarm.map;

import java.awt.Graphics2D;

import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Projection;

public interface MapLayer {

  public void draw(Graphics2D g2, GeoRange range, Projection projection, int widthPx, int heightPx, int insetPx);

}
