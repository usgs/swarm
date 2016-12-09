package gov.usgs.volcanoes.swarm.map.hypocenters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gov.usgs.plot.render.DataPointRenderer;
import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Projection;
import gov.usgs.volcanoes.core.quakeml.Event;
import gov.usgs.volcanoes.core.quakeml.Magnitude;
import gov.usgs.volcanoes.core.quakeml.Origin;
import gov.usgs.volcanoes.core.quakeml.QuakemlObserver;
import gov.usgs.volcanoes.core.quakeml.QuakemlSource;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.swarm.ConfigListener;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.map.MapFrame;
import gov.usgs.volcanoes.swarm.map.MapLayer;
import gov.usgs.volcanoes.swarm.map.MapPanel;

public final class HypocenterLayer implements MapLayer, ConfigListener, QuakemlObserver {
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

	private final SwarmConfig swarmConfig;
	private final DataPointRenderer renderer;
	private QuakemlSource quakemlSource;
	private Event hoverEvent;
	private Point hoverLocation;

	public HypocenterLayer() throws MalformedURLException {
		events = new ConcurrentHashMap<String, Event>();
		dateFormat = new SimpleDateFormat(DATE_FORMAT);
		swarmConfig = SwarmConfig.getInstance();
		swarmConfig.addListener(this);

		renderer = new DataPointRenderer();
		renderer.antiAlias = true;
		renderer.stroke = new BasicStroke(1f);
		renderer.filled = true;
		renderer.color = Color.BLACK;
		// r.shape = Geometry.STAR_10;
		renderer.shape = new Ellipse2D.Float(0f, 0f, 5f, 5f);
		HypocenterSource hypocenterSource = swarmConfig.getHypocenterSource();

		URL quakemlUrl = new URL(hypocenterSource.getUrl());
		if (quakemlUrl != null) {
			quakemlSource = new QuakemlSource(quakemlUrl, (long) REFRESH_INTERVAL);
			quakemlSource.addObserver(this);
			update(quakemlSource);
		}
	}

	public void setMapPanel(MapPanel mapPanel) {
		panel = mapPanel;
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

			double mag = event.getPerferredMagnitude().getMag();
			float diameter;
			// float diameter = (float) (2.5 + mag * 9 / 2.5);
			// diameter = Math.min(diameter, 10);
			// diameter = Math.max(diameter, 1);

			if (mag > 7) {
				diameter = 25;
			} else if (mag > 6) {
				diameter = 21;
			} else if (mag > 5) {
				diameter = 17;
			} else if (mag > 4) {
				diameter = 13;
			} else if (mag > 3) {
				diameter = 11;
			} else if (mag > 2) {
				diameter = 9;
			} else if (mag > 1) {
				diameter = 7;
			} else {
				diameter = 5;
			}

			long age = J2kSec.asEpoch(J2kSec.now()) - origin.getTime();
			renderer.shape = new Ellipse2D.Float(0f, 0f, diameter, diameter);
			Color markerColor;
			if (event == hoverEvent) {
				markerColor = Color.GREEN;
			} else if (age < ONE_HOUR) {
				markerColor = Color.RED;
			} else if (age < ONE_DAY) {
				// renderer.paint = Color.CYAN;
				markerColor = Color.ORANGE;
			} else if (age < ONE_WEEK) {
				markerColor = Color.YELLOW;
			} else {
				markerColor = Color.WHITE;
			}

			int alpha = 0x80FFFFFF;
			renderer.paint = new Color(alpha & markerColor.getRGB(), true);
			renderer.renderAtOrigin(g2);

//			if (event == hoverEvent) {
//			}
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

		final Point2D.Double xy = projection.forward(new Point2D.Double(origin.getLongitude(), origin.getLatitude()));
		final double[] ext = range.getProjectedExtents(projection);
		final double dx = (ext[1] - ext[0]);
		final double dy = (ext[3] - ext[2]);
		final Point2D.Double res = new Point2D.Double();

		res.x = (((xy.x - ext[0]) / dx) * widthPx + insetPx);
		int maxX = widthPx - popupWidth + POPUP_PADDING;
		res.x = Math.min(res.x, maxX);

		res.y = ((1 - (xy.y - ext[2]) / dy) * heightPx + insetPx);
		if (res.y < (insetPx + popupHeight)) {
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
		String mag = String.format("%.2f %s at %.2f km depth", magElement.getMag(), magElement.getType(),
				(origin.getDepth() / 1000));
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

			final Point2D.Double xy = projection
					.forward(new Point2D.Double(origin.getLongitude(), origin.getLatitude()));
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
		HypocenterSource hypocenterSource = swarmConfig.getHypocenterSource();
		if (quakemlSource == null) {
			return;
		}
		try {
			quakemlSource.stop();
			quakemlSource = new QuakemlSource(new URL(hypocenterSource.getUrl()), (long) REFRESH_INTERVAL);
			quakemlSource.addObserver(this);
			update(quakemlSource);
		} catch (MalformedURLException ex) {
			LOGGER.error("Unable to load hypocenter URL.", ex);
		}
	}

	public void stop() {
		run = false;
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

			final Point2D.Double xy = projection
					.forward(new Point2D.Double(origin.getLongitude(), origin.getLatitude()));
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

	public void update(QuakemlSource source) {
		events.clear();
		events.putAll(source.getEventSet());
		if (MapFrame.getInstance() != null) {
			MapFrame.getInstance().repaint();
		}
	}
}
