package gov.usgs.volcanoes.swarm.map;

import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

public interface MapLayer {

  public void draw(Graphics2D g2);

  public boolean mouseClicked(MouseEvent e);

  public void setMapPanel(MapPanel mapPanel);

  public void setVisible(boolean isVisible);

  public boolean mouseMoved(MouseEvent e);

}
