package gov.usgs.swarm;

import gov.usgs.plot.Plot;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.heli.HelicorderData;
import gov.usgs.vdx.data.heli.plot.HelicorderRenderer;
import gov.usgs.vdx.data.wave.Wave;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

/**
 * A <code>JComponent</code> for displaying and interacting with a helicorder.
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.4  2005/05/02 16:21:47  cervelli
 * Changed vertical margins.
 *
 * Revision 1.3  2005/03/28 00:13:58  cervelli
 * Fixed bug where yellow highlight against right side wouldn't show up on
 * the next line if that was the bottom of the helicorder.
 *
 * Revision 1.2  2004/10/28 20:15:50  cvs
 * Some comments.
 *
 * @author Dan Cervelli
 */
public class HelicorderViewPanel extends JComponent
{
	public static final long serialVersionUID = -1;
	
	private static final int X_OFFSET = 70;
	private static final int Y_OFFSET = 10;
	private static final int RIGHT_WIDTH = 70;
	private static final int BOTTOM_HEIGHT = 35;
	private static final int INSET_HEIGHT = 200;
	
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
	//private WigglerPanel wigglerPanel;
	
	private BufferedImage bufferImage, displayImage;
	private DateFormat dateFormat;
//	private NumberFormat numberFormat;
	
	private boolean working;
	private boolean resized;
	
	private int insetY;
	
	private boolean fullScreen;
	
	public HelicorderViewPanel(HelicorderViewerFrame hvf)
	{
		parent = hvf;
		this.setBorder(LineBorder.createGrayLineBorder());
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//		numberFormat = new DecimalFormat("#.###");
		plot = new Plot();
		plot.setBackgroundColor(new Color(0xf7, 0xf7, 0xf7));
		settings = hvf.getHelicorderViewerSettings();
		//heliRenderer = new HelicorderRenderer(null, settings);
		heliRenderer = new HelicorderRenderer();
		heliRenderer.setExtents(0, 1, Double.MAX_VALUE, -Double.MAX_VALUE);
		plot.addRenderer(heliRenderer);
		
		this.setRequestFocusEnabled(true);		
		this.addMouseListener(new HelicorderMouseListener());
		this.addMouseMotionListener(new HelicorderMouseMotionListener());
		this.addMouseWheelListener(new HelicorderMouseWheelListener());
		
		cursorChanged();
	}

	public void cursorChanged()
	{
		Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
		if (Swarm.getParentFrame().getConfig().get("useLargeCursor").equals("true"))
		{
			Image cursorImg = Toolkit.getDefaultToolkit().getImage("images/crosshair.gif");
			crosshair = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(16, 16), "Large crosshair");
		}
		
		this.setCursor(crosshair);
	}
	
	public void settingsChanged()
	{
		if (insetWavePanel != null)
		{
			double zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
			double j2k = insetWavePanel.getStartTime() + (insetWavePanel.getEndTime() - insetWavePanel.getStartTime()) / 2;
			loadInsetWave(j2k - zoomOffset, j2k + zoomOffset);
		}
		parent.settingsChanged();
		repaint();
	}
	
