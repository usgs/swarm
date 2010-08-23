package gov.usgs.swarm.wave;

import gov.usgs.math.Filter;
import gov.usgs.plot.FrameDecorator;
import gov.usgs.plot.Plot;
import gov.usgs.plot.TextRenderer;
import gov.usgs.swarm.Images;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.wave.WaveViewSettings.ViewType;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.SliceWave;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.vdx.data.wave.plot.SliceWaveRenderer;
import gov.usgs.vdx.data.wave.plot.SpectraRenderer;
import gov.usgs.vdx.data.wave.plot.SpectrogramRenderer;

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
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

/**
 * A component that renders a wave in either a standard wave view, a frequency spectra, or 
 * spectrogram.  Relies heavily on the Valve plotting package.
 *
 * TODO: move filter method
 *
 * $Log: not supported by cvs2svn $
 * Revision 1.13  2007/04/29 21:16:45  dcervelli
 * Bottom borders, multiselect support code.
 *
 * Revision 1.12  2007/03/12 22:29:01  dcervelli
 * Added null check in paintMark().
 *
 * Revision 1.11  2007/03/06 17:55:40  cervelli
 * Units can now be disabled
 *
 * Revision 1.10  2007/02/28 20:32:51  dcervelli
 * Added border at bottom of wave panel.
 *
 * Revision 1.9  2007/02/27 20:10:56  cervelli
 * Added support for turning calibration use on and off.
 *
 * Revision 1.8  2006/10/26 00:56:46  dcervelli
 * Manual scale adjusting and labeling.
 *
 * Revision 1.7  2006/08/12 00:36:42  dcervelli
 * Null check on paintCursor().
 *
 * Revision 1.6  2006/08/11 21:05:03  dcervelli
 * More repaint madness and filter labels.
 *
 * Revision 1.5  2006/08/09 21:50:54  cervelli
 * Changes so clipboard would work again.
 *
 * Revision 1.4  2006/08/07 22:39:47  cervelli
 * Desynchronized constructPlot() to avoid deadlock.
 *
 * Revision 1.3  2006/08/06 20:06:09  cervelli
 * Added decorator stuff for specta/spectrogram.
 *
 * Revision 1.2  2006/08/02 23:33:57  cervelli
 * Now constructs wave outside of the event thread.
 *
 * Revision 1.1  2006/08/01 23:45:23  cervelli
 * Moved package.
 *
 * Revision 1.21  2006/07/25 05:17:06  cervelli
 * Red line disappears when outside of plot box.
 *
 * Revision 1.20  2006/07/22 20:32:36  cervelli
 * Time zones and red line.
 *
 * Revision 1.19  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * Revision 1.18  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * Revision 1.17  2006/04/17 04:16:36  dcervelli
 * More 1.3 changes.
 *
 * Revision 1.16  2006/04/15 15:58:52  dcervelli
 * 1.3 changes (renaming, new datachooser, different config).
 *
 * Revision 1.15  2006/04/11 17:55:25  dcervelli
 * Duration magnitude option.
 *
 * Revision 1.14  2006/04/09 18:28:44  dcervelli
 * Eliminated warning.
 *
 * Revision 1.13  2006/04/02 17:17:21  cervelli
 * Commented out dread green lines
 *
 * Revision 1.12  2006/03/04 23:03:45  cervelli
 * Added alias feature. More thoroughly incorporated calibrations.  Got rid of 'waves' tab and combined all functionality under a 'channels' tab.
 *
 * Revision 1.11  2006/03/02 23:32:22  dcervelli
 * Added calibration stuff.
 *
 * Revision 1.10  2005/10/27 15:39:49  dcervelli
 * Only shows the Nyquist warning once.
 *
 * Revision 1.9  2005/09/23 21:57:34  dcervelli
 * Right click only for duration marker.
 *
 * Revision 1.8  2005/09/22 21:00:09  dcervelli
 * Changes for duration magnitude markers.
 *
 * Revision 1.7  2005/09/05 00:38:22  dcervelli
 * Uses new SpectraRenderer.
 *
 * Revision 1.6  2005/09/02 16:12:02  dcervelli
 * Changes for Butterworth enum.
 *
 * Revision 1.5  2005/09/01 00:31:49  dcervelli
 * Changes for SliceWave refactor.
 *
 * Revision 1.4  2005/08/30 00:34:55  tparker
 * Update to use Images class
 *
 * Revision 1.3  2005/08/27 00:33:27  tparker
 * Tidy code, no functional changes.
 *
 * Revision 1.2  2005/08/27 00:22:58  tparker
 * Create image constant
 *
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.4  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * Revision 1.3  2005/03/24 20:47:18  cervelli
 * Control-click on clipboard displays time in console.
 *
 * Revision 1.2  2004/10/28 20:14:05  cvs
 * Some comments.
 *
 * @author Dan Cervelli
 * @version $Id: WaveViewPanel.java,v 1.14 2007-05-21 02:44:17 dcervelli Exp $
 */
