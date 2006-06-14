package gov.usgs.swarm;

import java.util.EventListener;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public interface HelicorderViewPanelListener extends EventListener
{
	public void insetCreated(double st, double et);
}
