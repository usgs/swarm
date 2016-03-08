package gov.usgs.volcanoes.swarm.map.hypocenters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Projection;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.map.ClickableGeoLabel;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.map.MapLayer;
import gov.usgs.volcanoes.swarm.map.MapPanel;

public final class HypocenterPlotter implements MapLayer {

  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
  
  private static final Logger LOGGER = LoggerFactory.getLogger(HypocenterPlotter.class);
  private static final int REFRESH_INTERVAL = 5 * 60 * 1000;
  private List<Hypocenter> hypocenters;
  private final String URL =
      "http://earthquake.usgs.gov/earthquakes/feed/v1.0/summary/2.5_day.quakeml";

  private MapPanel panel;
  private SimpleDateFormat dateFormat;

  public HypocenterPlotter() {
    dateFormat = new SimpleDateFormat(DATE_FORMAT);
    panel = MapFrame.getInstance().addLayer(this);
    startPolling();
  }

  private void startPolling() {
    new Thread() {
      public void run() {
        try {
          hypocenters = pollHypocenters();
          MapFrame.getInstance().repaint();
          try {
            Thread.sleep(REFRESH_INTERVAL);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        } catch (ParserConfigurationException e) {
          LOGGER.debug("ParserConfigurationException while retrieving hypocenters ({})", e);
        } catch (SAXException e) {
          LOGGER.debug("SAXException while retrieving hypocenters ({})", e);
        } catch (IOException e) {
          LOGGER.debug("IOException while retrieving hypocenters ({})", e);
        } catch (DOMException e) {
          LOGGER.debug("DOMException while retrieving hypocenters ({})", e);
        } catch (ParseException e) {
          LOGGER.debug("ParseException while retrieving hypocenters ({})", e);
        }
      }
    }.start();
  }

  private List<Hypocenter> pollHypocenters()
      throws ParserConfigurationException, SAXException, IOException, DOMException, ParseException {
    List<Hypocenter> hypocenters = new ArrayList<Hypocenter>();
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document doc = dBuilder.parse(URL);
    doc.getDocumentElement().normalize();

    NodeList events = doc.getElementsByTagName("event");
    LOGGER.debug("Got {} events.", events.getLength());

    for (int idx = 0; idx < events.getLength(); idx++) {

      Element event = (Element) events.item(idx);
      Element origin = (Element) event.getElementsByTagName("origin").item(0);
      double lon = Double.parseDouble(((Element) origin.getElementsByTagName("longitude").item(0))
          .getElementsByTagName("value").item(0).getTextContent());
      double lat = Double.parseDouble(((Element) origin.getElementsByTagName("latitude").item(0))
          .getElementsByTagName("value").item(0).getTextContent());

      Date date = dateFormat.parse(((Element) origin.getElementsByTagName("time").item(0))
          .getElementsByTagName("value").item(0).getTextContent());
      
      double mag = Double.parseDouble(((Element) event.getElementsByTagName("mag").item(0))
          .getElementsByTagName("value").item(0).getTextContent());

      Hypocenter h = new Hypocenter();
      h.location = new Point2D.Double(lon, lat);
      h.text = "M" + mag;
      h.depth = 0;
      h.time = J2kSec.fromDate(date);
      hypocenters.add(h);
    }
    return hypocenters;
  }

  public void draw(Graphics2D g2) {

    if (hypocenters == null)
      return;

    GeoRange range = panel.getRange();
    Projection projection = panel.getProjection();
    int widthPx = panel.getGraphWidth();
    int heightPx = panel.getGraphHeight();
    int insetPx = panel.getInset();

    for (final ClickableGeoLabel label : hypocenters) {
      final Point2D.Double xy =
          projection.forward(new Point2D.Double(label.location.x, label.location.y));
      final double[] ext = range.getProjectedExtents(projection);
      final double dx = (ext[1] - ext[0]);
      final double dy = (ext[3] - ext[2]);
      final Point2D.Double res = new Point2D.Double();
      res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
      res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);

      g2.translate(res.x, res.y);
      label.draw(g2);
      g2.translate(-res.x, -res.y);

    }
  }

  public boolean mouseClicked(final MouseEvent e) {

    if (hypocenters == null) {
      return false;
    }

    boolean handled = false;

    GeoRange range = panel.getRange();
    Projection projection = panel.getProjection();
    int widthPx = panel.getGraphWidth();
    int heightPx = panel.getGraphHeight();
    int insetPx = panel.getInset();

    for (ClickableGeoLabel label : hypocenters) {
      final Rectangle r = label.getClickBox();


      final Point2D.Double xy =
          projection.forward(new Point2D.Double(label.location.x, label.location.y));
      final double[] ext = range.getProjectedExtents(projection);
      final double dx = (ext[1] - ext[0]);
      final double dy = (ext[3] - ext[2]);
      final Point2D.Double res = new Point2D.Double();
      res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
      res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);

      r.translate((int) res.x, (int) res.y);
      if (r.contains(e.getPoint())) {
        label.mouseClicked(e);
      }
    }

    return handled;
  }
}
