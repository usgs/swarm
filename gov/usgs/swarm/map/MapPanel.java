package gov.usgs.swarm.map;

import gov.usgs.plot.Plot;
import gov.usgs.plot.TextRenderer;
import gov.usgs.plot.map.GeoImageSet;
import gov.usgs.plot.map.MapRenderer;
import gov.usgs.proj.GeoRange;
import gov.usgs.proj.Mercator;
import gov.usgs.proj.Projection;
import gov.usgs.proj.TransverseMercator;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.TimeListener;
import gov.usgs.swarm.map.MapMiniPanel.Position;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.5  2006/07/30 16:16:22  cervelli
 * Added ruler.
 *
 * Revision 1.4  2006/07/28 14:52:12  cervelli
 * Changes for moved GeoRange.
 *
 * Revision 1.3  2006/07/26 22:41:00  cervelli
 * Bunch more development for 2.0.
 *
 * Revision 1.2  2006/07/26 00:39:36  cervelli
 * New resetImage() behavior.
 *
 * @author Dan Cervelli
 */
public class MapPanel extends JPanel
{
	public enum DragMode
	{
		BOX, RULER;
	}
	
	private static final long serialVersionUID = 1L;

	private static final int INSET = 30;
	
	private List<Line2D.Double> lines;
	
	private GeoImageSet images;
	private GeoRange range;
	private Projection projection;
	private RenderedImage image;
	
	private Point2D.Double center;
	private double scale = 100000;
	
	private JLayeredPane pane;
	private MapImagePanel mapImagePanel;
	private BufferedImage mapImage;
	
	private MapRenderer renderer;

	private Map<Double, MapMiniPanel> miniPanels;
	private List<MapMiniPanel> visiblePanels;

	private DragMode dragMode = DragMode.BOX;
	private Point mouseDown;
	private Point mouseNow;
	private Rectangle dragRectangle;
	
	private MapFrame parent;
	
	private Stack<double[]> mapHistory;
	private Stack<double[]> timeHistory;
	
	private int missing;
	
	private Set<MapMiniPanel> selectedPanels;
	
	private double startTime;
	private double endTime;
	
	public MapPanel(MapFrame f)
	{
		parent = f;
		mapHistory = new Stack<double[]>();
		timeHistory = new Stack<double[]>();
		lines = new ArrayList<Line2D.Double>();
		miniPanels = Collections.synchronizedMap(new HashMap<Double, MapMiniPanel>());
		visiblePanels = Collections.synchronizedList(new ArrayList<MapMiniPanel>());
		selectedPanels = new HashSet<MapMiniPanel>();
		
		Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
		this.setCursor(crosshair);
		
		createUI();
	}
	
	private void createUI()
	{
		images = GeoImageSet.loadMapPacks(Swarm.config.mapPath);
		if (images == null)
		{
			System.out.println("No map images found.");
			images = new GeoImageSet();
		}

		center = new Point2D.Double(Swarm.config.mapLongitude, Swarm.config.mapLatitude);
		scale = Swarm.config.mapScale;
		
		setLayout(new BorderLayout());
		pane = new JLayeredPane();
		mapImagePanel = new MapImagePanel();
		addMouseWheelListener(new MouseWheelListener()
				{
					public void mouseWheelMoved(MouseWheelEvent e)
					{
						if (e.isControlDown())
						{
							int cnt = -e.getWheelRotation();
							for (MapMiniPanel panel : miniPanels.values())
								panel.changeSize(cnt);
						}
					}
				});
		
		addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						requestFocusInWindow();
						if (SwingUtilities.isRightMouseButton(e))
						{
							mapPush();
							center = getLonLat(e.getX(), e.getY());
							resetImage();
						}
						else
						{
							mouseDown = e.getPoint();
							dragRectangle = new Rectangle();
							dragRectangle.setFrameFromDiagonal(mouseDown, mouseDown);	
						}
					}
					
