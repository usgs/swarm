package gov.usgs.volcanoes.swarm.map;

import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;

import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Projection;

public interface MapLayer {

  public void draw(Graphics2D g2);
  public boolean mouseClicked(MouseEvent e);
  public void setMapPanel(MapPanel mapPanel);
  public void stop();

}
