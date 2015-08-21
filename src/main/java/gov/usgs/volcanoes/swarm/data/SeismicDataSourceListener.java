package gov.usgs.volcanoes.swarm.data;

import java.util.EventListener;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.2  2006/08/12 21:51:53  dcervelli
 * Addition of id to channelProgress().
 *
 * Revision 1.1  2006/08/07 22:35:46  cervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public interface SeismicDataSourceListener extends EventListener
{
	public void channelsUpdated();
	public void channelsProgress(String id, double progress);
	
	public void helicorderProgress(String channel, double progress);
}
