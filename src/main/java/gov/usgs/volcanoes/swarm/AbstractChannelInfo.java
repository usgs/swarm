package gov.usgs.volcanoes.swarm;

/**
 * Abstract channel information.
 * 
 * @author Kevin Frechette (ISTI)
 */
public abstract class AbstractChannelInfo extends ChannelUtil implements
		IChannelInfo
{
	/**
	 * Determines if this channel information is the same as another.
	 * 
	 * @return true if this channel information is the same as another, false
	 *         otherwise.
	 */
	public boolean equals(Object obj)
	{
		return obj instanceof IChannelInfo
				&& getFormattedSCNL().equals(
						((IChannelInfo) obj).getFormattedSCNL());
	}

	/**
	 * Get the formatted SCNL.
	 * 
	 * @return the formatted SCNL.
	 */
	public String getFormattedSCNL()
	{
		return getFormattedSCNL(getStation(), getChannel(), getNetwork(),
				getLocation());
	}

	/**
	 * Get the hash code.
	 * 
	 * @return the hash code.
	 */
	public int hashCode()
	{
		return getFormattedSCNL().hashCode();
	}

	/**
	 * Get the string representation of the channel information.
	 * 
	 * @return the string representation of the channel information.
	 */
	public String toString()
	{
		return getFormattedSCNL();
	}
}
