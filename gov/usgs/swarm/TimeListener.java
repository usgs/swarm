package gov.usgs.swarm;

import java.util.EventListener;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public interface TimeListener extends EventListener
{
	public void timeChanged(double j2k); 
}
