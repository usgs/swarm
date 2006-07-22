package gov.usgs.swarm.map;

import gov.usgs.plot.Plot;
import gov.usgs.plot.map.GeoImageSet;
import gov.usgs.plot.map.GeoRange;
import gov.usgs.plot.map.MapRenderer;
import gov.usgs.proj.Mercator;
import gov.usgs.proj.Projection;
import gov.usgs.proj.TransverseMercator;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.TimeListener;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * 
 * @author Dan Cervelli
 */
public class MapPanel extends JPanel
{
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
	
	private Point mouseDown;
	private Rectangle dragRectangle;
	
	private MapFrame parent;
	
	private Stack<double[]> history;
	
	private int missing;
	
	public MapPanel(MapFrame f)
	{
		parent = f;
		history = new Stack<double[]>();
		lines = new ArrayList<Line2D.Double>();
		miniPanels = new HashMap<Double, MapMiniPanel>();
		createUI();
	}
	
	private void createUI()
	{
		images = GeoImageSet.loadMapPacks("c:\\mapdata");
		
//		images = new GeoImageSet("c:\\mapdata\\index.txt");

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
						if (SwingUtilities.isRightMouseButton(e))
						{
//							scale *= 1.2;
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
						if (dragRectangle != null)
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
								push();
								double xs = (double)dx / (double)(getWidth() - INSET * 2);
								double ys = (double)dy / (double)(getHeight() - INSET * 2);
								scale = scale * Math.max(xs, ys);
								center = getLonLat((int)Math.round(dragRectangle.getCenterX()), (int)Math.round(dragRectangle.getCenterY()));
								resetImage();
							}
							dragRectangle = null;
							repaint();
						}
						
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
						if (dragRectangle != null)
						{
							dragRectangle.setFrameFromDiagonal(mouseDown, e.getPoint());
							repaint();
						}
						Point2D.Double latLon = getLonLat(e.getX(), e.getY());
						if (latLon != null)
							parent.setStatusText(Util.longitudeToString(latLon.x) + " " + Util.latitudeToString(latLon.y));
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
		
