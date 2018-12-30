/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0
 * Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.data.seedlink;

import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.BlocketteDecoratorFactory;
import edu.iris.Fissures.seed.container.Btime;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.exception.SeedException;

import gov.usgs.volcanoes.core.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.swarm.ChannelInfo;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;

import nl.knmi.orfeus.seedlink.SLLog;
import nl.knmi.orfeus.seedlink.SLPacket;
import nl.knmi.orfeus.seedlink.SeedLinkException;
import nl.knmi.orfeus.seedlink.client.SeedLinkConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SeedLink client.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkClient implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SeedLinkClient.class);

  /** SeedLink server address. */
  private String sladdr;

  /** The wave list or null if none. */
  private List<Wave> waveList;

  /** BaseSLConnection object for communicating with the BaseSLConnection over a socket. */
  private SeedLinkConnection slconn;

  /** INFO LEVEL for info request only. */
  private String infolevel = null;

  /** Multiselect string.  Example: "IU_KONO:BHE BHN,GE_WLF,MN_AQU:HH?.D" */
  private String multiselect = null;
  
  /** SCNL's and last request time. */
  private TreeMap<String, Double> scnlMap = new TreeMap<String, Double>(); 
  
  /** Client thread. */
  private Thread thread;

  /** Start and end time of thread. In J2k seconds. */
  private double startTime = Double.MAX_VALUE;
  private double endTime = 0;

  /**
   * Create SeedLink client with channel, start and end time.
   * 
   * @param host seedlink server host
   * @param port seedlink server port
   * @param startTime data request start time
   * @param endTime data request end time
   * @param scnl channel to get
   */
  public SeedLinkClient(String host, int port, double startTime, double endTime, String scnl) {
    super();
    sladdr = host + ":" + port;
    scnlMap.put(scnl, J2kSec.now());
    createConnection();
    setStartEndTimes(startTime, endTime);
    // slconn.setLastpkttime(true);
    LOGGER.debug("SeedLinkClient initialized: {} {} {} {} ", sladdr, multiselect,
        j2kToSeedLinkDateString(startTime), j2kToSeedLinkDateString(endTime));
  }

  /**
   * Create SeedLink client.
   * 
   * @param host the server host.
   * @param port the server port.
   */
  public SeedLinkClient(String host, int port) {
    super();   
    sladdr = host + ":" + port;
    createConnection();
  }

  protected void setStartEndTimes(double st, double et) {
    this.startTime = st;
    this.endTime = et;
    slconn.setBeginTime(j2kToSeedLinkDateString(st));
    slconn.setEndTime(j2kToSeedLinkDateString(et));
  }

  /**
   * Creates SeedLink connection.
   *
   * @exception SeedLinkException on error.
   * @exception UnknownHostException if no IP address for the local host could be found.
   *
   */
  private void createConnection() {

    slconn = new SeedLinkConnection(new SLLog());
    slconn.setSLAddress(sladdr);

    // Make sure a server was specified
    if (slconn.getSLAddress() == null) {
      String message = "No SeedLink server specified";
      LOGGER.error(message);
      return;
    }

    // If no host is given for the SeedLink server, add 'localhost'
    if (slconn.getSLAddress().startsWith(":")) {
      try {
        slconn.setSLAddress(InetAddress.getLocalHost().toString() + slconn.getSLAddress());
      } catch (UnknownHostException e) {
        LOGGER.error(e.getMessage());
        return;
      }
    }
    slconn.setBeginTime(j2kToSeedLinkDateString(startTime));

    updateMultiSelect();
    if (multiselect != null) {
      try {
        slconn.parseStreamlist(multiselect, null);
      } catch (SeedLinkException e) {
        LOGGER.error("Unable to parse stream list: " + multiselect);
      }
    }
  }

  /**
   * Get the SeedLink information string.
   * 
   * @param info should be ID, STATIONS, STREAMS, GAPS, CONNECTIONS, ALL
   * @return the SeedLink information string or null if error.
   */
  public String getInfoString(String info) {
    try {
      infolevel = info;
      run();
      return slconn.getInfoString();
    } catch (Exception ex) {
      LOGGER.warn("Could not get channels", ex);
    }
    return null;
  }

  protected synchronized void add(String scnl) {
    add(scnl, Double.MAX_VALUE);
  }

  /**
   * Add channel for client to get.
   * @param key gulper listener
   * @param scnl channel string
   * @param t1 start time
   */
  protected synchronized void add(String scnl, double t1) {
    this.startTime = t1;
    boolean reconnect = false;
    if (!scnlMap.keySet().contains(scnl)) {
      reconnect = true;
    }
    scnlMap.put(scnl, J2kSec.now());
    if (reconnect) {
      LOGGER.debug("Added {}", scnl);
      infolevel = null;
      closeConnection();
      createConnection();
    }
  }

  /**
   * Remove station from list of channels to get.
   */
  protected synchronized void remove(String scnl) {
    Double lrt = scnlMap.remove(scnl);
    if (lrt != null && !Double.isNaN(lrt)) {
      closeConnection();
      createConnection();
    }
    LOGGER.debug("Removed {}", scnl);
  }

  /**
   * Update multiselect statement.
   */
  private void updateMultiSelect() {
    if (scnlMap.size() == 0) {
      multiselect = null;
      return;
    }
    String tmpMs = "";
    String prevStation = "";
    for (String scnl : scnlMap.keySet()) {
      ChannelInfo channelInfo = new ChannelInfo(scnl);
      String station = channelInfo.getNetwork() + "_" + channelInfo.getStation();
      String selector = channelInfo.getLocation() + channelInfo.getChannel();
      if (station.equals(prevStation)) {
        tmpMs += " " + selector;
      } else {
        if (!tmpMs.equals("")) {
          tmpMs += ",";
        }
        tmpMs += station + ":" + selector;
      }
      prevStation = station;
    }
    tmpMs += "." + SeedLinkChannelInfo.DATA_TYPE;
    multiselect = tmpMs;
    LOGGER.debug("Multiselect updated: {} {}", sladdr, multiselect);
  }

  /**
   * Cache the wave.
   * 
   * @param scnl the SCNL.
   * @param wave the wave.
   */
  private void cacheWave(String scnl, Wave wave) {
    if (scnl == null || wave == null) {
      return;
    }
    Double lastRequestTime = scnlMap.get(scnl);
    if (scnlMap.keySet().contains(scnl)) {
      if (Double.isNaN(lastRequestTime) || J2kSec.now() - lastRequestTime < 300) {
        CachedDataSource.getInstance().putWave(scnl, wave);
        CachedDataSource.getInstance().cacheWaveAsHelicorder(scnl, wave);
      } else {
        // Don't save if last request time is more than 5 min ago.
        // Remove SCNL from list.
        remove(scnl);
      }
    }
  }

  /*
   * taken from Robert Casey's PDCC seed code.
   */
  private float getSampleRate(double factor, double multiplier) {
    float sampleRate = (float) 10000.0; // default (impossible) value;
    if ((factor * multiplier) != 0.0) { // in the case of log records
      sampleRate = (float) (Math.pow(Math.abs(factor),
          (factor / Math.abs(factor)))
          * Math.pow(Math.abs(multiplier),
              (multiplier / Math.abs(multiplier))));
    }
    return sampleRate;
  }

  private Date btimeToDate(Btime btime) {
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.set(Calendar.YEAR, btime.getYear());
    cal.set(Calendar.DAY_OF_YEAR, btime.getDayOfYear());
    cal.set(Calendar.HOUR_OF_DAY, btime.getHour());
    cal.set(Calendar.MINUTE, btime.getMinute());
    cal.set(Calendar.SECOND, btime.getSecond());
    cal.set(Calendar.MILLISECOND, btime.getTenthMill() / 10);
    return cal.getTime();
  }

  /**
   * Converts a j2ksec to a SeedLink date string
   * ("year,month,day,hour,minute,second").
   * 
   * @param j the j2ksec or NaN if none.
   * @return a SeedLink date string or null if none.
   */
  private static String j2kToSeedLinkDateString(double j) {
    if (Double.isNaN(j)) {
      return null;
    }
    return J2kSec.format("yyyy,MM,dd,HH,mm,ss", j);
  }

  /**
   * Get the Btime value from the specified blockette field.
   * 
   * @param blockette the blockette.
   * @param fieldNum the field number.
   * @return the Btime value.
   * @throws SeedException if error.
   */
  private static Btime getBtime(Blockette blockette, int fieldNum)
      throws SeedException {
    Object obj = blockette.getFieldVal(fieldNum);
    if (obj instanceof Btime) {
      return (Btime) obj;
    }
    return new Btime(obj.toString());
  }

  /**
   * Get the double value from the specified blockette field.
   * 
   * @param blockette the blockette.
   * @param fieldNum the field number.
   * @return the double value.
   * @throws SeedException if error.
   */
  private static double getDouble(Blockette blockette, int fieldNum)
      throws SeedException {
    Object obj = blockette.getFieldVal(fieldNum);
    if (obj instanceof Number) {
      return ((Number) obj).doubleValue();
    }
    return Double.parseDouble(obj.toString());
  }

  /**
   * Method that processes each packet received from the SeedLink server. This
   * is based on code lifted from SeedLinkManager in SeisGram2K with clock
   * logic removed.
   * 
   * @param count the packet to process.
   * @param slpack the packet to process.
   * 
   * @return true if connection to SeedLink server should be closed and
   *         session terminated, false otherwise.
   * 
   * @exception implementation dependent
   * 
   */
  private boolean packetHandler(int count, SLPacket slpack) throws Exception {

    if (count % 10000 == 0) {
      Runtime.getRuntime().gc();
      LOGGER.debug("Packet count reached limit of 10000, garbage collection performed.");
    }

    // may not be on AWT-Event Thread, so do not call any GUI methods

    // check if not a complete packet
    if (slpack == null || slpack == SLPacket.SLNOPACKET
        || slpack == SLPacket.SLERROR) {
      return false; // do not close the connection
    }

    // get basic packet info
    final int type = slpack.getType();

    // process INFO packets here
    // return if unterminated
    if (type == SLPacket.TYPE_SLINF) {
      return false; // do not close the connection
    }
    // process message and return if terminated
    if (type == SLPacket.TYPE_SLINFT) {
      // LOGGER.debug("received INFO packet:\n{}", slconn.getInfoString());
      if (infolevel != null) {
        return true; // close the connection
      } else {
        return false; // do not close the connection
      }
    }

    // if here, must be a blockette
    final Blockette blockette = slpack.getBlockette();
    LOGGER.trace("packet seqnum={}, packet type={}, blockette type={}, blockette={}",
        slpack.getSequenceNumber(), type, blockette.getType(), blockette);

    final Waveform waveform = blockette.getWaveform();
    // if waveform and FSDH
    if (waveform != null && blockette.getType() == 999
        && Swarm.getApplicationFrame() != null) {
      // convert waveform to wave (also done in
      // gov.usgs.swarm.data.FileDataSource)
      try {
        final Btime bTime = getBtime(blockette, 8);
        final double factor = getDouble(blockette, 10);
        final double multiplier = getDouble(blockette, 11);
        final double startTime = J2kSec.fromDate(btimeToDate(bTime));
        final double samplingRate = getSampleRate(factor,
            multiplier);
        final Wave wave = new Wave();
        wave.setSamplingRate(samplingRate);
        wave.setStartTime(startTime);
        wave.buffer = waveform.getDecodedIntegers();
        wave.register();
        String network = (String) blockette.getFieldVal(7);
        String station = (String) blockette.getFieldVal(4);
        String location = (String) blockette.getFieldVal(5);
        String channel = (String) blockette.getFieldVal(6);
        String scnl = station + " " + channel + " " + network + " " + location;
        scnl = scnl.trim().replace(" ", "$");
        cacheWave(scnl, wave);
        if (waveList != null) {
          waveList.add(wave);
        }
      } catch (Exception ex) {
        LOGGER.warn("packetHandler: could create wave", ex);
        return true; // close the connection
      }
    }
    return false; // do not close the connection
  }

  /**
   * Start this SeedLinkClient.
   */
  public void run() {

    try {

      if (infolevel != null) {
        LOGGER.debug("Requesting SeedLink info: " + infolevel);
        slconn.requestInfo(infolevel);
      }

      // Loop with the connection manager
      SLPacket slpack = null;
      int count = 1;
      while ((slpack = slconn.collect()) != null) {

        if (slpack == SLPacket.SLTERMINATE) {
          break;
        }

        slpack.getType(); // ensure the blockette is created if needed
        // reset the volume counter
        BlocketteDecoratorFactory.reset();

        try {
          // do something with packet
          boolean terminate = packetHandler(count, slpack);
          if (terminate) {
            break;
          }

        } catch (SeedLinkException sle) {
          LOGGER.debug("packetHandler error: ", sle);
        }
        // 20081127 AJL - test modification to prevent "Error: out of java heap space" problems
        // identified by pwiejacz@igf.edu.pl
        if (count >= Integer.MAX_VALUE) {
          count = 1;
          LOGGER.debug("Packet count reset to 1");
        } else {
          count++;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    if (multiselect != null) {
      if (endTime > 0) {
        LOGGER.debug("SeedLinkClient ended: {} {} {} {} ", sladdr, multiselect,
            j2kToSeedLinkDateString(startTime), j2kToSeedLinkDateString(endTime));
      }
    }
    // Close the BaseSLConnection
    slconn.close();
    thread = null;
  }

  protected boolean isRunning() {
    if (thread == null) {
      return false;
    }
    return thread.isAlive();
  }

  protected synchronized void start() {
    if (thread == null) {
      thread = new Thread(this);
      thread.start();
    }
  }

  /**
   * Close the SeedLink connection.
   */
  public synchronized void closeConnection() {
    LOGGER.debug("Closing the SeedLinkConnection");
    slconn.terminate();
  }

}
