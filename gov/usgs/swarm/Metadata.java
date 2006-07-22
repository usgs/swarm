package gov.usgs.swarm;

import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * 
 * $Log: not supported by cvs2svn $
 * Revision 1.1  2006/06/14 19:19:31  dcervelli
 * Major 1.3.4 changes.
 *
 * @author Dan Cervelli
 */
public class Metadata implements Comparable<Metadata>
{
	public static final String DEFAULT_METADATA_FILENAME = "SwarmMetadata.config";
	
	public SCNL scnl;
	
	public String channel = null;
	
	public String alias = null;
	public String unit = null;

	public double multiplier = 1;
	public double offset = 0;
	
	public double longitude = Double.NaN;
	public double latitude = Double.NaN;
	public double height = Double.NaN;
	
	public SeismicDataSource source;
	
	public TimeZone timeZone = TimeZone.getTimeZone("UTC");
	
	public Set<String> groups = null;
	
	public Map<String, String> ancillaryMetadata = null;
	
	public void interpret(String s)
	{
		String[] kv = new String[2];
		kv[0] = s.substring(0, s.indexOf(":")).trim();
		kv[1] = s.substring(s.indexOf(":") + 1).trim();
		if (kv[0].equals("Alias"))
		{
			alias = kv[1];
		}
		else if (kv[0].equals("Unit"))
		{
			unit = kv[1];
		}
		else if (kv[0].equals("Multiplier"))
		{
			multiplier = Double.parseDouble(kv[1]);	
		}
		else if (kv[0].equals("Offset"))
		{
			offset = Double.parseDouble(kv[1]);
		}
		else if (kv[0].equals("Longitude"))
		{
			longitude = Double.parseDouble(kv[1]);
		}
		else if (kv[0].equals("Latitude"))
		{
			latitude = Double.parseDouble(kv[1]);	
		}
		else if (kv[0].equals("Height"))
		{
			height = Double.parseDouble(kv[1]);
		}
		else if (kv[0].equals("TimeZone"))
		{
			timeZone = TimeZone.getTimeZone(kv[1]);
		}
		else if (kv[0].equals("Group"))
		{
			if (groups == null)
				groups = new HashSet<String>();
			groups.add(kv[1]);	
		}
		else
		{
			if (ancillaryMetadata == null)
				ancillaryMetadata = new HashMap<String, String>();
			ancillaryMetadata.put(kv[0], kv[1]);	
		}
	}
	
	public double getLocationHashCode()
	{
		return longitude * 100000 + latitude;
	}
	
	public static Map<String, Metadata> loadMetadata(String fn)
	{
		Map<String, Metadata> data = new HashMap<String, Metadata>();
		ConfigFile cf = new ConfigFile(fn);
		
		Map<String, List<String>> config = cf.getConfig();
		
		for (String key : config.keySet())
		{
			Metadata md = data.get(key);
			if (md == null)
			{
				md = new Metadata();
				data.put(key, md);
				md.channel = key;
				md.scnl = new SCNL(md.channel);
			}
			else
				md = data.get(key);
			
			for (String value : config.get(key))
			{
				for (String item : value.split(";"))
				{
					md.interpret(item);
				}
			}
		}
		
		return data;
	}
	
	public double distanceTo(Metadata other)
	{
		if (other == null || Double.isNaN(latitude) || Double.isNaN(longitude) || Double.isNaN(other.latitude) || Double.isNaN(other.longitude))
			return Double.NaN;
		
		double phi1 = Math.toRadians(latitude);
		double phi2 = Math.toRadians(other.latitude);
		double lam1 = Math.toRadians(longitude);
		double lam2 = Math.toRadians(other.longitude);
		double dlam = lam2 - lam1;
		
		double a = Math.cos(phi2) * Math.sin(dlam);
		double b = Math.cos(phi1) * Math.sin(phi2);
		double c = Math.sin(phi1) * Math.cos(phi2) * Math.cos(dlam);
		double d = Math.sin(phi1) * Math.sin(phi2);
		double e = Math.cos(phi1) * Math.cos(phi2) * Math.cos(dlam);
		double f = Math.atan2(Math.sqrt(a * a + (b - c) * (b - c)), (d + e));
		
		double r = 6372.795;
		
		return f * r;
	}
	
	public static List<Pair<Double, String>> findNearest(Map<String, Metadata> metadata, String channel)
	{
		Metadata md = metadata.get(channel);
		if (md == null || Double.isNaN(md.latitude) || Double.isNaN(md.longitude))
			return null;
		
		ArrayList<Pair<Double, String>> result = new ArrayList<Pair<Double, String>>();
		for (String key : metadata.keySet())
		{
			Metadata other = metadata.get(key);
			double d = md.distanceTo(other);
			if (!Double.isNaN(d) && !other.channel.equals(channel))
			{
				result.add(new Pair<Double, String>(new Double(d), other.channel));
			}
		}
		Collections.sort(result, new Comparator<Pair<Double, String>>()
				{
					public int compare(Pair<Double, String> o1, Pair<Double, String> o2)
					{
						if (Math.abs(o1.item1 - o2.item1) < 0.00001)
							return o1.item2.compareTo(o2.item2);
						else
							return Double.compare(o1.item1, o2.item1);
					}
				});
		return result.size() == 0 ? null : result;
	}
	
	public String toString()
	{
		return channel +  "," + alias + "," + unit + "," + multiplier + "," + offset + "," + longitude + "," + latitude + "," + height + "," + timeZone;
	}

	public int compareTo(Metadata o)
	{
		return channel.compareTo(o.channel);
	}
}
