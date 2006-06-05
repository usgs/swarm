package gov.usgs.swarm;

import java.awt.event.MouseEvent;
import java.util.EventListener;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public interface WaveViewPanelListener extends EventListener
{
	public void waveZoomed(double st, double et);
	public void mousePressed(MouseEvent e);
	public void waveClosed();
	public void waveTimePressed(MouseEvent e, double j2k);
}
