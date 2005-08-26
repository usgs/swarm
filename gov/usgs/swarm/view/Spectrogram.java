package gov.usgs.swarm.view;


/**
 * @author cervelli
 */
public class Spectrogram
{}
/*extends WaveComponent
{
	protected void constructImage() 
	{
		Graphics2D g2 = createImage(this.getWidth(), this.getHeight(), 
				BufferedImage.TYPE_3BYTE_BGR);
		
		Wave wv = model.getWave();
		if (wv == null)
			return;
		
		plot.clear();
		Dimension dim = this.getSize();
		plot.setSize(dim);
		
		SpectrogramRenderer sr = new SpectrogramRenderer(wv);
		sr.setViewStartTime(viewStartTime);
		sr.setViewEndTime(viewEndTime);
		sr.setLocation(STANDARD_X_OFFSET, STANDARD_Y_OFFSET, dim.width - STANDARD_RIGHT_WIDTH - STANDARD_X_OFFSET, dim.height - STANDARD_BOTTOM_HEIGHT - STANDARD_Y_OFFSET);
		sr.update(0);
		plot.addRenderer(sr);
		plot.render(g2);
		imageValid = true;
	}
	
	
	private static final int[] SAMPLE_SIZES = new int[] {64, 128, 256, 512, 1024, 2048};
	
	private String fftSize = "Auto";
	
	protected boolean autoScale = true;
	protected boolean autoScaleMemory = true;
	protected boolean logPower = false;
	protected boolean logFreq = false;
	
	protected double minFreq = 0.75;
	protected double maxFreq = 20;
	protected double maxPower = -Double.MAX_VALUE;
	
	protected double spectrogramOverlap = 0.2;
	
	protected byte[] imgBuffer = new byte[64000];
	protected BufferedImage img;
	
	protected Spectrum spectrum = Jet.getInstance();
	//protected SampleModel sm = spectrum.palette.createCompatibleSampleModel(imgXSize, imgYSize);
	protected SampleModel sm;
	
	protected ImageDataRenderer image = new ImageDataRenderer(null);
	protected AxisRenderer axis = new AxisRenderer(image);
	
	protected MemoryImageSource mis;
	protected Image im;
	
	protected void constructImage() 
	{
		Graphics2D g2 = createImage(this.getWidth(), this.getHeight(), 
				BufferedImage.TYPE_3BYTE_BGR);
		
		Wave wv = model.getWave();
		if (wv == null)
			return;
		wv.setSlice(viewStartTime, viewEndTime);
		
		plot.clear();
		Dimension dim = this.getSize();
		plot.setSize(dim);
		int hTicks = dim.width / 108;
		int vTicks = dim.height / 24;
		int sampleSize = 128;
			
		if (fftSize.equals("Auto"))
		{
			double bestFit = 1E300;
			int bestIndex = -1;
			for (int i = 0; i < SAMPLE_SIZES.length; i++)
			{
				double xs = (double)wv.samples() / (double)SAMPLE_SIZES[i];
				double ys = (double)SAMPLE_SIZES[i] / 2;
				double ar = xs / ys;
				double fit = Math.abs(ar - 1);
				if (fit < bestFit)
				{
					bestFit = fit;
					bestIndex = i;
				}
			}
			sampleSize = SAMPLE_SIZES[bestIndex];
		}
		else
			sampleSize = Integer.parseInt(fftSize);

		int imgXSize = wv.samples() / sampleSize;
		int imgYSize = sampleSize / 2;
		if (imgXSize <= 0 || imgYSize <= 0)
			return;

		double minF = minFreq;
		double maxF = maxFreq;
		double maxMag = -1E300;
		double mag, f;
		double[][] powerBuffer = wv.toSpectrogram(sampleSize, logFreq, logPower, spectrogramOverlap);
		imgYSize = powerBuffer[0].length;
		imgXSize = powerBuffer.length;
		
		double tm = viewEndTime - viewStartTime;
		for (int i = 0; i < imgXSize; i++)
		{
			for (int j = 0; j < imgYSize; j++)
			{
				f = ((double)j / (double)(imgYSize)) * wv.getSamplingRate();
				mag = powerBuffer[i][j];
				if (f >= minF && f <= maxF && mag > maxMag)
					maxMag = mag;
			}
		}

		if (autoScale)		
		{
			if (logPower)
				maxMag = Math.pow(10, maxMag);
			
			if (autoScaleMemory)
			{
				maxPower = Math.max(maxPower, maxMag);
				maxMag = maxPower;
			}
		}
		else
			maxMag = maxPower;

		if (logPower)
			maxMag = Math.log(maxMag) / Math.log(10);
		
		if (imgBuffer.length < imgXSize * imgYSize)
			imgBuffer = new byte[imgXSize * imgYSize];
		
		for (int i = 0; i < imgXSize; i++)
			for (int j = imgYSize - 1, k = 0; j >= 0; j--, k++)
				imgBuffer[i + imgXSize * k] = (byte)(spectrum.getColorIndexByRatio(powerBuffer[i][j] / maxMag) + 9);

		//if (sm == null || sm.getWidth() != imgXSize || sm.getHeight() != imgYSize)
		//	sm = spectrum.palette.createCompatibleSampleModel(imgXSize, imgYSize);
		
		if (mis == null || im.getWidth(null) != imgXSize || im.getHeight(null) != imgYSize)
		{
			//img = new BufferedImage(imgXSize, imgYSize, BufferedImage.TYPE_BYTE_INDEXED, spectrum.palette);
			mis = new MemoryImageSource(imgXSize, imgYSize, spectrum.palette,
					imgBuffer, 0, imgXSize);
		}
		
		im = Toolkit.getDefaultToolkit().createImage(mis);
		//DataBufferByte dbb = new DataBufferByte(imgBuffer, imgXSize * imgYSize);
		//Raster raster = Raster.createRaster(sm, dbb, new Point(0, 0));
		//img.setData(raster);
		//img.getGraphics().drawImage(im, 0, 0, null);
		
		image.setImage(im);
		image.setDataExtents(viewStartTime, viewEndTime, 0, wv.getSamplingRate() / 2);				 
		image.setLocation(STANDARD_X_OFFSET, STANDARD_Y_OFFSET, dim.width - STANDARD_RIGHT_WIDTH - STANDARD_X_OFFSET, dim.height - STANDARD_BOTTOM_HEIGHT - STANDARD_Y_OFFSET);
		
		image.setExtents(viewStartTime, viewEndTime, minF, maxF);
		image.createDefaultAxis(hTicks, vTicks);
		image.getAxis().createDefault();
		image.setXAxisToTime(hTicks);
		image.getAxis().setLeftLabelAsText("Frequency (Hz)", -52);
		image.getAxis().setBottomLabelAsTextLowerLeft("Time");
		plot.addRenderer(image);

		plot.render(g2);
		imageValid = true;
	}
	
}
*/