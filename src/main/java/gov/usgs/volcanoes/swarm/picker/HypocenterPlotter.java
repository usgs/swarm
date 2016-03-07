package gov.usgs.volcanoes.swarm.picker;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Projection;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.map.MapLayer;

public final class HypocenterPlotter implements MapLayer {

  Logger LOGGER = LoggerFactory.getLogger(HypocenterPlotter.class);
  private final List<Hypocenter> hypocenters;

  public HypocenterPlotter(List<Hypocenter> hypocenters) {
    this.hypocenters = hypocenters;
    // Hypocenters.addListener(this);
    MapFrame.getInstance().addLayer(this);
  }

  public void draw(Graphics2D g2, GeoRange range, Projection projection, int widthPx, int heightPx,
      int insetPx) {

    for (Hypocenter hypocenter : hypocenters) {
      LOGGER.debug("Hypo at {}, {}", hypocenter.lon, hypocenter.lat);
      final Point2D.Double xy =
          projection.forward(new Point2D.Double(-hypocenter.lon, hypocenter.lat));
      final double[] ext = range.getProjectedExtents(projection);
      final double dx = (ext[1] - ext[0]);
      final double dy = (ext[3] - ext[2]);
      final Point2D.Double res = new Point2D.Double();
      res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
      res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);

      g2.translate(res.x, res.y);
      g2.setColor(Color.RED);
      g2.drawOval(0, 0, 10, 10);
      g2.translate(-res.x, -res.y);
    }
  }
}
