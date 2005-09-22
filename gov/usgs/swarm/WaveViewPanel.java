package gov.usgs.swarm;

import gov.usgs.math.Filter;
import gov.usgs.plot.FrameRenderer;
import gov.usgs.plot.Plot;
import gov.usgs.plot.TextRenderer;
import gov.usgs.swarm.data.SeismicDataSource;
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
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * A component that renders a wave in either a standard wave view, a frequency spectra, or 
 * spectrogram.  Relies heavily on the Valve plotting package.
 *
 * $Log: not supported by cvs2svn $
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
 */
public class WaveViewPanel extends JComponent
{
	public static final long serialVersionUID = -1;
	
	/** X pixel location of where the main plot axis should be located on the component. 
	*/
	public static final int X_OFFSET = 60;
	
	/** Y pixel location of where the main plot axis should be located on the component.
	*/
	public static final int Y_OFFSET = 20;
	
	/** The amount of padding space on the right side.
	*/
	public static final int RIGHT_WIDTH = 20;
	
	/** The amount of padding space on the bottom.
	*/
	public static final int BOTTOM_HEIGHT = 20;
	
//	private static final int[] SAMPLE_SIZES = new int[] {64, 128, 256, 512, 1024, 2048};
//	private static final double LOG10 = Math.log(10);
	
	private Plot plot;
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
	
	/** The data source to use for zoom drags.  This should probably be moved from this class
	 * to follow a stricter interpretation of MVC.
	 */
	private SeismicDataSource source;
	
	/** A flag to indicate wheter the plot should display a title.  Currently used
	 * when the plot is on the clipboard.
	 */
	private boolean displayTitle;
	
	/** The frame renderers whose axis the title will be attached to if the title is 
	 * to be displayed.
	 */
	private FrameRenderer titleFrame;
	
	private Color backgroundColor;
	private DateFormat dateFormat;
	private NumberFormat numberFormat;
	private JLabel statusLabel;
	
	// variable used for zoom dragging mode
	private boolean allowDragging;
	private boolean dragging;
	private double j2k1;
	private double j2k2;
	private int highlightX1;
	private int highlightX2;
	
	private static Image closeImg;
	private ActionListener closeListener;
	
	/** A callback reference to the clipboard if this wave view lives in a clipboard.
	 */
	private ClipboardWaveViewPanel clipboardPanel;
	
	/** A callback reference to the helicorder view if this wave view lives in a helicorder.
	 */
	private HelicorderViewPanel heliViewPanel;
	
	/** A callback reference to the clipboard if this wave view lives in a monitor.
	 */
	private MultiMonitor monitor;
	
	/** A flag that indicates whether data are being loaded for this panel.
	 */
	private boolean working;

	/** The wave is rendered to an image that is only updated when the settings change for repaint efficiency.
	 */
	private BufferedImage image;
	
