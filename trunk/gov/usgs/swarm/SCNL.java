package gov.usgs.swarm;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class SCNL
{
	public String station;
	public String channel;
	public String network;
	public String location;
	
	public SCNL(String s)
	{
		String[] ss = s.split(" ");
		station = ss[0];
		if (ss.length >= 3)
		{
			channel = ss[1];
			network = ss[2];
		}
		if (ss.length >= 4)
		{
			location = ss[3];
		}
	}
	
	public String toString()
	{
		return station + " " + channel + " " + network + (location != null ? (" " + location) : "");
	}
}
