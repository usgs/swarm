package nz.org.geonet.data;

import gov.usgs.util.ResourceReader;
import gov.usgs.util.xml.SimpleXMLParser;
import gov.usgs.util.xml.XMLDocHandler;
import gov.usgs.util.xml.XMLToMap;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import gov.usgs.volcanoes.swarm.map.ClickableGeoLabel;
import gov.usgs.volcanoes.swarm.map.LabelSource;
import gov.usgs.volcanoes.swarm.map.hypocenters.Hypocenter;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class for getting hypocenter information from the GeoNet RSS feed.
 * 
 * $Log: not supported by cvs2svn $
 * @author Dan Cervelli
 * @version $Id: RSSHypocenters.java,v 1.1 2007-05-21 02:59:38 dcervelli Exp $
 */
public class RSSHypocenters implements LabelSource
{
	public static final String RSS_URL = "http://www.geonet.org.nz/feeds/earthquake/geonet-recent-quakes-1.0.xml";
	public static final String QUAKE_URL = "http://www.geonet.org.nz/services/quake/";
	
	private static class GetQuakeIDs implements XMLDocHandler
	{
		public List<String> ids = new ArrayList<String>();
		private boolean idTag;
		
		public void endDocument() throws Exception
		{
		}

		public void endElement(String tag) throws Exception
		{
		}

		public void startDocument() throws Exception
		{
		}

		public void startElement(String tag, Map<String, String> h) throws Exception
		{
			if ("quake:id".equals(tag))
				idTag = true;
		}

		public void text(String str) throws Exception
		{
			if (idTag)
			{
				ids.add(str);
				idTag = false;
			}
		}
	}
	
	public List<? extends ClickableGeoLabel> getLabels()
	{
		ResourceReader rr = ResourceReader.getResourceReader(RSS_URL);
		List<Hypocenter> hypos = new ArrayList<Hypocenter>();
		try
		{
			GetQuakeIDs q = new GetQuakeIDs();
			SimpleXMLParser.parse(q, rr.getReader());
			for (String id : q.ids)
			{
				System.out.println(id);
				XMLToMap qm = new XMLToMap();
				ResourceReader qr = ResourceReader.getResourceReader(QUAKE_URL + "/" + id);
				SimpleXMLParser.parse(qm, qr.getReader());
				Hypocenter h = new Hypocenter();
				double lat = Double.parseDouble(qm.text.get("report/lat"));
				double lon = Double.parseDouble(qm.text.get("report/lon"));
				h.location = new Point2D.Double(lon, lat);
				h.text = "M" + qm.text.get("report/mag");
				h.depth = Double.parseDouble(qm.text.get("report/depth"));
				String year = qm.text.get("report/uttime/year");
				String mo = qm.text.get("report/uttime/month");
				String day = qm.text.get("report/uttime/day");
				String hour = qm.text.get("report/uttime/hour");
				String minute = qm.text.get("report/uttime/minute");
				String second = qm.text.get("report/uttime/second");
				String ms = qm.text.get("report/uttime/msec");
				String ds = String.format("%s-%s-%s %s:%s:%s.%s", year, mo, day, hour, minute, second, ms);
				h.time = J2kSec.parse(Time.STANDARD_TIME_FORMAT_MS, ds);
				hypos.add(h);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return hypos;
	}
	
	public static void main(String[] args)
	{
		new RSSHypocenters().getLabels();
	}
}
