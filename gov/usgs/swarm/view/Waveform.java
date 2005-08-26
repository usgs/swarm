package gov.usgs.swarm.view;


/**
 * @author cervelli
 */
public class Waveform
{}
/*

extends WaveComponent implements Configurable
{
	protected WaveRenderer waveRenderer;
	
	protected double minY = Double.MAX_VALUE;
	protected double maxY = -Double.MAX_VALUE;
	
	protected boolean autoScale = true;
	protected boolean autoScaleMemory = true;
	
	protected boolean removeBias = true;
	
	protected double highlightX1;
	protected double highlightX2;
	 
	public Waveform()
	{
		super();
		waveRenderer = new WaveRenderer();
	}
	
	public void setHighlight(double x1, double x2)
	{
		highlightX1 = x1;
		highlightX2 = x2;
	}
	
	public double getMaxY() 
	{
		return maxY;
	}
	
	public void setMaxY(double maxY) 
	{
		this.maxY = maxY;
	}
	
	public double getMinY() 
	{
		return minY;
	}
	
	public void setMinY(double minY) 
	{
		this.minY = minY;
	}
	
	public boolean isAutoScale() 
	{
		return autoScale;
	}
	
	public boolean isAutoScaleMemory() 
	{
		return autoScaleMemory;
	}
	
	public boolean isRemoveBias() 
	{
		return removeBias;
	}
	
	public void setAutoScale(boolean b)
	{
		autoScale = b;
	}
	
	public void setAutoScaleMemory(boolean b)
	{
		autoScaleMemory = b;
	}
	
	public void resetAutoScaleMemory()
	{
		minY = Double.MAX_VALUE;
		maxY = -Double.MAX_VALUE;
	}
	
	public void setYLimits(double min, double max)
	{
		minY = min;
		maxY = max;
	}
	
	public void setRemoveBias(boolean b)
	{
		removeBias = b;
	}
	
	protected void constructImage() 
	{
		long ms = Runtime.getRuntime().freeMemory();
		Graphics2D g2 = createImage(this.getWidth(), this.getHeight(), 
				BufferedImage.TYPE_3BYTE_BGR);
		
		if (model == null || model.getWave() == null)
			return;
		
		plot.clear();
		plot.setSize(this.getSize());
		waveRenderer.setLocation(STANDARD_X_OFFSET, STANDARD_Y_OFFSET,
				this.getWidth() - STANDARD_RIGHT_WIDTH - STANDARD_X_OFFSET,
				this.getHeight() - STANDARD_BOTTOM_HEIGHT - STANDARD_Y_OFFSET);

		Wave wave = model.getWave();
		wave.setSlice(viewStartTime, viewEndTime);
		
		double bias = 0;
		if (removeBias)
			bias = wave.mean();
		
		double wMin = wave.min();
		double wMax = wave.max();
		
		if (autoScale)
		{
			if (autoScaleMemory)
			{
				minY = Math.min(minY, wMin - bias);
				maxY = Math.max(maxY, wMax - bias);
			}
			else
			{
				minY = wMin - bias;
				maxY = wMax - bias;
			}
		}
		
		waveRenderer.setExtents(viewStartTime, viewEndTime, minY, maxY);
		waveRenderer.createDefaultAxis(getStandardHTicks(), getStandardVTicks());
		waveRenderer.getAxis().setLeftLabelAsText("Counts", -52);
		waveRenderer.getAxis().setBottomLeftLabelAsText("Time");
		waveRenderer.setXAxisToTime(getStandardHTicks());
		waveRenderer.setWave(wave);
		waveRenderer.setBias(bias);
		plot.addRenderer(waveRenderer);
		
		plot.render(g2);
		imageValid = true;
	}
	
	public void paint(Graphics g)
	{
		super.paint(g);
		if (!Double.isNaN(highlightX1))
		{
			Graphics2D g2 = (Graphics2D)g;
			int x1 = (int)waveRenderer.getXPixel(highlightX1);
			int x2 = (int)waveRenderer.getXPixel(highlightX2);
			int width = x2 - x1 + 1;
			Paint pnt = g2.getPaint();
			g2.setPaint(new Color(255, 0, 0, 128));
			g2.fillRect(x1, waveRenderer.getGraphY(), width, waveRenderer.getGraphHeight());
			g2.setPaint(pnt);
		}
	}
	
	public BaseDialog getConfigurationDialog() 
	{
		return new WaveformDialog(this);
	}
	
	class WaveRenderer extends FrameRenderer
	{
		private Rectangle rectangle = new Rectangle();
		private Line2D.Double line = new Line2D.Double();
		private double bias;
		private GeneralPath gp;
		private Wave wave;
		
		public void setWave(Wave w)
		{
			wave = w;
		}
		
		public void setBias(double b)
		{
			bias = b;
		}
		
		public void render(Graphics2D g)
		{
			Shape origClip = g.getClip();
			
			if (axis != null)
				axis.render(g);
			
	        g.clip(new Rectangle(graphX + 1, graphY + 1, graphWidth - 1, graphHeight - 1));
			
			double st = wave.getStartTime();
			double step = 1 / wave.getSamplingRate();
			
			if (gp == null)
				gp = new GeneralPath();
			
			gp.reset();
			wave.reset();
			int y = wave.next();
			
			gp.moveTo((float)getXPixel(st), (float)(getYPixel(y - bias)));
			
			g.setColor(Color.blue);

			long ms = Runtime.getRuntime().freeMemory();
			float lastY = (float)getYPixel(y - bias);
			while (wave.hasNext())
			{
				st += step;
				y = wave.next();
				if (y == SampledWave.NO_DATA)
					gp.moveTo((float)getXPixel(st), (float)getYPixel(lastY));
				else
				{
					lastY = (float)getYPixel(y - bias);
					gp.lineTo((float)getXPixel(st), lastY);
				}
			}
//			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.draw(gp);
//			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

			g.setClip(origClip);
		}
	}
}
*/