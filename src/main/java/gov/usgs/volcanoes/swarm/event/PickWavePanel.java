package gov.usgs.volcanoes.swarm.event;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.swing.JScrollPane;
import javax.swing.JViewport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.time.TimeListener;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelAdapter;

public class PickWavePanel extends AbstractWavePanel implements EventObserver, Comparable<PickWavePanel> {

  private static final Logger LOGGER = LoggerFactory.getLogger(PickWavePanel.class);

  private static final Font ANNOTATION_FONT = new Font("Monospaced", Font.BOLD, 12);
  private static final Color P_BACKGROUND = new Color(128, 255, 128, 192);
  private static final Color S_BACKGROUND = new Color(128, 128, 255, 192);
  private static final Color RESIDUAL_COLOR = new Color(128, 128, 128, 32);
  private final List<Arrival> arrivals;
  private final Stack<double[]> zoomHistory;
private JViewport viewport;

  public PickWavePanel() {
    super();
    allowDragging = true;
    arrivals = new ArrayList<Arrival>();
    zoomHistory = new Stack<double[]>();
    createListeners();
  }

  public void addArrival(Arrival arrival) {
    arrivals.add(arrival);
  }


  private void createListeners() {
    WaveViewTime.addTimeListener(new TimeListener() {
      public void timeChanged(final double j2k) {
        setCursorMark(j2k);
      }
    });

    this.addListener(new WaveViewPanelAdapter() {
      @Override
      public void waveZoomed(final AbstractWavePanel src, final double st, final double et,
          final double nst, final double net) {
        final double[] t = new double[] {st, et};
        zoomHistory.push(t);
        zoom(nst, net);
      }
    });
  }

  @Override
  protected void annotateImage(Graphics2D g2) {
    for (Arrival arrival : arrivals) {
      Pick pick = arrival.getPick();

      String tag = arrival.getTag();
      double j2k = J2kSec.fromEpoch(pick.getTime());
      double[] t = getTranslation();
      if (t == null)
        continue;

      double x = 2 + (j2k - t[1]) / t[0];
      g2.setColor(DARK_GREEN);
      g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

      double residual = arrival.getTimeResidual();
      if (residual != 0) {
        double residualMark = 2 + (j2k + residual - t[1]) / t[0];
        g2.setColor(RESIDUAL_COLOR);
        g2.fill(new Rectangle2D.Double(Math.min(x, residualMark), yOffset,
            Math.abs(x - residualMark), getHeight() - bottomHeight - 1));
      }
      
      g2.setFont(ANNOTATION_FONT);

      FontMetrics fm = g2.getFontMetrics();
      int width = fm.stringWidth(tag);
      int height = fm.getAscent();

      int offset = 2;
      int lw = width + 2 * offset;

      if (tag.indexOf('P') != -1) {
        g2.setColor(P_BACKGROUND);
      } else if (tag.indexOf('S') != -1) {
        g2.setColor(S_BACKGROUND);
      }

      g2.fillRect((int) x, 3, lw, height + 2 * offset);
      g2.setColor(Color.black);
      g2.drawRect((int) x, 3, lw, height + 2 * offset);

      g2.drawString(tag, (int) x + offset, 3 + (fm.getAscent() + offset));
    }
  }

  @Override
  protected void processRightMouseRelease(MouseEvent e) {
    pauseCursorMark = false;
  }

  public void eventUpdated() {
    repaint();
  }

  @Override
  protected void processRightMousePress(MouseEvent e) {
    // TODO Auto-generated method stub

  }
  
  public void setViewport(JViewport viewport) {
    this.viewport = viewport;
  }

  public int compareTo(PickWavePanel o) {
    return Arrival.distanceComparator().compare(arrivals.get(0),o.arrivals.get(0));
  }    
         
}