					public void mouseReleased(MouseEvent e)
					{
						if (dragMode == DragMode.BOX && dragRectangle != null)
						{
							Point mouseUp = e.getPoint();
							int x1 = Math.min(mouseUp.x, mouseDown.x);
							int x2 = Math.max(mouseUp.x, mouseDown.x);
							int y1 = Math.min(mouseUp.y, mouseDown.y);
							int y2 = Math.max(mouseUp.y, mouseDown.y);
							int dx = x2 - x1;
							int dy = y2 - y1;
							if (dx > 3 && dy > 3)
							{
								mapPush();
								double xs = (double)dx / (double)(getWidth() - INSET * 2);
								double ys = (double)dy / (double)(getHeight() - INSET * 2);
								scale = scale * Math.max(xs, ys);
								center = getLonLat((int)Math.round(dragRectangle.getCenterX()), (int)Math.round(dragRectangle.getCenterY()));
								resetImage();
							}
							dragRectangle = null;
							
						}
						mouseDown = null;
						mouseNow = null;
						repaint();
					}
				});
		
		addMouseMotionListener(new MouseMotionAdapter()
				{
					public void mouseMoved(MouseEvent e)
					{
						Point2D.Double latLon = getLonLat(e.getX(), e.getY());
						if (latLon != null)
							parent.setStatusText(Util.longitudeToString(latLon.x) + " " + Util.latitudeToString(latLon.y));
					}
					
					public void mouseDragged(MouseEvent e)
					{
						mouseNow = e.getPoint();
						Point2D.Double lonLat = getLonLat(e.getX(), e.getY());
						if (dragMode == DragMode.BOX)
						{
							if (lonLat != null)
//								parent.setStatusText(Util.longitudeToString(latLon.x) + " " + Util.latitudeToString(latLon.y));
								parent.setStatusText(Util.lonLatToString(lonLat));
						}
						else if (dragMode == DragMode.RULER)
						{
							Point2D.Double origin = getLonLat(mouseDown.x, mouseDown.y);
							double d = Projection.distanceBetween(origin, lonLat);
							double az = Projection.azimuthTo(origin, lonLat);
							String label = "m";
							if (d > 10000)
							{
								d /= 1000;
								label = "km";
							}
							parent.setStatusText(String.format("%s to %s, distance: %.1f %s, azimuth: %.2f%c", 
									Util.lonLatToString(origin), Util.lonLatToString(lonLat), d, label, az, Util.DEGREE_SYMBOL));
						}
						repaint();
					}
				});
		
		addComponentListener(new ComponentAdapter()
				{
					public void componentResized(ComponentEvent e)
					{
						mapImagePanel.setSize(pane.getSize());
						resetImage();
						repaint();
					}
				});
		
		Swarm.getApplication().addTimeListener(new TimeListener()
				{
					public void timeChanged(double j2k)
					{
						for (MapMiniPanel panel : miniPanels.values())
						{
							if (panel != null && panel.getWaveViewPanel() != null)
								panel.getWaveViewPanel().setCursorMark(j2k);
						}
					}
				});
		
		addKeyListener(new KeyListener()
				{
					public void keyPressed(KeyEvent e)
					{
						if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_A)
						{
							System.out.println("select all");
							deselectAllPanels();
							synchronized (visiblePanels)
							{
								for (MapMiniPanel panel : visiblePanels)
								{
									if (panel.isWaveVisible())
										addSelectedPanel(panel);
								}
							}
						}
					}
		
					public void keyReleased(KeyEvent e)
					{
					}
		
					public void keyTyped(KeyEvent e)
					{
					}
				});

		
		pane.add(mapImagePanel, new Integer(10));
		add(pane, BorderLayout.CENTER);
		resetImage();
	}

	public Throbber getThrobber()
	{
		return parent.getThrobber();
	}
	
	public synchronized void deselectAllPanels()
	{
		for (MapMiniPanel panel : selectedPanels)
			panel.setSelected(false);
		selectedPanels.clear();
	}
	
	public synchronized void deselectPanel(MapMiniPanel p)
	{
		if (selectedPanels.contains(p))
		{
			p.setSelected(false);
			selectedPanels.remove(p);
		}
	}
	
	public synchronized void addSelectedPanel(MapMiniPanel p)
	{
		p.setSelected(true);
		selectedPanels.add(p);
	}
	
	public synchronized void setSelectedPanel(MapMiniPanel p)
	{
		deselectAllPanels();
		p.setSelected(true);
		selectedPanels.add(p);
	}
	
	public void setDragMode(DragMode mode)
	{
		dragMode = mode;
	}
	
	public void mapPush()
	{
		mapHistory.push(new double[] { center.x, center.y, scale });
	}
	
	public void mapPop()
	{
		if (!mapHistory.isEmpty())
		{
			double[] last = mapHistory.pop();
			center = new Point2D.Double(last[0], last[1]);
			scale = last[2];
			resetImage();
		}
	}
	
	public void timePush()
	{
		timeHistory.push(new double[] { startTime, endTime });
	}
	
	public void timePop()
	{
		if (!timeHistory.isEmpty())
		{
			double[] t = timeHistory.pop();
			setTimes(t[0], t[1]);
		}
	}
	
	public void zoom(double f)
	{
		mapPush();
		scale *= f;
		resetImage();
	}
	
	public double getStartTime()
	{
		return startTime;
	}
	
	public double getEndTime()
	{
		return endTime;
	}
	
	public void scaleTime(double pct)
	{
		timePush();
		double dt = (endTime - startTime) * (1 - pct);
		double mt = (endTime - startTime) / 2 + startTime;
		startTime = mt - dt / 2;
		endTime = mt + dt / 2;
		setTimes(startTime, endTime);
	}
	
	public void shiftTime(double pct)
	{
		timePush();
		double dt = (endTime - startTime) * pct;
		startTime += dt;
		endTime += dt;
		setTimes(startTime, endTime);
	}
	
