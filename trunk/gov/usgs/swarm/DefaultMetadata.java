package gov.usgs.swarm;

import gov.usgs.util.CodeTimer;
import gov.usgs.util.ResourceReader;

import java.util.ArrayList;
import java.util.Collections;

/**
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 */
public class DefaultMetadata
{
	private static ArrayList<Entry> entries;
	
	private static class Entry implements Comparable<String>
	{
		public String station;
		public double longitude;
		public double latitude;
		
		public int compareTo(String oe)
		{
			return station.compareTo(oe);
		}
	}
	
	public static Metadata getMetadata(String ch)
	{
		if (entries == null)
		{
			entries = new ArrayList<Entry>();
			ResourceReader rr = ResourceReader.getResourceReader("DefaultLatLon.config");
			if (rr == null)
				return null;
			
			String s = null;
			while ((s = rr.nextLine()) != null)
			{
				String[] ss = s.split("\t");
				Entry e = new Entry();
				e.station = ss[0];
				e.longitude = Double.parseDouble(ss[1]);
				e.latitude = Double.parseDouble(ss[2]);
				entries.add(e);
			}
		}
		
		String station = ch.split(" ")[0];
		int i = Collections.binarySearch(entries, station);
		Metadata md = null;
		if (i >= 0)
		{
			Entry e = entries.get(i);
			md = new Metadata(ch);
			md.updateLongitude(e.longitude);
			md.updateLatitude(e.latitude);
		}
		return md;
	}
	
	public static void main(String[] args)
	{
		CodeTimer ct = new CodeTimer();
		for (String a : args)
		{
			Metadata md = DefaultMetadata.getMetadata(a);
			System.out.println(md);
		}
		ct.stopAndReport();
	}
}
