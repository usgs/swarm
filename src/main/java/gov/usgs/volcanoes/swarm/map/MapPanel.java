package gov.usgs.volcanoes.swarm.map;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.Semaphore;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.usgs.plot.Plot;
import gov.usgs.plot.map.GeoImageSet;
import gov.usgs.plot.map.GeoLabelSet;
import gov.usgs.plot.map.MapRenderer;
import gov.usgs.plot.map.WMSGeoImageSet;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Mercator;
import gov.usgs.proj.Projection;
import gov.usgs.proj.TransverseMercator;
import gov.usgs.util.Pair;
import gov.usgs.util.Util;
import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.util.StringUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.map.MapMiniPanel.Position;
import gov.usgs.volcanoes.swarm.time.TimeListener;
import gov.usgs.volcanoes.swarm.time.WaveViewTime;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

/**
 * @author Dan Cervelli
 */
public class MapPanel extends JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(MapPanel.class);

  private static SwarmConfig swarmConfig;

  public enum DragMode {
    DRAG_MAP, BOX, RULER;
  }

  public enum LabelSetting {
    NONE("N", Icons.label_none), SOME("S", Icons.label_some), ALL("A", Icons.label_all);

    public String code;
    public ImageIcon image;

    LabelSetting(final String s, final ImageIcon i) {
      code = s;
      image = i;
    }

    public LabelSetting next() {
      switch (this) {
        case SOME:
          return LabelSetting.ALL;
        case ALL:
          return LabelSetting.NONE;
        case NONE:
        default:
          return LabelSetting.SOME;
      }
    }

    public Icon getIcon() {
      return image;
    }

    public static LabelSetting fromString(final String s) {
      if (s == null)
        return SOME;

      if (s.equals("N"))
        return NONE;
      else if (s.equals("A"))
        return ALL;
      else if (s.equals("S"))
        return SOME;
      else
        return SOME;
    }

  }

  private static final long serialVersionUID = 1L;

  private static final int INSET = 30;

  private List<Line2D.Double> lines;

  private GeoImageSet images;
  private GeoLabelSet labels;
  private GeoRange range;
  private Projection projection;
  private RenderedImage image;

  private Point2D.Double center;
  private double scale = 100000;

  private JLayeredPane pane;
  private MapImagePanel mapImagePanel;
  private BufferedImage mapImage;

  private MapRenderer renderer;

  private final Map<Double, MapMiniPanel> miniPanels;
  private final Map<Double, ConfigFile> layouts;
  private final List<MapMiniPanel> visiblePanels;

  private DragMode dragMode = DragMode.DRAG_MAP;
  private Point mouseDown;
  private Point mouseNow;
  private Rectangle dragRectangle;

  private final Stack<double[]> mapHistory;
  private final Stack<double[]> timeHistory;

  private int missing;

  private final Set<MapMiniPanel> selectedPanels;
  private final boolean allowMultiSelection = false;

  private double startTime;
  private double endTime;

  private int dragDX = Integer.MAX_VALUE;
  private int dragDY = Integer.MAX_VALUE;

  private LabelSetting labelSetting = LabelSetting.SOME;

  private List<? extends ClickableGeoLabel> clickableLabels;
  private List<MapLayer> layers;

  public MapPanel() {
    swarmConfig = SwarmConfig.getInstance();
    mapHistory = new Stack<double[]>();
    timeHistory = new Stack<double[]>();
    lines = new ArrayList<Line2D.Double>();
    miniPanels = Collections.synchronizedMap(new HashMap<Double, MapMiniPanel>());
    layouts = Collections.synchronizedMap(new HashMap<Double, ConfigFile>());
    visiblePanels = Collections.synchronizedList(new ArrayList<MapMiniPanel>());
    selectedPanels = new HashSet<MapMiniPanel>();
    layers = new ArrayList<MapLayer>();

    final Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
    this.setCursor(crosshair);
    createUI();
  }

  public void addLayer(MapLayer layer) {
    LOGGER.debug("Layer added");
    layers.add(layer);
  }
  
  public void saveLayout(final ConfigFile cf, final String prefix) {
    cf.put(prefix + ".longitude", Double.toString(center.x));
    cf.put(prefix + ".latitude", Double.toString(center.y));
    cf.put(prefix + ".scale", Double.toString(scale));
    cf.put(prefix + ".labelSetting", labelSetting.code);
    synchronized (visiblePanels) {
      int waves = 0;
      for (final MapMiniPanel panel : visiblePanels) {
        if (panel.isWaveVisible()) {
          cf.put(prefix + ".wave-" + waves + ".hash",
              Double.toString(panel.getActiveMetadata().getLocationHashCode()));
          panel.saveLayout(cf, prefix + ".wave-" + waves++);
        }
      }
      cf.put(prefix + ".waves", Integer.toString(waves));
    }
  }

  public void processLayout(final ConfigFile cf) {
    final int waves = Integer.parseInt(cf.getString("waves"));
    for (int i = 0; i < waves; i++) {
      final String w = "wave-" + i;
      final double hash = Double.parseDouble(cf.getString(w + ".hash"));
      final ConfigFile scf = cf.getSubConfig(w);
      layouts.put(hash, scf);
    }

    labelSetting = LabelSetting.fromString(cf.getString("labelSetting"));
    final double lon = Double.parseDouble(cf.getString("longitude"));
    final double lat = Double.parseDouble(cf.getString("latitude"));
    final Point2D.Double c = new Point2D.Double(lon, lat);
    final double sc = Double.parseDouble(cf.getString("scale"));
    setCenterAndScale(c, sc);
  }

  public void loadMaps(final boolean redraw) {
    Pair<GeoImageSet, GeoLabelSet> pair;
    if (swarmConfig.useWMS) {
      // TODO: what about GeoLabelSet?
      final WMSGeoImageSet wms = new WMSGeoImageSet();
      wms.setServer(swarmConfig.wmsServer);
      wms.setLayer(swarmConfig.wmsLayer);
      wms.setStyle(swarmConfig.wmsStyles);
      pair = new Pair<GeoImageSet, GeoLabelSet>(wms, new GeoLabelSet());
    } else {
      pair = GeoImageSet.loadMapPacks(swarmConfig.mapPath);
    }
    if (pair != null) {
      images = pair.item1;
      labels = pair.item2;
    }

    if (images == null) {
      LOGGER.warn("No map images found in {}.", swarmConfig.mapPath);
      images = new GeoImageSet();
    }
    images.setArealCacheSort(false);
    final int mp = (int) Math.round(Runtime.getRuntime().maxMemory() / 1024.0 / 1024.0 / 8.0);
    images.setMaxLoadedImagesSize(mp);

    if (redraw)
      resetImage(true);
  }

  private void createUI() {
    loadMaps(false);

    center = new Point2D.Double(swarmConfig.mapLongitude, swarmConfig.mapLatitude);
    scale = swarmConfig.mapScale;

    setLayout(new BorderLayout());
    pane = new JLayeredPane();
    mapImagePanel = new MapImagePanel();
    addMouseWheelListener(new MouseWheelListener() {
      public void mouseWheelMoved(final MouseWheelEvent e) {
        if (e.isControlDown()) {
          final int cnt = -e.getWheelRotation();
          for (final MapMiniPanel panel : miniPanels.values())
            panel.changeSize(cnt);
        }
      }
    });

    addMouseListener(new MapMouseAdapter());

    addMouseMotionListener(new MapMouseMotionListener());

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        mapImagePanel.setSize(pane.getSize());

        resetImage();
        repaint();
      }
    });

    WaveViewTime.addTimeListener(new TimeListener() {
      public void timeChanged(final double j2k) {
        for (final MapMiniPanel panel : miniPanels.values()) {
          if (panel != null && panel.getWaveViewPanel() != null)
            panel.getWaveViewPanel().setCursorMark(j2k);
        }
      }
    });

    addKeyListener(new KeyListener() {
      public void keyPressed(final KeyEvent e) {
        if (allowMultiSelection && e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A) {
          deselectAllPanels();
          synchronized (visiblePanels) {
            for (final MapMiniPanel panel : visiblePanels) {
              if (panel.isWaveVisible())
                addSelectedPanel(panel);
            }
          }
        }
        if (e.isShiftDown() && e.getKeyCode() == KeyEvent.VK_R) {
          resetAllAutoScaleMemory();
        }
      }

      public void keyReleased(final KeyEvent e) {}

      public void keyTyped(final KeyEvent e) {}
    });

    pane.add(mapImagePanel, new Integer(10));
    add(pane, BorderLayout.CENTER);

    loadLabels();

  }

  public void loadLabels() {
    if (swarmConfig.labelSource.isEmpty())
      return;

    try {
      final Class<?> cl = Class.forName(swarmConfig.labelSource);
      final LabelSource src = (LabelSource) cl.newInstance();
      clickableLabels = src.getLabels();
      repaint();
    } catch (final Exception e) {
      LOGGER.warn("Can't load labelSource {}", swarmConfig.labelSource);
      e.printStackTrace();
    }
  }

  public void wavesToClipboard() {
    synchronized (visiblePanels) {
      final WaveClipboardFrame cb = WaveClipboardFrame.getInstance();
      int cnt = 0;
      for (final MapMiniPanel panel : visiblePanels) {
        if (panel.isWaveVisible()) {
          cnt++;
          final WaveViewPanel p = new WaveViewPanel(panel.getWaveViewPanel());
          final SeismicDataSource src = panel.getWaveViewPanel().getDataSource();
          if (src != null)
            p.setDataSource(src.getCopy());

          cb.addWave(p);
        }
      }
      if (cnt > 0) {
        cb.setVisible(true);
        requestFocus();
      }
    }
  }

  public synchronized void deselectAllPanels() {
    for (final MapMiniPanel panel : selectedPanels)
      panel.setSelected(false);
    selectedPanels.clear();
  }

  public synchronized void deselectPanel(final MapMiniPanel p) {
    if (selectedPanels.contains(p)) {
      p.setSelected(false);
      selectedPanels.remove(p);
    }
  }

  public synchronized void addSelectedPanel(final MapMiniPanel p) {
    if (allowMultiSelection) {
      p.setSelected(true);
      selectedPanels.add(p);
    } else
      setSelectedPanel(p);
  }

  public synchronized void setSelectedPanel(final MapMiniPanel p) {
    deselectAllPanels();
    p.setSelected(true);
    MapFrame.getInstance().setSelectedWave(p.getWaveViewPanel());
    selectedPanels.add(p);
  }

  public void setDragMode(final DragMode mode) {
    dragMode = mode;
  }

  public LabelSetting getLabelSetting() {
    return labelSetting;
  }

  public void setLabelSetting(final LabelSetting ls) {
    labelSetting = ls;
    resetImage(false);
  }

  public void mapPush() {
    if (center != null)
      mapHistory.push(new double[] {center.x, center.y, scale});
  }

  public boolean mapPop() {
    if (!mapHistory.isEmpty()) {
      final double[] last = mapHistory.pop();
      center = new Point2D.Double(last[0], last[1]);
      scale = last[2];
      resetImage();
      return true;
    } else
      return false;
  }

  public void timePush() {
    timeHistory.push(new double[] {startTime, endTime});
  }

  public boolean timePop() {
    if (!timeHistory.isEmpty()) {
      final double[] t = timeHistory.pop();
      setTimes(t[0], t[1]);
      return true;
    } else
      return false;
  }

  public void zoom(final double f) {
    mapPush();
    scale *= f;
    resetImage();
  }

  public double getStartTime() {
    return startTime;
  }

  public double getEndTime() {
    return endTime;
  }

  public void scaleTime(final double pct) {
    timePush();
    final double dt = (endTime - startTime) * (1 - pct);
    final double mt = (endTime - startTime) / 2 + startTime;
    startTime = mt - dt / 2;
    endTime = mt + dt / 2;
    setTimes(startTime, endTime, true);
  }

  public void shiftTime(final double pct) {
    timePush();
    final double dt = (endTime - startTime) * pct;
    startTime += dt;
    endTime += dt;
    setTimes(startTime, endTime, true);
  }

  public void gotoTime(final double j2k) {
    timePush();
    final double dt = (endTime - startTime);
    startTime = j2k - dt / 2;
    endTime = j2k + dt / 2;
    setTimes(startTime, endTime, true);
  }

  public Point2D.Double getXY(final double lon, final double lat) {
    if (range == null || projection == null || image == null || renderer == null)
      return null;
    final Point2D.Double xy = projection.forward(new Point2D.Double(lon, lat));
    final double[] ext = range.getProjectedExtents(projection);
    final double dx = (ext[1] - ext[0]);
    final double dy = (ext[3] - ext[2]);
    final Point2D.Double res = new Point2D.Double();
    res.x = (((xy.x - ext[0]) / dx) * renderer.getGraphWidth() + INSET);
    res.y = ((1 - (xy.y - ext[2]) / dy) * renderer.getGraphHeight() + INSET);
    return res;
  }

  public Point2D.Double getLonLat(final int x, final int y) {
    if (range == null || projection == null || renderer == null)
      return null;

    final int tx = x - INSET;
    final int ty = y - INSET;
    final double[] ext = range.getProjectedExtents(projection);
    final double dx = (ext[1] - ext[0]) / renderer.getGraphWidth();
    final double dy = (ext[3] - ext[2]) / renderer.getGraphHeight();
    final double px = tx * dx + ext[0];
    final double py = ext[3] - ty * dy;

    final Point2D.Double pt = projection.inverse(new Point2D.Double(px, py));
    pt.x = pt.x % 360;
    if (pt.x > 180)
      pt.x -= 360;
    if (pt.x < -180)
      pt.x += 360;
    return pt;
  }

  public void setTimes(final double st, final double et) {
    setTimes(st, et, false);
  }

  public void setTimes(final double st, final double et, final boolean repaint) {
    startTime = st;
    endTime = et;
    boolean updated = false;
    synchronized (visiblePanels) {
      for (final MapMiniPanel panel : visiblePanels) {
        if (panel.isWaveVisible()) {
          updated = true;
          panel.updateWave(startTime, endTime, false, repaint);
        }
      }
      if (updated)
        repaint();
    }
  }

  public Point2D.Double getCenter() {
    return center;
  }

  public double getScale() {
    return scale;
  }

  public void setCenterAndScale(final Point2D.Double c, final double s) {
    center = c;
    scale = s;
    resetImage();
  }

  public void setCenterAndScale(final GeoRange gr) {
    mapPush();
    final int width = mapImagePanel.getWidth() - (INSET * 2);
    final int height = mapImagePanel.getHeight() - (INSET * 2);
    center = gr.getCenter();
    final TransverseMercator tm = new TransverseMercator();
    tm.setOrigin(center);
    scale = gr.getScale(tm, width, height) * 1.1;
    if (scale > 6000) {
      final Mercator merc = new Mercator();
      merc.setOrigin(center);
      scale = gr.getScale(merc, width, height) * 1.1;
    }
    resetImage();
  }

  public void pickMapParameters(final int width, final int height) {
    double xm = scale * width;
    double ym = scale * height;
    if (xm > 3000000) {
      // use Mercator
      projection = new Mercator();
      if (xm > Mercator.getMaxWidth()) {
        xm = Mercator.getMaxWidth() * 0.999999;
        scale = xm / width;
        ym = scale * height;
      }
    } else {
      // use Transverse Mercator
      projection = new TransverseMercator();
    }
    projection.setOrigin(center);
    range = projection.getGeoRange(center, xm, ym);
  }

  private Point getLabelPosition(final GeneralPath boxes, final int x, final int y, final int w,
      final int h) {
    final int[] dxy =
        new int[] {x + 5, y - 5, x + 5, y, x + 5, y - 10, x - w - 5, y - 5, x - w - 5, y, x - w - 5,
            y - 10, x + 5, y - 15, x + 5, y + 5, x, y - 15, x, y + 5, x, y - 20, x, y + 10,
            x - w - 5, y - 15, x - w - 5, y + 5, x, y - 20, x + 40, y + 10, x - w - 40, y - 15,};

    for (int i = 0; i < dxy.length / 2; i++) {
      final int px = dxy[i * 2];
      final int py = dxy[i * 2 + 1];
      if (px < 0 || py < 0)
        continue;
      final Rectangle rect = new Rectangle(px, py, w, h);
      if (!boxes.intersects(rect))
        return new Point(px, py);
    }

    return null;
  }

  public int getMissing() {
    return missing;
  }

  public boolean imageValid() {
    return mapImage != null;
  }

  public void resetAllAutoScaleMemory() {
    for (final MapMiniPanel panel : visiblePanels)
      panel.getWaveViewPanel().resetAutoScaleMemory();
  }

  public void resetImage() {
    resetImage(true);
  }

  private void checkLayouts() {
    if (layouts.size() == 0)
      return;

    final Set<Double> ks = layouts.keySet();
    final Set<Double> toRemove = new HashSet<Double>();
    for (final double hash : ks) {
      final MapMiniPanel mmp = miniPanels.get(hash);
      if (mmp != null) {
        mmp.processLayout(layouts.get(hash));
        toRemove.add(hash);
      }
    }
    for (final double hash : toRemove)
      layouts.remove(hash);
  }

  // thoughts:
  // here's what should happen when someone wants to redraw the map
  // -- if the underlying map is being redrawn then that happens first
  // -- all non-event thread processing should be done in one method
  // -- all component adjustments should be done on the event thread
  // after the above is done

  private BufferedImage updateMapRenderer() {
    BufferedImage mi = null;
    final CodeTimer ct = new CodeTimer("whole map");
    try {
      swarmConfig.mapScale = scale;
      swarmConfig.mapLongitude = center.x;
      swarmConfig.mapLatitude = center.y;

      MapFrame.getInstance().getThrobber().increment();

      final int width = mapImagePanel.getWidth() - (INSET * 2);
      final int height = mapImagePanel.getHeight() - (INSET * 2);
      pickMapParameters(width, height);

      LOGGER.debug("map scale: " + scale);
      LOGGER.debug("center: " + center.x + " " + center.y);
      final MapRenderer mr = new MapRenderer(range, projection);
      ct.mark("pre bg");
      image = images.getMapBackground(projection, range, width, scale);
      ct.mark("bg");
      mr.setLocation(INSET, INSET, width);
      mr.setMapImage(image);
      mr.setGeoLabelSet(labels);
      mr.createGraticule(6, true);
      mr.createBox(6); // The black outline of the map

      final File linedir = new File("mapdata/Lines"); // DCK : deal with missing
      // Lines directory
      if (linedir != null) {
        final File[] files = linedir.listFiles();
        if (files != null)
          for (final File f : files)
            if (f.isFile())
              mr.createLine(f.toString());
      }

      mr.createScaleRenderer(1 / projection.getScale(center), INSET, 14);
      final TextRenderer tr = new TextRenderer(mapImagePanel.getWidth() - INSET, 14,
          projection.getName() + " Projection");
      tr.antiAlias = false;
      tr.font = new Font("Arial", Font.PLAIN, 10);
      tr.horizJustification = TextRenderer.RIGHT;
      mr.addRenderer(tr);
      renderer = mr;

      final Plot plot = new Plot();
      plot.setSize(mapImagePanel.getWidth(), mapImagePanel.getHeight());
      plot.addRenderer(renderer);
      ct.mark("pre plot");
      mi = plot.getAsBufferedImage(false);
      ct.mark("plot");
      dragDX = Integer.MAX_VALUE;
      dragDY = Integer.MAX_VALUE;
      ct.stopAndReport();
    } catch (final Exception e) {
      LOGGER.error("Exception during map creation. {}", e);
    } finally {
      MapFrame.getInstance().getThrobber().decrement();
    }
    return mi;
  }

  private Pair<List<JComponent>, List<Line2D.Double>> updateMiniPanels() {
    final List<JComponent> compsToAdd = new ArrayList<JComponent>();
    final List<Line2D.Double> linesToAdd = new ArrayList<Line2D.Double>();

    final FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);

    final GeneralPath boxes = new GeneralPath();
    missing = 0;

    final Map<String, Metadata> allMetadata = swarmConfig.getMetadata();
    synchronized (allMetadata) {
      for (final MapMiniPanel panel : miniPanels.values()) {
        if (panel.getPosition() == MapMiniPanel.Position.MANUAL_SET)
          panel.setPosition(Position.MANUAL_UNSET);
        else
          panel.setPosition(Position.UNSET);
      }
      for (final Metadata md : allMetadata.values()) {
        if (!range.contains(new Point2D.Double(md.getLongitude(), md.getLatitude()))) {
          final MapMiniPanel mmp = miniPanels.get(md.getLocationHashCode());
          if (mmp != null) {
            miniPanels.remove(md.getLocationHashCode());
            deselectPanel(mmp);
          }
        } else {
          MapMiniPanel cmp = miniPanels.get(md.getLocationHashCode());
          final Point2D.Double xy = getXY(md.getLongitude(), md.getLatitude());
          if (xy == null)
            continue;
          final int iconX = (int) xy.x - 8;
          final int iconY = (int) xy.y - 8;
          if (cmp == null || cmp.getPosition() == Position.UNSET
              || cmp.getPosition() == Position.MANUAL_UNSET) {
            final JLabel icon = new JLabel(Icons.bullet);
            icon.setBounds(iconX, iconY, 16, 16);
            compsToAdd.add(icon);
            if (cmp == null)
              cmp = new MapMiniPanel(MapPanel.this);
          }

          if (cmp.getPosition() == Position.UNSET || cmp.getPosition() == Position.MANUAL_UNSET) {
            if (labelSetting == LabelSetting.NONE
                && !layouts.containsKey(md.getLocationHashCode())) {
              if (cmp.getPosition() == Position.UNSET)
                continue;
              if (cmp.getPosition() == Position.MANUAL_UNSET && !cmp.isWaveVisible())
                continue;
            }

            final int w = (int) Math
                .round(MapMiniPanel.FONT.getStringBounds(md.getSCNL().station + 6, frc).getWidth());
            int locX = (int) xy.x;
            int locY = (int) xy.y;
            Point pt = null;
            if (cmp.getPosition() == Position.MANUAL_UNSET) {
              final Point2D.Double mp = cmp.getManualPosition();
              final Point2D.Double xy2 = mp;// getXY(mp.x, mp.y);
              locX = (int) xy2.x;
              locY = (int) xy2.y;
              cmp.setPosition(Position.MANUAL_SET);
              pt = new Point(locX, locY);
            } else
              pt = getLabelPosition(boxes, locX, locY, w, 13);

            if (pt == null && labelSetting == LabelSetting.ALL)
              pt = new Point(locX, locY);

            if (pt != null) {
              locX = pt.x;
              locY = pt.y;
              boxes.append(new Rectangle(locX, locY, w, 13), false);
              cmp.setLocation(locX, locY);
              if (cmp.getPosition() == Position.UNSET)
                cmp.setPosition(Position.AUTOMATIC);

              final Line2D.Double line = new Line2D.Double(locX, locY, iconX + 8, iconY + 8);
              cmp.setLine(line);
              cmp.adjustLine();
              linesToAdd.add(line);

              compsToAdd.add(cmp);
              miniPanels.put(md.getLocationHashCode(), cmp);
            } else {
              missing++;
              cmp.setPosition(Position.HIDDEN);
            }
          }
          cmp.addMetadata(md);
        }
      }
    }
    return new Pair<List<JComponent>, List<Line2D.Double>>(compsToAdd, linesToAdd);
  }

  private final Semaphore lock = new Semaphore(1);

  // this function should not allow reentrancy
  public void resetImage(final boolean doMap) {
    // if there's any problem with the container holding the panel, just
    // forget it.
    if (!MapFrame.getInstance().isVisible() || mapImagePanel.getHeight() == 0
        || mapImagePanel.getWidth() == 0)
      return;

    // first, get the map renderer up and running.
    // this occurs in the construct() method below which does NOT occur
    // on the event thread.
    final SwingWorker worker = new SwingWorker() {
      private List<JComponent> compsToAdd;
      private List<Line2D.Double> linesToAdd;
      private BufferedImage tempMapImage;

      @Override
      public Object construct() {
        try {
          lock.acquire();
        } catch (final InterruptedException ex) {
          return new Boolean(false);
        }

        // if other threads are waiting to update then don't bother
        // continuing
        if (lock.hasQueuedThreads()) {
          return new Boolean(false);
        }

        if (doMap) {
          tempMapImage = updateMapRenderer();
        }

        return new Boolean(true);
      }

      @Override
      public void finished() {
        // if we abort due to queueing, or, in the meantime, another
        // thread has queued then don't bother finishing, the next
        // thread
        // will
        if (tempMapImage != null)
          mapImage = tempMapImage;

        if (((Boolean) this.get()).booleanValue() && !lock.hasQueuedThreads()) {
          // ideally you'd call updateMiniPanels in construct()
          // however they set the position of the panels so has to be
          // done in the event thread
          // TODO: make changes in updateMiniPanels run in non-event
          // thread
          final Pair<List<JComponent>, List<Line2D.Double>> p = updateMiniPanels();
          compsToAdd = p.item1;
          linesToAdd = p.item2;

          if (lines != null)
            lines.clear();
          pane.removeAll();
          pane.add(mapImagePanel, new Integer(10));
          // /
          visiblePanels.clear();
          for (final MapMiniPanel mp : miniPanels.values())
            visiblePanels.add(mp);

          pane.removeAll();
          pane.add(mapImagePanel, new Integer(10));

          if (compsToAdd != null) {
            for (final JComponent comp : compsToAdd) {
              if (comp instanceof JLabel)
                pane.add(comp, new Integer(15));

              else
                pane.add(comp, new Integer(20));
            }
          }
          lines = linesToAdd;
          MapFrame.getInstance().setStatusText(" ");
          checkLayouts();
          // /
          repaint();
        }
        lock.release();
      }
    };
    worker.start();
  }

  private class MapImagePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private void paintRadius(final Graphics2D g2) {
      final Point2D.Double lonLat = getLonLat(mouseNow.x, mouseNow.y);
      final Point2D.Double origin = getLonLat(mouseDown.x, mouseDown.y);
      final double d = Projection.distanceBetween(origin, lonLat);
      final int n = 720;
      final Point2D.Double[] pts = Projection.getPointsFrom(origin, d, n);
      final GeneralPath gp = new GeneralPath();
      Point2D.Double xy = getXY(pts[0].x, pts[0].y);
      Point lastXY = new Point();
      lastXY.x = (int) Math.round(xy.x);
      lastXY.y = (int) Math.round(xy.y);
      gp.moveTo(lastXY.x - 2, lastXY.y - 1);
      for (int i = 1; i <= pts.length; i++) {
        xy = getXY(pts[i % n].x, pts[i % n].y);
        final Point thisXY = new Point();
        thisXY.x = (int) Math.round(xy.x);
        thisXY.y = (int) Math.round(xy.y);
        final double a = thisXY.x - lastXY.x;
        final double b = thisXY.y - lastXY.y;
        final double dist = Math.sqrt(a * a + b * b);
        if (dist > 100)
          gp.moveTo(thisXY.x - 2, thisXY.y - 1);
        else
          gp.lineTo(thisXY.x - 2, thisXY.y - 1);
        lastXY = thisXY;
      }
      g2.setColor(Color.YELLOW);
      g2.draw(gp);
    }

    private void paintGreatCircleRoute(final Graphics2D g2) {
      final Point2D.Double lonLat = getLonLat(mouseNow.x, mouseNow.y);
      Point2D.Double origin = getLonLat(mouseDown.x, mouseDown.y);
      final GeneralPath gp = new GeneralPath();
      Point2D.Double xy = getXY(origin.x, origin.y);
      Point lastXY = new Point();
      lastXY.x = (int) Math.round(xy.x);
      lastXY.y = (int) Math.round(xy.y);
      gp.moveTo(lastXY.x - 2, lastXY.y - 1);
      double d = Projection.distanceBetween(origin, lonLat);
      while (d > 20 * 1000) {
        final double az = Projection.azimuthTo(origin, lonLat);
        final Point2D.Double p0 = Projection.getPointFrom(origin, 20 * 1000, az);

        xy = getXY(p0.x, p0.y);
        final Point thisXY = new Point();
        thisXY.x = (int) Math.round(xy.x);
        thisXY.y = (int) Math.round(xy.y);
        final double a = thisXY.x - lastXY.x;
        final double b = thisXY.y - lastXY.y;
        final double dist = Math.sqrt(a * a + b * b);
        if (dist > 100)
          gp.moveTo(thisXY.x - 2, thisXY.y - 1);
        else
          gp.lineTo(thisXY.x - 2, thisXY.y - 1);
        lastXY = thisXY;

        origin = p0;
        d = Projection.distanceBetween(origin, lonLat);
      }
      g2.setColor(Color.GREEN);
      g2.draw(gp);
    }

    @Override
    public void paintComponent(final Graphics g) {
      if (renderer == null || mapImage == null) {
        final Dimension d = getSize();
        g.drawString("Loading map...", d.width / 2 - 50, d.height / 2);
      } else {
        final Graphics2D g2 = (Graphics2D) g;
        int dx = 0;
        int dy = 0;
        if (dragMode == DragMode.DRAG_MAP && mouseDown != null && mouseNow != null) {
          dx = mouseDown.x - mouseNow.x;
          dy = mouseDown.y - mouseNow.y;
          g2.drawImage(mapImage, -dx, -dy, null);
        } else if (dragDX != Integer.MAX_VALUE && dragDY != Integer.MAX_VALUE) {
          g2.drawImage(mapImage, -dragDX, -dragDY, null);
        } else {
          g2.drawImage(mapImage, 0, 0, null);
        }

        g.setXORMode(Color.WHITE);
        if (lines != null) {
          for (final Line2D.Double line : lines) {
            final Stroke s = g2.getStroke();
            final Color c = g2.getColor();
            g2.setPaintMode();
            g2.setStroke(new BasicStroke(swarmConfig.mapLineWidth));
            g2.setColor(new Color(swarmConfig.mapLineColor));
            g2.draw(line);
            g2.setStroke(s);
            g2.setColor(c);
          }
        }
        g.setPaintMode();

        g.setColor(Color.RED);
        if (dragRectangle != null) {
          if (dragMode == DragMode.BOX && mouseNow != null) {
            dragRectangle.setFrameFromDiagonal(mouseDown, mouseNow);
            g2.draw(dragRectangle);
          } else if (dragMode == DragMode.RULER && mouseDown != null && mouseNow != null) {
            g2.drawLine(mouseDown.x - MapPanel.this.getInsets().left,
                mouseDown.y - MapPanel.this.getInsets().top,
                mouseNow.x - MapPanel.this.getInsets().left,
                mouseNow.y - MapPanel.this.getInsets().top);

            paintRadius(g2);
            paintGreatCircleRoute(g2);
          }
        }

        final Object oldaa = g2.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        final AffineTransform at = g2.getTransform();
        g2.setFont(Font.decode("dialog-plain-12"));
        if (clickableLabels != null) {
          for (final ClickableGeoLabel label : clickableLabels) {
            final Point2D.Double xy = getXY(label.location.x, label.location.y);
            if (xy != null) {
              g2.translate(xy.x - dx, xy.y - dy);
              label.draw(g2);
              g2.translate(-xy.x + dx, -xy.y + dy);
            }
          }
        }

        g2.translate(-dx, -dy);
        for (MapLayer layer : layers) {
          layer.draw(g2, range, projection, renderer.getGraphWidth(), renderer.getGraphHeight(), INSET);
        }
        g2.translate(dx, dy);
        
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, oldaa);
        g2.setTransform(at);
      }
    }
  }

  public GeoRange getRange() {
    return range;
  }

  public class MapMouseAdapter extends MouseAdapter {
    @Override
    public void mouseExited(final MouseEvent e) {
      MapFrame.getInstance().setStatusText(" ");
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
      if (clickableLabels != null) {
        for (final ClickableGeoLabel label : clickableLabels) {
          final Rectangle r = label.getClickBox();
          final Point2D.Double xy = getXY(label.location.x, label.location.y);
          if (xy != null) {
            r.translate((int) xy.x, (int) xy.y);
            if (r.contains(e.getPoint()))
              label.mouseClicked(e);
          }
        }
      }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
      requestFocusInWindow();
      if (SwingUtilities.isRightMouseButton(e)) {
        mapPush();
        center = getLonLat(e.getX(), e.getY());
        resetImage();
      } else {
        if (dragMode == DragMode.DRAG_MAP)
          setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        mouseDown = e.getPoint();
        dragRectangle = new Rectangle();
        dragRectangle.setFrameFromDiagonal(mouseDown, mouseDown);
      }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
      setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
      if (dragMode == DragMode.DRAG_MAP && mouseDown != null && mouseNow != null) {
        mapPush();
        dragDX = mouseDown.x - mouseNow.x;
        dragDY = mouseDown.y - mouseNow.y;
        center = getLonLat(getWidth() / 2 + dragDX, getHeight() / 2 + dragDY);
        resetImage();
      }
      if (dragMode == DragMode.BOX && dragRectangle != null) {
        final Point mouseUp = e.getPoint();
        final int x1 = Math.min(mouseUp.x, mouseDown.x);
        final int x2 = Math.max(mouseUp.x, mouseDown.x);
        final int y1 = Math.min(mouseUp.y, mouseDown.y);
        final int y2 = Math.max(mouseUp.y, mouseDown.y);
        final int dx = x2 - x1;
        final int dy = y2 - y1;
        if (dx > 3 && dy > 3) {
          mapPush();
          final double xs = (double) dx / (double) (getWidth() - INSET * 2);
          final double ys = (double) dy / (double) (getHeight() - INSET * 2);
          scale = scale * Math.max(xs, ys);
          center = getLonLat((int) Math.round(dragRectangle.getCenterX()),
              (int) Math.round(dragRectangle.getCenterY()));
          resetImage();
        }
        dragRectangle = null;

      }
      mouseDown = null;
      mouseNow = null;
      repaint();
    }
  }

  public class MapMouseMotionListener implements MouseMotionListener {

    public void mouseMoved(final MouseEvent e) {
      final Point2D.Double latLon = getLonLat(e.getX(), e.getY());
      if (latLon != null)
        MapFrame.getInstance().setStatusText(Util.lonLatToString(latLon));
    }

    public void mouseDragged(final MouseEvent e) {
      mouseNow = e.getPoint();
      final Point2D.Double lonLat = getLonLat(e.getX(), e.getY());
      if (dragMode == DragMode.DRAG_MAP) {
        if (SwingUtilities.isLeftMouseButton(e) && pane.getComponentCount() != 1) {
          lines.clear();
          pane.removeAll();
          pane.add(mapImagePanel, new Integer(10));

          repaint();
        }
      } else if (dragMode == DragMode.BOX) {
        if (lonLat != null)
          MapFrame.getInstance().setStatusText(Util.lonLatToString(lonLat));
      } else if (dragMode == DragMode.RULER) {
        final Point2D.Double origin = getLonLat(mouseDown.x, mouseDown.y);
        double d = Projection.distanceBetween(origin, lonLat);
        final double az = Projection.azimuthTo(origin, lonLat);
        String label = "m";
        if (d > 10000) {
          d /= 1000;
          label = "km";
        }
        MapFrame.getInstance()
            .setStatusText(String.format("%s to %s, distance: %.1f %s, azimuth: %.2f%c",
                Util.lonLatToString(origin), Util.lonLatToString(lonLat), d, label, az,
                StringUtils.DEGREE_SYMBOL));
      }
      repaint();
    }
  }
}
