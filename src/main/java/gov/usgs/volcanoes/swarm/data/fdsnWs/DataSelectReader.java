package gov.usgs.volcanoes.swarm.data.fdsnWs;

import edu.sc.seis.seisFile.SeisFileException;
import edu.sc.seis.seisFile.StringMSeedQueryReader;
import edu.sc.seis.seisFile.mseed.DataRecord;
import edu.sc.seis.seisFile.mseed.SeedFormatException;
import edu.sc.seis.seisFile.mseed.SeedRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.Version;

/**
 * The data select reader reads data from the web services. This class a based
 * on the edu.sc.seis.seisFile.dataSelectWS.DataSelectReader class with minor
 * changes for Swarm.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class DataSelectReader extends StringMSeedQueryReader {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataSelectReader.class);

  /** The timeout in milliseconds. */
  protected int timeoutMillis;
  /** The URL base. */
  protected String urlBase;
  /** The user agent. */
  protected String userAgent = "Swarm/" + Version.POM_VERSION;
  /** The default web services URL. */
  public static final String DEFAULT_WS_URL = "http://service.iris.edu/fdsnws/dataselect/1/query";

  /**
   * Create the data select reader with the default URL and timeout.
   */
  public DataSelectReader() {
    this(DEFAULT_WS_URL);
  }

  /**
   * Create the data select reader with the specified URL and the default
   * timeout.
   * 
   * @param urlBase the URL base.
   */
  public DataSelectReader(String urlBase) {
    this.urlBase = urlBase;
  }

  /**
   * Create the data select reader with the specified URL and timeout.
   * 
   * @param urlBase the URL base.
   * @param timeoutMillis the timeout in milliseconds.
   */
  public DataSelectReader(String urlBase, int timeoutMillis) {
    this.urlBase = urlBase;
    this.timeoutMillis = timeoutMillis;
  }

  /**
   * Create the query. All arguments must be provided.
   * 
   * @param network the network.
   * @param station the station.
   * @param location the location or empty for the empty location code.
   * @param channel the channel.
   * @return the query.
   * @throws IOException if an I/O Exception occurs.
   * @throws DataSelectException if data select error.
   * @throws SeedFormatException if SEED format error.
   * @see WebServiceUtils.EMPTY_LOC_CODE
   */
  protected String createQuery(String network, String station, String location, String channel) {
    if (location == null || location.trim().length() == 0) {
      location = WebServiceUtils.EMPTY_LOC_CODE;
    }
    String query = "net=" + network;
    query += "&sta=" + station;
    query += "&loc=" + location;
    query += "&cha=" + channel;
    return query;
  }

  /**
   * Create the query. All arguments must be provided.
   * 
   * @param network the network.
   * @param station the station.
   * @param location the location or empty for the empty location code.
   * @param channel the channel.
   * @param begin the begin date.
   * @param end the end date.
   * @return the query.
   * @see WebServiceUtils.EMPTY_LOC_CODE
   * @see edu.sc.seis.seisFile.dataSelectWS.MSeedQueryReader#createQuery(java.lang
   *      .String, java.lang.String, java.lang.String, java.lang.String,
   *      java.util.Date, float)
   */
  @Override
  public String createQuery(String network, String station, String location, String channel,
      Date begin, Date end) {
    String query;
    query = createQuery(network, station, location, channel);
    SimpleDateFormat longFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    longFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    query += "&start=" + longFormat.format(begin);
    query += "&end=" + longFormat.format(end);
    return query;
  }

  /**
   * Get the timeout in milliseconds.
   * 
   * @return the timeout in milliseconds.
   */
  public int getTimeoutMillis() {
    return timeoutMillis;
  }

  /**
   * Get the URL base.
   * 
   * @return the URL base.
   */
  public String getUrlBase() {
    return urlBase;
  }

  /**
   * Get the user agent.
   * 
   * @return the user agent.
   */
  public String getUserAgent() {
    return userAgent;
  }

  /**
   * Process a data record.
   * 
   * @param dr the data record.
   * @return true if data record should be added to the list, false otherwise.
   */
  public boolean processRecord(DataRecord dr) {
    return true;
  }

  /*
   * Read the data records.
   * 
   * @param query the query.
   * 
   * @return the data record list.
   * 
   * @see
   * edu.sc.seis.seisFile.dataSelectWS.MSeedQueryReader#read(java.lang.String)
   */
  public List<DataRecord> read(String query) throws IOException, SeisFileException {
    return read(query, new ArrayList<DataRecord>());
  }

  /**
   * Read the data records.
   * 
   * @param query the query.
   * @param records the data record list or null if none.
   * @return the data record list.
   * @throws IOException if an I/O exception occurs.
   * @throws SeisFileException
   * @throws DataSelectException if not OK repsonse code.
   */
  public List<DataRecord> read(String query, List<DataRecord> records)
      throws IOException, SeisFileException {
    URL requestURL = new URL(urlBase + "?" + query);
    HttpURLConnection conn = (HttpURLConnection) requestURL.openConnection();
    if (timeoutMillis != 0) {
      conn.setReadTimeout(timeoutMillis);
    }
    conn.setRequestProperty("User-Agent", userAgent);
    conn.connect();
    if (conn.getResponseCode() != 200) {
      if (conn.getResponseCode() == 404) {
        LOGGER.info("reponse code 404, no data");
        return records;
      } else {
        throw new SeisFileException("Did not get an OK repsonse code (code="
            + conn.getResponseCode() + ", url=" + requestURL + "\"");
      }
    }
    BufferedInputStream bif = new BufferedInputStream(conn.getInputStream());
    DataInputStream in = new DataInputStream(bif);
    while (true) {
      try {
        SeedRecord sr = SeedRecord.read(in);
        if (sr instanceof DataRecord) {
          if (processRecord((DataRecord) sr) && records != null) {
            records.add((DataRecord) sr);
          }
        } else {
          LOGGER.warn("Not a data record, skipping... {} {}",
              sr.getControlHeader().getSequenceNum(), sr.getControlHeader().getTypeCode());
        }
      } catch (EOFException e) {
        // end of data?
        break;
      }
    }
    in.close();
    return records;
  }

  /**
   * Set the timeout in milliseconds.
   * 
   * @param timeoutMillis the timeout in milliseconds.
   */
  public void setTimeoutMillis(int timeoutMillis) {
    this.timeoutMillis = timeoutMillis;
  }

  /**
   * Set the user agent.
   * 
   * @param userAgent the user agent.
   */
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }
}