public class WaveViewPanel extends JComponent
{
	public static final long serialVersionUID = -1;
	
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	/** X pixel location of where the main plot axis should be located on the component. */
	private int xOffset = 60;
	
	/** Y pixel location of where the main plot axis should be located on the component. */
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
	
	private static boolean shownNyquistWarning = false;
	
	/** The data source to use for zoom drags.  This should probably be moved from this class
	 * to follow a stricter interpretation of MVC. */
	private SeismicDataSource source;
	
	/** A flag to indicate wheter the plot should display a title.  Currently used
	 * when the plot is on the clipboard or monitor. */
	private boolean displayTitle;
	
	/** The frame renderer whose axis the title will be attached to if the title is 
	 * to be displayed. */
//	private FrameRenderer titleFrame;
	
	private Color backgroundColor;
	private Color bottomBorderColor;
//	private DateFormat dateFormat;
//	private NumberFormat numberFormat;
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
	
	/** A flag that indicates whether data are being loaded for this panel.
	 */
	private boolean working;

	/** The wave is rendered to an image that is only updated when the settings change for repaint efficiency.
	 */
	private BufferedImage image;
	
	private double mark1 = Double.NaN;
	private double mark2 = Double.NaN;
	
	private double cursorMark = Double.NaN;
	
	private boolean useFilterLabel = true;
	
	private Color borderColor;
	
	/** Constructs a WaveViewPanel with default settings.
	 */	
	public WaveViewPanel()
	{
		this(new WaveViewSettings());
	}
	
	/** Constructs a WaveViewPanel with specified settings.
	 * @param s the settings
	 */
	public WaveViewPanel(WaveViewSettings s)
	{
//		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
//		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
//		numberFormat = new DecimalFormat("#.###");

		settings = s;
		s.view = this;
		
		backgroundColor = new Color(0xf7, 0xf7, 0xf7);
		setupMouseHandler();
	}
	
