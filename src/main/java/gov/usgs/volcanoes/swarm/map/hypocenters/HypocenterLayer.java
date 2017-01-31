package gov.usgs.volcanoes.swarm.map.hypocenters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gov.usgs.plot.render.DataPointRenderer;
import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Projection;
import gov.usgs.volcanoes.core.CodeTimer;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.swarm.ConfigListener;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.event.Event;
import gov.usgs.volcanoes.swarm.event.Magnitude;
import gov.usgs.volcanoes.swarm.event.Origin;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.map.MapLayer;
import gov.usgs.volcanoes.swarm.map.MapPanel;

public final class HypocenterLayer implements MapLayer, ConfigListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(HypocenterLayer.class);
  private static final int REFRESH_INTERVAL = 5 * 60 * 1000;
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
  private static final int ONE_HOUR = 60 * 60 * 1000;
  private static final int ONE_DAY = ONE_HOUR * 24;
  private static final int ONE_WEEK = ONE_DAY * 7;

  private static final int POPUP_PADDING = 2;

  // private List<Hypocenter> hypocenters;
  private final Map<String, Event> events;
  private boolean run = true;

  private MapPanel panel;
  private SimpleDateFormat dateFormat;

  private Thread refreshThread;
  private final SwarmConfig swarmConfig;
  private final DataPointRenderer renderer;
  private Event hoverEvent;
  private Point hoverLocation;

  public HypocenterLayer() {
    // events = new HashMap<String, Event>();
    events = new ConcurrentHashMap<String, Event>();
    dateFormat = new SimpleDateFormat(DATE_FORMAT);
    swarmConfig = SwarmConfig.getInstance();
    swarmConfig.addListener(this);

    renderer = new DataPointRenderer();
    renderer.antiAlias = true;
    renderer.stroke = new BasicStroke(2f);
    renderer.filled = true;
    renderer.color = Color.BLACK;
    // r.shape = Geometry.STAR_10;
    renderer.shape = new Ellipse2D.Float(0f, 0f, 7f, 7f);

    startPolling();
  }

  public void setMapPanel(MapPanel mapPanel) {
    panel = mapPanel;
  }

  private void startPolling() {

    Runnable r = new Runnable() {
      public void run() {
        while (run) {
          try {
            pollHypocenters();
          } catch (ParserConfigurationException e) {
            LOGGER.debug("ParserConfigurationException while retrieving hypocenters ({})", e);
          } catch (SAXException e) {
            events.clear();
            LOGGER.debug(
                "SAXException while retrieving hypocenters. Typically this means there were no hypocenters to display.");
          } catch (IOException e) {
            LOGGER.debug("IOException while retrieving hypocenters ({})", e);
          } catch (DOMException e) {
            LOGGER.debug("DOMException while retrieving hypocenters ({})", e);
          } catch (ParseException e) {
            LOGGER.debug("ParseException while retrieving hypocenters ({})", e);
          }

          MapFrame.getInstance().repaint();

          try {
            Thread.sleep(REFRESH_INTERVAL);
          } catch (InterruptedException e) {
            LOGGER.debug("hypocenter update interupted.");
          }

        }
      }
    };
    refreshThread = new Thread(r);
    refreshThread.start();
  }

  private void pollHypocenters()
      throws ParserConfigurationException, SAXException, IOException, DOMException, ParseException {
    events.clear();
    HypocenterSource hypocenterSource = swarmConfig.getHypocenterSource();
    if (hypocenterSource == HypocenterSource.NONE) {
      return;
    }

    LOGGER.debug("Polling hypocenters");
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(hypocenterSource.getUrl());
    doc.getDocumentElement().normalize();

    NodeList eventElements = doc.getElementsByTagName("event");
    LOGGER.debug("Got {} events.", eventElements.getLength());
    int eventCount = eventElements.getLength();
    for (int idx = 0; idx < eventCount; idx++) {
      Event event = new Event((Element) eventElements.item(idx));
      events.put(event.publicId, event);
    }
  }

  public void draw(Graphics2D g2) {
    if (events.size() < 1)
      return;

    GeoRange range = panel.getRange();
    Projection projection = panel.getProjection();
    int widthPx = panel.getGraphWidth();
    int heightPx = panel.getGraphHeight();
    int insetPx = panel.getInset();

    for (final Event event : events.values()) {
      Origin origin = event.getPreferredOrigin();

      Point2D.Double originLoc = new Point2D.Double(origin.getLongitude(), origin.getLatitude());
      if (!range.contains(originLoc)) {
        continue;
      }

      final Point2D.Double xy = projection.forward(originLoc);

      final double[] ext = range.getProjectedExtents(projection);
      final double dx = (ext[1] - ext[0]);
      final double dy = (ext[3] - ext[2]);
      final Point2D.Double res = new Point2D.Double();
      res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
      res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);

      g2.translate(res.x, res.y);
      long age = J2kSec.asEpoch(J2kSec.now()) - origin.getTime();
      if (event == hoverEvent) {
        renderer.paint = Color.GREEN;
      } else if (age < ONE_HOUR) {
        renderer.paint = Color.RED;
      } else if (age < ONE_DAY) {
        renderer.paint = Color.ORANGE;
      } else if (age < ONE_WEEK) {
        renderer.paint = Color.YELLOW;
      } else {
        renderer.paint = Color.WHITE;
      }
      renderer.renderAtOrigin(g2);

      if (event == hoverEvent) {
      }
      g2.translate(-res.x, -res.y);
    }

    drawPopup(g2);
  }

  private void drawPopup(Graphics2D g2) {
    if (hoverEvent == null) {
      return;
    }
    Origin origin = hoverEvent.getPreferredOrigin();
    GeoRange range = panel.getRange();
    Projection projection = panel.getProjection();
    List<String> text = generatePopupText(origin);
    
    FontMetrics fm = g2.getFontMetrics();
    int popupHeight = 2 * POPUP_PADDING;
    int popupWidth = 0;
    for (String string : text) {
      Rectangle2D bounds = fm.getStringBounds(string, g2);
      popupHeight += (int) (Math.ceil(bounds.getHeight()) + 2);
      popupWidth = Math.max(popupWidth, (int) (Math.ceil(bounds.getWidth()) + 2 * POPUP_PADDING));
    }

    int widthPx = panel.getGraphWidth();
    int heightPx = panel.getGraphHeight();
    int insetPx = panel.getInset();

    final Point2D.Double xy =
        projection.forward(new Point2D.Double(origin.getLongitude(), origin.getLatitude()));
    final double[] ext = range.getProjectedExtents(projection);
    final double dx = (ext[1] - ext[0]);
    final double dy = (ext[3] - ext[2]);
    final Point2D.Double res = new Point2D.Double();
    
    res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
    int maxX = widthPx - popupWidth + POPUP_PADDING;
    res.x = Math.min(res.x, maxX);
    
    res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);
    if (res.y < ( insetPx + popupHeight)) {
      res.y += popupHeight;
    }

    g2.translate(res.x, res.y);

    g2.setStroke(new BasicStroke(1.2f));
    g2.setColor(new Color(0, 0, 0, 128));
    g2.drawRect(0, -(popupHeight - POPUP_PADDING), popupWidth, popupHeight);
    g2.fillRect(0, -(popupHeight - POPUP_PADDING), popupWidth, popupHeight);

    g2.setColor(Color.WHITE);
    int baseY = POPUP_PADDING;
    for (String string : text) {
      g2.drawString(string, POPUP_PADDING, -baseY);
      Rectangle2D bounds = fm.getStringBounds(string, g2);
      baseY += (int) (Math.ceil(bounds.getHeight()) + 2);
    }
    g2.translate(-res.x, -res.y);

  }

  private List<String> generatePopupText(Origin origin) {
    List<String> text = new ArrayList<String>(3);
    
    Magnitude magElement = hoverEvent.getPerferredMagnitude();
    String mag = String.format("%.2f %s at %.2f km depth", magElement.getMag(),
        magElement.getType(), (origin.getDepth() / 1000));
    text.add(mag);

    String date = Time.format(Time.STANDARD_TIME_FORMAT, new Date(origin.getTime()));
    text.add(date + " UTC");

    String description = hoverEvent.getDescription();
    text.add(description);

    return text;
  }

  public boolean mouseClicked(final MouseEvent e) {

    hoverEvent = null;
    if (events.size() < 1l) {
      return false;
    }

    GeoRange range = panel.getRange();
    Projection projection = panel.getProjection();
    int widthPx = panel.getGraphWidth();
    int heightPx = panel.getGraphHeight();
    int insetPx = panel.getInset();

    Iterator<Event> it = events.values().iterator();
    boolean handled = false;
    while (it.hasNext() && handled == false) {
      Event event = it.next();
      Origin origin = event.getPreferredOrigin();
      final Rectangle r = new Rectangle(-7, -7, 17, 17);

      final Point2D.Double xy =
          projection.forward(new Point2D.Double(origin.getLongitude(), origin.getLatitude()));
      final double[] ext = range.getProjectedExtents(projection);
      final double dx = (ext[1] - ext[0]);
      final double dy = (ext[3] - ext[2]);
      final Point2D.Double res = new Point2D.Double();
      res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
      res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);

      r.translate((int) res.x, (int) res.y);

      if (r.contains(e.getPoint())) {
        LOGGER.debug("event clicked");
        Swarm.openEvent(event);
        return true;
      }
    }

    return handled;
  }


  public void settingsChanged() {
    LOGGER.debug("hypocenter plotter sees changed settings.");
    refreshThread.interrupt();
  }

  public void stop() {
    run = false;
    refreshThread.interrupt();
  }

  public boolean mouseMoved(MouseEvent e) {
    if (events.size() < 1) {
      return false;
    }

    GeoRange range = panel.getRange();
    Projection projection = panel.getProjection();
    if (projection == null) {
      return false;
    }
    
    int widthPx = panel.getGraphWidth();
    int heightPx = panel.getGraphHeight();
    int insetPx = panel.getInset();

    Iterator<Event> it = events.values().iterator();
    boolean handled = false;
    while (it.hasNext() && handled == false) {
      Event event = it.next();
      Origin origin = event.getPreferredOrigin();
      if (origin == null) {
        continue;
      }

      Point2D.Double originLoc = new Point2D.Double(origin.getLongitude(), origin.getLatitude());
      if (!range.contains(originLoc)) {
        continue;
      }
      
      final Rectangle r = new Rectangle(0, 0, 10, 10);

      
      final Point2D.Double xy =
          projection.forward(new Point2D.Double(origin.getLongitude(), origin.getLatitude()));
      final double[] ext = range.getProjectedExtents(projection);
      final double dx = (ext[1] - ext[0]);
      final double dy = (ext[3] - ext[2]);
      final Point2D.Double res = new Point2D.Double();
      res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
      res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);

      r.translate((int) res.x, (int) res.y);
      if (r.contains(e.getPoint())) {
        LOGGER.debug("set hover event {}", event.publicId);
        hoverEvent = event;
        handled = true;
      } else if (event == hoverEvent) {
        LOGGER.debug("unset hover event {}", event.publicId);

        hoverEvent = null;
        hoverLocation = e.getPoint();
        handled = true;
      }
    }
    return handled;
  }
}
