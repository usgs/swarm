package gov.usgs.volcanoes.swarm.heli;

import gov.usgs.volcanoes.core.data.HelicorderData;
import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.legacy.plot.Plot;
import gov.usgs.volcanoes.core.legacy.plot.PlotException;
import gov.usgs.volcanoes.core.legacy.plot.decorate.FrameDecorator;
import gov.usgs.volcanoes.core.legacy.plot.decorate.SmartTick;
import gov.usgs.volcanoes.core.legacy.plot.render.AxisRenderer;
import gov.usgs.volcanoes.core.legacy.plot.render.FrameRenderer;
import gov.usgs.volcanoes.core.legacy.plot.render.HelicorderRenderer;
import gov.usgs.volcanoes.core.legacy.plot.render.TextRenderer;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.event.TagData;
import gov.usgs.volcanoes.swarm.event.TagMenu;
import gov.usgs.volcanoes.swarm.options.SwarmOptions;
import gov.usgs.volcanoes.swarm.options.SwarmOptionsListener;
import gov.usgs.volcanoes.swarm.time.UiTime;
import gov.usgs.volcanoes.swarm.wave.StatusTextArea;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanelAdapter;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * A <code>JComponent</code> for displaying and interacting with a helicorder.
 * 
 * @author Dan Cervelli
 */
public class HelicorderViewPanel extends JComponent implements SwarmOptionsListener {
  //private static final Logger LOGGER = LoggerFactory.getLogger(HelicorderViewPanel.class);

  private static final Color BACKGROUND_COLOR = new Color(0xf7, 0xf7, 0xf7);
  public static final long serialVersionUID = -1;

  public static final int X_OFFSET = 70;
  public static final int Y_OFFSET = 10;
  public static final int RIGHT_WIDTH = 70;
  public static final int BOTTOM_HEIGHT = 35;
  private int insetHeight = 200;

  // TODO: extract translation into an object
  private static final int GRAPH_LEFT = 0;
  private static final int GRAPH_RIGHT = 1;
  private static final int ROW_HEIGHT = 2;
  private static final int GRAPH_Y = 3;
  //private static final int MIN_TIME_LOCAL = 4;
  //private static final int MAX_TIME_LOCAL = 5;
  //private static final int TIME_CHUNK = 6;
  private static final int PIXEL_TIME_SPAN = 7;

  private Plot plot;
  private HelicorderRenderer heliRenderer;
  private HelicorderViewerSettings settings;
  private HelicorderData heliData;
  private double startTime;
  private double endTime;
  private double mean;
  private double bias;
  private HelicorderViewerFrame parent;

  private double[] translation;

  private WaveViewPanel insetWavePanel;

  private BufferedImage bufferImage;
  private BufferedImage displayImage;

  private boolean working;
  private boolean resized;

  private int insetY;

  private boolean fullScreen;

  private boolean minimal;

  private double startMark = Double.NaN;
  private double endMark = Double.NaN;
  private double lastMark = Double.NaN;

  private EventListenerList listeners = new EventListenerList();
  private static SwarmConfig swarmConfig;

  private TagMenu tagMenu;
  protected Vector<TagData> tagData = new Vector<TagData>();
  
  /**
   * Constructor.
   * @param hvf helicorder viewer frame
   */
  public HelicorderViewPanel(HelicorderViewerFrame hvf) {
    swarmConfig = SwarmConfig.getInstance();

    parent = hvf;
    plot = new Plot();
    plot.setBackgroundColor(BACKGROUND_COLOR);
    settings = hvf.getHelicorderViewerSettings();
    heliRenderer = new HelicorderRenderer();
    if (swarmConfig.heliColors != null) {
      heliRenderer.setDefaultColors(swarmConfig.heliColors);// DCK: add configured colors
    }
    heliRenderer.setExtents(0, 1, Double.MAX_VALUE, -Double.MAX_VALUE);
    plot.addRenderer(heliRenderer);

    this.setRequestFocusEnabled(true);
    this.addMouseListener(new HelicorderMouseListener());
    this.addMouseMotionListener(new HelicorderMouseMotionListener());
    this.addMouseWheelListener(new HelicorderMouseWheelListener());

    cursorChanged();

    SwarmOptions.addOptionsListener(this);
  }