	/** Constructs a WaveViewPanel set up the same as a source WaveViewPanel.
	 * Used when copying a waveform to the clipboard.
	 * @param p the source WaveViewPanel
	 */
	public WaveViewPanel(WaveViewPanel p)
	{
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

	public void setOffsets(int xo, int yo, int rw, int bh)
	{
		xOffset = xo;
		yOffset = yo;
		rightWidth = rw;
		bottomHeight = bh;
	}
	
	public void addListener(WaveViewPanelListener listener)
	{
		listeners.add(WaveViewPanelListener.class, listener);
	}
	
	public void removeListener(WaveViewPanelListener listener)
	{
		listeners.remove(WaveViewPanelListener.class, listener);
	}

	public void fireZoomed(MouseEvent e, double oldST, double oldET, double newST, double newET)
	{
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
			if (ls[i] == WaveViewPanelListener.class)
				((WaveViewPanelListener)ls[i + 1]).waveZoomed(this, oldST, oldET, newST, newET);
	}
	
	public void fireTimePressed(MouseEvent e, double j2k)
	{
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
			if (ls[i] == WaveViewPanelListener.class)
				((WaveViewPanelListener)ls[i + 1]).waveTimePressed(this, e, j2k);
	}
	
	public void fireMousePressed(MouseEvent e)
	{
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
			if (ls[i] == WaveViewPanelListener.class)
				((WaveViewPanelListener)ls[i + 1]).mousePressed(this, e, dragging);
	}
	
	public void fireClose()
	{
		Object[] ls = listeners.getListenerList();
		for (int i = ls.length - 2; i >= 0; i -= 2)
			if (ls[i] == WaveViewPanelListener.class)
				((WaveViewPanelListener)ls[i + 1]).waveClosed(this);
	}
	
	public void setAllowClose(boolean b)
	{
		allowClose = b;
	}
	
	private void setupMouseHandler()
	{
		Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
		this.setCursor(crosshair);
		this.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						Swarm.getApplication().touchUITime();
						
						double[] t = getTranslation();
						if (t != null)
						{
							int x = e.getX();
							double j2k = x * t[0] + t[1];
							if (timeSeries)
								System.out.printf("%s UTC: %s j2k: %.3f ew: %.3f\n", channel, Time.format(DATE_FORMAT, Util.j2KToDate(j2k)), j2k, Util.j2KToEW(j2k));
							
							if (SwingUtilities.isRightMouseButton(e) )
							{
								settings.cycleType();
							}
	
							if (timeSeries && j2k >= startTime && j2k <= endTime)
								fireTimePressed(e, j2k);
							
							if (timeSeries && allowDragging && SwingUtilities.isLeftMouseButton(e))
							{
								Dimension size = getSize();
								int y = e.getY();
								if (t != null && y > yOffset && y < (size.height - bottomHeight) 
									&& x > xOffset && x < size.width - rightWidth)
								{
									j2k1 = j2k2 = j2k;
								    if (e.isControlDown())
								    {
										System.out.println(channel + ": " + Time.format(DATE_FORMAT, Util.j2KToDate(j2k1)));
								    }
								    else if (!e.isShiftDown())
								    {
										highlightX1 = highlightX2 = x;
										dragging = true;
								    }
								}
							}
						}
						
						fireMousePressed(e);
					}
					
					public void mouseReleased(MouseEvent e)
					{
						Swarm.getApplication().touchUITime();
						if (SwingUtilities.isLeftMouseButton(e) && dragging)
						{	
							dragging = false;
							if (j2k1 != j2k2 && source != null)
							{
								double st = Math.min(j2k1, j2k2);	
								double et = Math.max(j2k1, j2k2);
								zoom(st, et);
								fireZoomed(e, getStartTime(), getEndTime(), st, et);
							}
//							zoomDraggedArea();
							repaint();
						}
						
						int mx = e.getX();
						int my = e.getY();
						if (allowClose && SwingUtilities.isLeftMouseButton(e) &&  
								mx > WaveViewPanel.this.getWidth() - 17 && mx < WaveViewPanel.this.getWidth() - 3 && 
								my > 2 && my < 17)
						{
							fireClose();
						}
					}
					
					public void mouseExited(MouseEvent e)
					{
						Swarm.getApplication().fireTimeChanged(Double.NaN);
						dragging = false;
						repaint();
					}
				});
		
		this.addMouseMotionListener(new MouseMotionListener()
				{
					public void mouseMoved(MouseEvent e)
					{
						Swarm.getApplication().touchUITime();
						processMousePosition(e.getX(), e.getY());
					}	
					
					public void mouseDragged(MouseEvent e)
					{
						Swarm.getApplication().touchUITime();
					    /*
					    // This used to be the launcher for the microview.
					    // It was removed because it wasn't very useful, but this
					    // stub is left here in case something like it ever gets
					    // put in
						if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown() && settings.type != WaveViewSettings.SPECTRA)
						{
							Dimension size = getSize();
							double[] t = getTranslation();
							int x = e.getX();
							int y = e.getY();
							if (t != null && y > Y_OFFSET && y < (size.height - BOTTOM_HEIGHT) 
								&& x > X_OFFSET && x < size.width - RIGHT_WIDTH)
							{
								double j2k = x * t[0] + t[1];
								createMicroView(j2k);
							}
						}
						*/
						
						processMousePosition(e.getX(), e.getY());
						if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown() && dragging)
						{
							double[] t = getTranslation();
							int x = e.getX();
							int y = e.getY();
							Dimension size = getSize();
							if (t != null && y > yOffset && y < (size.height - bottomHeight) 
								&& x > xOffset && x < size.width - rightWidth)
							{
								j2k2 = x * t[0] + t[1];
								highlightX2 = x;
								repaint();
							}
						}
					}
				});
	}

	public void zoom(final double st, final double et)
	{
		final SwingWorker worker = new SwingWorker()
		{
			public Object construct()
			{
				Wave sw = null;
				if (source instanceof CachedDataSource)
					sw = ((CachedDataSource)source).getBestWave(channel, st, et);
				else
					sw = source.getWave(channel, st, et);
				setWave(sw, st, et);
				return null;
			}
			
			public void finished()
			{
				
				repaint();	
			}
		};
		worker.start();
	}
	
