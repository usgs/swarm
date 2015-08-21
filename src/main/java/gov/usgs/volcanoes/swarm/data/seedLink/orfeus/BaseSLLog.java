package gov.usgs.volcanoes.swarm.data.seedLink.orfeus;

import nl.knmi.orfeus.seedlink.SLLog;

/**
 * Class to manage the logging of information and error messages for SeedLink
 * which allows setting the verbosity level.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class BaseSLLog extends SLLog
{
	public String getErrPrefix()
	{
		return errPrefix;
	}

	public String getLogPrefix()
	{
		return logPrefix;
	}

	public int getVerbosity()
	{
		return verbosity;
	}

	public void setErrPrefix(String errPrefix)
	{
		this.errPrefix = errPrefix;
	}

	public void setLogPrefix(String logPrefix)
	{
		this.logPrefix = logPrefix;
	}

	public void setVerbosity(int verbosity)
	{
		this.verbosity = verbosity;
	}
}
