package gov.usgs.volcanoes.swarm.wave;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
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
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import gov.usgs.math.Filter;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.SliceWave;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.decorate.FrameDecorator;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.plot.render.wave.SliceWaveRenderer;
import gov.usgs.plot.render.wave.SpectraRenderer;
import gov.usgs.plot.render.wave.SpectrogramRenderer;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.time.UiTime;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings.ViewType;

/**
 * A component that renders a wave in either a standard wave view, a frequency
 * spectra, or spectrogram. Relies heavily on the Valve plotting package.
 * 
 * TODO: move filter method
 * 
 * 
 * @author Dan Cervelli
 */
public class WaveViewPanel extends JComponent {
  public static final long serialVersionUID = -1;

  private static SwarmConfig swarmConfig;

  /**
   * X pixel location of where the main plot axis should be located on the
   * component.
   */
  private int xOffset = 60;

  /**
   * Y pixel location of where the main plot axis should be located on the
   * component.
   */
  private int yOffset = 20;

  /** The amount of padding space on the right side. */
  private int rightWidth = 20;

  /** The amount of padding space on the bottom. */
  private int bottomHeight = 20;

  private FrameDecorator decorator;
  private SliceWaveRenderer waveRenderer;
  private SpectrogramRenderer spectrogramRenderer;
  private SpectraRenderer spectraRenderer;

  private Wave wave;

  private double startTime;
  private double endTime;
  private WaveViewSettings settings;
  private int bias;

  private double minAmp = 1E300;
  private double maxAmp = -1E300;
  private double maxSpectraPower = -1E300;
  private double maxSpectrogramPower = -1E300;
  private double[] translation;

  private boolean timeSeries;
  private String channel;

  /**
   * The data source to use for zoom drags. This should probably be moved from
   * this class to follow a stricter interpretation of MVC.
   */
  private SeismicDataSource source;

  /**
   * A flag to indicate wheter the plot should display a title. Currently used
   * when the plot is on the clipboard or monitor.
   */
  private boolean displayTitle;

  private Color backgroundColor;
  private Color bottomBorderColor;
  private JLabel statusLabel;

  private boolean allowDragging;
  private boolean dragging;
  private double j2k1;
  private double j2k2;
  private int highlightX1;
  private int highlightX2;

  private static Image closeImg;
  private boolean allowClose;

  private EventListenerList listeners = new EventListenerList();

  /**
   * A flag that indicates whether data are being loaded for this panel.
   */
  private boolean working;

  /**
   * The wave is rendered to an image that is only updated when the settings
   * change for repaint efficiency.
   */
  private BufferedImage image;

  private double mark1 = Double.NaN;
  private double mark2 = Double.NaN;

  private double cursorMark = Double.NaN;

  private boolean useFilterLabel = true;

  private Color borderColor;

  /**
   * Constructs a WaveViewPanel with default settings.
   */
  public WaveViewPanel() {
    this(new WaveViewSettings());
  }

  /**
   * Constructs a WaveViewPanel with specified settings.
   * 
   * @param s
   *          the settings
   */
  public WaveViewPanel(WaveViewSettings s) {
    swarmConfig = SwarmConfig.getInstance();
    settings = s;
    s.view = this;

    backgroundColor = new Color(0xf7, 0xf7, 0xf7);
    setupMouseHandler();
  }

  /**
   * Constructs a WaveViewPanel set up the same as a source WaveViewPanel.
   * Used when copying a waveform to the clipboard.
   * 
   * @param p
   *          the source WaveViewPanel
   */
  public WaveViewPanel(WaveViewPanel p) {
    swarmConfig = SwarmConfig.getInstance();
    channel = p.channel;
    source = p.source;
    startTime = p.startTime;
    endTime = p.endTime;
    bias = p.bias;
    maxSpectraPower = p.maxSpectraPower;
    maxSpectrogramPower = p.maxSpectrogramPower;
    translation = new double[8];
    if (p.translation != null)
      System.arraycopy(p.translation, 0, translation, 0, 8);
    timeSeries = p.timeSeries;
    allowDragging = p.allowDragging;
    settings = new WaveViewSettings(p.settings);
    settings.view = this;
    wave = p.wave;
    displayTitle = p.displayTitle;
    backgroundColor = p.backgroundColor;
    setupMouseHandler();
    processSettings();

  }

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