  public void addListener(HelicorderViewPanelListener listener) {
    listeners.add(HelicorderViewPanelListener.class, listener);
  }

  public void removeListener(HelicorderViewPanelListener listener) {
    listeners.remove(HelicorderViewPanelListener.class, listener);
  }

  /**
   * Trigger on inset creation.
   * @param st start time 
   * @param et end time
   */
  public void fireInsetCreated(double st, double et) {
    Object[] ls = listeners.getListenerList();
    for (int i = ls.length - 2; i >= 0; i -= 2) {
      if (ls[i] == HelicorderViewPanelListener.class) {
        ((HelicorderViewPanelListener) ls[i + 1]).insetCreated(st, et);
      }
    }
  }

  /**
   * Trigger on cursor change.
   */
  public void cursorChanged() {
    Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
    if (swarmConfig.useLargeCursor) {
      Image cursorImg = Icons.crosshair.getImage();
      crosshair = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(16, 16),
          "Large crosshair");
    }

    this.setCursor(crosshair);
  }

  /**
   * Trigger on settings change.
   */
  public void settingsChanged() {
    if (insetWavePanel != null) {
      double zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
      double j2k = insetWavePanel.getStartTime()
          + (insetWavePanel.getEndTime() - insetWavePanel.getStartTime()) / 2;
      loadInsetWave(j2k - zoomOffset, j2k + zoomOffset);
    }

    repaint();
  }

  public void setStartMark(double t) {
    startMark = t;
  }

  public void setEndMark(double t) {
    endMark = t;
  }

  public void clearMarks() {
    startMark = endMark = Double.NaN;
  }

  /**
   * Set cursor mark on inset wave panel to given time.
   * @param t time
   */
  public void setCursorMark(double t) {
    if (insetWavePanel != null) {
      insetWavePanel.setCursorMark(t);
    }
  }

  /**
   * Mark time on inset wave panel.
   * @param t time
   */
  public void markTime(double t) {
    if (Double.isNaN(startMark) && Double.isNaN(endMark)) {
      startMark = t;
    } else if (!Double.isNaN(startMark) && Double.isNaN(endMark)) {
      endMark = t;
      if (endMark < startMark) {
        double tm = startMark;
        startMark = endMark;
        endMark = tm;
      }
    } else {
      startMark = Math.min(lastMark, t);
      endMark = Math.max(lastMark, t);
    }
    lastMark = t;
    if (insetWavePanel != null) {
      insetWavePanel.setMarks(startMark, endMark);
    }
    repaint();
  }

  class HelicorderMouseMotionListener implements MouseMotionListener {
    public void mouseDragged(MouseEvent e) {
      UiTime.touchTime();
      HelicorderViewPanel.this.requestFocus();
      int mx = e.getX();
      int my = e.getY();

      if (mx < heliRenderer.getGraphX() || my < heliRenderer.getGraphY()
          || mx > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1
          || my > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1) {
        /*
         * // removed because it wasn't helpful! if (insetWavePanel != null) { removeWaveInset();
         * repaint(); } return;
         */
      } else {
        double j2k = getMouseJ2K(mx, my);
        processMousePosition(mx, my);

        if (SwingUtilities.isLeftMouseButton(e)) {
          createWaveInset(j2k, mx, my);
        }
      }
    }

    public void mouseMoved(MouseEvent e) {
      UiTime.touchTime();
      processMousePosition(e.getX(), e.getY());
    }
  }

  class HelicorderMouseWheelListener implements MouseWheelListener {
    int totalScroll = 0;
    Delay delay;

    public void mouseWheelMoved(MouseWheelEvent e) {
      UiTime.touchTime();
      totalScroll += e.getWheelRotation();
      if (delay == null) {
        delay = new Delay(250);
      } else {
        delay.restart();
      }
    }

    public void delayOver() {
      removeWaveInset();
      SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          parent.scroll(totalScroll);
          delay = null;
          totalScroll = 0;
        }
      });
    }

    class Delay extends Thread {
      long delayLeft;

      public Delay(long ms) {
        delayLeft = ms;
        start();
      }

      public void restart() {
        interrupt();
      }

      public void run() {
        boolean done = false;
        while (!done) {
          try {
            Thread.sleep(delayLeft);
            done = true;
          } catch (Exception e) {
            // do nothing?
          }
        }
        delayOver();
      }
    }
  }

  class HelicorderMouseListener implements MouseListener {
    public void mouseClicked(MouseEvent e) {
      UiTime.touchTime();
      HelicorderViewPanel.this.requestFocus();
    }

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
      UiTime.touchTime();
      int mx = e.getX();
      int my = e.getY();
      if (mx < heliRenderer.getGraphX() || my < heliRenderer.getGraphY()
          || mx > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1
          || my > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1) {
        return;
      }
      double j2k = getMouseJ2K(mx, my);

      if (e.getButton() == MouseEvent.BUTTON1) {
        if (j2k != -1E300) {
          if (insetWavePanel != null) {
            insetWavePanel.setWave(null, 0, 1);
          }
          createWaveInset(j2k, mx, my);
        }
      } else if (SwingUtilities.isRightMouseButton(e)) {
        if (parent.isTagEnabled()) {
          TagMenu tagMenu = getTagMenu();
          tagMenu.setJ2k(j2k);
          tagMenu.show(HelicorderViewPanel.this, mx + 30, mx);
        } 
      }
      /*
       * else if (e.getButton() == MouseEvent.BUTTON3) { if (insetWavePanel != null) {
       * removeWaveInset(); repaint(); } }
       */
    }

    public void mouseReleased(MouseEvent e) {}
  }

  public boolean hasInset() {
    return insetWavePanel != null;
  }

  public HelicorderData getData() {
    return heliData;
  }

  public double getStartTime() {
    return startTime;
  }

  public double getEndTime() {
    return endTime;
  }

  /**
   * Send inset to clipboard.
   */
  public void insetToClipboard() {
    if (insetWavePanel != null) {
      WaveViewPanel p = new WaveViewPanel(insetWavePanel);
      p.setDataSource(insetWavePanel.getDataSource());
      WaveClipboardFrame cb = WaveClipboardFrame.getInstance();
      cb.setVisible(true);
      cb.addWave(p);
      requestFocus();
    }
  }

  /**
   * Process mouse position.
   * @param x mouse x position
   * @param y mouse y position
   */
  private void processMousePosition(int x, int y) {
    if (heliData == null) {
      return;
    }

    boolean wp = false;
    if (insetWavePanel != null) {
      Point loc = insetWavePanel.getLocation();
      wp = insetWavePanel.processMousePosition(x - loc.x, y - loc.y);
    }

    if (!wp) {
      if (!(x < heliRenderer.getGraphX() || y < heliRenderer.getGraphY()
          || x > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1
          || y > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1)) {
        // set status text at bottom

        double j2k = getMouseJ2K(x, y);
        String status =
            StatusTextArea.getTimeString(j2k, swarmConfig.getTimeZone(settings.channel));
        
        // look for event to show
        if (parent.isTagEnabled()) {
          double mindiff = Double.MAX_VALUE;
          TagData showTag = null;
          for (TagData tag : tagData) { // get closest within
            if (tag.channel.equals(settings.channel)) {
              double diff = Math.abs(j2k - tag.startTime);
              if (diff < 60 && diff < mindiff) {
                mindiff = diff;
                showTag = tag;
              }
            }
          }
          if (showTag != null) {
            status += "\n" + showTag.toString();
          }
        }
        
        parent.setStatus(status);
      } else {
        parent.setStatus(" ");
      }
    }
  }

  /**
   * Get time at point.
   * @param mx mouse x position
   * @param my mouse y position
   * @return time in J2K
   */
  public double getMouseJ2K(int mx, int my) {
    double j2k = 0;
    if (translation != null) {
      j2k = translation[4];
      j2k += (mx - translation[0]) * translation[7];
      j2k += getHelicorderRow(my) * translation[6];
    }
    return j2k;
  }

  public int getHelicorderRow(int my) {
    return (int) Math.floor((my - translation[3]) / translation[2]);
  }

  /**
   * Remove wave inset from panel.
   */
  public void removeWaveInset() {
    if (insetWavePanel != null) {
      parent.setInsetButtonsEnabled(false);
      this.remove(insetWavePanel);
      insetWavePanel = null;
      repaint();
    }
  }

  /**
   * Move selected wave window.
   * 
   * @param offset window lengths to move
   */
  public void moveInset(int offset) {
    if (insetWavePanel == null) {
      return;
    }

    double st = insetWavePanel.getStartTime();
    double et = insetWavePanel.getEndTime();
    double dt = et - st;
    double newStartTime = st + dt * offset;
    double newEndTime = et + dt * offset;

    int firstRow = heliRenderer.getRow(newStartTime);
    int lastRow = heliRenderer.getRow(newEndTime);
    if (lastRow < 0 || firstRow > heliRenderer.getNumRows() - 1) {
      return;
    }

    loadInsetWave(newStartTime, newEndTime);
  }

  /**
   * Create wave inset.
   * @param j2k time
   * @param mx mouse x position
   * @param my mouse y position
   */
  public void createWaveInset(final double j2k, final int mx, final int my) {
    if (working) {
      return;
    }

    insetY = my;

    if (insetWavePanel == null) {
      insetWavePanel = new WaveViewPanel(parent.getWaveViewSettings());
      insetWavePanel.addListener(new WaveViewPanelAdapter() {
        public void waveClosed(WaveViewPanel src) {
          removeWaveInset();
        }

        public void waveTimePressed(WaveViewPanel src, MouseEvent e, double j2k) {
          if (swarmConfig.durationEnabled && SwingUtilities.isLeftMouseButton(e)) {
            markTime(j2k);   
          }
          insetWavePanel.processMousePosition(e.getX(), e.getY());
        }
      });
    }

    // insetWavePanel.setHelicorderPanel(this);
    insetWavePanel.setMarks(startMark, endMark);
    insetWavePanel.setChannel(settings.channel);
    insetWavePanel.setDataSource(parent.getDataSource());
    insetWavePanel.setStatusText(parent.getStatusText());
    makeInsetPanelTagEnabled();
    
    Dimension d = getSize();
    insetHeight = getHeight() / 4;
    int height = insetHeight;
    int row = heliRenderer.getRow(j2k);

    insetWavePanel.setSize(d.width + 2, height);
    double zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
    int rowSpan = (int) Math.ceil(2 * zoomOffset / heliRenderer.getTimeChunk());
    if (insetY - heliRenderer.getRowHeight() > height + translation[GRAPH_Y]) {
      int y = (int) Math.ceil((row - rowSpan) * translation[ROW_HEIGHT] + translation[GRAPH_Y]);
      insetWavePanel.setLocation(-1, y - height);
    } else {
      int y = (int) Math.ceil((row + 1 + rowSpan) * translation[ROW_HEIGHT] + translation[GRAPH_Y]);
      insetWavePanel.setLocation(-1, y);
    }

    insetWavePanel.setAllowClose(true);
    insetWavePanel.setWorking(true);

    loadInsetWave(j2k - zoomOffset, j2k + zoomOffset);
    parent.setInsetButtonsEnabled(true);
    this.add(insetWavePanel);
    repaint();
  }

  private void loadInsetWave(final double st, final double et) {
    fireInsetCreated(st, et);
    final SwingWorker worker = new SwingWorker() {
      private Wave sw;

      public Object construct() {
        parent.getThrobber().increment();
        working = true;
        sw = parent.getWave(st, et);
        return null;
      }

      public void finished() {
        parent.getThrobber().decrement();
        working = false;
        if (insetWavePanel != null) {
          insetWavePanel.setWave(sw, st, et);
          insetWavePanel.setWorking(false);
        }
        repaint();
      }
    };
    worker.start();
  }

  /**
   * Make inset panel tag enabled.
   */
  protected void makeInsetPanelTagEnabled() {
    if (insetWavePanel != null) {
      insetWavePanel.getSettings().tagEnabled = parent.isTagEnabled();
      insetWavePanel.setTagData(tagData);
      insetWavePanel.setTagMenu(getTagMenu());    
    }
  }
  
  /**
   * Set helicorder.
   * @param d helicorder data
   * @param time1 start time
   * @param time2 end time
   */
  public void setHelicorder(HelicorderData d, double time1, double time2) {
    heliData = d;
    if (heliData != null) {
      startTime = time1;
      endTime = time2;
      heliRenderer.setData(heliData);
      heliRenderer.setTimeChunk(settings.timeChunk);
      heliRenderer.setTimeZone(swarmConfig.getTimeZone(settings.channel));
      heliRenderer.setForceCenter(settings.forceCenter);
      heliRenderer.setClipBars(settings.clipBars);
      heliRenderer.setShowClip(settings.showClip);
      heliRenderer.setAlertClip(settings.alertClip);
      heliRenderer.setClipWav("clip.wav");
      heliRenderer.setClipAlertTimeout(settings.alertClipTimeout);
      mean = heliData.getMeanMax();
      bias = heliData.getBias();
      if (bias != mean) {
        mean = Math.abs(bias - mean);
      }
      heliRenderer.setClipValue(settings.clipValue);

    }
  }

  /**
   * Invalidate image.
   */
  public void invalidateImage() {
    final SwingWorker worker = new SwingWorker() {
      public Object construct() {
        createImage();
        return null;
      }

      public void finished() {
        displayImage = bufferImage;
        repaint();
      }
    };
    worker.start();
  }

  protected void setResized(boolean b) {
    resized = b;
  }

  private void createImage() {
    if (heliData == null) {
      return;
    }

    Dimension d = this.getSize();
    if (d.width <= 0 || d.height <= 0) {
      return;
    }

    bufferImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_4BYTE_ABGR);

    Graphics2D ig = (Graphics2D) bufferImage.getGraphics();
    plot.setSize(d);

    double offset = 0;
    double multiplier = 1;
    Metadata md = swarmConfig.getMetadata(settings.channel);
    if (md != null) {
      offset = md.getOffset();
      multiplier = md.getMultiplier();
    }

    if (minimal) {
      heliRenderer.setLocation(X_OFFSET / 2, Y_OFFSET, d.width - X_OFFSET - 5,
          d.height - Y_OFFSET - BOTTOM_HEIGHT / 2);
    } else {
      heliRenderer.setLocation(X_OFFSET, Y_OFFSET, d.width - X_OFFSET - RIGHT_WIDTH,
          d.height - Y_OFFSET - BOTTOM_HEIGHT);
    }

    if (settings.autoScale) {
      settings.barRange = (int) (mean * settings.barMult);
      settings.clipValue = (int) (mean * settings.clipBars);
      heliRenderer.setHelicorderExtents(startTime, endTime, -1 * Math.abs(settings.barRange),
          Math.abs(settings.barRange));

    } else {
      heliRenderer.setHelicorderExtents(startTime, endTime,
          -1 * Math.abs((settings.barRange - offset) / multiplier),
          Math.abs((settings.barRange - offset) / multiplier));
    }

    heliRenderer.setTimeZone(swarmConfig.getTimeZone(settings.channel));
    heliRenderer.setClipValue(settings.clipValue);
    if (minimal) {
      // System.out.println("minimal");
      // heliRenderer.createMinimumAxis();
      heliRenderer.setFrameDecorator(new SmallDecorator());
    } else {
      heliRenderer.createDefaultAxis();
    }

    if (md == null || md.getAlias() == null) {
      heliRenderer.setChannel(settings.channel);
    } else {
      heliRenderer.setChannel(md.getAlias());
    }

    translation = heliRenderer.getTranslationInfo(false);
    heliRenderer.setLargeChannelDisplay(fullScreen);

    try {
      plot.render(ig);
    } catch (PlotException e) {
      e.printStackTrace();
    }
  }

  class SmallDecorator extends FrameDecorator {
    public void decorate(FrameRenderer fr) {
      AxisRenderer axis = new AxisRenderer(fr);
      axis.createDefault();
      fr.setAxis(axis);

      int minutes = (int) Math.round(settings.timeChunk / 60.0);
      int majorTicks = minutes;
      if (minutes > 30 && minutes < 180) {
        majorTicks = minutes / 5;
      } else if (minutes >= 180 && minutes < 360) {
        majorTicks = minutes / 10;
      } else if (minutes >= 360) {
        majorTicks = minutes / 20;
      }
      double[] mjt = SmartTick.intervalTick(0, settings.timeChunk, majorTicks);

      axis.createBottomTicks(null, mjt);
      axis.createTopTicks(null, mjt);
      axis.createVerticalGridLines(mjt);

      int bc = (settings.timeChunk / 5) + 2;
      String[] btl = new String[bc];
      double[] btlv = new double[bc];
      btl[0] = "+";
      btlv[0] = 30;
      for (int i = 0, j = 1; i < mjt.length; i++) {
        long m = Math.round(mjt[i] / 60.0);
        if (m % 5 == 0) {
          btl[j] = Long.toString(m);
          btlv[j++] = mjt[i];
        }
      }
      axis.createBottomTickLabels(btlv, btl);

      HelicorderRenderer hr = (HelicorderRenderer) fr;
      int numRows = hr.getNumRows();
      double[] labelPosLr = new double[numRows];
      String[] leftLabelText = new String[numRows];
      String[] rightLabelText = new String[numRows];
      TimeZone timeZone = swarmConfig.getTimeZone(settings.channel);

      DateFormat localTimeFormat = new SimpleDateFormat("HH:mm");
      localTimeFormat.setTimeZone(timeZone);

      DateFormat localDayFormat = new SimpleDateFormat("MM-dd");
      localDayFormat.setTimeZone(timeZone);

      double pixelsPast = 0;
      double pixelsPerRow = fr.getGraphHeight() / numRows;
      String lastLocalDay = "";
      for (int i = numRows - 1; i >= 0; i--) {
        pixelsPast += pixelsPerRow;
        labelPosLr[i] = i + 0.5;

        String localTime = localTimeFormat
            .format(J2kSec.asDate(hr.getHelicorderMaxX() - (i + 1) * settings.timeChunk));
        leftLabelText[i] = null;
        if (pixelsPast > 20) {
          leftLabelText[i] = localTime;
          pixelsPast = 0;
        }

        Date dtz = J2kSec.asDate(hr.getHelicorderMaxX() - (i + 1) * settings.timeChunk);
        String localDay =
            localDayFormat.format(new Date(dtz.getTime() + settings.timeChunk * 1000));
        if (!localDay.equals(lastLocalDay)) {
          rightLabelText[i] = localDay;
          lastLocalDay = localDay;
        }
      }

      axis.createLeftTickLabels(labelPosLr, leftLabelText);
      axis.createRightTickLabels(labelPosLr, rightLabelText);

      boolean dst = timeZone.inDaylightTime(J2kSec.asDate(hr.getViewEndTime()));
      String timeZoneName = timeZone.getDisplayName(dst, TimeZone.SHORT);
      TextRenderer tr =
          new TextRenderer(3, fr.getGraphY() + fr.getGraphHeight() + 14, timeZoneName);
      tr.font = Font.decode("dialog-PLAIN-9");
      tr.antiAlias = false;
      axis.addPostRenderer(tr);

      double[] hg = new double[numRows - 1];
      for (int i = 0; i < numRows - 1; i++) {
        hg[i] = i + 1.0;
      }

      axis.createHorizontalGridLines(hg);

      axis.setBackgroundColor(Color.white);
    }
  }

  public void setFullScreen(boolean b) {
    fullScreen = b;
  }

  public void setMinimal(boolean b) {
    minimal = b;
  }
  
  private static final Color DARK_GREEN = new Color(0, 168, 0);
  private static final Color ORANGE = new Color(255, 69, 0, 180);

  private void drawMark(Graphics2D g2, double t, Color color) {
    if (Double.isNaN(t)) {
      return;
    }

    int x = (int) (heliRenderer.helicorderGetXPixel(t));
    int row = heliRenderer.getRow(t);
    int y = (int) Math.ceil(row * translation[2] + translation[3]);

    if (!(x < heliRenderer.getGraphX() || y < heliRenderer.getGraphY()
        || x > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1
        || y > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1)) {
      g2.setColor(color);
      g2.draw(new Line2D.Double(x, y, x, y + translation[2]));
  
      GeneralPath gp = new GeneralPath();
      gp.moveTo(x, y);
      gp.lineTo((float) x - 4, (float) y - 6);
      gp.lineTo((float) x + 4, (float) y - 6);
      gp.closePath();
      g2.fill(gp);
      g2.draw(gp);
  
      gp.reset();
      gp.moveTo(x, (float) (y + translation[2]));
      gp.lineTo((float) x - 4, (float) (y + 6 + translation[2]));
      gp.lineTo((float) x + 4, (float) (y + 6 + translation[2]));
      gp.closePath();
      gp.closePath();
      g2.fill(gp);
      g2.draw(gp);
    }
  }

  private void drawEvent(Graphics2D g2, double t, Color color) {
    if (Double.isNaN(t)) {
      return;
    }
    int x = (int) (heliRenderer.helicorderGetXPixel(t));
    int row = heliRenderer.getRow(t);
    int y = (int) Math.ceil(row * translation[2] + translation[3]);
    if (!(x < heliRenderer.getGraphX() || y < heliRenderer.getGraphY()
        || x > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1
        || y > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1)) {
      g2.setColor(color);
      int r = 12;
      g2.fillOval(x - (r / 2), y, r, r);
    }
  }

  /**
   * Paint.
   * @see javax.swing.JComponent#paint(java.awt.Graphics)
   */
  public void paint(Graphics g) {
    Graphics2D g2 = (Graphics2D) g;
    Dimension d = this.getSize();
    if (heliData == null) {
      if (!parent.isWorking()) {
        parent.setStatus("The server returned no helicorder data.");
      }
    } else if (displayImage != null) {
      g2.drawImage(displayImage, 0, 0, null);
    }

    drawMark(g2, startMark, DARK_GREEN);
    drawMark(g2, endMark, DARK_GREEN);
    if (parent.isTagEnabled()) {
      for (TagData tag : tagData) {
        if (tag.channel.equals(settings.channel)) {
          drawEvent(g2, tag.startTime, ORANGE);
        }
      }      
    }

    if (insetWavePanel != null) {
      // find out where time highlight will be, possibly reposition the
      // insetWavePanel
      double t1 = insetWavePanel.getStartTime();
      double t2 = insetWavePanel.getEndTime();
      double dt = t2 - t1;
      double spanCenter = (t2 - t1) / 2 + t1;
      int row = heliRenderer.getRow(spanCenter);

      int rowSpan = (int) Math.ceil(dt / heliRenderer.getMaxX()) + 1;
      double rowHeight = translation[ROW_HEIGHT];
      double graphY = translation[GRAPH_Y];

      if (resized) {
        insetWavePanel.setSize(d.width, insetHeight);
        insetWavePanel.createImage();
      }

      int panelY;
      if ((row - rowSpan) * rowHeight > insetHeight + graphY) {
        panelY = (int) Math.ceil((row - rowSpan) * rowHeight + graphY - insetHeight);
      } else {
        panelY = (int) Math.ceil((row + rowSpan) * rowHeight + graphY);
      }

      if (panelY != insetWavePanel.getLocation().y) {
        insetWavePanel.setLocation(0, panelY);
      }

      // now it's safe to draw the waveInsetPanel
      Point p = insetWavePanel.getLocation();
      g2.translate(p.x, p.y);
      insetWavePanel.paint(g2);
      Dimension wvd = insetWavePanel.getSize();
      g.setColor(Color.gray);
      g.drawRect(0, 0, wvd.width - 1, wvd.height);
      g2.translate(-p.x, -p.y);

      /*
       * Not sure what below is meant to address. I removed the check because negative row offsets
       * were necessary to handle multi-row highlights. If there's a problem with wave loading, find
       * a more direct fix. --tjp
       */
      // fixes bug where highlight was being drawn before wave loaded
      // if (row < 0)
      // return;

      // finally, draw the highlight.
      Paint pnt = g2.getPaint();
      g2.setPaint(new Color(255, 255, 0, 128));
      Rectangle2D.Double rect = new Rectangle2D.Double();

      int zoomTimeSpan = parent.getHelicorderViewerSettings().waveZoomOffset * 2;
      double zoomPixelSpan = 1.0 / translation[PIXEL_TIME_SPAN] * zoomTimeSpan;

      // highlight left
      int highlightStart = (int) heliRenderer.helicorderGetXPixel(spanCenter);
      int highlightSpan = (int) (zoomPixelSpan / 2);
      int rowOffset = 0;
      while (highlightSpan > 0 && row - rowOffset >= 0) {
        int width = Math.min(highlightSpan, (int) (highlightStart - translation[GRAPH_LEFT]));

        if (row - rowOffset < heliRenderer.getNumRows()) {
          rect.setRect(highlightStart - width,
              (int) Math.ceil((row - rowOffset) * translation[ROW_HEIGHT] + translation[GRAPH_Y]),
              width, (int) Math.ceil(translation[ROW_HEIGHT]));
          g2.fill(rect);
        }
        highlightSpan -= width;
        highlightStart = (int) translation[GRAPH_RIGHT];
        rowOffset++;

      }

      // highlight right
      highlightStart = (int) heliRenderer.helicorderGetXPixel(spanCenter);
      highlightSpan = (int) (zoomPixelSpan / 2);
      rowOffset = 0;
      while (highlightSpan > 0 && row + rowOffset < heliRenderer.getNumRows()) {
        int width = Math.min(highlightSpan, (int) (translation[GRAPH_RIGHT] - highlightStart));

        if (row + rowOffset >= 0) {
          rect.setRect(highlightStart,
              (int) Math.ceil((row + rowOffset) * translation[ROW_HEIGHT] + translation[GRAPH_Y]),
              width, (int) Math.ceil(translation[ROW_HEIGHT]));
          g2.fill(rect);
        }

        highlightSpan -= width;
        highlightStart = (int) translation[GRAPH_LEFT];
        rowOffset++;
      }

      g2.setPaint(pnt);
    }

    resized = false;
  }

  /**
   * Option changed.
   * @see gov.usgs.volcanoes.swarm.options.SwarmOptionsListener#optionsChanged()
   */
  public void optionsChanged() {
    cursorChanged();
    invalidateImage();
    if (!SwarmConfig.getInstance().durationEnabled) {
      clearMarks();
    }
  }

  public Vector<TagData> getTagData() {
    return tagData;
  }
  
  /**
   * Get tag menu.
   * @return
   */
  public TagMenu getTagMenu() {
    if (tagMenu == null) {
      tagMenu = new TagMenu(this);
    }
    return tagMenu;
  }
  
  public HelicorderViewerSettings getSettings() {
    return settings;
  }
  
  public WaveViewPanel getInsetPanel() {
    return insetWavePanel;
  }
  
  public HelicorderViewerFrame getFrame() {
    return parent;
  }

}
