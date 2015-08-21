package gov.usgs.volcanoes.swarm.time;

import java.util.EventListener;

/**
 * @author Dan Cervelli
 */
public interface TimeListener extends EventListener
{
	public void timeChanged(double j2k); 
}
