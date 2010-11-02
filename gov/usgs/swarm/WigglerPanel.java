package gov.usgs.swarm;

import gov.usgs.plot.AxisRenderer;
import gov.usgs.plot.FrameRenderer;
import gov.usgs.plot.GradientSpectrum;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.Spectrum;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.CurrentTime;
import gov.usgs.vdx.data.wave.Wave;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Line2D;

import javax.swing.JComponent;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2005/08/26 20:40:28  dcervelli
 * Initial avosouth commit.
 *
 * Revision 1.2  2005/05/02 16:22:11  cervelli
 * Moved data classes to separate package.
 *
 * @author Dan Cervelli
 */
public class WigglerPanel extends JComponent implements Runnable
{
	public static final long serialVersionUID = -1;
	/** X pixel location of where the main plot axis should be located on the component. 
	*/
	public static final int X_OFFSET = 2;
	
	/** Y pixel location of where the main plot axis should be located on the component.
	*/
	public static final int Y_OFFSET = 2;
	
	/** The amount of padding space on the right side.
	*/
	public static final int RIGHT_WIDTH = 2;
	
	/** The amount of padding space on the bottom.
	*/
	public static final int BOTTOM_HEIGHT = 2;
	private static final Spectrum spectrum = new GradientSpectrum(255, 0.15, 0.15, 0.15, 1, 1, 1);
	private Wave wave;
	private Thread animator;
	private boolean kill = false;
	private int maxY = Integer.MIN_VALUE;
	private int minY = Integer.MAX_VALUE;
	private AnimatedWaveRenderer waveRenderer;
	
	private Plot plot;
	private SeismicDataSource dataSource;
	private String channel;
	private BasicStroke boldStroke = new BasicStroke(2.0f);
	private Line2D.Double line = new Line2D.Double();
	private Rectangle rectangle = new Rectangle();
	
	private static int wigglers = 0;
	
	public WigglerPanel(SeismicDataSource sds, String c)
	{
		wigglers++;
		plot = new Plot();
		dataSource = sds;
		channel = c;
		waveRenderer = new AnimatedWaveRenderer();
		plot.addRenderer(waveRenderer);
		AxisRenderer axis = new AxisRenderer(waveRenderer);
		
		axis.setBackgroundColor(Color.white);
		waveRenderer.setAxis(axis);
		
		plot.setBackgroundColor(new Color(0xf7, 0xf7, 0xf7));
		animator = new Thread(this);
		animator.setPriority(Thread.MIN_PRIORITY);
		animator.start();
	}
	
	public synchronized void setWave(Wave w)
	{
		wave = w;
	}
	
	public void kill()
	{
		kill = true;
		wigglers--;
	}

	public void run()
	{
		while (!kill)
		{
			try
			{
				Thread.sleep(100 * wigglers);
				double now = CurrentTime.getInstance().nowJ2K();
				setWave(dataSource.getWave(channel, now - 20, now - 10));
				waveRenderer.setWave(wave);
				repaint();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
		dataSource.close();
		System.out.println("Wiggler killed.");
	}

	public void paint(Graphics g)
	{
		if (getWidth() <= 0 || getHeight() <= 0 || wave == null)
			return;
		
		plot.setSize(getWidth(), getHeight());
		
		int[] dr = wave.getDataRange();
		int miny = Math.min(dr[0], minY);
		int maxy = Math.max(dr[1], maxY);
		if (dr[0] > minY)
			minY += 2;
		if (dr[1] < maxY)
			maxY -= 2;
		
		waveRenderer.setStop(wave.getEndTime() - 0.2);
		waveRenderer.setExtents(wave.getStartTime(), wave.getEndTime(), miny, maxy);
		waveRenderer.setLocation(X_OFFSET, Y_OFFSET, getWidth() - RIGHT_WIDTH - X_OFFSET, getHeight() - BOTTOM_HEIGHT - Y_OFFSET);
		//waveRenderer.getAxis().createDefault();
		
		try {
			plot.render((Graphics2D)g);
		} catch (PlotException e) {
			e.printStackTrace();
		}
	}
	
	class AnimatedWaveRenderer extends FrameRenderer
	{
		private double stop;
		private Wave renderWave;
		
		public void setWave(Wave sw)
		{
			renderWave = sw;	
		}
		
		public void setStop(double d)
		{
			stop = d;
		}
		
		public void render(Graphics2D g)
		{
			Shape origClip = g.getClip();
			
			if (axis != null)
				axis.render(g);
			
			if (renderWave == null || renderWave.buffer == null || renderWave.buffer.length == 0)
				return;
        
			rectangle.setRect(graphX + 1, graphY + 1, graphWidth - 1, graphHeight - 1);
			g.clip(rectangle);
			
			double st = renderWave.getStartTime();
			double step = 1 / renderWave.getSamplingRate();
			int[] buffer = renderWave.buffer;

			int bias = 0;
			int y = buffer[0];
				y = bias;
			
			g.setColor(Color.blue);
			double lastX = getXPixel(st);
			double lastY = getYPixel(y - bias);
			
			boolean flip = false;
			
			Stroke oldStroke = g.getStroke();
			
			for (int i = 1; i < buffer.length - 1; i++)
			{
				float r = 1 - ((float)i / (float)buffer.length);
				if (!flip && r < 0.04)
				{
					g.setColor(Color.red);
					g.setStroke(boldStroke);
					flip = true;
				}
				else if (r >= 0.04)
					g.setColor(spectrum.getColorByRatio(r));
				st += step;
				if (st > stop)
					break;
				
				y = buffer[i];
				
				if (y != Wave.NO_DATA)
					line.setLine(lastX, lastY, getXPixel(st), getYPixel(y));
				
				lastX = getXPixel(st);
				lastY = getYPixel(y);
				
				g.draw(line);
			}
			
			//g.draw(gp);
			g.setStroke(oldStroke);
			g.setClip(origClip);
		}
	}
}
