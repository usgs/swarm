package gov.usgs.volcanoes.swarm.rsam;

import cern.colt.matrix.DoubleMatrix2D;

import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.GenericDataMatrix;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.decorate.SmartTick;
import gov.usgs.plot.render.AxisRenderer;
import gov.usgs.plot.render.HistogramRenderer;
import gov.usgs.plot.render.MatrixRenderer;
import gov.usgs.plot.render.ShapeRenderer;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.time.UiTime;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * A component that renders a RSAM plot.
 * 
 * 
 * @author Tom Parker
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(value = "SE_BAD_FIELD",
    justification = "Class not serializable")
public class RsamViewPanel extends JComponent implements SettingsListener {
  public static final long serialVersionUID = -1;

  private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);

  /**
   * X pixel location of where the main plot axis should be located on the
   * component.
   */
  private static final int X_OFFSET = 60;

  /**
   * Y pixel location of where the main plot axis should be located on the
   * component.
   */
  private static final int Y_OFFSET = 20;

  /** The amount of padding space on the right side. */
  private static final int RIGHT_WIDTH = 60;

  /** The amount of padding space on the bottom. */
  private static final int BOTTOM_HEIGHT = 20;

  private RSAMData data;

  private double startTime;
  private double endTime;
  private RsamViewSettings settings;

  private String channel;

  /**
   * A flag that indicates whether data are being loaded for this panel.
   */
  private boolean working;

  /**
   * The wave is rendered to an image that is only updated when the settings
   * change for repaint efficiency.
   */
  private BufferedImage image;

  /**
   * Constructs a WaveViewPanel with default settings.
   */
  public RsamViewPanel() {
    this(new RsamViewSettings());
    settings.addListener(this);
  }

  /**
   * Constructs a WaveViewPanel with specified settings.
   * 
   * @param s
   *          the settings
   */
  public RsamViewPanel(RsamViewSettings s) {
    settings = s;

    setupMouseHandler();
    settings.addListener(this);
  }


  private void setupMouseHandler() {
    Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
    this.setCursor(crosshair);

    this.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        UiTime.touchTime();
        if (SwingUtilities.isRightMouseButton(e)) {
          settings.cycleType();
        }
      }
    });
  }


  /**
   * Set the working flag. This flag indicates whether data are being loaded
   * for this panel.
   * 
   * @param b
   *          the working flag state
   */
  public void setWorking(boolean b) {
    working = b;
  }


  public String getChannel() {
    return channel;
  }

  public void setChannel(String c) {
    channel = c;
  }

  public void settingsChanged() {
    processSettings();
  }


  /**
   * Set RSAM data.
   * @param data RSAM data
   * @param st start time
   * @param et end time
   */
  public void setData(RSAMData data, double st, double et) {
    this.data = data;
    startTime = st;
    endTime = et;
    processSettings();
  }

  private synchronized void setImage(BufferedImage bi) {
    image = bi;
  }

  private synchronized BufferedImage getImage() {
    return image;
  }

  private void createImage() {
    final Runnable r = new Runnable() {
      public void run() {
        if (getWidth() > 0 && getHeight() > 0) {
          BufferedImage bi =
              new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
          Graphics2D ig = (Graphics2D) bi.getGraphics();
          constructPlot(ig);
          setImage(bi);
        }
      }
    };

    if (SwingUtilities.isEventDispatchThread()) {
      SwingWorker worker = new SwingWorker() {
        public Object construct() {
          r.run();
          return null;
        }

        public void finished() {
          repaint();
        }
      };
      worker.start();
    } else {
      r.run();
    }
  }

  /**
   * Does NOT call repaint for efficiency purposes, that is left to the
   * container.
   */
  private void processSettings() {
    if (data == null || data.getData() == null || data.getData().rows() == 0) {
      return;
    }

    createImage();
  }

  /**
   * Paints the component on the specified graphics context.
   * 
   * @param g
   *          the graphics context
   */
  public void paint(Graphics g) {
    if (!(g instanceof Graphics2D)) {
      throw new RuntimeException("Fatal error in RsamViewPanel.paint()");
    }

    Graphics2D g2 = (Graphics2D) g;
    Dimension dim = this.getSize();

    if (data == null) {
      g2.setColor(BACKGROUND_COLOR);
      g2.fillRect(0, 0, dim.width, dim.height);
      g2.setColor(Color.black);
      if (working) {
        g2.drawString("Retrieving data...", dim.width / 2 - 50, dim.height / 2);
      } else {
        String error = "No RSAM data.";
        if (channel != null) {
          error = "No RSAM data for " + channel + ".";
        }
        int w = g2.getFontMetrics().stringWidth(error);
        g2.drawString(error, dim.width / 2 - w / 2, dim.height / 2);
      }
    } else {
      BufferedImage bi = getImage();
      if (bi != null) {
        g2.drawImage(bi, 0, 0, null);
      }

    }
  }



  /**
   * Constructs the plot on the specified graphics context.
   * 
   * @param g2
   *          the graphics context
   */
  private synchronized void constructPlot(Graphics2D g2) {
    Dimension dim = this.getSize();

    Plot plot = new Plot();
    plot.setBackgroundColor(BACKGROUND_COLOR);
    plot.setSize(dim);

    switch (settings.getType()) {
      case VALUES:
        plotValues(plot, data);
        break;
      case COUNTS:
        plotCounts(plot, data);
        break;
      default:
        break;
    }

    try {
      plot.render(g2);
    } catch (PlotException e) {
      e.printStackTrace();
    }
  }

  /**
   * Plots RSAM values.
   * 
   * @param data
   *          the RSAM values to plot
   */
  private void plotValues(Plot plot, RSAMData data) {
    if (data == null || data.getData() == null || data.getData().rows() == 0) {
      return;
    }
   
    GenericDataMatrix gdm = new GenericDataMatrix(data.getData().copy());

    gdm.despike(1, settings.valuesPeriodS);

    if (settings.detrend) {
      gdm.detrend(1);
    }

    if (settings.despike) {
      gdm.despike(1, settings.despikePeriod);
    }

    if (settings.runningMedian) {
      gdm.set2median(1, settings.runningMedianPeriodS);
    }

    if (settings.runningMean) {
      gdm.set2mean(1, settings.runningMeanPeriodS);
    }

    MatrixRenderer mr = new MatrixRenderer(gdm.getData(), false);
    double max;
    double min;
    if (settings.getAutoScale()) {
      max = gdm.max(1) + gdm.max(1) * .1;
      min = gdm.min(1) - gdm.max(1) * .1;
    } else {
      max = settings.scaleMax;
      min = settings.scaleMin;
    }

    mr.setExtents(startTime, endTime, min, max);
    mr.setLocation(X_OFFSET, Y_OFFSET, this.getWidth() - X_OFFSET - RIGHT_WIDTH,
        this.getHeight() - Y_OFFSET - BOTTOM_HEIGHT);
    mr.createDefaultAxis();
    mr.setXAxisToTime(8, true, true);

    mr.getAxis().setLeftLabelAsText("RSAM Values", -55, Color.BLACK);

    mr.createDefaultLineRenderers(Color.blue);
    plot.addRenderer(mr);

/*    if (settings.filterOn) {
      plot.addRenderer(getFilterLabel(getWidth() - RIGHT_WIDTH, getHeight() - BOTTOM_HEIGHT,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));
    }*/
  }

  /**
   * Plots RSAM counts.
   * 
   * @param data the RSAM values to plot
   */
  private void plotCounts(Plot plot, RSAMData data) {
    if (data == null || data.getData() == null || data.getData().rows() == 0) {
      return;
    }

    // get the relevant information for this channel
    data.countEvents(settings.eventThreshold, settings.eventRatio, settings.eventMaxLengthS);

    // setup the histogram renderer with this data
    HistogramRenderer hr = new HistogramRenderer(data.getCountsHistogram(settings.binSize));
    hr.setLocation(X_OFFSET, Y_OFFSET, this.getWidth() - X_OFFSET - RIGHT_WIDTH,
        this.getHeight() - Y_OFFSET - BOTTOM_HEIGHT);
    hr.setDefaultExtents();
    hr.setMinX(startTime);
    hr.setMaxX(endTime);

    // x axis decorations
    hr.createDefaultAxis(8, 8, true, true, false, true, true, true);
    hr.setXAxisToTime(8, true, true);

    hr.getAxis().setLeftLabelAsText("Events per " + settings.binSize, -55, Color.BLACK);

    DoubleMatrix2D countsData = data.getCumulativeCounts();
    if (countsData != null && countsData.rows() > 0) {

      double cmin = countsData.get(0, 1);
      double cmax = countsData.get(countsData.rows() - 1, 1);

      MatrixRenderer mr = new MatrixRenderer(countsData, false);
      mr.setAllVisible(true);
      mr.setLocation(X_OFFSET, Y_OFFSET, this.getWidth() - X_OFFSET - RIGHT_WIDTH,
          this.getHeight() - Y_OFFSET - BOTTOM_HEIGHT);
      mr.setExtents(startTime, endTime, cmin, cmax + 1);
      mr.createDefaultLineRenderers(Color.RED);
      ShapeRenderer[] r = mr.getLineRenderers();
      ((ShapeRenderer) r[0]).color = Color.RED;
      ((ShapeRenderer) r[0]).stroke = new BasicStroke(2.0f);

      // create the axis for the right hand side
      AxisRenderer ar = new AxisRenderer(mr);
      ar.createRightTickLabels(SmartTick.autoTick(cmin, cmax, 8, false), null);
      mr.setAxis(ar);

      hr.addRenderer(mr);
      hr.getAxis().setRightLabelAsText("Cumulative Counts");

    }
    plot.addRenderer(hr);
  }


  /**
   * Overload of Component. Always returns the developer-specified size.
   * 
   * @return the size of the component
   */
  public Dimension getPreferredSize() {
    return getSize();
  }

  /**
   * Overload of Component. Always returns the developer-specified size.
   * 
   * @return the size of the component
   */
  public Dimension getMinimumSize() {
    return getSize();
  }

  /**
   * Get filter label.
   * @param x x text location
   * @param y y text location
   * @param horizJustification horizontal justification
   * @param vertJustification vertical justification
   * @return text renderer
   */
/*  public TextRenderer getFilterLabel(int x, int y, int horizJustification, int vertJustification) {
    String ft = "";
    switch (settings.filter.getType()) {
      case BANDPASS:
        ft = "Band pass [" + settings.filter.getCorner1() + "-" + settings.filter.getCorner2()
            + " Hz]";
        break;
      case HIGHPASS:
        ft = "High pass [" + settings.filter.getCorner1() + " Hz]";
        break;
      case LOWPASS:
        ft = "Low pass [" + settings.filter.getCorner1() + " Hz]";
        break;
      default:
        break;
    }
    TextRenderer tr = new TextRenderer(x, y, ft);
    tr.horizJustification = horizJustification;
    tr.vertJustification = vertJustification;
    tr.color = Color.red;
    return tr;
  }*/
}
