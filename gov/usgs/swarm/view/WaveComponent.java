package gov.usgs.swarm.view;


/**
 * @author cervelli
 */
abstract public class WaveComponent 
{}
/*
extends JComponent 
{
	protected static final int STANDARD_X_OFFSET = 60;
	protected static final int STANDARD_Y_OFFSET = 20;
	protected static final int STANDARD_RIGHT_WIDTH = 20;
	protected static final int STANDARD_BOTTOM_HEIGHT = 20;
	
	protected WaveModel model;
	protected Plot plot;
	protected BufferedImage image;
	
	protected double viewStartTime = Double.NaN;
	protected double viewEndTime = Double.NaN;
	
	protected boolean imageValid;
	
	protected ActionListener actionListener = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					doCompleteRepaint();
				}
			};
	
	public WaveComponent()
	{
		plot = new Plot();
		plot.setBackgroundColor(new Color(0xf7, 0xf7, 0xf7));
		this.addComponentListener(new ComponentAdapter()
				{
					public void componentResized(ComponentEvent e) 
					{
						wasResized();
					}
				});
	}
	
	public void setModel(WaveModel m)
	{
		model = m;
		if (model != null)
			model.addListener(actionListener);
	}
	
	public WaveModel getModel()
	{
		return model;
	}
	
	public void setViewTimes(double t1, double t2)
	{
		viewStartTime = t1;
		viewEndTime = t2;
		imageValid = false;
	}
	 
	protected int getStandardHTicks()
	{
		return this.getWidth() / 108;
	}
	
	protected int getStandardVTicks()
	{
		return this.getHeight() / 24;
	}
	
	protected void wasResized() 
	{
		doCompleteRepaint();
	}
	
	public void doCompleteRepaint()
	{
		invalidateImage();
		repaint();
	}
	
	public synchronized boolean isImageValid()
	{
		return imageValid;
	}
	
	public synchronized void invalidateImage()
	{
		imageValid = false;
	}
	
	protected synchronized Graphics2D createImage(int w, int h, int t)
	{
		Graphics2D g2 = null;
		if (image != null && image.getWidth() == w && image.getHeight() == h)
			g2 = (Graphics2D)image.getGraphics();
		else
		{
			image = new BufferedImage(w, h, t);
			g2 = (Graphics2D)image.getGraphics();
		}
		return g2;
	}
	
	abstract protected void constructImage();
	
	public synchronized void paint(Graphics g)
	{
		if (!imageValid)
			constructImage();
		
		g.drawImage(image, 0, 0, null);
	}

	public void setSize(int x, int y)
	{
		super.setSize(x, y);
	}
	
	public Dimension getPreferredSize()
	{
		return getSize();	
	}
	
	public Dimension getMinimumSize()
	{
		return getSize();	
	}
	
	public Dimension getMaximumSize()
	{
		return getSize();	
	}
	
	public void dispose()
	{
		model.removeListener(actionListener);
	}
}
*/