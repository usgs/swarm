package gov.usgs.swarm.exp;


/**
 * @author dcervelli
 *
 */
public class ParticleView
{}
/*
extends JComponent
{
	private Plot plot;
	private WaveViewPanel wvp1;
	private WaveViewPanel wvp2;
	private SampledWave eastWave;
	private SampledWave northWave;
	//private SampledWave upWave;
	
	public ParticleView()
	{
		this.setBorder(LineBorder.createGrayLineBorder());
		plot = new Plot();
		plot.setBackgroundColor(new Color(0xf7, 0xf7, 0xf7));
	}
	
	public void setWaves(SampledWave ew, SampledWave nw, SampledWave uw)
	{
		eastWave = ew;
		northWave = nw;
		wvp1 = new WaveViewPanel();
		wvp1.setSampledWave(eastWave, eastWave.getStartTime(), eastWave.getEndTime());
		wvp1.setSize(600, 200);
		wvp1.setLocation(0, 600);
		wvp2 = new WaveViewPanel();
		wvp2.setSampledWave(northWave, northWave.getStartTime(), northWave.getEndTime());
		wvp2.setSize(600, 200);
		wvp2.setLocation(0, 800);
		this.add(wvp1);
		this.add(wvp2);
		//upWave = uw;
	}
	
	public void paint(Graphics g)
	{
		Graphics2D g2 = (Graphics2D)g;
		Dimension dim = this.getSize();
		plot.setSize(dim.width, dim.width);
		
		int[] eastRange = eastWave.getDataRange();
		int[] northRange = northWave.getDataRange();
		
		
		double[] e = eastWave.integrate();
		double[] n = northWave.integrate();
		double[][] dd = new double[e.length][2];
		
		double maxE = -Double.MAX_VALUE;
		double minE = Double.MAX_VALUE;
		double maxN = -Double.MAX_VALUE;
		double minN = Double.MAX_VALUE;
		for (int i = 0; i < e.length; i++)
		{
			dd[i][0] = e[i];
			dd[i][1] = n[i];
			minE = Math.min(minE, e[i]);
			maxE = Math.max(maxE, e[i]);
			minN = Math.min(minN, e[i]);
			maxN = Math.max(maxN, e[i]);
		}
		
		ParticleRenderer dr = new ParticleRenderer(dd);
		dr.setLocation(60, 60, dim.width - 120, dim.width - 120);
		wvp1.setSize(dim.width, 220);
		wvp1.setLocation(0, dim.width);
		wvp2.setSize(dim.width, 220);
		wvp2.setLocation(0, dim.width + 220);
		dr.setExtents(minE, maxE, minN, maxN);
		dr.createDefaultAxis(8, 8);
		dr.getAxis().setLeftLabelAsText("North");
		dr.getAxis().setBottomLeftLabelAsText("East");
		
		plot.addRenderer(dr);
		super.paint(g);
		plot.render(g2);
	}
	
	class ParticleRenderer extends FrameRenderer
	{
		double[][] data;
		
		public ParticleRenderer(double[][] d)
		{
			data = d;
		}
		
		public void render(Graphics2D g)
		{
			Shape origClip = g.getClip();
			
			if (axis != null)
				axis.render(g);
			
        
	        g.clip(new Rectangle(graphX + 1, graphY + 1, graphWidth - 1, graphHeight - 1));
			
			g.setColor(Color.blue);
			
			Line2D.Double line = new Line2D.Double();
			
			Jet jet = (Jet)Jet.getInstance();
			for (int i = 1; i < data.length - 1; i++)
			{
				g.setColor(jet.getColorByRatio((float)i / (float)data.length));
				line.setLine(getXPixel(data[i][0]), getYPixel(data[i][1]),
						getXPixel(data[i + 1][0]), getYPixel(data[i + 1][1]));
				
				g.draw(line);
			}

			g.setClip(origClip);
		}	
	}
}
*/