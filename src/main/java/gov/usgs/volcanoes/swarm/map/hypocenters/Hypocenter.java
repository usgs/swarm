package gov.usgs.volcanoes.swarm.map.hypocenters;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.usgs.plot.render.DataPointRenderer;
import gov.usgs.util.Pair;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.map.ClickableGeoLabel;
import gov.usgs.volcanoes.swarm.picker.Phase;
import gov.usgs.volcanoes.swarm.picker.Phase.Builder;
import gov.usgs.volcanoes.swarm.picker.Phase.FirstMotion;
import gov.usgs.volcanoes.swarm.picker.Phase.Onset;
import gov.usgs.volcanoes.swarm.picker.Phase.PhaseType;
import gov.usgs.volcanoes.swarm.wave.MultiMonitor;
import gov.usgs.volcanoes.swarm.wave.SwarmMultiMonitors;

/**
 * 
 * @author Dan Cervelli
 * @author Tom Parker
 */
public class Hypocenter extends ClickableGeoLabel {
  private static final int ONE_HOUR = 60 * 60;
  private static final int ONE_DAY = ONE_HOUR * 24;
  private static final int ONE_WEEK = ONE_DAY * 7;
  
  private static final SwarmConfig swarmConfig = SwarmConfig.getInstance();

  private final double time;
  private final double depth;
  private final double magnitude;
  private final DataPointRenderer renderer;

  private Hypocenter(Builder builder) {
    time = builder.time;
    depth = builder.depth;
    magnitude = builder.magnitude;

    renderer = new DataPointRenderer();
    renderer.antiAlias = true;
    renderer.stroke = new BasicStroke(1.2f);
    renderer.filled = true;
    renderer.color = Color.LIGHT_GRAY;
    // r.shape = Geometry.STAR_10;
    renderer.shape = new Ellipse2D.Float(0f, 0f, 7f, 7f);
    marker = renderer;
  }

  @Override
  public Rectangle getClickBox() {
    return new Rectangle(-7, -7, 17, 17);
  }

  public void setColor(Color color) {
    renderer.paint = color;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    Map<String, Metadata> metadata = swarmConfig.getMetadata();
    List<Pair<Double, String>> nrst =
        Metadata.findNearest(swarmConfig.getMetadata(), location, true);
    Set<MultiMonitor> cleared = new HashSet<MultiMonitor>();
    if (nrst != null) {
      for (int i = 0, total = 0; i < nrst.size() && total < 10; i++) {
        String ch = nrst.get(i).item2;
        if (ch.matches(".* ..Z .*")) {
          Metadata md = metadata.get(ch);
          MultiMonitor mm = SwarmMultiMonitors.getMonitor(md.source);
          if (!cleared.contains(mm)) {
            mm.removeAllWaves();
            cleared.add(mm);
          }
          mm.addChannel(ch);
          mm.setVisible(true);
          mm.setPauseStartTime(time - 4);
          total++;
        }
      }
    }
  }
  
  public void draw(Graphics2D g2) {
    double age = J2kSec.now() - time;
    
    if (age < ONE_HOUR) {
      renderer.paint = Color.RED;
    } else if (age < ONE_DAY) {
      renderer.paint = Color.ORANGE;
    } else if (age < ONE_WEEK) {
      renderer.paint = Color.YELLOW;
    } else {
      renderer.paint = Color.WHITE;
    }
    super.draw(g2);
  }
  
  public static class Builder {
    private double time;
    private double depth;
    private double magnitude;

    public Builder() {}

    public Builder magnitude(double magnitude) {
      this.magnitude = magnitude;
      return this;
    }

    public Builder depth(double depth) {
      this.depth = depth;
      return this;
    }

    public Builder time(double time) {
      this.time = time;
      return this;
    }

    public Hypocenter build() {
      return new Hypocenter(this);
    }
  }
}
