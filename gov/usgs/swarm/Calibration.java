package gov.usgs.swarm;

/**
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class Calibration
{
	public static final Calibration IDENTITY = new Calibration("Counts", 1, 0);
	public double multiplier;
	public double offset;
	public String unit;
	
	public Calibration(String u, double m, double b)
	{
		multiplier = m;
		offset = b;
		unit = u;
	}
	
	public static Calibration fromString(String c)
	{
		try
		{
			String[] ss = c.split(";");
			Calibration cal = new Calibration(ss[0], Double.parseDouble(ss[1]), Double.parseDouble(ss[2]));
			return cal;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
