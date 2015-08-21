package gov.usgs.volcanoes.swarm.data.seedLink.orfeus;

import nl.knmi.orfeus.seedlink.SLLog;
import nl.knmi.orfeus.seedlink.client.SeedLinkConnection;

/**
 * Base class to create a SeedLink connection.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class BaseSLConnection extends SeedLinkConnection
{

	public BaseSLConnection(SLLog sllog)
	{
		super(sllog);
	}

	/**
	 * 
	 * Returns the results of the last INFO request and clears the string.
	 * 
	 * @return concatenation of contents of last terminated set of INFO packets
	 * 
	 */
	public String clearInfoString()
	{
		final String s = getInfoString();
		infoString = "";
		return s;
	}

	/**
	 * 
	 * Returns the results of the last INFO request.
	 * 
	 * @return concatenation of contents of last terminated set of INFO packets
	 * @see #clearInfoString()
	 */
	public String getInfoString()
	{
		return infoString;
	}
}