	private double mark1 = Double.NaN;
	private double mark2 = Double.NaN;
	
//	private boolean stackMode;

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
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		numberFormat = new DecimalFormat("#.###");

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
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		numberFormat = new DecimalFormat("#.###");
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
		closeListener = null;
		setupMouseHandler();
		processSettings();
	}

	public void setCloseListener(ActionListener al)
	{
		closeListener = al;
	}
	
	private void setupMouseHandler()
	{
		Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
		this.setCursor(crosshair);
		this.addMouseListener(new MouseAdapter()
				{
					public void mousePressed(MouseEvent e)
					{
						Swarm.getParentFrame().touchUITime();
						
						double[] t = getTranslation();
						int x = e.getX();
						double j2k = x * t[0] + t[1];
						if (timeSeries)
							System.out.printf("%s, UTC: %s, j2k: %.3f, ew: %.3f\n", channel, dateFormat.format(Util.j2KToDate(j2k)), j2k, Util.j2KToEW(j2k));
						
						if (SwingUtilities.isRightMouseButton(e) )
						{
							settings.cycleType();
						}

						if (timeSeries && heliViewPanel != null)
						{
							if (j2k >= startTime && j2k <= endTime)
								heliViewPanel.markTime(j2k);
							
							processMousePosition(x, e.getY());
						}
						
						if (timeSeries && allowDragging && SwingUtilities.isLeftMouseButton(e))
						{
							Dimension size = getSize();
							int y = e.getY();
							if (t != null && y > Y_OFFSET && y < (size.height - BOTTOM_HEIGHT) 
								&& x > X_OFFSET && x < size.width - RIGHT_WIDTH)
							{
								j2k1 = j2k2 = j2k;
							    if (e.isControlDown())
							    {
									System.out.println(channel + ": " + dateFormat.format(Util.j2KToDate(j2k1)));
							    }
							    else
							    {
									highlightX1 = highlightX2 = x;
									dragging = true;
							    }
							}
						}
						if (clipboardPanel != null)
							clipboardPanel.select();
						if (monitor != null)
							monitor.select(WaveViewPanel.this);
					}
					
					public void mouseReleased(MouseEvent e)
					{
						Swarm.getParentFrame().touchUITime();
						if (SwingUtilities.isLeftMouseButton(e) && dragging)
						{	
							dragging = false;
							zoomDraggedArea();
							repaint();
						}
						
						int mx = e.getX();
						int my = e.getY();
						if (closeListener != null && SwingUtilities.isLeftMouseButton(e) &&  
								mx > WaveViewPanel.this.getWidth() - 17 && mx < WaveViewPanel.this.getWidth() - 3 && 
								my > 2 && my < 17)
						{
							closeListener.actionPerformed(null);
						}
					}
					
					public void mouseExited(MouseEvent e)
					{
						dragging = false;	
						repaint();
					}
				});
		
		this.addMouseMotionListener(new MouseMotionListener()
				{
					public void mouseMoved(MouseEvent e)
					{
						Swarm.getParentFrame().touchUITime();
						processMousePosition(e.getX(), e.getY());
					}	
					
					public void mouseDragged(MouseEvent e)
					{
						Swarm.getParentFrame().touchUITime();
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
							if (t != null && y > Y_OFFSET && y < (size.height - BOTTOM_HEIGHT) 
								&& x > X_OFFSET && x < size.width - RIGHT_WIDTH)
							{
								j2k2 = x * t[0] + t[1];
								highlightX2 = x;
								repaint();
							}
						}
					}
				});
	}

	public void zoomDraggedArea()
	{
		if (j2k1 == j2k2 || source == null)
			return;
		
		if (clipboardPanel != null)
			clipboardPanel.didZoom(startTime, endTime);	
			
		final double st = Math.min(j2k1, j2k2);	
		final double et = Math.max(j2k1, j2k2);
		
		final SwingWorker worker = new SwingWorker()
				{
					public Object construct()
					{
						Swarm.getParentFrame().incThreadCount();
						Wave sw = source.getWave(channel, st, et);
						setWave(sw, st, et);
						return null;
					}
					
					public void finished()
					{
						Swarm.getParentFrame().decThreadCount();
						repaint();	
					}
				};
		worker.start();	
	}

	/*
	public void setStackMode(boolean b)
	{
		stackMode = b;
	}
	*/
	
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

	public void setClipboardPanel(ClipboardWaveViewPanel p)
	{
		clipboardPanel = p;	
	}
	
	public void setHelicorderPanel(HelicorderViewPanel p)
	{
		heliViewPanel = p;
	}
	
	public void setMonitor(MultiMonitor m)
	{
		monitor = m;
	}

	public void setStatusLabel(JLabel l)
	{
		statusLabel = l;	
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
	
	public void setDisplayTitle(boolean b)
	{
		displayTitle = b;	
	}
	
	public void settingsChanged()
	{
		processSettings();	
	}
	
	public void invalidateImage()
	{
		image = null;
	}
	
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
		if (t != null && y > Y_OFFSET && y < (size.height - BOTTOM_HEIGHT) 
			&& x > X_OFFSET && x < size.width - RIGHT_WIDTH)
		{
			double j2k = x * t[0] + t[1];
			double yi = y * -t[2] + t[3];
			if (timeSeries)
			{
				String utc = dateFormat.format(Util.j2KToDate(j2k));
				double tzo = Double.parseDouble(Swarm.getParentFrame().getConfig().getString("timeZoneOffset"));
				if (tzo != 0)
				{
					String tza = Swarm.getParentFrame().getConfig().getString("timeZoneAbbr");
					status = dateFormat.format(Util.j2KToDate(j2k + tzo * 3600)) + " (" + tza + "), " +
							utc + " (UTC)";
				}
				else
					status = utc;
				status = status + ", Y: " + numberFormat.format(yi);
			}
			else
			{
				double xi = j2k;
				if (settings.type == WaveViewSettings.SPECTRA && settings.logFreq)
					xi = Math.pow(10.0, xi);
				if (settings.type == WaveViewSettings.SPECTRA && settings.logPower)
					yi = Math.pow(10.0, yi);
				status = "X: " + numberFormat.format(xi) + ", Y: " + numberFormat.format(yi);
			}
		}
		else
			status = " ";
		
		if (status == null)
			status = " ";
			
		if (!Double.isNaN(mark1) && !Double.isNaN(mark2))
		{
			double dur = Math.abs(mark1 - mark2);
			String pre = String.format("Duration: %.2fs (Md: %.2f)", dur, Swarm.getParentFrame().getDurationMagnitude(dur));
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
		processSettings();
	}
	
	private void processSettings()
	{
		if (wave == null || wave.buffer == null || wave.buffer.length == 0)
			return;

		image = null;
		
		if (settings.maxFreq > wave.getSamplingRate() / 2)
		{
			JOptionPane.showMessageDialog(Swarm.getParentFrame(), "The maximum frequency was set too high and has been automatically adjusted to the Nyquist frequency.", "Warning", JOptionPane.WARNING_MESSAGE);
			settings.maxFreq = wave.getSamplingRate() / 2;
		}
			
		timeSeries = !(settings.type == WaveViewSettings.SPECTRA);
		
		if (getParent() != null)
			getParent().repaint();
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

	private static final Color DARK_GREEN = new Color(0, 168, 0);
	
	private void paintMark(Graphics2D g2, double j2k)
	{
		if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime)
			return;
		
		double[] t = getTranslation();
		double x = (j2k - t[1]) / t[0];
		g2.setColor(DARK_GREEN);
		g2.draw(new Line2D.Double(x, Y_OFFSET, x, getSize().height - Y_OFFSET));
		
		GeneralPath gp = new GeneralPath();
		gp.moveTo((float)x, (float)Y_OFFSET);
		gp.lineTo((float)x - 5, Y_OFFSET - 7);
		gp.lineTo((float)x + 5, Y_OFFSET - 7);
		gp.closePath();
		g2.setPaint(Color.GREEN);
		g2.fill(gp);
		g2.setColor(DARK_GREEN);
		g2.draw(gp);
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
			g2.setColor(Color.lightGray);
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
			if (image == null || image.getWidth() != dim.width || image.getHeight() != dim.height)
			{
				image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_4BYTE_ABGR);
				Graphics2D ig = (Graphics2D)image.getGraphics();
				constructPlot(ig);
			}
			
			if (image != null)
				g2.drawImage(image, 0, 0, null);
			
			if (dragging)
				paintDragBox(g2);
			
			paintMark(g2, mark1);
			paintMark(g2, mark2);
			
			if (closeListener != null)
			{
				if (closeImg == null)
					closeImg = Toolkit.getDefaultToolkit().createImage(Images.get("close"));
				g2.drawImage(closeImg, dim.width - 17, 3, null);
			}
		}
	}

	private TextRenderer getFilterLabel()
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
		TextRenderer tr = new TextRenderer(X_OFFSET, 15, ft);
		tr.color = Color.red;
		return tr;
	}
	
	/** Constructs the plot on the specified graphics context.
	 * @param g2 the graphics context
	 */
	private void constructPlot(Graphics2D g2)
	{
		Dimension dim = this.getSize();		
		
		plot = new Plot();
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
						
		if (settings.type == WaveViewSettings.WAVE)
			plotWave(renderWave);
		else if (settings.type == WaveViewSettings.SPECTRA)
			plotSpectra(renderWave);
		else if (settings.type == WaveViewSettings.SPECTROGRAM)
			plotSpectrogram(renderWave);
			
		if (channel != null && displayTitle && titleFrame != null)
			titleFrame.getAxis().setTopLabelAsText(channel);
		
		plot.render(g2);
	}

	/** Plots a wave.
	 * @param renderWave the wave to plot
	 */
	private void plotWave(Wave renderWave)
	{
	    if (renderWave == null || renderWave.samples() == 0)
			return;
	    
	    SliceWave wv = new SliceWave(renderWave);
	    wv.setSlice(startTime, endTime);
	    
	    double bias = 0;
	    if (settings.removeBias)
	        bias = wv.mean();
	    
	    double minY = settings.minAmp;
		double maxY = settings.maxAmp;
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
		    
		waveRenderer.setLocation(X_OFFSET, Y_OFFSET, this.getWidth() - X_OFFSET - RIGHT_WIDTH, this.getHeight() - Y_OFFSET - BOTTOM_HEIGHT);
//		waveRenderer.setYLimits(minAmp, maxAmp);
		waveRenderer.setYLimits(minY, maxY);
		waveRenderer.setViewTimes(startTime, endTime);
		waveRenderer.setWave(wv);
		waveRenderer.setRemoveBias(settings.removeBias);
		waveRenderer.setAutoScale(true);
		waveRenderer.update();
	    plot.addRenderer(waveRenderer);
		if (settings.filterOn)
			plot.addRenderer(getFilterLabel());
		translation = waveRenderer.getDefaultTranslation();
		titleFrame = waveRenderer;
	}
	
	/** Plots frequency spectra.
	 * @param renderWave the wave to plot
	 */
	private void plotSpectra(Wave renderWave)
	{
		if (renderWave == null || renderWave.samples() == 0)
			return;
	    
	    SliceWave wv = new SliceWave(renderWave);
	    wv.setSlice(startTime, endTime);
	    
	    if (spectraRenderer == null)
	        spectraRenderer = new SpectraRenderer();
	    
	    spectraRenderer.setLocation(X_OFFSET, Y_OFFSET, this.getWidth() - RIGHT_WIDTH - X_OFFSET, this.getHeight() - BOTTOM_HEIGHT - Y_OFFSET);
	    spectraRenderer.setWave(wv);
	    spectraRenderer.setAutoScale(settings.autoScalePower);
	    spectraRenderer.setLogPower(settings.logPower);
	    spectraRenderer.setLogFreq(settings.logFreq);
	    spectraRenderer.setMaxFreq(settings.maxFreq);
	    spectraRenderer.setMinFreq(settings.minFreq);
	    double power = spectraRenderer.update(maxSpectraPower);
	    maxSpectraPower = Math.max(maxSpectraPower, power);
		if (settings.filterOn)
			plot.addRenderer(getFilterLabel());
		translation = spectraRenderer.getDefaultTranslation();
		titleFrame = spectraRenderer;
		plot.addRenderer(spectraRenderer);
	}

	/** Plots a spectrogram.
	 *  TODO: Fix logPower.
	 * @param renderWave the wave to plot
	 */
	private void plotSpectrogram(Wave renderWave)
	{
	    if (renderWave == null || renderWave.samples() == 0)
			return;
	    
	    SliceWave wv = new SliceWave(renderWave);
	    wv.setSlice(startTime, endTime);
	    
	    if (spectrogramRenderer == null)
	        spectrogramRenderer = new SpectrogramRenderer();
	    
	    spectrogramRenderer.setLocation(X_OFFSET, Y_OFFSET, this.getWidth() - RIGHT_WIDTH - X_OFFSET, this.getHeight() - BOTTOM_HEIGHT - Y_OFFSET);
	    spectrogramRenderer.setWave(wv);
	    spectrogramRenderer.setViewStartTime(startTime);
	    spectrogramRenderer.setViewEndTime(endTime);
	    spectrogramRenderer.setAutoScale(settings.autoScalePower);
	    spectrogramRenderer.setFftSize(settings.fftSize);
	    spectrogramRenderer.setLogPower(settings.logPower);
	    spectrogramRenderer.setOverlap(settings.spectrogramOverlap);
	    spectrogramRenderer.setMaxFreq(settings.maxFreq);
	    spectrogramRenderer.setMinFreq(settings.minFreq);
	    double power = spectrogramRenderer.update(maxSpectrogramPower);
	    maxSpectrogramPower = Math.max(maxSpectrogramPower, power);
	    plot.addRenderer(spectrogramRenderer);
		if (settings.filterOn)
			plot.addRenderer(getFilterLabel());
		translation = spectrogramRenderer.getDefaultTranslation();
		titleFrame = spectrogramRenderer;
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
		g2.fillRect(x1, Y_OFFSET + 1, width, getSize().height - BOTTOM_HEIGHT - Y_OFFSET);
		g2.setPaint(pnt);
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