//	public void zoomDraggedArea()
//	{
//		if (j2k1 == j2k2 || source == null)
//			return;
//		
//		double st = Math.min(j2k1, j2k2);	
//		double et = Math.max(j2k1, j2k2);
//		
//		zoom(st, et);	
//	}

	/** Set the working flag.  This flag indicates whether data are being loaded for this panel.
	 * @param b the working flag state
	 */
	public void setWorking(boolean b)
	{
		working = b;	
	}

	/** Set the allow dragging flag.  This flag enables zoom dragging.  Currently only allowed on
	 * the clipboard, but could be implemented within the helicorder view.
	 * @param b the allow dragging flag state
	 */
	public void setAllowDragging(boolean b)
	{
		allowDragging = b;	
	}

	public void setStatusLabel(JLabel l)
	{
		statusLabel = l;	
	}

	public int getXOffset()
	{
		return xOffset;
	}
	
	public int getYOffset()
	{
		return yOffset;
	}
	
	public WaveViewSettings getSettings()
	{
		return settings;
	}
	
	public double getStartTime()
	{
		return startTime;	
	}
	
	public double getEndTime()
	{
		return endTime;	
	}
	
	public Wave getWave()
	{
		return wave;
	}
	
	public WaveViewSettings getWaveViewSettings()
	{
		return settings;	
	}

	public String getChannel()
	{
		return channel;
	}	

	public void setChannel(String c)
	{
		channel = c;	
	}
	
	public void setSettings(WaveViewSettings s)
	{
		settings = s;	
		processSettings();
	}

	public SeismicDataSource getDataSource()
	{
		return source;
	}
	
	public void setDataSource(SeismicDataSource s)
	{
		source = s;	
	}
	
	public void setFrameDecorator(FrameDecorator fd)
	{
		decorator = fd;
	}
	
	public void setDisplayTitle(boolean b)
	{
		displayTitle = b;	
	}
	
	public void settingsChanged()
	{
		processSettings();	
	}
	
