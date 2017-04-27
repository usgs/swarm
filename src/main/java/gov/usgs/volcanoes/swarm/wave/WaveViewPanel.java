package gov.usgs.volcanoes.swarm.wave;

import gov.usgs.math.Filter;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.SliceWave;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.decorate.FrameDecorator;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.plot.render.wave.ParticleMotionRenderer;
import gov.usgs.plot.render.wave.SliceWaveRenderer;
import gov.usgs.plot.render.wave.SpectraRenderer;
import gov.usgs.plot.render.wave.SpectrogramRenderer;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.event.PickMenu;
import gov.usgs.volcanoes.swarm.event.PickWavePanel;
import gov.usgs.volcanoes.swarm.time.UiTime;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WaveViewPanel extends JComponent {

  private static final Logger LOGGER = LoggerFactory.getLogger(WaveViewPanel.class);
  public static final long serialVersionUID = -1;
  protected static SwarmConfig swarmConfig;
  /**
   * X pixel location of where the main plot axis should be located on the component.
   */
  protected int xOffset = 60;
  /**
   * Y pixel location of where the main plot axis should be located on the component.
   */
  protected int yOffset = 20;
  /** The amount of padding space on the right side. */
  protected int rightWidth = 20;
  /** The amount of padding space on the bottom. */
  protected int bottomHeight = 20;
  protected FrameDecorator decorator;
  protected SliceWaveRenderer waveRenderer;
  protected SpectrogramRenderer spectrogramRenderer;
  protected SpectraRenderer spectraRenderer;
  protected Wave wave;
  protected double startTime;
  protected double endTime;
  protected WaveViewSettings settings;
  protected double minAmp = 1E300;
  protected double maxAmp = -1E300;
  protected double maxSpectraPower = -1E300;
  protected double maxSpectrogramPower = -1E300;
  protected double[] translation;
  protected boolean timeSeries;
  protected String channel;
  /**
   * The data source to use for zoom drags. This should probably be moved from this class to follow
   * a stricter interpretation of MVC.
   */
  protected SeismicDataSource source;
  /**
   * A flag to indicate whether the plot should display a title. Currently used when the plot is on
   * the clipboard or monitor.
   */
  protected boolean displayTitle;
  protected Color backgroundColor;
  protected Color bottomBorderColor;
  protected StatusTextArea statusText;
  protected boolean allowDragging = true;
  protected boolean dragging;
  protected double j2k1;
  protected double j2k2;
  protected int highlightX1;
  protected int highlightX2;
  protected static Image closeImg;
  protected boolean allowClose;
  protected EventListenerList listeners = new EventListenerList();
  /**
   * A flag that indicates whether data are being loaded for this panel.
   */
  protected boolean working;
  /**
   * The wave is rendered to an image that is only updated when the settings change for repaint
   * efficiency.
   */
  protected BufferedImage image;
  protected double mark1 = Double.NaN;
  protected double mark2 = Double.NaN;
  protected double cursorMark = Double.NaN;
  protected boolean useFilterLabel = true;
  protected Color borderColor;
  protected static final Color DARK_RED = new Color(168, 0, 0);
  protected static final Color DARK_GREEN = new Color(0, 168, 0);

  protected boolean pauseCursorMark;
  protected double time;

  // picker
  private PickMenu pickMenu;

  /**
   * Default constructor.
   */
  public WaveViewPanel() {
    super();
    swarmConfig = SwarmConfig.getInstance();
    pauseCursorMark = false;
    backgroundColor = new Color(0xf7, 0xf7, 0xf7);
    settings = new WaveViewSettings();

    setupMouseHandler();

  }

  /**
   * Constructor.
   * @param s wave settings
   */
  public WaveViewPanel(WaveViewSettings s) {
    this();
    settings = s;
    s.view = this;
  }

  /**
   * Constructs a WaveViewPanel set up the same as a source WaveViewPanel. Used when copying a
   * waveform to the clipboard.
   * 
   * @param p the source WaveViewPanel
   */
  public WaveViewPanel(WaveViewPanel p) {
    this();

    channel = p.channel;
    source = p.source;
    startTime = p.startTime;
    endTime = p.endTime;
    maxSpectraPower = p.maxSpectraPower;
    maxSpectrogramPower = p.maxSpectrogramPower;
    timeSeries = p.timeSeries;
    allowDragging = p.allowDragging;
    wave = p.wave;
    displayTitle = p.displayTitle;
    backgroundColor = p.backgroundColor;
    mark1 = p.mark1;
    mark2 = p.mark2;
    pickMenu = p.pickMenu;

    translation = new double[8];
    if (p.translation != null) {
      System.arraycopy(p.translation, 0, translation, 0, 8);
    }

    settings = new WaveViewSettings(p.settings);
    settings.view = this;

    processSettings();
  }

  /**
   * Set offsets.
   * @param xo x offset
   * @param yo y offset
   * @param rw right width
   * @param bh bottom height
   */
  public void setOffsets(int xo, int yo, int rw, int bh) {
    xOffset = xo;
    yOffset = yo;
    rightWidth = rw;
    bottomHeight = bh;
  }

  public void addListener(WaveViewPanelListener listener) {
    listeners.add(WaveViewPanelListener.class, listener);
  }

  public void removeListener(WaveViewPanelListener listener) {
    listeners.remove(WaveViewPanelListener.class, listener);
  }

  /**
   * Fire zoom on wave.
   * @param e mouse event
   * @param oldStartTime old start time
   * @param oldEndTime old end time
   * @param newStartTime new start time
   * @param newEndTime new end time
   */
  public void fireZoomed(MouseEvent e, double oldStartTime, double oldEndTime, 
                                        double newStartTime, double newEndTime) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == WaveViewPanelListener.class) {
        ((WaveViewPanelListener) ls[i + 1]).waveZoomed(
                  this, oldStartTime, oldEndTime, newStartTime, newEndTime);
      }
    }
  }

  /**
   * Fire time pressed.
   * @param e mouse event
   * @param j2k J2000 time
   */
  public void fireTimePressed(MouseEvent e, double j2k) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == WaveViewPanelListener.class) {
        ((WaveViewPanelListener) ls[i + 1]).waveTimePressed(this, e, j2k);
      }
    }
  }

  /**
   * Fire mouse pressed event.
   * @param e mouse event
   */
  public void fireMousePressed(MouseEvent e) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == WaveViewPanelListener.class) {
        LOGGER.debug("Notifying mouse listeners.");
        ((WaveViewPanelListener) ls[i + 1]).mousePressed(this, e, dragging);
      }
    }
  }

  /**
   * Fire wave close event.
   */
  public void fireClose() {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == WaveViewPanelListener.class) {
        ((WaveViewPanelListener) ls[i + 1]).waveClosed(this);
      }
    }
  }

  public void setAllowClose(boolean b) {
    allowClose = b;
  }

  /**
   * Process right mouse press.
   * @param e mouse event
   */
  protected void processRightMousePress(MouseEvent e) {
    if (settings.pickEnabled && (settings.viewType.equals(ViewType.WAVE) 
        || settings.viewType.equals(ViewType.SPECTROGRAM))) {
      double[] t = getTranslation();
      if (t != null) {
        double j2k = e.getX() * t[0] + t[1];
        if (j2k >= startTime && j2k <= endTime) {
          if (pickMenu == null) {
            pickMenu = new PickMenu(this);
          }
          pickMenu.setJ2k(j2k);
          pickMenu.show(this, e.getX(), e.getY());
        }
      }
    } else {
      settings.cycleType();
    }
  }

  protected void processRightMouseRelease(MouseEvent e) {
    
  }

  protected void setupMouseHandler() {
    Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
    this.setCursor(crosshair);
    this.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        UiTime.touchTime();

        if (SwingUtilities.isRightMouseButton(e)) {
          processRightMousePress(e);
        }

        double[] t = getTranslation();
        if (t != null) {
          int x = e.getX();
          double j2k = x * t[0] + t[1];
          if (timeSeries) {
            System.out.printf("%s UTC: %s j2k: %.3f ew: %d\n", channel, J2kSec.toDateString(j2k),
                j2k, J2kSec.asEpoch(j2k));
          }

          if (timeSeries && j2k >= startTime && j2k <= endTime) {
            fireTimePressed(e, j2k);
          }

          if (timeSeries && allowDragging && SwingUtilities.isLeftMouseButton(e)) {
            Dimension size = getSize();
            int y = e.getY();
            if (t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset
                && x < size.width - rightWidth) {
              j2k1 = j2k2 = j2k;
              if (e.isControlDown()) {
                System.out.println(channel + ": " + J2kSec.toDateString(j2k1));
              } else if (!e.isShiftDown()) {
                highlightX1 = highlightX2 = x;
                dragging = true;
              }
            }
          }
        }

        fireMousePressed(e);
      }

      public void mouseReleased(MouseEvent e) {
        UiTime.touchTime();
        if (SwingUtilities.isLeftMouseButton(e) && dragging) {
          dragging = false;
          if (j2k1 != j2k2 && source != null) {
            double st = Math.min(j2k1, j2k2);
            double et = Math.max(j2k1, j2k2);
            zoom(st, et);
            fireZoomed(e, getStartTime(), getEndTime(), st, et);
          }
          repaint();
        }

        if (SwingUtilities.isRightMouseButton(e)) {
          processRightMouseRelease(e);
        }
        int mx = e.getX();
        int my = e.getY();
        if (allowClose && SwingUtilities.isLeftMouseButton(e) && mx > getWidth() - 17
            && mx < getWidth() - 3 && my > 2 && my < 17) {
          fireClose();
        }
      }

      public void mouseExited(MouseEvent e) {
        WaveViewTime.fireTimeChanged(Double.NaN);
        pauseCursorMark = false;
        dragging = false;
        repaint();
      }
    });

    this.addMouseMotionListener(new MouseMotionListener() {
      public void mouseMoved(MouseEvent e) {
        UiTime.touchTime();
        processMousePosition(e.getX(), e.getY());
      }

      public void mouseDragged(MouseEvent e) {
        UiTime.touchTime();
        /*
         * // This used to be the launcher for the microview. // It was removed because it wasn't
         * very useful, but this // stub is left here in case something like it ever gets // put in
         * if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown() && settings.type !=
         * WaveViewSettings.SPECTRA) { Dimension size = getSize(); double[] t = getTranslation();
         * int x = e.getX(); int y = e.getY(); if (t != null && y > Y_OFFSET && y < (size.height -
         * BOTTOM_HEIGHT) && x > X_OFFSET && x < size.width - RIGHT_WIDTH) { double j2k = x * t[0] +
         * t[1]; createMicroView(j2k); } }
         */

        processMousePosition(e.getX(), e.getY());
        if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown() && dragging) {
          double[] t = getTranslation();
          int x = e.getX();
          int y = e.getY();
          Dimension size = getSize();
          if (t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset
              && x < size.width - rightWidth) {
            j2k2 = x * t[0] + t[1];
            highlightX2 = x;
            repaint();
          }
        }
      }
    });
  }

  /**
   * Wave zoom.
   * @param st start time
   * @param et end time
   */
  public void zoom(final double st, final double et) {
    final SwingWorker worker = new SwingWorker() {
      public Object construct() {
        Wave sw = null;
        if (source instanceof CachedDataSource) {
          sw = ((CachedDataSource) source).getBestWave(channel, st, et);
        } else {
          sw = source.getWave(channel, st, et);
        }
        setWave(sw, st, et);
        return null;
      }

      public void finished() {

        repaint();
      }
    };
    worker.start();
  }

  /**
   * Set the working flag. This flag indicates whether data are being loaded for this panel.
   * 
   * @param b the working flag state
   */
  public void setWorking(boolean b) {
    working = b;
  }

  /**
   * Set the allow dragging flag. This flag enables zoom dragging. Currently only allowed on the
   * clipboard, but could be implemented within the helicorder view.
   * 
   * @param b the allow dragging flag state
   */
  public void setAllowDragging(boolean b) {
    allowDragging = b;
  }

  public void setStatusText(StatusTextArea text) {
    statusText = text;
  }

  public int getXOffset() {
    return xOffset;
  }

  public int getYOffset() {
    return yOffset;
  }

  public WaveViewSettings getSettings() {
    return settings;
  }

  public double getStartTime() {
    return startTime;
  }

  public void setStartTime(double startTime) {
    this.startTime = startTime;
  }

  public double getEndTime() {
    return endTime;
  }

  public void setEndTime(double endTime) {
    this.endTime = endTime;
  }

  public Wave getWave() {
    return wave;
  }

  public WaveViewSettings getWaveViewSettings() {
    return settings;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String c) {
    channel = c;
  }

  public void setSettings(WaveViewSettings s) {
    settings = s;
    processSettings();
  }

  public SeismicDataSource getDataSource() {
    return source;
  }

  public void setDataSource(SeismicDataSource s) {
    source = s;
  }

  public void setFrameDecorator(FrameDecorator fd) {
    decorator = fd;
  }

  public void setDisplayTitle(boolean b) {
    displayTitle = b;
  }

  public void settingsChanged() {
    processSettings();
  }

  public boolean isTimeSeries() {
    return timeSeries;
  }

  /**
   * Gets the translation info for this panel. The translation info is used to convert from pixel
   * coordinates on the panel into time or data coordinates.
   * 
   * @return the transformation information
   */
  public double[] getTranslation() {
    return translation;
  }

  /**
   * Set the background color of the panel.
   * 
   * @param c the background color
   */
  public void setBackgroundColor(Color c) {
    backgroundColor = c;
  }

  public void setBottomBorderColor(Color c) {
    bottomBorderColor = c;
  }

  public void setBorderColor(Color c) {
    borderColor = c;
  }

  /**
   * Processes the mouse position variables when the cursor is over the panel. Currently, the only
   * thing this does is set the status bar text.
   * 
   * @param x the mouse x position
   * @param y the mouse y position
   */
  public boolean processMousePosition(int x, int y) {
    StringBuffer status = new StringBuffer();
    String unit = null;

    Dimension size = getSize();
    double[] t = getTranslation();
    time = Double.NaN;

    if (wave != null && t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset
        && x < size.width - rightWidth) {
      time = x * t[0] + t[1];
      double yi = y * -t[2] + t[3];

      status.append(StatusTextArea.getWaveInfo(wave));

      if (timeSeries) {

        if (swarmConfig.durationEnabled && !Double.isNaN(mark1) && !Double.isNaN(mark2)) {
          String durationString = StatusTextArea.getDuration(mark1, mark2);
          status.append(",");
          status.append(durationString);
        }
        
        String timeString = StatusTextArea.getTimeString(time, swarmConfig.getTimeZone(channel));
        if (timeString != null) {
          status.append("\n");
          status.append(timeString);
        }

        double offset = 0;
        double multiplier = 1;

        if (settings.viewType == ViewType.SPECTROGRAM) {
          unit = "Frequency (Hz)";
        } else {
          Metadata md = swarmConfig.getMetadata(channel);
          if (md != null) {
            offset = md.getOffset();
            multiplier = md.getMultiplier();
            unit = md.getUnit();
          }

          if (unit == null) {
            unit = "Counts";
          }
        }

        status.append(String.format(", %s: %.3f", unit, multiplier * yi + offset));
      } else {
        double xi = time;
        if (settings.viewType == ViewType.SPECTRA && settings.logFreq) {
          xi = Math.pow(10.0, xi);
        }
        if (settings.viewType == ViewType.SPECTRA && settings.logPower) {
          yi = Math.pow(10.0, yi);
        }
        status.append(String.format("\nFrequency (Hz): %.6f, Power: %.3f", xi, yi));
      }

      if (settings.pickEnabled && pickMenu != null) {
        String pickStatus = "";
        Pick p = pickMenu.getP();
        Pick s = pickMenu.getS();
        if (p != null && s != null) {
          pickStatus = StatusTextArea.getSpString(p.getTime(), s.getTime());
        }
        Pick c1 = pickMenu.getCoda1();
        Pick c2 = pickMenu.getCoda2();
        if (c1 != null && c2 != null) {
          if (!pickStatus.equals("")) {
            pickStatus += ", ";
          }
          pickStatus += StatusTextArea.getCodaDuration(c1.getTime(), c2.getTime());
        }
        if (!pickStatus.equals("")) {
          status.append("\n");
          status.append(pickStatus);
        }
      }
    } 
    
    if (!pauseCursorMark) {
      WaveViewTime.fireTimeChanged(time);
    }

    if (status != null && statusText != null) {
      final String st = status.toString();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          statusText.setText(st);
        }
      });
    }

    return !status.equals(" ");
  }

  /**
   * Set wave.
   * @param sw wave to set to
   * @param st start time
   * @param et end time
   */
  public void setWave(Wave sw, double st, double et) {
    wave = sw;
    startTime = st;
    endTime = et;
    processSettings();
  }

  /**
   * Reset auto scale memory settings.
   */
  public void resetAutoScaleMemory() {
    minAmp = 1E300;
    maxAmp = -1E300;
    maxSpectraPower = -1E300;
    maxSpectrogramPower = -1E300;
    settings.autoScaleAmp = true;
    settings.autoScalePower = true;
    processSettings();
  }

  /**
   * Adjust scale by percent.
   * @param pct percent to scale to.
   */
  public void adjustScale(double pct) {
    double maxa = settings.autoScaleAmp ? maxAmp : settings.maxAmp;
    double mina = settings.autoScaleAmp ? minAmp : settings.minAmp;
    settings.autoScaleAmp = false;
    double range = maxa - mina;
    double center = range / 2 + mina;
    double newRange = range * pct;
    settings.minAmp = center - newRange / 2;
    settings.maxAmp = center + newRange / 2;

    if (settings.viewType == ViewType.SPECTROGRAM) {
      double maxf = settings.maxFreq * pct;
      System.out.printf("WaveViewPanel(804): maxf = %f\n", maxf);
      settings.maxFreq = (maxf > wave.getSamplingRate() / 2) ? wave.getSamplingRate() / 2 : maxf;
      System.out.printf("WaveViewPanel(806): settings.maxFreq = %f\n", settings.maxFreq);

    }

    processSettings();
  }

  private synchronized void setImage(BufferedImage bi) {
    image = bi;
  }

  private synchronized BufferedImage getImage() {
    return image;
  }

  /**
   * Create image.
   */
  public void createImage() {
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
   * Does NOT call repaint for efficiency purposes, that is left to the container.
   */
  protected void processSettings() {
    if (wave == null || wave.buffer == null || wave.buffer.length == 0) {
      return;
    }

    if (settings.maxFreq > wave.getNyquist()) {
      settings.maxFreq = wave.getNyquist();
    }

    switch (settings.viewType) {
      case SPECTRA:
      case PARTICLE_MOTION:
        timeSeries = false;
        break;
      default:
        timeSeries = true;
    }

    createImage();
  }

  private void filter(Wave w) {
    double mean = w.mean();

    double[] buf = new double[w.buffer.length + (int) (w.buffer.length * 0.5)];
    Arrays.fill(buf, mean);
    int trueStart = (int) (w.buffer.length * 0.25);
    for (int i = 0; i < w.buffer.length; i++) {
      if (w.buffer[i] != Wave.NO_DATA) {
        buf[i + trueStart] = w.buffer[i];
      }
    }

    settings.filter.setSamplingRate(w.getSamplingRate());
    settings.filter.create();
    Filter.filter(buf, settings.filter.getSize(), settings.filter.getXCoeffs(),
        settings.filter.getYCoeffs(), settings.filter.getGain(), 0, 0);
    if (settings.zeroPhaseShift) {
      double[] buf2 = new double[buf.length];
      for (int i = 0, j = buf.length - 1; i < buf.length; i++, j--) {
        buf2[j] = buf[i];
      }

      Filter.filter(buf2, settings.filter.getSize(), settings.filter.getXCoeffs(),
          settings.filter.getYCoeffs(), settings.filter.getGain(), 0, 0);

      for (int i = 0, j = buf2.length - 1 - trueStart; i < w.buffer.length; i++, j--) {
        w.buffer[i] = (int) Math.round(buf2[j]);
      }
    } else {
      for (int i = 0; i < w.buffer.length; i++) {
        w.buffer[i] = (int) Math.round(buf[i + trueStart]);
      }
    }
    w.invalidateStatistics();
  }

  /**
   * Annotate image with pick marks.
   * @param g2 graphics
   */
  protected void annotateImage(Graphics2D g2) {
    if (!Double.isNaN(mark1)) {
      paintMark(g2, mark1);
    }
    if (!Double.isNaN(mark2)) {
      paintMark(g2, mark2);
    }
    if (settings.pickEnabled && pickMenu != null) {
      double[] t = getTranslation();
      if (t == null) {
        return;
      }
      if (!pickMenu.isHidePhases()) {
        drawPick(pickMenu.getP(), g2);
        drawPick(pickMenu.getS(), g2);
      }
      if (!pickMenu.isHideCoda()) {
        drawPick(pickMenu.getCoda1(), g2);
        drawPick(pickMenu.getCoda2(), g2);
      }
    }
  }

  /**
   * Paints the component on the specified graphics context.
   * 
   * @param g the graphics context
   */
  public void paint(Graphics g) {
    if (getVisibleRect().isEmpty()) {
      return;
    }
    Graphics2D g2 = (Graphics2D) g;
    Dimension dim = this.getSize();
    if (wave == null) {
      g2.setColor(backgroundColor);
      g2.fillRect(0, 0, dim.width, dim.height);
      g2.setColor(Color.black);
      if (working) {
        g2.drawString("Retrieving data...", dim.width / 2 - 50, dim.height / 2);
      } else {
        String error = "No wave data.";
        if (channel != null) {
          error = "No wave data for " + channel + ".";
        }
        int w = g2.getFontMetrics().stringWidth(error);
        g2.drawString(error, dim.width / 2 - w / 2, dim.height / 2);
      }
    } else {
      BufferedImage bi = getImage();
      if (bi != null) {
        g2.drawImage(bi, 0, 0, null);
      }

      if (dragging) {
        paintDragBox(g2);
      }

      annotateImage(g2);


      if (!Double.isNaN(cursorMark)) {
        paintCursor(g2);
      }
    }

    if (allowClose) {
      if (closeImg == null) {
        closeImg = Icons.close_view.getImage();
      }

      g2.drawImage(closeImg, dim.width - 17, 3, null);
    }
    if (bottomBorderColor != null) {
      g2.setColor(bottomBorderColor);
      g2.drawLine(0, dim.height - 1, dim.width, dim.height - 1);
    }
    if (borderColor != null) {
      g2.setColor(borderColor);
      g2.drawRect(0, 0, dim.width - 1, dim.height - 2);
    }
  }

  /**
   * Draw duration markers.
   * @param g2 graphics
   * @param j2k time in j2k
   */
  private void paintMark(Graphics2D g2, double j2k) {
    if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime) {
      return;
    }

    double[] t = getTranslation();
    if (t == null) {
      return;
    }

    double x = (j2k - t[1]) / t[0];
    g2.setColor(DARK_GREEN);
    g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

    GeneralPath gp = new GeneralPath();
    gp.moveTo((float) x, yOffset);
    gp.lineTo((float) x - 5, yOffset - 7);
    gp.lineTo((float) x + 5, yOffset - 7);
    gp.closePath();
    g2.setPaint(Color.GREEN);
    g2.fill(gp);
    g2.setColor(DARK_GREEN);
    g2.draw(gp);
  }
  
  /**
   * Draw pick marks.
   * @param pick pick to draw
   * @param g2 graphics
   */
  private void drawPick(Pick pick, Graphics2D g2) {
    if (pick == null) {
      return;
    }
    String tag = pick.getTag();
    long time = pick.getTime();
    double[] t = getTranslation();
    double j2k = J2kSec.fromEpoch(time);
    double x = 2 + (j2k - t[1]) / t[0];
    g2.setColor(DARK_GREEN);
    g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));
    FontMetrics fm = g2.getFontMetrics();
    int width = fm.stringWidth(tag);
    int height = fm.getAscent();

    int offset = 2;
    int lw = width + 2 * offset;

    if (tag.indexOf('P') != -1) {
      g2.setColor(PickWavePanel.P_BACKGROUND);
    } else if (tag.indexOf('S') != -1) {
      g2.setColor(PickWavePanel.S_BACKGROUND);
    } else if (tag.indexOf('C') != -1) {
      g2.setColor(Color.YELLOW);
    } else {
      g2.setColor(Color.GRAY);      
    }

    g2.fillRect((int) x, 3, lw, height + 2 * offset);
    g2.setColor(Color.BLACK);
    g2.drawRect((int) x, 3, lw, height + 2 * offset);
    
    g2.drawString(tag, (int) x + offset, 3 + (fm.getAscent() + offset));
  }
  
  public void setUseFilterLabel(boolean b) {
    useFilterLabel = b;
  }

  public TextRenderer getFilterLabel() {
    return getFilterLabel(xOffset + 5, 148, TextRenderer.NONE, TextRenderer.NONE);
  }

  /**
   * Get filter label.
   * @param x x text location
   * @param y y text location
   * @param horizJustification horizontal justification
   * @param vertJustification vertical justification
   * @return text renderer
   */
  public TextRenderer getFilterLabel(int x, int y, int horizJustification, int vertJustification) {
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
  }

  /**
   * Constructs the plot on the specified graphics context.
   * 
   * @param g2 the graphics context
   */
  private synchronized void constructPlot(Graphics2D g2) {
    Dimension dim = this.getSize();
    
    Plot plot = new Plot();
    plot.setBackgroundColor(backgroundColor);
    plot.setSize(dim);
    Wave renderWave = wave;
    
    if (settings.filterOn) {
      renderWave = new Wave(wave);
      filter(renderWave);
    }
    switch (settings.viewType) {
      case WAVE:
        plotWave(plot, renderWave);
        break;
      case SPECTRA:
        plotSpectra(plot, renderWave);
        break;
      case SPECTROGRAM:
        plotSpectrogram(plot, renderWave);
        break;
      case PARTICLE_MOTION:
        plotParticleMotion(plot, renderWave);
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
   * Plots a wave.
   * 
   * @param renderWave the wave to plot
   */
  protected void plotWave(Plot plot, Wave renderWave) {
    if (renderWave == null || renderWave.numSamples() == 0) {
      return;
    }

    SliceWave wv = new SliceWave(renderWave);
    wv.setSlice(startTime, endTime);

    double offset = 0;
    double multiplier = 1;
    Metadata md = swarmConfig.getMetadata(channel);

    if (settings.useUnits && md != null) {
      offset = md.getOffset();
      multiplier = md.getMultiplier();
    }

    double bias = 0;
    if (settings.removeBias) {
      bias = wv.mean();
    }
    
    double minY = (settings.minAmp - offset) / multiplier;
    double maxY = (settings.maxAmp - offset) / multiplier;

    if (settings.autoScaleAmp) {
      double[] dr = new double[] {wv.min(), wv.max()};
      if (settings.autoScaleAmpMemory) {
        minY = Math.min(minAmp, dr[0] - bias);
        maxY = Math.max(maxAmp, dr[1] - bias);
        minAmp = Math.min(minY, minAmp);
        maxAmp = Math.max(maxY, maxAmp);
      } else {
        minY = dr[0] - bias;
        maxY = dr[1] - bias;
      }
    }

    if (waveRenderer == null) {
      waveRenderer = new SliceWaveRenderer();
    }

    if (decorator != null) {
      waveRenderer.setFrameDecorator(decorator);
    }

    if (settings.useUnits && md != null && md.getUnit() != null) {
      waveRenderer.setYLabelText(md.getUnit());
    } else {
      waveRenderer.setYLabelText("Counts");
    }

    waveRenderer.setYAxisCoefficients(multiplier, offset);
    waveRenderer.setLocation(xOffset, yOffset, this.getWidth() - xOffset - rightWidth,
        this.getHeight() - yOffset - bottomHeight);
    waveRenderer.setYLimits(minY, maxY);
    waveRenderer.setViewTimes(startTime, endTime, "");
    waveRenderer.setWave(wv);
    waveRenderer.setRemoveBias(settings.removeBias);
    if (channel != null && displayTitle) {
      waveRenderer.setTitle(channel);
    }

    waveRenderer.update();
    plot.addRenderer(waveRenderer);
    if (useFilterLabel && settings.filterOn) {
      plot.addRenderer(getFilterLabel(getWidth() - rightWidth, getHeight() - bottomHeight,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));
    }
    translation = waveRenderer.getDefaultTranslation();
  }

  /**
   * Plots frequency spectra.
   * 
   * @param renderWave the wave to plot
   */
  private void plotSpectra(Plot plot, Wave renderWave) {
    if (renderWave == null || renderWave.numSamples() == 0) {
      return;
    }

    SliceWave wv = new SliceWave(renderWave);
    wv.setSlice(startTime, endTime);

    if (spectraRenderer == null) {
      spectraRenderer = new SpectraRenderer();
    }

    if (decorator != null) {
      spectraRenderer.setFrameDecorator(decorator);
    }

    spectraRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset,
        this.getHeight() - bottomHeight - yOffset);
    spectraRenderer.setWave(wv);

    spectraRenderer.setAutoScale(settings.autoScalePower);
    spectraRenderer.setLogPower(settings.logPower);
    spectraRenderer.setLogFreq(settings.logFreq);
    spectraRenderer.setMaxFreq(settings.maxFreq);
    spectraRenderer.setMinFreq(settings.minFreq);
    spectraRenderer.setYUnitText("Power");
    if (channel != null && displayTitle) {
      spectraRenderer.setTitle(channel);
    }

    spectraRenderer.update();
    if (useFilterLabel && settings.filterOn) {
      plot.addRenderer(getFilterLabel(getWidth() - rightWidth, getHeight() - bottomHeight,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));
    }

    translation = spectraRenderer.getDefaultTranslation();
    plot.addRenderer(spectraRenderer);
  }

  /**
   * Plots a spectrogram. TODO: Fix logPower.
   * 
   * @param renderWave the wave to plot
   */
  private void plotSpectrogram(Plot plot, Wave renderWave) {
    if (renderWave == null || renderWave.numSamples() == 0) {
      return;
    }

    SliceWave wv = new SliceWave(renderWave);
    wv.setSlice(startTime, endTime);

    if (spectrogramRenderer == null) {
      spectrogramRenderer = new SpectrogramRenderer();
    }

    if (decorator != null) {
      spectrogramRenderer.setFrameDecorator(decorator);
    }

    spectrogramRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset,
        this.getHeight() - bottomHeight - yOffset);
    spectrogramRenderer.setWave(wv);

    spectrogramRenderer.setViewStartTime(startTime);
    spectrogramRenderer.setViewEndTime(endTime);
    spectrogramRenderer.setAutoScale(settings.autoScalePower);
    spectrogramRenderer.setLogPower(settings.logPower);

    spectrogramRenderer.setOverlap(settings.spectrogramOverlap);
    spectrogramRenderer.setMaxFreq(settings.maxFreq);
    spectrogramRenderer.setMinFreq(settings.minFreq);

    spectrogramRenderer.setMaxPower(settings.maxPower);
    spectrogramRenderer.setMinPower(settings.minPower);

    spectrogramRenderer.setBinSize((int) Math.pow(2,
        Math.ceil(Math.log(settings.binSize * wave.getSamplingRate()) / Math.log(2))));

    if (channel != null && displayTitle) {
      spectrogramRenderer.setTitle(channel);
    }

    spectrogramRenderer.setYUnitText("Frequency (Hz)");

    spectrogramRenderer.setNfft(settings.nfft);

    double[] power = spectrogramRenderer.update();

    settings.minPower = power[0];
    settings.maxPower = power[1];

    plot.addRenderer(spectrogramRenderer);
    if (useFilterLabel && settings.filterOn) {
      plot.addRenderer(getFilterLabel(getWidth() - rightWidth, getHeight() - bottomHeight,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));
    }
    translation = spectrogramRenderer.getDefaultTranslation();
  }
  
  /**
   * Plot particle motion using detrended data.   
   */
  private void plotParticleMotion(Plot plot, Wave wave) {
    Metadata md = swarmConfig.getMetadata(channel);
    if (md == null) {
      String message = "Unable to plot due to invalid metadata.";
      TextRenderer renderer = new TextRenderer(30, 30, message);
      plot.addRenderer(renderer);
      return;
    }
    String s = md.getSCNL().station;
    String c = md.getSCNL().channel;
    String n = md.getSCNL().network;
    String l = md.getSCNL().location == null ? "" : md.getSCNL().location;
    
    if (s == null || c == null || n == null) {
      String message = "Unable to plot due to missing SCNL information.";
      TextRenderer renderer = new TextRenderer(30, 30, message);
      plot.addRenderer(renderer);
      return;
    }

    SliceWave swave = new SliceWave(wave);
    swave.setSlice(startTime, endTime);
    HashMap<String, double[]> data = new HashMap<String, double[]>();
    HashMap<String, String> stations = new HashMap<String, String>();
    String component = c.substring(2);
    data.put(component, swave.getSignal());
    stations.put(component, channel);
    for (String direction : new String[] {"Z", "N", "E"}) {
      if (!component.equals(direction)) {
        String newChannel = c.replaceFirst(".$", direction);
        String newStation = s + " " + newChannel + " " + n + " " + l;
        stations.put(direction,  newStation);
        Wave w = source.getWave(newStation, startTime, endTime);
        if (w != null) {
          if (settings.filterOn) {
            filter(w);
          }
          SliceWave sw = new SliceWave(w);
          sw.setSlice(startTime, endTime);
          data.put(direction, sw.getSignal());
        } else {
          data.put(direction, new double[0]);
        }
      }
    }
    
    ParticleMotionRenderer particleMotionRenderer =
        new ParticleMotionRenderer(data.get("E"), data.get("N"), data.get("Z"), 
            stations.get("E"), stations.get("N"), stations.get("Z"));
    particleMotionRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset,
        this.getHeight());

    if (channel != null && displayTitle) {
      String title = s + " " + c.replaceFirst(".$", "*") + " " + n + " " + l;
      particleMotionRenderer.setTitle(title);
    }
    plot.addRenderer(particleMotionRenderer);
    if (useFilterLabel && settings.filterOn) {
      plot.addRenderer(getFilterLabel(getWidth() - rightWidth, getHeight() - bottomHeight,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));
    }
    translation = null;
  }
  
  /**
   * Paints the zoom drag box.
   * 
   * @param g2 the graphics context
   */
  private void paintDragBox(Graphics2D g2) {
    int x1 = Math.min(highlightX1, highlightX2);
    int x2 = Math.max(highlightX1, highlightX2);
    int width = x2 - x1 + 1;
    Paint pnt = g2.getPaint();
    g2.setPaint(new Color(255, 255, 0, 128));
    g2.fillRect(x1, yOffset + 1, width, getSize().height - bottomHeight - yOffset);
    g2.setPaint(pnt);
  }

  public void setCursorMark(double j2k) {
    cursorMark = j2k;
    repaint();
  }

  private void paintCursor(Graphics2D g2) {
    if (Double.isNaN(cursorMark) || cursorMark < startTime || cursorMark > endTime) {
      return;
    }

    double[] t = getTranslation();
    if (t == null) {
      return;
    }
    double x = (cursorMark - t[1]) / t[0];
    g2.setColor(DARK_RED);
    g2.draw(new Line2D.Double(x, yOffset + 1, x, getHeight() - bottomHeight - 1));
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
   * Overload of Component. Always returns the developer-specified size.
   * 
   * @return the size of the component
   */
  public Dimension getMaximumSize() {
    return getSize();
  }

  public void setMarks(double m1, double m2) {
    mark1 = m1;
    mark2 = m2;
  }
  
  /**
   * Determines if the other wave view panel represents a wave from the
   * same station. This means it has the same station, network, and location.
   * @param wvp another wave view panel
   * @return true if same station
   */
  public boolean isSameStation(WaveViewPanel wvp) {
    Metadata thisMd = swarmConfig.getMetadata(channel);
    if (thisMd == null) {
      return false;
    }
    String thatChannel = wvp.channel;
    Metadata thatMd = swarmConfig.getMetadata(thatChannel);
    if (thatMd == null) {
      return false;
    }
    
    // get this station info
    String thisS = thisMd.getSCNL().station;
    String thisN = thisMd.getSCNL().network;
    String thisL = thisMd.getSCNL().location == null ? "" : thisMd.getSCNL().location;

    // get that station info
    String thatS = thatMd.getSCNL().station;
    String thatN = thatMd.getSCNL().network;
    String thatL = thatMd.getSCNL().location == null ? "" : thatMd.getSCNL().location;

    // compare
    if (!thisS.equals(thatS)) {
      return false;
    }
    if (!thisN.equals(thatN)) {
      return false;
    }
    if (!thisL.equals(thatL)) {
      return false;
    }
    return true;
  }

  /**
   * Get pick menu.
   * @return pick menu
   */
  public PickMenu getPickMenu() {
    if (pickMenu == null) {
      pickMenu = new PickMenu(this);
    }
    return pickMenu;
  }

}