//	public void clear()
//	{
//		lines.clear();
//	}
	
//	public void addLine(Line2D.Double line)
//	{
//		lines.add(line);
//	}
	
	public Point2D.Double getXY(double lon, double lat)
	{
		Point2D.Double xy = projection.forward(new Point2D.Double(lon, lat));
		double[] ext = range.getProjectedExtents(projection);
		double dx = (ext[1] - ext[0]);
		double dy = (ext[3] - ext[2]);
		Point2D.Double res = new Point2D.Double();
		res.x = Math.round(((xy.x - ext[0]) / dx) * renderer.getGraphWidth() + INSET + getInsets().left);
		res.y = Math.round((1 - (xy.y - ext[2]) / dy) * renderer.getGraphHeight() + INSET + getInsets().top);
		return res;
	}
	
	public Point2D.Double getLonLat(int x, int y)
	{
		if (range == null || projection == null || image == null || renderer == null)
			return null;
		int tx = x - INSET - getInsets().left;
		int ty = y - INSET - getInsets().top;
		double[] ext = range.getProjectedExtents(projection);
//		System.out.println("insets: " + getInsets());
//		System.out.println(image.getWidth() + " " + image.getHeight() + renderer.getWidth() + " " + renderer.getGraphWidth());
		double dx = (ext[1] - ext[0]) / renderer.getGraphWidth();
		double dy = (ext[3] - ext[2]) / renderer.getGraphHeight();
//		double px = (x - INSET) * dx + ext[0];
//		double py = ext[3] - (y - INSET) * dy;
		double px = tx * dx + ext[0];
		double py = ext[3] - ty * dy;
//		System.out.println(tx + " " + ty + " " + px + " " + py + " " + ext[0] + " " + ext[1] + " " + ext[2] + " " + ext[3]);
		Point2D.Double pt = projection.inverse(new Point2D.Double(px, py));
		return pt;
	}
	
	public void setTimes(double st, double et)
	{
		startTime = st;
		endTime = et;
		synchronized (visiblePanels)
		{
			for (MapMiniPanel panel : visiblePanels)
				panel.updateWave(startTime, endTime);
		}
	}
	
	public Point2D.Double getCenter()
	{
		return center;
	}
	
	public double getScale()
	{
		return scale;
	}
	
	public void setCenterAndScale(Point2D.Double c, double s)
	{
		center = c;
		scale = s;
		resetImage();
	}
	
	public void setCenterAndScale(GeoRange gr)
	{
		mapPush();
		int width = mapImagePanel.getWidth() - (INSET * 2);
		int height = mapImagePanel.getHeight() - (INSET * 2);
		center = gr.getCenter();
		TransverseMercator tm = new TransverseMercator();
		tm.setOrigin(center);
		scale = gr.getScale(tm, width, height) * 1.1;
		if (scale > 6000)
		{
			Mercator merc = new Mercator();
			merc.setOrigin(center);
			scale = gr.getScale(merc, width, height) * 1.1;
		}
		resetImage();
	}
	
	public void pickMapParameters(int width, int height)
    {
		double xm = scale * (double)width;
		double ym = scale * (double)height;
		
		if (xm > 3000000)
		{
			// use Mercator
			projection = new Mercator();
			projection.setOrigin(center);
			if (xm > Mercator.getMaxWidth())
			{
				xm = Mercator.getMaxWidth() * 0.999999;
				scale = xm / width;
				ym = scale * (double)height;
			}
//			range = new GeoRange(projection, center, xm, ym);
			range = projection.getGeoRange(center, xm, ym);
		}
		else
		{
			// use Transverse Mercator
			TransverseMercator tm = new TransverseMercator();
			tm.setOrigin(center);
			projection = tm;
//			range = new GeoRange(projection, center, xm, ym);
			System.out.println("xm: " + xm + " ym: " + ym);
			range = projection.getGeoRange(center, xm, ym);
		}
    }
	
	private Point getLabelPosition(GeneralPath boxes, int x, int y, int w, int h)
	{
		int[] dxy = new int[] {
					x + 5, y - 5,
					x + 5, y,
					x + 5, y - 10,
					x - w - 5, y - 5,
					x - w - 5, y,
					x - w - 5, y - 10,
					x + 5, y - 15,
					x + 5, y + 5,
					x, y - 15,
					x, y + 5,
					x, y - 20,
					x, y + 10,
					x - w - 5, y - 15,
					x - w - 5, y + 5,
					x, y - 20,
					x + 40, y + 10,
					x - w - 40, y - 15,
				};
		
		for (int i = 0; i < dxy.length / 2; i++)
		{
			int px = dxy[i * 2];
			int py = dxy[i * 2 + 1];
			if (px < 0 || py < 0)
				continue;
			Rectangle rect = new Rectangle(px, py, w, h);
			if (!boxes.intersects(rect))
				return new Point(px, py);
		}
		
		return null;
	}
	
	public int getMissing()
	{
		return missing;
	}
	
	public void resetImage()
	{
		if (mapImagePanel.getHeight() == 0 || mapImagePanel.getWidth() == 0)
			return;
		
		final SwingWorker worker = new SwingWorker()
		{
			public Object construct()
			{
				Swarm.config.mapScale = scale;
				Swarm.config.mapLongitude = center.x;
				Swarm.config.mapLatitude = center.y;
				
				parent.getThrobber().increment();
				
				int width = mapImagePanel.getWidth() - (INSET * 2);
				int height = mapImagePanel.getHeight() - (INSET * 2);
				
				pickMapParameters(width, height);
				
//				System.out.println("proj: " + projection.getName());
//				System.out.println("center: " + center);
				System.out.println("scale: " + scale);
				System.out.println("final range: " + range);
				
				MapRenderer mr = new MapRenderer(range, projection);
				image = images.getMapBackground(projection, range, width, scale);
				System.out.println("WH: " + image.getWidth() + " " + image.getHeight() + " " + width + " " + height);
				mr.setLocation(INSET, INSET, width);
				mr.setMapImage(image);
				mr.createGraticule(6, true);
				mr.createBox(6);
				mr.createScaleRenderer(1 / projection.getScale(center), INSET, 14);
				TextRenderer tr = new TextRenderer(mapImagePanel.getWidth() - INSET, 14, projection.getName() + " Projection");
				tr.antiAlias = false;
				tr.font = new Font("Arial", Font.PLAIN, 10);
				tr.horizJustification = TextRenderer.RIGHT;
				mr.addRenderer(tr);
				renderer = mr;
				
				Plot plot = new Plot();
				plot.setSize(mapImagePanel.getWidth(), mapImagePanel.getHeight());
				plot.addRenderer(renderer);
				
				mapImage = plot.getAsBufferedImage(false);
				
				return null;
			}
			
			public void finished()
			{
				System.out.println("finished");
				pane.removeAll();
				pane.add(mapImagePanel, new Integer(10));
				placeMiniPanels();
				repaint();
				parent.getThrobber().decrement();
			}
		};
		worker.start();
	}
	
	/**
	 * To be called after the icons and panels have been removed from the 
	 * pane.
	 * 
	 * @return
	 */
	private void placeMiniPanels()
	{
		final List<JComponent> compsToAdd = new ArrayList<JComponent>();
		final List<Line2D.Double> linesToAdd = new ArrayList<Line2D.Double>();
		
		final SwingWorker worker = new SwingWorker()
		{
			public Object construct()
			{
				lines.clear();
				// TODO: don't recreate every time
				FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
				
				GeneralPath boxes = new GeneralPath();
				missing = 0;
				for (MapMiniPanel panel : miniPanels.values())
				{
					if (panel.getPosition() == MapMiniPanel.Position.MANUAL_SET)
						panel.setPosition(Position.MANUAL_UNSET);
					else
						panel.setPosition(Position.UNSET);
				}
				// TODO: synch problem
				for (Metadata md : Swarm.config.metadata.values())
				{
					if (!range.contains(new Point2D.Double(md.longitude, md.latitude)))
					{
						MapMiniPanel mmp = miniPanels.get(md.getLocationHashCode());
						if (mmp != null)
						{
							miniPanels.remove(md.getLocationHashCode());
							deselectPanel(mmp);
						}
					}
					else
					{
						MapMiniPanel cmp = miniPanels.get(md.getLocationHashCode());
						Point2D.Double xy = getXY(md.longitude, md.latitude);
						int iconX = (int)xy.x - 8;
						int iconY = (int)xy.y - 8;
						if (cmp == null || cmp.getPosition() == Position.UNSET || cmp.getPosition() == Position.MANUAL_UNSET)
						{
							final JLabel icon = new JLabel(Images.getIcon("bullet"));
							icon.setBounds(iconX, iconY, 16, 16);
							compsToAdd.add(icon);
							if (cmp == null)
								cmp = new MapMiniPanel(MapPanel.this);
						}
						
						if (cmp.getPosition() == Position.UNSET || cmp.getPosition() == Position.MANUAL_UNSET)
						{
							int w = (int)Math.round(MapMiniPanel.FONT.getStringBounds(md.scnl.station + 6, frc).getWidth());
							int locX = (int)xy.x;
							int locY = (int)xy.y;
							Point pt = null;
							if (cmp.getPosition() == Position.MANUAL_UNSET)
							{
								Point2D.Double mp = cmp.getManualPosition();
								Point2D.Double xy2 = mp;//getXY(mp.x, mp.y);
								locX = (int)xy2.x;
								locY = (int)xy2.y;
								cmp.setPosition(Position.MANUAL_SET);
								pt = new Point(locX, locY);
							}
							else
								pt = getLabelPosition(boxes, locX, locY, w, MapMiniPanel.LABEL_HEIGHT);
							
							if (pt != null)
							{
								locX = pt.x;
								locY = pt.y;
								boxes.append(new Rectangle(locX, locY, w, MapMiniPanel.LABEL_HEIGHT), false);
								cmp.setLocation(locX, locY);
								if (cmp.getPosition() == Position.UNSET)
									cmp.setPosition(Position.AUTOMATIC);
			
								Line2D.Double line = new Line2D.Double(locX, locY, iconX + 8, iconY + 8);
								cmp.setLine(line);
								cmp.adjustLine();
								linesToAdd.add(line);
								
								compsToAdd.add(cmp);
								miniPanels.put(md.getLocationHashCode(), cmp);
							}
							else
							{
								missing++;
								cmp.setPosition(Position.HIDDEN);
							}
						}
						cmp.addMetadata(md);
					}
				}
				
				visiblePanels.clear();
				for (MapMiniPanel mp : miniPanels.values())
					visiblePanels.add(mp);
					
				return null;
			}
			
			public void finished()
			{
				pane.removeAll();
				pane.add(mapImagePanel, new Integer(10));
				for (JComponent comp : compsToAdd)
				{
					if (comp instanceof JLabel)
						pane.add(comp, new Integer(15));
					else
						pane.add(comp, new Integer(20));
				}
				lines = linesToAdd;
				repaint();
			}
			
		};
		worker.start();
	}
	
	private class MapImagePanel extends JPanel
	{
		private static final long serialVersionUID = 1L;

		public void paint(Graphics g)
		{
			super.paint(g);
			if (renderer == null)
			{
				Dimension d = getSize();
				g.drawString("Loading map...", d.width / 2 - 50, d.height / 2);
			}
			else
			{
				Graphics2D g2 = (Graphics2D)g;
				g2.drawImage(mapImage, 0, 0, null);
				
				g.setColor(Color.BLACK);
				// TODO: synch
				for (Line2D.Double line : lines)
					g2.draw(line);
				
				g.setColor(Color.RED);
				if (dragRectangle != null)
				{
					if (dragMode == DragMode.BOX && mouseNow != null)
					{
						dragRectangle.setFrameFromDiagonal(mouseDown, mouseNow);
						g2.draw(dragRectangle);
					}
					else if (dragMode == DragMode.RULER && mouseDown != null && mouseNow != null)
					{
						g2.drawLine(
								mouseDown.x - MapPanel.this.getInsets().left, 
								mouseDown.y - MapPanel.this.getInsets().top,
								mouseNow.x - MapPanel.this.getInsets().left,
								mouseNow.y - MapPanel.this.getInsets().top);
					}
				}
			}
		}
	}
}