//	public void invalidateImage()
//	{
////		image = null;
//	}
	
	public boolean isTimeSeries()
	{
		return timeSeries;	
	}
	
	/** Gets the translation info for this panel.  The translation info is used to convert
	 * from pixel coordinates on the panel into time or data coordinates.
	 *
	 * @return the transformation information
	 */
	public double[] getTranslation()
	{
		return translation;	
	}
	
	/** Set the background color of the panel.
	 * @param c the background color
	 */
	public void setBackgroundColor(Color c)
	{
		backgroundColor = c;
	}
	
	public void setBottomBorderColor(Color c)
	{
		bottomBorderColor = c;
	}
	
	public void setBorderColor(Color c)
	{
		borderColor = c;
	}
	
	/** Processes the mouse position variables when the cursor is over the panel.
	 * Currently, the only thing this does is set the status bar text.
	 *
	 * @param x the mouse x position
	 * @param y the mouse y position
	 */
	public boolean processMousePosition(int x, int y)
	{
		String status = null;
		Dimension size = getSize();
		double[] t = getTranslation();
		double j2k = Double.NaN;
		if (t != null && y > yOffset && y < (size.height - bottomHeight) 
			&& x > xOffset && x < size.width - rightWidth)
		{
			j2k = x * t[0] + t[1];
			double yi = y * -t[2] + t[3];
			if (timeSeries)
			{
				String utc = Time.format(DATE_FORMAT, Util.j2KToDate(j2k));
				TimeZone tz = Swarm.config.getTimeZone(channel);
				double tzo = Time.getTimeZoneOffset(tz, j2k);
				if (tzo != 0)
				{
					String tza = tz.getDisplayName(tz.inDaylightTime(Util.j2KToDate(j2k)), TimeZone.SHORT);
					status = Time.format(DATE_FORMAT, Util.j2KToDate(j2k + tzo)) + " (" + tza + "), " +
							utc + " (UTC)";
				}
				else
					status = utc;
				double offset = 0;
				double multiplier = 1;
				Metadata md = Swarm.config.getMetadata(channel);
				if (md != null)
				{
					offset = md.getOffset();
					multiplier = md.getMultiplier();
				}
				status = String.format("%s, Y: %.3f", status, multiplier * yi + offset);
			}
			else
			{
				double xi = j2k;
				if (settings.viewType == ViewType.SPECTRA && settings.logFreq)
					xi = Math.pow(10.0, xi);
				if (settings.viewType == ViewType.SPECTRA && settings.logPower)
					yi = Math.pow(10.0, yi);
				status = String.format("X: %.3f, Y: %.3f", xi, yi);
			}
		}
		else
		{
			status = " ";
		}

		Swarm.getApplication().fireTimeChanged(j2k);
		
		if (status == null)
			status = " ";
			
		if (!Double.isNaN(mark1) && !Double.isNaN(mark2))
		{
			double dur = Math.abs(mark1 - mark2);
			String pre = String.format("Duration: %.2fs (Md: %.2f)", dur, Swarm.config.getDurationMagnitude(dur));
			if (status.length() > 2)
				status = pre + ", " + status;
			else
				status = pre;
		}
		
		if (status != null && statusLabel != null)
		{
			final String st = status;
			SwingUtilities.invokeLater(new Runnable() 
				{
					public void run()
					{
						statusLabel.setText(st);
					}
				});
		}
		
		return !status.equals(" ");
	}

	public void setWave(Wave sw, double st, double et)
	{
		wave = sw;
		startTime = st;
		endTime = et;
		processSettings();
		//findEvents();
	}

	public void resetAutoScaleMemory()
	{
		minAmp = 1E300;
		maxAmp = -1E300;
		maxSpectraPower = -1E300;
		maxSpectrogramPower = -1E300;
		settings.autoScaleAmp = true;
		settings.autoScalePower = true;
		processSettings();
	}
	
	public void adjustScale(double pct)
	{
		double maxa = settings.autoScaleAmp ? maxAmp : settings.maxAmp;
		double mina = settings.autoScaleAmp ? minAmp : settings.minAmp;
		settings.autoScaleAmp = false;
		double range = maxa - mina;
		double center = range / 2 + mina;
		double newRange = range * pct;
		settings.minAmp = center - newRange / 2;
		settings.maxAmp = center + newRange / 2;
		processSettings();
	}
	
	private synchronized void setImage(BufferedImage bi)
	{
		image = bi;
	}
	
	private synchronized BufferedImage getImage()
	{
		return image;
	}
	
	public void createImage()
	{
		final Runnable r = new Runnable()
				{
					public void run()
					{
						if (getWidth() >  0 && getHeight() > 0)
						{
							BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
							Graphics2D ig = (Graphics2D)bi.getGraphics();
							constructPlot(ig);
							setImage(bi);
//							repaint();
						}
					}
				};
		
		if (SwingUtilities.isEventDispatchThread())
		{
			SwingWorker worker = new SwingWorker()
					{
						public Object construct()
						{
							r.run();
							return null;
						}
						
						public void finished()
						{
							repaint();
						}
					};
			worker.start();
		}
		else
			r.run();
	}
	
	/** Does NOT call repaint for efficiency purposes, that is left to the 
	 * container.
	 */
	private void processSettings()
	{
		if (wave == null || wave.buffer == null || wave.buffer.length == 0)
			return;

//		invalidateImage();		
		
		if (settings.maxFreq > wave.getSamplingRate() / 2)
		{
			if (!shownNyquistWarning)
				JOptionPane.showMessageDialog(Swarm.getApplication(), "The maximum frequency was set too high and has been automatically adjusted to the Nyquist frequency. " +
						"This window will not be shown again.", "Warning", JOptionPane.WARNING_MESSAGE);
			settings.maxFreq = wave.getSamplingRate() / 2;
			shownNyquistWarning = true;
		}
			
		timeSeries = !(settings.viewType == ViewType.SPECTRA);
		
		createImage();
		
//		if (getWidth() != 0 && getHeight() != 0)
//		{
//			BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
//			Graphics2D ig = (Graphics2D)bi.getGraphics();
//			constructPlot(ig);
//			setImage(bi);
//		}
		
//		repaint();
//		if (getParent() != null)
//			getParent().repaint();
	}

	private void filter(Wave w)
	{
		double mean = w.mean();
		
		double[] dBuf = new double[w.buffer.length + (int)(w.buffer.length * 0.5)];
		Arrays.fill(dBuf, mean);
		int trueStart = (int)(w.buffer.length * 0.25);
		for (int i = 0; i < w.buffer.length; i++)
		{
			if (w.buffer[i] != Wave.NO_DATA)
				dBuf[i + trueStart] = w.buffer[i];
		}

		settings.filter.setSamplingRate(w.getSamplingRate());
		settings.filter.create();
		Filter.filter(dBuf, settings.filter.getSize(), settings.filter.getXCoeffs(), settings.filter.getYCoeffs(), settings.filter.getGain(), 0, 0);
		if (settings.zeroPhaseShift)
		{
			double[] dBuf2 = new double[dBuf.length];
			for (int i = 0, j = dBuf.length - 1; i < dBuf.length; i++, j--)
				dBuf2[j] = dBuf[i];	
			
			Filter.filter(dBuf2, settings.filter.getSize(), settings.filter.getXCoeffs(), settings.filter.getYCoeffs(), settings.filter.getGain(), 0, 0);
			
			for (int i = 0, j = dBuf2.length - 1 - trueStart; i < w.buffer.length; i++, j--)
				w.buffer[i] = (int)Math.round(dBuf2[j]);
		}
		else
		{
			for (int i = 0; i < w.buffer.length; i++)
				w.buffer[i] = (int)Math.round(dBuf[i + trueStart]);
		}
		w.invalidateStatistics();
	}

	/** Paints the component on the specified graphics context.
	 * @param g the graphics context
	 */
	public void paint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = this.getSize();
		if (wave == null)
		{
			g2.setColor(backgroundColor);
			g2.fillRect(0, 0, dim.width, dim.height);
			g2.setColor(Color.black);
			if (working)
				g2.drawString("Retrieving data...", dim.width / 2 - 50, dim.height / 2);
			else
			{
				String error = "No wave data.";
				if (channel != null)
					error = "No wave data for " + channel + ".";
				int w = g2.getFontMetrics().stringWidth(error);
				g2.drawString(error, dim.width / 2 - w / 2, dim.height / 2);
			}
		}
		else
		{
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
		
		if (allowClose)
		{
			if (closeImg == null)
				closeImg = Images.getIcon("close_view").getImage();
			
			g2.drawImage(closeImg, dim.width - 17, 3, null);
		}
		if (bottomBorderColor != null)
		{
			g2.setColor(bottomBorderColor);
			g2.drawLine(0, dim.height - 1, dim.width, dim.height - 1);
		}
		if (borderColor != null)
		{
			g2.setColor(borderColor);
			g2.drawRect(0, 0, dim.width - 1, dim.height - 2);
		}
	}

	public void setUseFilterLabel(boolean b)
	{
		useFilterLabel = b;
	}
	
	public TextRenderer getFilterLabel()
	{
		String ft = "";
		switch (settings.filter.getType())
		{
			case BANDPASS:
				ft = "Band pass [" + settings.filter.getCorner1() + "-" + settings.filter.getCorner2() + " Hz]";
				break;
			case HIGHPASS:
				ft = "High pass [" + settings.filter.getCorner1() + " Hz]";
				break;
			case LOWPASS:
				ft = "Low pass [" + settings.filter.getCorner1() + " Hz]";
				break;
		}
		TextRenderer tr = new TextRenderer(xOffset + 5, 148, ft);
		tr.color = Color.red;
		return tr;
	}
	
	/** Constructs the plot on the specified graphics context.
	 * @param g2 the graphics context
	 */
