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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import nl.knmi.orfeus.seedlink.SLLog;
import nl.knmi.orfeus.seedlink.SLPacket;
import nl.knmi.orfeus.seedlink.SeedLinkException;
import nl.knmi.orfeus.seedlink.client.SeedLinkConnection;

import org.apache.log4j.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SeedLink client.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkClient implements Runnable {

  private static final Logger LOGGER = LoggerFactory.getLogger(SeedLinkClient.class);

  private String sladdr;
  
  /** The start and end time. */
  private StartEndTime startEndTime;

  /** The wave list or null if none. */
  private List<Wave> waveList;

  /** BaseSLConnection object for communicating with the BaseSLConnection over a socket. */
  private SeedLinkConnection slconn;

  /** INFO LEVEL for info request only. */
  private String infolevel = null;
  
  /** List of SCNLs. */
  private HashMap<String, Double> scnlMap; // SCNL & last request time
  
  /** Client thread. */
  private Thread thread;
  
  private double startTime = Double.MAX_VALUE; // J2K start time
  private double endTime = Double.MIN_VALUE; // J2K start time
 

  /**
   * Create the SeedLink client.
   * 
   * @param host the server host.
   * @param port the server port.
   */
  public SeedLinkClient(String host, int port) {
    super();   
    sladdr = host + ":" + port;
    scnlMap = new HashMap<String, Double>();
    try {
      createConnection();
    } catch (UnknownHostException e) {
      LOGGER.error(e.getMessage());
    } catch (SeedLinkException e) {
      LOGGER.error(e.getMessage());
    }
  }
  
  /**
   * Creates SeedLink connection.
   *
   * @exception SeedLinkException on error.
   * @exception UnknownHostException if no IP address for the local host could be found.
   *
   */
  private void createConnection()
      throws UnknownHostException, SeedLinkException {

    slconn = new SeedLinkConnection(new SLLog());
    startEndTime = new StartEndTime();
    slconn.setSLAddress(sladdr);
    
    // Make sure a server was specified
    if (slconn.getSLAddress() == null) {
      String message = "no SeedLink server specified";
      throw (new SeedLinkException(message));
    }

    // If no host is given for the SeedLink server, add 'localhost'
    if (slconn.getSLAddress().startsWith(":")) {
      slconn.setSLAddress(InetAddress.getLocalHost().toString() + slconn.getSLAddress());
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
      LOGGER.warn("could not get channels", ex);
    }
    return null;
  }

  /**
   * Get the start and end time and clears the value for the next call.
   * 
   * @param o the start end time to set or null to return a new copy.   * 
   * @return the start and end time.
   */
  public StartEndTime getStartEndTime(StartEndTime o) {
    synchronized (startEndTime) {
      if (o == null) {
        o = new StartEndTime(startEndTime.getStartTime(), startEndTime.getEndTime());
      } else {
        o.set(startEndTime);
      }
      startEndTime.clear();
    }
    return o;
  }
  
  /**
   * Add channel for client to get.
   * @param key gulper listener
   * @param scnl channel string
   * @param t1 start time
   * @param t2 end time
   */
  protected synchronized void add(String scnl, double t1, double t2) {
    if (!scnlMap.keySet().contains(scnl)) {
      infolevel = null;
      ChannelInfo channelInfo = new ChannelInfo(scnl);
      String multiselect = getMultiSelect(channelInfo);
      try {
        slconn.parseStreamlist(multiselect, null);
        LOGGER
            .info("Added seedlink stream " + multiselect + ". Terminating connection to reconnect.");
        slconn.terminate();
      } catch (SeedLinkException e) {
        LOGGER.warn("Could not add SCNL", e);
      }
    }
    scnlMap.put(scnl, J2kSec.now());
    startTime = Math.min(t1, startTime);
    slconn.setBeginTime(j2kToSeedLinkDateString(startTime));    
  }
    
  /**
   * Get the multiple select text.
   * 
   * @param channelInfo the channel information.   
   * @return the multiple select text.
   */
  private String getMultiSelect(ChannelInfo channelInfo) {
    return channelInfo.getNetwork() + "_" + channelInfo.getStation() + ":"
        + channelInfo.getLocation() + channelInfo.getChannel() + "."
        + SeedLinkChannelInfo.DATA_TYPE;
  }

  /**
   * Remove station from list of channels to get.
   * @param key gulper listener
   */
  protected void remove(String scnl) {
    scnlMap.remove(scnl);
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
        LOGGER.debug("putting wave in cache ({}):", scnl, wave);
        CachedDataSource.getInstance().putWave(scnl, wave);
        CachedDataSource.getInstance().cacheWaveAsHelicorder(scnl, wave);
      } else {
        // Don't save if last request time is more than 5 min ago.
        // Remove SCNL from list.
        scnlMap.remove(scnl);
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
      LOGGER.debug("infolevel: {}", infolevel);
      if (infolevel != null) {
        return true; // close the connection
      } else {
        return false; // do not close the connection
      }
    }

    // if here, must be a blockette
    final Blockette blockette = slpack.getBlockette();
    LOGGER.debug("packet seqnum={}, packet type={}, blockette type={}, blockette={}",
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
        } else {
          final double endTime = wave.getEndTime();
          synchronized (startEndTime) {
            startEndTime.update(startTime, endTime);
          }
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
          LOGGER.debug("error: ", sle);
        }
        // 20081127 AJL - test modification to prevent "Error: out of java heap space" problem
        // identified by pwiejacz@igf.edu.pl
        if (count >= Integer.MAX_VALUE) {
          count = 1;
          LOGGER.debug("Packet count reset to 1");
        } else {
          count++;
        }
      }
    } catch (Exception ex) {
      LOGGER.debug("error in run", ex);
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
      try {
        Thread.sleep(2000);     // Give some time for the seedlink connection to get some data.
      } catch (InterruptedException e) {
        //
      }
    }
  }

  /**
   * Close the SeedLink connection.
   */
  public synchronized void close() {
    LOGGER.debug("close the SeedLinkConnection");
    slconn.terminate();
  }

}
