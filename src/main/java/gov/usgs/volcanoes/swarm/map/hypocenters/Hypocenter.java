package gov.usgs.volcanoes.swarm.map.hypocenters;

import gov.usgs.math.Geometry;
import gov.usgs.plot.render.DataPointRenderer;
import gov.usgs.util.Pair;
import gov.usgs.volcanoes.swarm.Metadata;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.map.ClickableGeoLabel;
import gov.usgs.volcanoes.swarm.wave.MultiMonitor;
import gov.usgs.volcanoes.swarm.wave.SwarmMultiMonitors;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 
 * @author Dan Cervelli
 */
public class Hypocenter extends ClickableGeoLabel
{
	public double time;
	public double depth;
	public double magnitude;
	
	private static SwarmConfig swarmConfig;
	
	public Hypocenter() 
	{
		swarmConfig = SwarmConfig.getInstance();
		
		DataPointRenderer r = new DataPointRenderer();
		r.antiAlias = true;
		r.stroke = new BasicStroke(1.2f);
		r.filled = true;
		r.paint = Color.RED;
		r.color = Color.yellow;
		r.shape = Geometry.STAR_10;
		marker = r;
	}
	
	@Override
	public Rectangle getClickBox()
	{
		return new Rectangle(-7, -7, 17, 17);
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		Map<String, Metadata> metadata = swarmConfig.getMetadata();
		List<Pair<Double, String>> nrst = Metadata.findNearest(swarmConfig.getMetadata(), location, true);
		Set<MultiMonitor> cleared = new HashSet<MultiMonitor>();
		if (nrst != null)
		{
			for (int i = 0, total = 0; i < nrst.size() && total < 10; i++)
			{
				String ch = nrst.get(i).item2;
				if (ch.matches(".* ..Z .*"))
				{
					Metadata md = metadata.get(ch);
					MultiMonitor mm = SwarmMultiMonitors.getMonitor(md.source);
					if (!cleared.contains(mm))
					{
						mm.removeAllWaves();
						cleared.add(mm);
					}
					mm.addChannel(ch);
					mm.setVisible(true);
					mm.setPauseStartTime(time - 4);
					total++;
				}
			}
		}
	}
}
