package gov.usgs.swarm.view;


/**
 * @author cervelli
 */
public class Spectra 
{}
/*
extends WaveComponent implements Configurable
{
	private static final double LN10 = Math.log(10);

	protected boolean autoScale = true;
	protected boolean autoScaleMemory = true;
	protected boolean logPower = false;
	protected boolean logFreq = false;
	
	protected double minFreq = 0.75;
	protected double maxFreq = 20;
	protected double maxPower = -Double.MAX_VALUE;

	public Spectra()
	{
		super();
	}
	
	protected void constructImage() 
	{
		Graphics2D g2 = createImage(this.getWidth(), this.getHeight(), 
				BufferedImage.TYPE_3BYTE_BGR);
		
		Wave wv = model.getWave();
		wv.setSlice(viewStartTime, viewEndTime);
		if (wv == null)
			return;
		
		plot.clear();
		plot.setSize(this.getSize());
		Dimension dim = this.getSize();
		int hTicks = dim.width / 92;
		int vTicks = dim.height / 24;
		double[][] data = wv.fft();
		data = FFT.halve(data);
		FFT.toPowerFreq(data, wv.getSamplingRate(), logPower, logFreq);
		double minF = minFreq;
		double maxF = maxFreq;
		if (logFreq) 
		{
			if (minF == 0)
				minF = data[3][0];
			else
				minF = Math.log(minF) / LN10;
			maxF = Math.log(maxF) / LN10;
		}
		double maxp = -1E300;
		double minp = 1E300;
		for (int i = 2; i < data.length; i++) 
		{
			if (data[i][0] >= minF && data[i][0] <= maxF) 
			{
				if (data[i][1] > maxp)
					maxp = data[i][1];
				if (data[i][1] < minp)
					minp = data[i][1];
			}
		}

		Data d = new Data(data);
		DataRenderer dr = new DataRenderer(d);
		dr.setLocation(STANDARD_X_OFFSET, STANDARD_Y_OFFSET, dim.width
				- STANDARD_RIGHT_WIDTH - STANDARD_X_OFFSET, dim.height
				- STANDARD_BOTTOM_HEIGHT - STANDARD_Y_OFFSET);

		if (autoScale) 
		{
			if (logPower)
				maxp = Math.pow(10, maxp);

			if (autoScaleMemory) 
			{
				maxPower = Math.max(maxPower, maxp);
				maxp = maxPower;
			}
		}
		else
			maxp = maxPower;

		if (logPower)
			maxp = Math.log(maxp) / Math.log(10);
		dr.setExtents(minF, maxF, 0, maxp);
		dr.createDefaultAxis(hTicks, vTicks, false, false);
		if (logFreq)
			dr.createDefaultLogXAxis(5);
		if (logPower)
			dr.createDefaultLogYAxis(2);

		dr.createDefaultLineRenderers();
		dr.getAxis().setLeftLabelAsText("Power", -52);
		dr.getAxis().setBottomLeftLabelAsText("Freq.");
		plot.addRenderer(dr);
		
		plot.render(g2);
		imageValid = true;
	}

	public BaseDialog getConfigurationDialog()
	{
		return new SpectraDialog(this);
	}
	
    public boolean isAutoScale()
    {
        return autoScale;
    }
    public void setAutoScale(boolean autoScale)
    {
        this.autoScale = autoScale;
    }
    public boolean isAutoScaleMemory()
    {
        return autoScaleMemory;
    }
    public void setAutoScaleMemory(boolean autoScaleMemory)
    {
        this.autoScaleMemory = autoScaleMemory;
    }
    public boolean isLogFreq()
    {
        return logFreq;
    }
    public void setLogFreq(boolean logFreq)
    {
        this.logFreq = logFreq;
    }
    public boolean isLogPower()
    {
        return logPower;
    }
    public void setLogPower(boolean logPower)
    {
        this.logPower = logPower;
    }
    public double getMaxFreq()
    {
        return maxFreq;
    }
    public void setMaxFreq(double maxFreq)
    {
        this.maxFreq = maxFreq;
    }
    public double getMaxPower()
    {
        return maxPower;
    }
    public void setMaxPower(double maxPower)
    {
        this.maxPower = maxPower;
    }
    public double getMinFreq()
    {
        return minFreq;
    }
    public void setMinFreq(double minFreq)
    {
        this.minFreq = minFreq;
    }
}*/