//	private void setStatus(double j2k)
//	{
//		parent.setStatus(dateFormat.format(Util.j2KToDate(j2k)));		
//	}

	class HelicorderMouseMotionListener implements MouseMotionListener
	{
		public void mouseDragged(MouseEvent e)
		{
			HelicorderViewPanel.this.requestFocus();
			int mx = e.getX();
			int my = e.getY();
			
			if (mx < heliRenderer.getGraphX() || my < heliRenderer.getGraphY() ||
					mx > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1 ||
					my > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1)
			{
				/*	
				// removed because it wasn't helpful!
				if (insetWavePanel != null)
				{
					removeWaveInset();
					repaint();
				}
				return;
				*/
			}
			else
			{
				double j2k = getMouseJ2K(mx, my);
				processMousePosition(mx, my);
				
				if (SwingUtilities.isLeftMouseButton(e))
					createWaveInset(j2k, mx, my);
			}
		}
		
		public void mouseMoved(MouseEvent e)
		{
			processMousePosition(e.getX(), e.getY());
		}
	}
	
	class HelicorderMouseWheelListener implements MouseWheelListener
	{
		int totalScroll = 0;
		Delay delay;
		
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			totalScroll += e.getWheelRotation();
			if (delay == null)
				delay = new Delay(250);
			else
				delay.restart();
		}	
		
		public void delayOver()
		{
			removeWaveInset();
			SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						parent.scroll(totalScroll);
						delay = null;
						totalScroll = 0;			
					}
				});
		}
		
		class Delay extends Thread
		{
			long delayLeft;
			
			public Delay(long ms)
			{
				delayLeft = ms;
				start();
			}
			
			public void restart()
			{
				interrupt();
			}
			
			public void run()
			{
				boolean done = false;
				while (!done)
				{
					try 
					{ 
						Thread.sleep(delayLeft);
						done = true;
					} catch (Exception e) {}
				}	
				delayOver();
			}
		}
	}
	
	class HelicorderMouseListener implements MouseListener
	{
		public void	mouseClicked(MouseEvent e)
		{
			HelicorderViewPanel.this.requestFocus();
		}
		public void mouseEntered(MouseEvent e)
		{}
		public void mouseExited(MouseEvent e)
		{}
		public void mousePressed(MouseEvent e)
		{
			if (e.getButton() == MouseEvent.BUTTON1)
			{
				int mx = e.getX();
				int my = e.getY();
				if (mx < heliRenderer.getGraphX() || my < heliRenderer.getGraphY() ||
					mx > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1 ||
					my > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1)
				return;

				double j2k = getMouseJ2K(mx, my);
				if (j2k != -1E300)
				{
					if (insetWavePanel != null)
						insetWavePanel.setWave(null, 0, 1);
					createWaveInset(j2k, mx, my);
				}
			}
			/*
			else if (e.getButton() == MouseEvent.BUTTON3)
			{
				if (insetWavePanel != null)
				{
					removeWaveInset();
					repaint();	
				}
			}
			*/
		}
		public void mouseReleased(MouseEvent e) 
		{}
	}

	public boolean hasInset()
	{
		return insetWavePanel != null;	
	}

	public double getStartTime()
	{
		return startTime;	
	}

	public double getEndTime()
	{
		return endTime;	
	}

	public void insetToClipboard()
	{
		if (insetWavePanel != null)
		{
			ClipboardWaveViewPanel p =  new ClipboardWaveViewPanel(insetWavePanel);
			p.getWaveViewPanel().setDataSource(insetWavePanel.getDataSource().getCopy());
			Swarm.getParentFrame().getWaveClipboard().addWave(p);
			requestFocus();
		}
	}

	private void processMousePosition(int x, int y)
	{
		if (heliData == null)
			return;
		
		String status = null;
		
		boolean wp = false;
		if (insetWavePanel != null)
		{
			Point loc = insetWavePanel.getLocation();
			wp = insetWavePanel.processMousePosition(x - loc.x, y - loc.y);
		}

		if (!wp)
		{
			if (status == null)
			{
				if (!(x < heliRenderer.getGraphX() || y < heliRenderer.getGraphY() ||
						x > heliRenderer.getGraphX() + heliRenderer.getGraphWidth() - 1 ||
						y > heliRenderer.getGraphY() + heliRenderer.getGraphHeight() - 1))
				{
					double j2k = getMouseJ2K(x, y);
					status = dateFormat.format(Util.j2KToDate(j2k));
					double tzo = Double.parseDouble(Swarm.getParentFrame().getConfig().getString("timeZoneOffset"));
					if (tzo != 0)
					{
						String tza = Swarm.getParentFrame().getConfig().getString("timeZoneAbbr");
						status = dateFormat.format(Util.j2KToDate(j2k + tzo * 3600)) + " (" + tza + "), " +
								status + " (UTC)";
					}
				}
			}
			
			if (status == null)
				status = " ";
				
			if (status != null)
				parent.setStatus(status);
		}
	}

	public double getMouseJ2K(int mx, int my)
	{
		double j2k = 0;
		if (translation != null)
		{
			j2k = translation[4];
			j2k += (mx - translation[0]) * translation[7];
			j2k += getHelicorderRow(my) * translation[6];
		}
		return j2k;
	}
 
	public int getHelicorderRow(int my)
	{
		return (int)Math.floor((my - translation[3]) / translation[2]);
	}

	public void removeWaveInset()
	{
		if (insetWavePanel != null)
		{
			parent.disableInsetButtons();
			this.remove(insetWavePanel);
			insetWavePanel = null;
			repaint();
		}
	}

	public void moveInset(int offset)
	{
		if (insetWavePanel != null)
		{
			double st = insetWavePanel.getStartTime();
			double et = insetWavePanel.getEndTime();
			double dt = et - st;
			double nst = st + dt * offset;
			double net = et + dt * offset;
			
			int row = heliRenderer.getRow(st + dt * offset + (dt / 2));
			if (row < 0 || row >= heliRenderer.getNumRows())
				return;
			
			loadInsetWave(nst, net);
			int height = INSET_HEIGHT;
			
			if (row * translation[2] + translation[3] > height + translation[3])
			{
				int y = (int)Math.ceil((row - 1) * translation[2] + translation[3]);
				insetWavePanel.setLocation(0, y - height);
			}
			else
			{
				int y = (int)Math.ceil((row + 2) * translation[2] + translation[3]);
				insetWavePanel.setLocation(0, y);
			}
		}	
	}

	public void createWaveInset(final double j2k, final int mx, final int my)
	{
		if (working)
			return;

		insetY = my;
		
		if (insetWavePanel == null)
			insetWavePanel = new WaveViewPanel(parent.getWaveViewSettings());
		
		this.add(insetWavePanel);
		insetWavePanel.setChannel(parent.getChannel());
		insetWavePanel.setDataSource(parent.getDataSource());
		insetWavePanel.setStatusLabel(parent.getStatusLabel());
		
		if (insetWavePanel != null)
		{
			Dimension d = getSize();
			int height = INSET_HEIGHT;
			int row = heliRenderer.getRow(j2k);
			//int row = getHelicorderRow(insetY);
			
			insetWavePanel.setSize(d.width, height);
			if (insetY > height + translation[3])
			{
				int y = (int)Math.ceil((row - 1) * translation[2] + translation[3]);
				insetWavePanel.setLocation(0, y - height);
			}
			else
			{
				int y = (int)Math.ceil((row + 2) * translation[2] + translation[3]);
				insetWavePanel.setLocation(0, y);
			}
			
			insetWavePanel.setCloseListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
						{
							removeWaveInset();
						}
					});
			insetWavePanel.setWorking(true);
		}

		double zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
		loadInsetWave(j2k - zoomOffset, j2k + zoomOffset);
		parent.enableInsetButtons();		
		repaint();
	}
	 
	private void loadInsetWave(final double st, final double et)
	{
		final SwingWorker worker = new SwingWorker()
				{
					private Wave sw;
//					private int zoomOffset;
					
					public Object construct()
					{
						Swarm.getParentFrame().incThreadCount();
						working = true;
						sw = parent.getWave(st, et);
						return null;
					}
					
					public void finished()
					{
						Swarm.getParentFrame().decThreadCount();
						working = false;
						if (insetWavePanel != null)
						{
							insetWavePanel.setWave(sw, st, et);
							insetWavePanel.setWorking(false);
						}
						repaint();
					}
				};
		worker.start();
	}
	
	public void setHelicorder(HelicorderData d, double time1, double time2)
	{
		heliData = d;
		if (heliData != null)
		{
			startTime = time1;
			endTime = time2;
			heliRenderer.setData(heliData);
			heliRenderer.setTimeChunk(settings.timeChunk);
			heliRenderer.setTimeZoneOffset(Double.parseDouble(Swarm.getParentFrame().getConfig().getString("timeZoneOffset")));
			heliRenderer.setTimeZoneAbbr(Swarm.getParentFrame().getConfig().getString("timeZoneAbbr"));
			heliRenderer.setForceCenter(settings.forceCenter);
			heliRenderer.setClipBars(settings.clipBars);
			heliRenderer.setShowClip(settings.showClip);
			mean = heliData.getMeanMax();
			bias = heliData.getBias();
			mean = Math.abs(bias - mean);
			heliRenderer.setClipValue(settings.clipValue);
		}
	}
	
	public void invalidateImage()
	{
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						Swarm.getParentFrame().incThreadCount();
						createImage();	
						return null;
					}
					
					public void finished()
					{
						displayImage = bufferImage;
						repaint();	
						Swarm.getParentFrame().decThreadCount();
					}
				};
		worker.start();
	}
	
	public void setResized(boolean b)
	{
		resized = b;	
	}

	private void createImage()
	{
		if (heliData == null)
			return;
		
		Dimension d = this.getSize();
		if (d.width <= 0 || d.height <= 0)
			return;
		
		bufferImage = new BufferedImage(d.width, d.height, BufferedImage.TYPE_4BYTE_ABGR);
		
		Graphics2D ig = (Graphics2D)bufferImage.getGraphics();
		plot.setSize(d);
		
//		boolean rd = parent.getHelicorderViewerSettings().forceCenter;
		heliRenderer.setLocation(X_OFFSET, Y_OFFSET, d.width - X_OFFSET - RIGHT_WIDTH, d.height - Y_OFFSET - BOTTOM_HEIGHT);
		
		if (settings.autoScale)
		{
			//settings.clipValue = (int)(21 * mean);
			//settings.barRange = (int)(3 * mean);
			settings.barRange = (int) (mean * settings.barMult);
			settings.clipValue = (int) (mean *  settings.clipBars);

//			heliRenderer.setHelicorderExtents(startTime, endTime, -1 * Math.abs(3 * mean), Math.abs(3 * mean));
		}
//		else
//		{
//			//heliRenderer.setHelicorderExtents(startTime, endTime, -1 * Math.abs(settings.barRange + bias), Math.abs(settings.barRange + bias));
//			heliRenderer.setHelicorderExtents(startTime, endTime, -1 * Math.abs(settings.barRange), Math.abs(settings.barRange));
//		}
		heliRenderer.setHelicorderExtents(startTime, endTime, -1 * Math.abs(settings.barRange), Math.abs(settings.barRange));
		heliRenderer.setClipValue(settings.clipValue);
		heliRenderer.createDefaultAxis();
		translation = heliRenderer.getTranslationInfo(false);
		plot.render(ig);
		if (fullScreen)
			drawFullScreenLabel(ig, d);
	}
	
	public void setFullScreen(boolean b)
	{
		fullScreen = b;		
	}
	
	public void paint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		Dimension d = this.getSize();
		if (heliData == null)
		{
			if (parent.isWorking())
				g2.drawString("Retrieving data...", d.width / 2 - 50, d.height / 2);
			else
				g2.drawString("No helicorder data.", d.width / 2 - 50, d.height / 2);
		}
		else if (displayImage != null)
			g2.drawImage(displayImage, 0, 0, null);
			
		if (insetWavePanel != null)
		{
			// find out where time highlight will be, possibly reposition the
			// insetWavePanel
			double t1 = insetWavePanel.getStartTime();
			double t2 = insetWavePanel.getEndTime();
			double t = (t2 - t1) / 2 + t1;
			int row = heliRenderer.getRow(t);
			if (resized)
			{
				insetWavePanel.setSize(d.width, INSET_HEIGHT);
				if (row * translation[2] > INSET_HEIGHT + translation[3])
				{
					int y = (int)Math.ceil((row - 1) * translation[2] + translation[3]);
					insetWavePanel.setLocation(0, y - INSET_HEIGHT);
				}
				else
				{
					int y = (int)Math.ceil((row + 2) * translation[2] + translation[3]);
					insetWavePanel.setLocation(0, y);
				}
			}	
			
			// now it's safe to draw the waveInsetPanel
			Point p = insetWavePanel.getLocation();
			g2.translate(p.x, p.y);
			insetWavePanel.paint(g2);
			Dimension wvd = insetWavePanel.getSize();
			g.setColor(Color.gray);
			g.drawRect(0, 0, wvd.width - 1, wvd.height);
			g2.translate(-p.x, -p.y);
			
			// fixes bug where highlight was being drawn before wave loaded
			if (row < 0)
				return;
			
			// finally, draw the highlight.  support for spanning one row
			// above and one row below the center point.
			Paint pnt = g2.getPaint();
			g2.setPaint(new Color(255, 255, 0, 128));
			Rectangle2D.Double rect = new Rectangle2D.Double();
			int zoomOffset = parent.getHelicorderViewerSettings().waveZoomOffset;
			double xo = 1.0 / translation[7] * (double)zoomOffset;
			int bx = (int)(heliRenderer.helicorderGetXPixel(t) - xo);
			int width = (int)(xo * 2);
			int right = heliRenderer.getGraphX() + heliRenderer.getGraphWidth();
			if (bx < heliRenderer.getGraphX())
			{
				int width2 = heliRenderer.getGraphX() - bx;
				rect.setRect(right - width2, (int)Math.ceil((row - 1) * translation[2] + translation[3]),
					width2,
					(int)Math.ceil(translation[2]));
				if (row - 1 >= 0)
					g2.fill(rect);
				bx = heliRenderer.getGraphX();
				width = width - width2;
			}
			if (bx + width > right)
			{
				int width2 = bx + width - right;
				rect.setRect(heliRenderer.getGraphX() + 1, (int)Math.ceil((row + 1) * translation[2] + translation[3]),
					width2,
					(int)Math.ceil(translation[2]));
				if (row + 1 < heliRenderer.getNumRows())
					g2.fill(rect);
				width = width - width2;
			}			
			rect.setRect(bx, 
					(int)Math.ceil(row * translation[2] + translation[3]),
					width,
					(int)Math.floor(translation[2]));
					
			g2.fill(rect);
			g2.setPaint(pnt);
		}
		
		resized = false;
	}

	private void drawFullScreenLabel(Graphics g, Dimension d)
	{
		String channel = parent.getChannel();
		Point loc = this.getLocation();
		
		g.setFont(Font.decode("Arial-BOLD-48"));
		String c = channel.replace('_', ' ');
		int width = g.getFontMetrics().stringWidth(c);
		int lw = width + 20;
		g.setColor(Color.white);
		g.fillRect(loc.x + d.width / 2 - lw / 2, 3, lw, 50);
		g.setColor(Color.black);
		g.drawRect(loc.x + d.width / 2 - lw / 2, 3, lw, 50);
		
		Font oldFont = g.getFont();
		
		g.drawString(c, loc.x + d.width / 2 - width / 2, 46);
		g.setFont(oldFont);
	}
}