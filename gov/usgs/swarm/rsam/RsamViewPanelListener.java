package gov.usgs.swarm.rsam;

import gov.usgs.swarm.wave.WaveViewPanel;

import java.awt.event.MouseEvent;
import java.util.EventListener;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/08/01 23:45:23  cervelli
 * Moved package.
 *
 * Revision 1.1  2006/06/05 18:06:49  dcervelli
 * Major 1.3 changes.
 *
 * @author Dan Cervelli
 */
public interface RsamViewPanelListener extends EventListener
{
	public void waveZoomed(RsamViewPanel src, double oldST, double oldET, double newST, double newET);
	public void mousePressed(RsamViewPanel src, MouseEvent e, boolean dragging);
	public void waveClosed(RsamViewPanel src);
	public void waveTimePressed(RsamViewPanel src, MouseEvent e, double j2k);
}
