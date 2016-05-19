package gov.usgs.volcanoes.swarm.wave;

import java.awt.event.MouseEvent;

/**
 * 
 * @author Dan Cervelli
 */
public abstract class WaveViewPanelAdapter implements WaveViewPanelListener
{
	public void waveZoomed(AbstractWavePanel src, double st, double et, double nst, double net)
	{}
	
	public void mousePressed(AbstractWavePanel src, MouseEvent e, boolean dragging)
	{}
	
	public void waveClosed(AbstractWavePanel src)
	{}

	public void waveTimePressed(AbstractWavePanel src, MouseEvent e, double j2k)
	{}
}
