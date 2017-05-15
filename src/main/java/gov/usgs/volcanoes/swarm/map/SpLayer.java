package gov.usgs.volcanoes.swarm.map;

import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Projection;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.event.PickMenu;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Vector;

/**
 * This layer will draw S-P circles on the map.
 * @author Diana Norgaard
 *
 */
public class SpLayer implements MapLayer {

  private static final float[] dash = { 2.0f };
  private static final BasicStroke dashed =
      new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f);
  private static final BasicStroke solid =
      new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
  
  private Vector<Sp> spList = new Vector<Sp>();
  private MapPanel panel;

  /**
   * Constructor.
   */
  public SpLayer() {
    // TODO Auto-generated constructor stub
  }

  /**
   * Update list of S-P circles to draw.
   */
  private void updateSpList() {
    spList.clear();
    List<WaveViewPanel> waves = WaveClipboardFrame.getInstance().getWaves();
    for (WaveViewPanel wvp : waves) {
      if (wvp.getSettings().pickEnabled) {
        PickMenu pickMenu = wvp.getPickMenu();
        if (pickMenu.getP() != null && pickMenu.isPickChannelP()) {
          String channel = wvp.getChannel();
          double distance = pickMenu.getSpDistance();
          if (!Double.isNaN(distance)) {
            spList.add(new Sp(channel, distance, pickMenu.getSpMinDistance(),
                pickMenu.getSpMaxDistance()));
          }
        }
      }
    }
    
  }
  
  /**
   * @see gov.usgs.volcanoes.swarm.map.MapLayer#draw(java.awt.Graphics2D)
   */
  public void draw(Graphics2D g2) {
    //Color color = g2.getColor();
    Stroke stroke = g2.getStroke();
    Shape clip = g2.getClip();
    
    // First get updated list of S-P
    updateSpList();
    if (spList.size() == 0) {
      return;
    }
    if (panel == null) {
      panel = MapFrame.getInstance().getMapPanel();
    }
    // set clip
    int widthPx = panel.getGraphWidth();
    int heightPx = panel.getGraphHeight();
    int inset = panel.getInset();
    Rectangle2D rectangle = new Rectangle2D.Double(inset, inset, widthPx, heightPx);
    g2.setClip(rectangle);
    
    // draw
    GeoRange range = panel.getRange();
    Projection projection = panel.getProjection();
    final double[] ext = range.getProjectedExtents(projection);
    final double dx = (ext[1] - ext[0]);
    for (Sp sp : spList) {
      Metadata md = SwarmConfig.getInstance().getMetadata(sp.channel);
      if (md.hasLonLat()) {
        Point2D.Double center = panel.getXy(md.getLongitude(), md.getLatitude());

        // draw S-P circle
        g2.setStroke(solid);
        double radius = widthPx * 1000 * sp.distance / dx;
        g2.drawOval((int) (center.x - radius), (int) (center.y - radius), (int) radius * 2,
            (int) radius * 2);
        // draw S-P uncertainty
        g2.setStroke(dashed);
        if (!Double.isNaN(sp.minDistance)) {
          double minRadius = widthPx * 1000 * sp.minDistance / dx;
          g2.drawOval((int) (center.x - minRadius), (int) (center.y - minRadius),
              (int) minRadius * 2, (int) minRadius * 2);
        }
        if (!Double.isNaN(sp.maxDistance)) {
          double maxRadius = widthPx * 1000 * sp.maxDistance / dx;
          g2.drawOval((int) (center.x - maxRadius), (int) (center.y - maxRadius),
              (int) maxRadius * 2, (int) maxRadius * 2);
        }
      }
    }
    
    //g2.setColor(color);
    g2.setStroke(stroke);
    g2.setClip(clip);
  }
  
  /**
   * S-P data.
   */
  class Sp {
    private String channel;
    private double distance;
    private double minDistance;
    private double maxDistance;

    Sp(String channel, double distance, double minDistance, double maxDistance) {
      this.channel = channel;
      this.distance = distance;
      this.minDistance = minDistance;
      this.maxDistance = maxDistance;
    }
  }

  public boolean mouseClicked(MouseEvent e) {
    // TODO Auto-generated method stub
    return false;
  }

  public void setMapPanel(MapPanel mapPanel) {
    this.panel = mapPanel;
  }

  public void setVisible(boolean isVisible) {
    
  }

  public boolean mouseMoved(MouseEvent e) {
    // TODO Auto-generated method stub
    return false;
  }

}