		pane.add(mapImagePanel, new Integer(10));
		add(pane, BorderLayout.CENTER);
		resetImage();
	}
	
	public void push()
	{
		history.push(new double[] { center.x, center.y, scale });
	}
	
	public void back()
	{
		if (!history.isEmpty())
		{
			double[] last = history.pop();
			center = new Point2D.Double(last[0], last[1]);
			scale = last[2];
			resetImage();
		}
	}
	
	public void zoom(double f)
	{
		push();
		scale *= f;
		resetImage();
	}
	
	public void clear()
	{
		lines.clear();
	}
	
	public void addLine(Line2D.Double line)
	{
		lines.add(line);
	}
	
	private Point2D.Double getXY(double lon, double lat)
	{
		Point2D.Double xy = projection.forward(new Point2D.Double(lon, lat));
		double[] ext = range.getProjectedExtents(projection);
		double dx = (ext[1] - ext[0]);
		double dy = (ext[3] - ext[2]);
		Point2D.Double res = new Point2D.Double();
		res.x = ((xy.x - ext[0]) / dx) * image.getWidth() + INSET;
		res.y = (1 - (xy.y - ext[2]) / dy) * image.getHeight() + INSET;
		return res;
	}
	
	private Point2D.Double getLonLat(int x, int y)
	{
		if (range == null || projection == null || image == null)
			return null;
		double[] ext = range.getProjectedExtents(projection);
		double dx = (ext[1] - ext[0]) / image.getWidth();
		double dy = (ext[3] - ext[2]) / image.getHeight();
		double px = (x - INSET) * dx + ext[0];
		double py = ext[3] - (y - INSET) * dy;
		Point2D.Double pt = projection.inverse(new Point2D.Double(px, py));
		return pt;
	}
	
	public void refresh(double st, double et)
	{
//		throbber.increment();
//		double now = CurrentTime.getInstance().nowJ2K();
//		double start = now - 1 * 60;
		for (MapMiniPanel panel : miniPanels.values())
			panel.updateWave(st, et);
//		throbber.decrement();
	}
	
	public void setCenterAndScale(GeoRange gr)
	{
		push();
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
			range = new GeoRange(projection, center, xm, ym);
		}
		else
		{
			// use Transverse Mercator
			TransverseMercator tm = new TransverseMercator();
			tm.setOrigin(center);
			projection = tm;
			range = new GeoRange(projection, center, xm, ym);
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
//				System.out.println("final range: " + range);
				
				MapRenderer mr = new MapRenderer(range, projection);
				image = images.getMapBackground(projection, range, width, scale);
				mr.setLocation(INSET, INSET, width);
				mr.setMapImage(image);
				mr.createGraticule(6, true);
				mr.createBox(6);
				renderer = mr;
				
				Plot plot = new Plot();
				plot.setSize(mapImagePanel.getWidth(), mapImagePanel.getHeight());
				plot.addRenderer(renderer);
				mapImage = plot.getAsBufferedImage(false);
				
				clear();
				miniPanels.clear();
				FontRenderContext frc = new FontRenderContext(new AffineTransform(), false, false);
				final List<JComponent> comps = new ArrayList<JComponent>();
				GeneralPath boxes = new GeneralPath();
				missing = 0;
				for (Metadata md : Swarm.config.metadata.values())
				{
					if (range.contains(new Point2D.Double(md.longitude, md.latitude)))
					{
						MapMiniPanel cmp = miniPanels.get(md.getLocationHashCode());
						if (cmp == null)
						{
							cmp = new MapMiniPanel();
							int w = (int)Math.round(MapMiniPanel.FONT.getStringBounds(md.scnl.station + 6, frc).getWidth());
							Point2D.Double xy = getXY(md.longitude, md.latitude);
							int locX = (int)xy.x;
							int locY = (int)xy.y;
							Point pt = getLabelPosition(boxes, locX, locY, w, MapMiniPanel.LABEL_HEIGHT);
							if (pt != null)
							{
								locX = pt.x;
								locY = pt.y;
								boxes.append(new Rectangle(locX, locY, w, MapMiniPanel.LABEL_HEIGHT), false);
								cmp.setLocation(locX, locY);
	
								int iconX = (int)xy.x - 8;
								int iconY = (int)xy.y - 8;
								
								Line2D.Double line = new Line2D.Double(locX, locY, iconX + 8, iconY + 8);
								cmp.setLine(line);
								addLine(line);
								
								final JLabel icon = new JLabel(Images.getIcon("bullet"));
								icon.setBounds(iconX, iconY, 16, 16);
								
								comps.add(cmp);
								comps.add(icon);
								miniPanels.put(md.getLocationHashCode(), cmp);
							}
							else
							{
								int iconX = (int)xy.x - 8;
								int iconY = (int)xy.y - 8;
								final JLabel icon = new JLabel(Images.getIcon("bullet"));
								icon.setBounds(iconX, iconY, 16, 16);
								comps.add(icon);
								missing++;
							}
						}
						cmp.addMetadata(md);
					}
				}
				
				SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								pane.removeAll();
								pane.add(mapImagePanel, new Integer(10));
								for (JComponent comp : comps)
								{
									if (comp instanceof JLabel)
										pane.add(comp, new Integer(15));
									else
										pane.add(comp, new Integer(20));
								}
//								pane.add(dragPanel, new Integer(30));
								repaint();
							}
						});
				
				return null;
			}
			
			public void finished()
			{
				parent.getThrobber().decrement();
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
				for (Line2D.Double line : lines)
				{
					g2.draw(line);
				}
				
				g.setColor(Color.RED);
				if (dragRectangle != null)
					g2.draw(dragRectangle);
			}
		}
	}
}