//	private synchronized void constructPlot(Graphics2D g2)
	private synchronized void constructPlot(Graphics2D g2)
	{
		Dimension dim = this.getSize();		
		
		Plot plot = new Plot();
		plot.setBackgroundColor(backgroundColor);
		plot.setSize(dim);
		Wave renderWave = wave;
		if (settings.filterOn)
		{
			renderWave = new Wave(wave);
			filter(renderWave);
			if (settings.removeBias)
				bias = (int)Math.round(renderWave.mean());
		}
						
		switch (settings.viewType)
		{
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

//		if (channel != null && displayTitle && titleFrame != null)
//			titleFrame.getAxis().setTopLabelAsText(channel);

		plot.render(g2);
		
	}

	/** Plots a wave.
	 * @param renderWave the wave to plot
	 */
	private void plotWave(Plot plot, Wave renderWave)
	{
	    if (renderWave == null || renderWave.samples() == 0)
			return;
	    
	    SliceWave wv = new SliceWave(renderWave);
	    wv.setSlice(startTime, endTime);
	    
	    double offset = 0;
		double multiplier = 1;
		Metadata md = Swarm.config.getMetadata(channel);

		if (settings.useUnits && md != null)
		{
			offset = md.getOffset();
			multiplier = md.getMultiplier();
		}
	    
//	    Calibration cal = Swarm.getApplication().getCalibration(channel);
//		if (cal == null)
//			cal = Calibration.IDENTITY;
		
	    double bias = 0;
	    if (settings.removeBias)
	        bias = wv.mean();
	    
	    double minY = (settings.minAmp - offset) / multiplier;
		double maxY = (settings.maxAmp - offset) / multiplier;

		if (settings.autoScaleAmp)
		{
			double[] dr = new double[] {wv.min(), wv.max()};
			if (settings.autoScaleAmpMemory)
			{
				minY = Math.min(minAmp, dr[0] - bias);
				maxY = Math.max(maxAmp, dr[1] - bias);
				minAmp = Math.min(minY, minAmp);
				maxAmp = Math.max(maxY, maxAmp);
			}
			else
			{
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
		waveRenderer.setLocation(xOffset, yOffset, this.getWidth() - xOffset - rightWidth, this.getHeight() - yOffset - bottomHeight);
		waveRenderer.setYLimits(minY, maxY);
		waveRenderer.setViewTimes(startTime, endTime, "");
		waveRenderer.setWave(wv);
		waveRenderer.setRemoveBias(settings.removeBias);
		waveRenderer.setAutoScale(true);
		if (channel != null && displayTitle)
			waveRenderer.setTitle(channel);
		
		waveRenderer.update();
	    plot.addRenderer(waveRenderer);
		if (useFilterLabel && settings.filterOn)
			plot.addRenderer(getFilterLabel());
		translation = waveRenderer.getDefaultTranslation();
	}
	
	/** Plots frequency spectra.
	 * @param renderWave the wave to plot
	 */
	private void plotSpectra(Plot plot, Wave renderWave)
	{
		if (renderWave == null || renderWave.samples() == 0)
			return;
	    
	    SliceWave wv = new SliceWave(renderWave);
	    wv.setSlice(startTime, endTime);
	    
	    if (spectraRenderer == null)
	        spectraRenderer = new SpectraRenderer();
	    
	    if (decorator != null)
			spectraRenderer.setFrameDecorator(decorator);
	    
	    spectraRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset, this.getHeight() - bottomHeight - yOffset);
	    spectraRenderer.setWave(wv);
	    spectraRenderer.setAutoScale(settings.autoScalePower);
	    spectraRenderer.setLogPower(settings.logPower);
	    spectraRenderer.setLogFreq(settings.logFreq);
	    spectraRenderer.setMaxFreq(settings.maxFreq);
	    spectraRenderer.setMinFreq(settings.minFreq);
	    if (channel != null && displayTitle)
			spectraRenderer.setTitle(channel);
	    
	    double power = spectraRenderer.update(maxSpectraPower);
	    maxSpectraPower = Math.max(maxSpectraPower, power);
		if (useFilterLabel && settings.filterOn)
			plot.addRenderer(getFilterLabel());
		
		translation = spectraRenderer.getDefaultTranslation();
		plot.addRenderer(spectraRenderer);
	}

	/** Plots a spectrogram.
	 *  TODO: Fix logPower.
	 * @param renderWave the wave to plot
	 */
	private void plotSpectrogram(Plot plot, Wave renderWave)
	{
	    if (renderWave == null || renderWave.samples() == 0)
			return;
	    
	    SliceWave wv = new SliceWave(renderWave);
	    wv.setSlice(startTime, endTime);
	    
	    if (spectrogramRenderer == null)
	        spectrogramRenderer = new SpectrogramRenderer();
	    
	    if (decorator != null)
			spectrogramRenderer.setFrameDecorator(decorator);
	    
	    spectrogramRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset, this.getHeight() - bottomHeight - yOffset);
	    spectrogramRenderer.setWave(wv);
	    spectrogramRenderer.setViewStartTime(startTime);
	    spectrogramRenderer.setViewEndTime(endTime);
	    spectrogramRenderer.setAutoScale(settings.autoScalePower);
	    spectrogramRenderer.setFftSize(settings.fftSize);
	    spectrogramRenderer.setLogPower(settings.logPower);
	    spectrogramRenderer.setOverlap(settings.spectrogramOverlap);
	    spectrogramRenderer.setMaxFreq(settings.maxFreq);
	    spectrogramRenderer.setMinFreq(settings.minFreq);
	    if (channel != null && displayTitle)
			spectrogramRenderer.setTitle(channel);
	    double power = spectrogramRenderer.update(maxSpectrogramPower);
	    maxSpectrogramPower = Math.max(maxSpectrogramPower, power);
	    plot.addRenderer(spectrogramRenderer);
		if (useFilterLabel && settings.filterOn)
			plot.addRenderer(getFilterLabel());
		translation = spectrogramRenderer.getDefaultTranslation();
//		titleFrame = spectrogramRenderer;
	}

	/** Paints the zoom drag box. 
	 * @param g2 the graphics context
	 */	
	private void paintDragBox(Graphics2D g2)
	{
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
	
	public void setCursorMark(double j2k)
	{
		cursorMark = j2k;
		repaint();
	}
	
	private void paintCursor(Graphics2D g2)
	{
		if (Double.isNaN(cursorMark) || cursorMark < startTime || cursorMark > endTime)
			return;
		
		double[] t = getTranslation();
		if (t == null)
			return;
		double x = (cursorMark - t[1]) / t[0];
		g2.setColor(DARK_RED);
		g2.draw(new Line2D.Double(x, yOffset + 1, x, getHeight() - bottomHeight - 1));
	}
	
	private void paintMark(Graphics2D g2, double j2k)
	{
		if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime)
			return;
		
		double[] t = getTranslation();
		if (t == null)
			return;
		
		double x = (j2k - t[1]) / t[0];
		g2.setColor(DARK_GREEN);
		g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo((float)x, yOffset);
		gp.lineTo((float)x - 5, yOffset - 7);
		gp.lineTo((float)x + 5, yOffset - 7);
		gp.closePath();
		g2.setPaint(Color.GREEN);
		g2.fill(gp);
		g2.setColor(DARK_GREEN);
		g2.draw(gp);
	}
	
	/** Overload of Component.  Always returns the developer-specified size.
	 * @return the size of the component
	 */	
	public Dimension getPreferredSize()
	{
		return getSize();	
	}
	
	/** Overload of Component.  Always returns the developer-specified size.
	 * @return the size of the component
	 */	
	public Dimension getMinimumSize()
	{
		return getSize();	
	}
	
	/** Overload of Component.  Always returns the developer-specified size.
	 * @return the size of the component
	 */	
	public Dimension getMaximumSize()
	{
		return getSize();	
	}
	
	public void setMarks(double m1, double m2)
	{
		mark1 = m1;
		mark2 = m2;
	}
}
