package gov.usgs.volcanoes.swarm.data.seedLink;

import gov.usgs.util.xml.SimpleXMLParser;
import gov.usgs.util.xml.XMLDocHandler;
import gov.usgs.volcanoes.swarm.AbstractChannelInfo;
import gov.usgs.volcanoes.swarm.GroupsType;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SeedLink channel information.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkChannelInfo extends AbstractChannelInfo
{
	/**
	 * SeedLink XML document handler.
	 */
	protected class SeedLinkStationXMLDocHandler implements XMLDocHandler
	{
		private void clearChannel()
		{
			channel = null;
			location = null;
			type = null;
		}

		private void clearStation()
		{
			station = null;
			network = null;
			clearChannel();
		}

		/**
		 * Document ended
		 * 
		 * @throws Exception
		 */
		public void endDocument() throws Exception
		{
		}

		/**
		 * Element ended
		 * 
		 * @param tag Tag name
		 * @throws Exception
		 */
		public void endElement(String tag) throws Exception
		{
		}

		/**
		 * Document started
		 * 
		 * @throws Exception
		 */
		public void startDocument() throws Exception
		{
		}

		/**
		 * Element started
		 * 
		 * @param tag Tag name
		 * @param h map of tag attributes and values
		 * @throws Exception
		 */
		public void startElement(String tag, Map<String, String> h)
				throws Exception
		{
			if (stationTag.equals(tag))
			{
				clearStation();
				for (String key : h.keySet())
				{
					String val = h.get(key);
					if (stationNameTag.equals(key))
					{
						station = val;
					}
					else if (networkTag.equals(key))
					{
						network = val;
					}
				}
			}
			else if (streamTag.equals(tag))
			{
				clearChannel();
				for (String key : h.keySet())
				{
					String val = h.get(key);
					if (channelTag.equals(key))
					{
						channel = val;
					}
					else if (locTag.equals(key))
					{
						location = val;
					}
					else if (typeTag.equals(key))
					{
						type = val;
					}
				}
				if (station != null && network != null && channel != null
						&& location != null && DATA_TYPE.equals(type))
				{
					addChannel(channels, SeedLinkChannelInfo.this, getSource());
				}
			}
		}

		/**
		 * Text or CDATA found
		 * 
		 * @param str
		 * @throws Exception
		 */
		public void text(String str) throws Exception
		{
		}
	}

	/** Data type. */
	public static final String DATA_TYPE = "D";

	public static String readFile(File f) throws IOException
	{
		final FileInputStream stream = new FileInputStream(f);
		try
		{
			FileChannel fc = stream.getChannel();
			MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0,
					fc.size());
			return Charset.defaultCharset().decode(bb).toString();
		}
		finally
		{
			stream.close();
		}
	}

	/**
	 * Write the string to a file.
	 * 
	 * @param f the file.
	 * @param s the string.
	 * @throws IOException if an I/O exception occurs.
	 */
	public static void writeString(File f, String s) throws IOException
	{
		final FileWriter writer = new FileWriter(f);
		try
		{
			writer.write(s);
		}
		finally
		{
			writer.close();
		}
	}

	private String channel;

	private final List<String> channels = new ArrayList<String>();

	private final String channelTag = "seedname";

	/** The data source. */
	private final SeedLinkSource dataSource;

	/** Groups type. */
	private final GroupsType groupsType = GroupsType.NETWORK_AND_SITE;

	private double latitude = Double.NaN, longitude = Double.NaN;

	private String location;

	private final String locTag = "location";

	private String network;

	private final String networkTag = "network";

	private String siteName;

	private String station;

	private final String stationNameTag = "name";

	private final String stationTag = "station";

	private final String streamTag = "stream";

	private String type;

	private final String typeTag = "type";

	public SeedLinkChannelInfo(SeedLinkSource dataSource)
	{
		this.dataSource = dataSource;
	}

	/**
	 * Get the channel name.
	 * 
	 * @return the channel name.
	 */
	public String getChannel()
	{
		return channel;
	}

	/**
	 * Get the channels.
	 * 
	 * @return the list of channels.
	 */
	public List<String> getChannels()
	{
		return channels;
	}

	/**
	 * Get the groups.
	 * 
	 * @return the list of groups.
	 */
	public List<String> getGroups()
	{
		return getGroups(this, groupsType);
	}

	/**
	 * Get the latitude.
	 * 
	 * @return the latitude.
	 */
	public double getLatitude()
	{
		return latitude;
	}

	/**
	 * Get the location.
	 * 
	 * @return the location.
	 */
	public String getLocation()
	{
		return location;
	}

	/**
	 * Get the longitude.
	 * 
	 * @return the longitude.
	 */
	public double getLongitude()
	{
		return longitude;
	}

	/**
	 * Get the network name.
	 * 
	 * @return the network name.
	 */
	public String getNetwork()
	{
		return network;
	}

	/**
	 * Get the site name.
	 * 
	 * @return the site name.
	 */
	public String getSiteName()
	{
		return siteName;
	}

	/**
	 * Get the seismic data source.
	 * 
	 * @return the seismic data source.
	 */
	public SeismicDataSource getSource()
	{
		return dataSource;
	}

	/**
	 * Get the station name.
	 * 
	 * @return the station name.
	 */
	public String getStation()
	{
		return station;
	}

	/**
	 * Get the type.
	 * 
	 * @return the type.
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * Parse the SeedLink XML.
	 * 
	 * @param reader the reader for the SeedLink XML.
	 * @throws Exception if error.
	 */
	public void parse(Reader reader) throws Exception
	{
		SimpleXMLParser.parse(new SeedLinkStationXMLDocHandler(), reader);
	}

	/**
	 * Parse the SeedLink information string.
	 * 
	 * @param infoStr the SeedLink information string.
	 * @throws Exception if error.
	 */
	public void parse(String infoStr) throws Exception
	{
		parse(new StringReader(infoStr));
	}
}