  public void fireZoomed(MouseEvent e, double oldST, double oldET, double newST, double newET) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2)
      if (ls[i] == WaveViewPanelListener.class)
        ((WaveViewPanelListener) ls[i + 1]).waveZoomed(this, oldST, oldET, newST, newET);
  }

  public void fireTimePressed(MouseEvent e, double j2k) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2)
      if (ls[i] == WaveViewPanelListener.class)
        ((WaveViewPanelListener) ls[i + 1]).waveTimePressed(this, e, j2k);
  }

  public void fireMousePressed(MouseEvent e) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2)
      if (ls[i] == WaveViewPanelListener.class)
        ((WaveViewPanelListener) ls[i + 1]).mousePressed(this, e, dragging);
  }

  public void fireClose() {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2)
      if (ls[i] == WaveViewPanelListener.class)
        ((WaveViewPanelListener) ls[i + 1]).waveClosed(this);
  }

  public void setAllowClose(boolean b) {
    allowClose = b;
  }

  private void setupMouseHandler() {
    Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
    this.setCursor(crosshair);
    this.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        UiTime.touchTime();

        double[] t = getTranslation();
        if (t != null) {
          int x = e.getX();
          double j2k = x * t[0] + t[1];
          if (timeSeries)
            System.out.printf("%s UTC: %s j2k: %.3f ew: %.3f\n", channel, J2kSec.toDateString(j2k),
                j2k, J2kSec.asEpoch(j2k));

          if (SwingUtilities.isRightMouseButton(e)) {
            settings.cycleType();
          }

          if (timeSeries && j2k >= startTime && j2k <= endTime)
            fireTimePressed(e, j2k);

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

        int mx = e.getX();
        int my = e.getY();
        if (allowClose && SwingUtilities.isLeftMouseButton(e)
            && mx > WaveViewPanel.this.getWidth() - 17 && mx < WaveViewPanel.this.getWidth() - 3
            && my > 2 && my < 17) {
          fireClose();
        }
      }

      public void mouseExited(MouseEvent e) {
        WaveViewTime.fireTimeChanged(Double.NaN);
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
         * // This used to be the launcher for the microview. // It was
         * removed because it wasn't very useful, but this // stub is
         * left here in case something like it ever gets // put in if
         * (SwingUtilities.isLeftMouseButton(e) && e.isControlDown() &&
         * settings.type != WaveViewSettings.SPECTRA) { Dimension size =
         * getSize(); double[] t = getTranslation(); int x = e.getX();
         * int y = e.getY(); if (t != null && y > Y_OFFSET && y <
         * (size.height - BOTTOM_HEIGHT) && x > X_OFFSET && x <
         * size.width - RIGHT_WIDTH) { double j2k = x * t[0] + t[1];
         * createMicroView(j2k); } }
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

  public void zoom(final double st, final double et) {
    final SwingWorker worker = new SwingWorker() {
      public Object construct() {
        Wave sw = null;
        if (source instanceof CachedDataSource)
          sw = ((CachedDataSource) source).getBestWave(channel, st, et);
        else
          sw = source.getWave(channel, st, et);
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
   * Set the working flag. This flag indicates whether data are being loaded
   * for this panel.
   * 
   * @param b
   *          the working flag state
   */
  public void setWorking(boolean b) {
    working = b;
  }

  /**
   * Set the allow dragging flag. This flag enables zoom dragging. Currently
   * only allowed on the clipboard, but could be implemented within the
   * helicorder view.
   * 
   * @param b
   *          the allow dragging flag state
   */
  public void setAllowDragging(boolean b) {
    allowDragging = b;
  }

  public void setStatusLabel(JLabel l) {
    statusLabel = l;
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

  public double getEndTime() {
    return endTime;
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
   * Gets the translation info for this panel. The translation info is used to
   * convert from pixel coordinates on the panel into time or data
   * coordinates.
   * 
   * @return the transformation information
   */
  public double[] getTranslation() {
    return translation;
  }

  /**
   * Set the background color of the panel.
   * 
   * @param c
   *          the background color
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
   * Processes the mouse position variables when the cursor is over the panel.
   * Currently, the only thing this does is set the status bar text.
   * 
   * @param x
   *          the mouse x position
   * @param y
   *          the mouse y position
   */
  public boolean processMousePosition(int x, int y) {
    String status = null;
    String unit = null;
    String waveInfo = null;

    Dimension size = getSize();
    double[] t = getTranslation();
    double j2k = Double.NaN;

    if (wave != null && t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset
        && x < size.width - rightWidth) {
      j2k = x * t[0] + t[1];
      double yi = y * -t[2] + t[3];

      int[] dataRange = wave.getDataRange();
      waveInfo = String.format("[%s - %s (UTC), %d samples (%.2f s), %d samples/s, %d, %d]",
          J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, wave.getStartTime()),
          J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, wave.getEndTime()), wave.numSamples(),
          wave.numSamples() / wave.getSamplingRate(), (int) wave.getSamplingRate(), dataRange[0],
          dataRange[1]);

      if (timeSeries) {
        String utc = J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, j2k);
        TimeZone tz = swarmConfig.getTimeZone(channel);
        double tzo = tz.getOffset(J2kSec.asEpochMs(j2k));
        if (tzo != 0) {
          String tza = tz.getDisplayName(tz.inDaylightTime(J2kSec.asDate(j2k)), TimeZone.SHORT);
          status = J2kSec.format(Time.STANDARD_TIME_FORMAT_MS, j2k + tzo) + " (" + tza + "), " + utc
              + " (UTC)";
        } else
          status = utc;

        double offset = 0;
        double multiplier = 1;

        if (settings.viewType == ViewType.SPECTROGRAM)
          unit = "Frequency (Hz)";
        else {
          Metadata md = swarmConfig.getMetadata(channel);
          if (md != null) {
            offset = md.getOffset();
            multiplier = md.getMultiplier();
            unit = md.getUnit();
          }

          if (unit == null)
            unit = "Counts";
        }

        // System.out.printf("Multipler: %f, Offset: %f\n", offset,
        // multiplier);
        status =
            String.format("%s, %s: %.3f, %s", status, unit, multiplier * yi + offset, waveInfo);

      }

      else {
        double xi = j2k;
        if (settings.viewType == ViewType.SPECTRA && settings.logFreq)
          xi = Math.pow(10.0, xi);
        if (settings.viewType == ViewType.SPECTRA && settings.logPower)
          yi = Math.pow(10.0, yi);
        status = String.format("%s, Frequency (Hz): %.3f, Power: %.3f", waveInfo, xi, yi);
      }
    } else {
      status = " ";
    }

    WaveViewTime.fireTimeChanged(j2k);

    if (status == null)
      status = " ";

    if (!Double.isNaN(mark1) && !Double.isNaN(mark2)) {
      double dur = Math.abs(mark1 - mark2);
      String pre =
          String.format("Duration: %.2fs (Md: %.2f)", dur, swarmConfig.getDurationMagnitude(dur));
      if (status.length() > 2)
        status = pre + ", " + status;
      else
        status = pre;
    }

    if (status != null && statusLabel != null) {
      final String st = status;
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          statusLabel.setText(st);
        }
      });
    }

    return !status.equals(" ");
  }

  public void setWave(Wave sw, double st, double et) {
    wave = sw;
    startTime = st;
    endTime = et;
    processSettings();
  }

  public void resetAutoScaleMemory() {
    minAmp = 1E300;
    maxAmp = -1E300;
    maxSpectraPower = -1E300;
    maxSpectrogramPower = -1E300;
    settings.autoScaleAmp = true;
    settings.autoScalePower = true;
    processSettings();
  }

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
    } else
      r.run();
  }

  /**
   * Does NOT call repaint for efficiency purposes, that is left to the
   * container.
   */
  private void processSettings() {
    if (wave == null || wave.buffer == null || wave.buffer.length == 0)
      return;

    if (settings.maxFreq > wave.getNyquist())
      settings.maxFreq = wave.getNyquist();

    timeSeries = !(settings.viewType == ViewType.SPECTRA);

    createImage();
  }

  private void filter(Wave w) {
    double mean = w.mean();

    double[] dBuf = new double[w.buffer.length + (int) (w.buffer.length * 0.5)];
    Arrays.fill(dBuf, mean);
    int trueStart = (int) (w.buffer.length * 0.25);
    for (int i = 0; i < w.buffer.length; i++) {
      if (w.buffer[i] != Wave.NO_DATA)
        dBuf[i + trueStart] = w.buffer[i];
    }

    settings.filter.setSamplingRate(w.getSamplingRate());
    settings.filter.create();
    Filter.filter(dBuf, settings.filter.getSize(), settings.filter.getXCoeffs(),
        settings.filter.getYCoeffs(), settings.filter.getGain(), 0, 0);
    if (settings.zeroPhaseShift) {
      double[] dBuf2 = new double[dBuf.length];
      for (int i = 0, j = dBuf.length - 1; i < dBuf.length; i++, j--)
        dBuf2[j] = dBuf[i];

      Filter.filter(dBuf2, settings.filter.getSize(), settings.filter.getXCoeffs(),
          settings.filter.getYCoeffs(), settings.filter.getGain(), 0, 0);

      for (int i = 0, j = dBuf2.length - 1 - trueStart; i < w.buffer.length; i++, j--)
        w.buffer[i] = (int) Math.round(dBuf2[j]);
    } else {
      for (int i = 0; i < w.buffer.length; i++)
        w.buffer[i] = (int) Math.round(dBuf[i + trueStart]);
    }
    w.invalidateStatistics();
  }

  /**
   * Paints the component on the specified graphics context.
   * 
   * @param g
   *          the graphics context
   */
  public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    Dimension dim = this.getSize();
    if (wave == null) {
      g2.setColor(backgroundColor);
      g2.fillRect(0, 0, dim.width, dim.height);
      g2.setColor(Color.black);
      if (working)
        g2.drawString("Retrieving data...", dim.width / 2 - 50, dim.height / 2);
      else {
        String error = "No wave data.";
        if (channel != null)
          error = "No wave data for " + channel + ".";
        int w = g2.getFontMetrics().stringWidth(error);
        g2.drawString(error, dim.width / 2 - w / 2, dim.height / 2);
      }
    } else {
      BufferedImage bi = getImage();
      if (bi != null)
        g2.drawImage(bi, 0, 0, null);

      if (dragging)
        paintDragBox(g2);

      if (!Double.isNaN(mark1))
        paintMark(g2, mark1);

      if (!Double.isNaN(mark2))
        paintMark(g2, mark2);

      if (!Double.isNaN(cursorMark))
        paintCursor(g2);
    }

    if (allowClose) {
      if (closeImg == null)
        closeImg = Icons.close_view.getImage();

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

  public void setUseFilterLabel(boolean b) {
    useFilterLabel = b;
  }

  public TextRenderer getFilterLabel() {
    return getFilterLabel(xOffset + 5, 148, TextRenderer.NONE, TextRenderer.NONE);
  }

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
   * @param g2
   *          the graphics context
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
      if (settings.removeBias)
        bias = (int) Math.round(renderWave.mean());
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
   * @param renderWave
   *          the wave to plot
   */
  private void plotWave(Plot plot, Wave renderWave) {
    if (renderWave == null || renderWave.numSamples() == 0)
      return;

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
    if (settings.removeBias)
      bias = wv.mean();

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

    if (waveRenderer == null)
      waveRenderer = new SliceWaveRenderer();

    if (decorator != null)
      waveRenderer.setFrameDecorator(decorator);

    if (settings.useUnits && md != null && md.getUnit() != null)
      waveRenderer.setYLabelText(md.getUnit());
    else
      waveRenderer.setYLabelText("Counts");

    waveRenderer.setYAxisCoefficients(multiplier, offset);
    waveRenderer.setLocation(xOffset, yOffset, this.getWidth() - xOffset - rightWidth,
        this.getHeight() - yOffset - bottomHeight);
    waveRenderer.setYLimits(minY, maxY);
    waveRenderer.setViewTimes(startTime, endTime, "");
    waveRenderer.setWave(wv);
    waveRenderer.setRemoveBias(settings.removeBias);
    if (channel != null && displayTitle)
      waveRenderer.setTitle(channel);

    waveRenderer.update();
    plot.addRenderer(waveRenderer);
    if (useFilterLabel && settings.filterOn)
      plot.addRenderer(getFilterLabel(getWidth() - rightWidth, getHeight() - bottomHeight,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));
    translation = waveRenderer.getDefaultTranslation();
  }

  /**
   * Plots frequency spectra.
   * 
   * @param renderWave
   *          the wave to plot
   */
  private void plotSpectra(Plot plot, Wave renderWave) {
    if (renderWave == null || renderWave.numSamples() == 0)
      return;

    SliceWave wv = new SliceWave(renderWave);
    wv.setSlice(startTime, endTime);

    if (spectraRenderer == null)
      spectraRenderer = new SpectraRenderer();

    if (decorator != null)
      spectraRenderer.setFrameDecorator(decorator);

    spectraRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset,
        this.getHeight() - bottomHeight - yOffset);
    spectraRenderer.setWave(wv);

    spectraRenderer.setAutoScale(settings.autoScalePower);
    spectraRenderer.setLogPower(settings.logPower);
    spectraRenderer.setLogFreq(settings.logFreq);
    spectraRenderer.setMaxFreq(settings.maxFreq);
    spectraRenderer.setMinFreq(settings.minFreq);
    spectraRenderer.setYUnitText("Power");
    if (channel != null && displayTitle)
      spectraRenderer.setTitle(channel);

    spectraRenderer.update();
    if (useFilterLabel && settings.filterOn)
      plot.addRenderer(getFilterLabel(getWidth() - rightWidth, getHeight() - bottomHeight,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));

    translation = spectraRenderer.getDefaultTranslation();
    plot.addRenderer(spectraRenderer);
  }

  /**
   * Plots a spectrogram. TODO: Fix logPower.
   * 
   * @param renderWave
   *          the wave to plot
   */
  private void plotSpectrogram(Plot plot, Wave renderWave) {
    if (renderWave == null || renderWave.numSamples() == 0)
      return;

    SliceWave wv = new SliceWave(renderWave);
    wv.setSlice(startTime, endTime);

    if (spectrogramRenderer == null)
      spectrogramRenderer = new SpectrogramRenderer();

    if (decorator != null)
      spectrogramRenderer.setFrameDecorator(decorator);

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

    if (channel != null && displayTitle)
      spectrogramRenderer.setTitle(channel);

    spectrogramRenderer.setYUnitText("Frequency (Hz)");

    spectrogramRenderer.setNfft(settings.nfft);

    double Power[] = spectrogramRenderer.update();

    settings.minPower = Power[0];
    settings.maxPower = Power[1];

    plot.addRenderer(spectrogramRenderer);
    if (useFilterLabel && settings.filterOn)
      plot.addRenderer(getFilterLabel(getWidth() - rightWidth, getHeight() - bottomHeight,
          TextRenderer.RIGHT, TextRenderer.BOTTOM));
    translation = spectrogramRenderer.getDefaultTranslation();
  }

  /**
   * Paints the zoom drag box.
   * 
   * @param g2
   *          the graphics context
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

  private static final Color DARK_RED = new Color(168, 0, 0);
  private static final Color DARK_GREEN = new Color(0, 168, 0);

  public void setCursorMark(double j2k) {
    cursorMark = j2k;
    repaint();
  }

  private void paintCursor(Graphics2D g2) {
    if (Double.isNaN(cursorMark) || cursorMark < startTime || cursorMark > endTime)
      return;

    double[] t = getTranslation();
    if (t == null)
      return;
    double x = (cursorMark - t[1]) / t[0];
    g2.setColor(DARK_RED);
    g2.draw(new Line2D.Double(x, yOffset + 1, x, getHeight() - bottomHeight - 1));
  }

  private void paintMark(Graphics2D g2, double j2k) {
    if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime)
      return;

    double[] t = getTranslation();
    if (t == null)
      return;

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
}
