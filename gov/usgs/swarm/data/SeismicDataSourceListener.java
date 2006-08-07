package gov.usgs.swarm.data;

import java.util.EventListener;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public interface SeismicDataSourceListener extends EventListener
{
	public void channelsUpdated();
	public void channelsProgress(double progress);
}
