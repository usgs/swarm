package gov.usgs.swarm;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/03/02 00:51:25  dcervelli
 * Initial commit.
 *
 * @author Dan Cervelli
 */
public class Calibration
{
	public static final Calibration IDENTITY = new Calibration("Counts", 1, 0, null);
	public double multiplier;
	public double offset;
	public String unit;
	public String alias;
	
	public Calibration(String u, double m, double b, String a)
	{
		multiplier = m;
		offset = b;
		unit = u;
		alias = a;
	}
	
	public static Calibration fromString(String c)
	{
		try
		{
			Calibration cal = null;
			String[] ss = c.split(";");
			if (ss.length < 4)
				cal = new Calibration(ss[0], Double.parseDouble(ss[1]), Double.parseDouble(ss[2]), null);
			else
				cal = new Calibration(ss[0], Double.parseDouble(ss[1]), Double.parseDouble(ss[2]), ss[3]);
			
			return cal;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
