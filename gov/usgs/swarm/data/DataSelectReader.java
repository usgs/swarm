package gov.usgs.swarm.data;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import edu.sc.seis.seisFile.BuildVersion;
import edu.sc.seis.seisFile.StringMSeedQueryReader;
import edu.sc.seis.seisFile.dataSelectWS.DataSelectException;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;

public class DataSelectReader extends StringMSeedQueryReader
{

	protected int timeoutMillis;

	protected String urlBase;

	protected String userAgent = "SeisFile/" + BuildVersion.getVersion();

	public static final String DEFAULT_WS_URL = "http://www.iris.edu/ws/dataselect/query";

	public DataSelectReader()
	{
		this(DEFAULT_WS_URL);
	}

	public DataSelectReader(String urlBase)
	{
		this.urlBase = urlBase;
	}

	public DataSelectReader(String urlBase, int timeoutMillis)
	{
		this.urlBase = urlBase;
		this.timeoutMillis = timeoutMillis;
	}

	protected String createQuery(String network, String station,
			String location, String channel) throws IOException,
			DataSelectException, SeedFormatException
	{
		if (location == null || location.trim().length() == 0)
		{
			location = WebServiceUtils.EMPTY_LOC_CODE;
		}
		String query = "net=" + network;
		query += "&sta=" + station;
		query += "&loc=" + location;
		query += "&cha=" + channel;
		return query;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.sc.seis.seisFile.dataSelectWS.MSeedQueryReader#createQuery(java.lang
	 * .String, java.lang.String, java.lang.String, java.lang.String,
	 * java.util.Date, float)
	 */
	@Override
	public String createQuery(String network, String station, String location,
			String channel, Date begin, Date end) throws IOException,
			DataSelectException, SeedFormatException
	{
		String query = createQuery(network, station, location, channel);
		SimpleDateFormat longFormat = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss");
		longFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
		query += "&start=" + longFormat.format(begin);
		query += "&end=" + longFormat.format(end);
		return query;
	}

	public int getTimeoutMillis()
	{
		return timeoutMillis;
	}

	public String getUrlBase()
	{
		return urlBase;
	}

	public String getUserAgent()
	{
		return userAgent;
	}

	/**
	 * Process a data record.
	 * 
	 * @param dr the data record.
	 * @return true if data record should be added to the list, false otherwise.
	 */
	public boolean processRecord(DataRecord dr)
	{
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * edu.sc.seis.seisFile.dataSelectWS.MSeedQueryReader#read(java.lang.String)
	 */
	public List<DataRecord> read(String query) throws IOException,
			DataSelectException, SeedFormatException
	{
		return read(query, new ArrayList<DataRecord>());
	}

	/**
	 * Read the data records.
	 * 
	 * @param query the query.
	 * @param records the data record list or null if none.
	 * @return the data record list.
	 * @throws IOException if an I/O exception occurs.
	 * @throws DataSelectException if not OK repsonse code.
	 * @throws SeedFormatException if the SEED format is invalid.
	 */
	public List<DataRecord> read(String query, List<DataRecord> records)
			throws IOException, DataSelectException, SeedFormatException
	{
		URL requestURL = new URL(urlBase + "?" + query);
		HttpURLConnection conn = (HttpURLConnection) requestURL
				.openConnection();
		if (timeoutMillis != 0)
		{
			conn.setReadTimeout(timeoutMillis);
		}
		conn.setRequestProperty("User-Agent", userAgent);
		conn.connect();
		if (conn.getResponseCode() != 200)
		{
			if (conn.getResponseCode() == 404)
			{
				WebServiceUtils.fine("reponse code 404, no data");
				return records;
			}
			else
			{
				throw new DataSelectException(
						"Did not get an OK repsonse code (code="
								+ conn.getResponseCode() + ", url="
								+ requestURL + "\"");
			}
		}
		BufferedInputStream bif = new BufferedInputStream(conn.getInputStream());
		DataInputStream in = new DataInputStream(bif);
		while (true)
		{
			try
			{
				SeedRecord sr = SeedRecord.read(in);
				if (sr instanceof DataRecord)
				{
					if (processRecord((DataRecord) sr) && records != null)
					{
						records.add((DataRecord) sr);
					}
				}
				else
				{
					WebServiceUtils.warning("Not a data record, skipping..."
							+ sr.getControlHeader().getSequenceNum() + " "
							+ sr.getControlHeader().getTypeCode());
				}
			}
			catch (EOFException e)
			{
				// end of data?
				break;
			}
		}
		in.close();
		return records;
	}

	public void setTimeoutMillis(int timeoutMillis)
	{
		this.timeoutMillis = timeoutMillis;
	}

	public void setUserAgent(String userAgent)
	{
		this.userAgent = userAgent;
	}
}
