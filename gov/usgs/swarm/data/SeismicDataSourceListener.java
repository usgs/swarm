package gov.usgs.swarm.data;

import java.util.EventListener;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/08/07 22:35:46  cervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public interface SeismicDataSourceListener extends EventListener
{
	public void channelsUpdated();
	public void channelsProgress(String id, double progress);
}
