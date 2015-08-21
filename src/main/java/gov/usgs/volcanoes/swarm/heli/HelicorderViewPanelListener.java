package gov.usgs.volcanoes.swarm.heli;

import java.util.EventListener;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * @author Dan Cervelli
 */
public interface HelicorderViewPanelListener extends EventListener
{
	public void insetCreated(double st, double et);
}
