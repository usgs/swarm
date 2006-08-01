package gov.usgs.swarm.wave;

import java.awt.event.MouseEvent;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * @author Dan Cervelli
 */
public class WaveViewPanelAdapter implements WaveViewPanelListener
{
	public void waveZoomed(double st, double et)
	{}
	
	public void mousePressed(MouseEvent e)
	{}
	
	public void waveClosed()
	{}

	public void waveTimePressed(MouseEvent e, double j2k)
	{}
}
