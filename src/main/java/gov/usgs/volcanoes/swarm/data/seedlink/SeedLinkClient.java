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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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

  /** The SCNL or null if none. */
  private String scnl;

  /** The start and end time. */
  private final StartEndTime startEndTime;

  /** The thread or null if none. */
  private Thread thread;

  /** The wave list or null if none. */
  private List<Wave> waveList;

  // parameters
  /** BaseSLConnection object for communicating with the BaseSLConnection over a socket. */
  private final SeedLinkConnection slconn;


  /** Selectors for uni-station or default selectors for multi-station. */
  private String selectors = null;

  /** Selectors for multi-station. */
  private String multiselect = null;

  /** INFO LEVEL for info request only. */
  private String infolevel = null;


  /**
   * Create the SeedLink client.
   * 
   * @param host the server host.
   * @param port the server port.
   */
  public SeedLinkClient(String host, int port) {
    super();
    slconn = new SeedLinkConnection(new SLLog());

    startEndTime = new StartEndTime();
    // create thread so that it is not killed by default
    thread = new Thread(this);
    final String sladdr = host + ":" + port;
    slconn.setSLAddress(sladdr);
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
    LOGGER.debug("putting wave in cache ({}):", scnl, wave);
    CachedDataSource.getInstance().putWave(scnl, wave);
    CachedDataSource.getInstance().cacheWaveAsHelicorder(scnl, wave);
  }

  /**
   * Close the SeedLink connection.
   */
  public void close() {
    LOGGER.debug("close the SeedLinkConnection");
    slconn.terminate();
    kill();
  }

  /**
   * Get the SeedLink information string.
   * 
   * @return the SeedLink information string or null if error.
   */
  public String getInfoString() {
    try {
      infolevel = "STREAMS";
      init(null, null);
      run();
      // I don't know why this is clearing the info string. Perhaps simply to reclaim memory? I'm
      // getting rid of it and just leaving the side effect in place. That seems to be the real
      // goal. --TJP
      // return slconn.clearInfoString();
      return slconn.getInfoString();
    } catch (Exception ex) {
      LOGGER.warn("could not get channels", ex);
    }
    return null;
  }

  /**
   * Get the multiple select text.
   * 
   * @param channelInfo the channel information.
   * 
   * @return the multiple select text.
   */
  private String getMultiSelect(ChannelInfo channelInfo) {
    return channelInfo.getNetwork() + "_" + channelInfo.getStation() + ":"
        + channelInfo.getLocation() + channelInfo.getChannel() + "."
        + SeedLinkChannelInfo.DATA_TYPE;
  }

  /**
   * Get the start and end time and clears the value for the next call.
   * 
   * @param o the start end time to set or null to return a new copy.
   * 
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
   * Get the wave, waiting until all data is available.
   * 
   * @param scnl the scnl.
   * @param t1 the start time.
   * @param t2 the end time.
   * @return the wave.
   */
  public Wave getWave(String scnl, double t1, double t2) {
    waveList = new ArrayList<Wave>();
    init(scnl, t1, t2);
    run();
    final Wave wave = Wave.join(waveList);
    waveList = null;
    return wave;
  }

  /**
   * Initialize the client.
   * 
   * @param scnl the scnl.
   * @param t1 the start time or NaN if none.
   * @param t2 the end time or NaN if none.
   */
  public void init(String scnl, double t1, double t2) {
    this.scnl = scnl;
    final ChannelInfo channelInfo = new ChannelInfo(scnl);
    infolevel = null;
    // selectors = channelInfo.getFormattedSCNL();
    multiselect = getMultiSelect(channelInfo);
    String beginTime = j2kToSeedLinkDateString(t1);
    String endTime = j2kToSeedLinkDateString(t2);
    try {
      init(beginTime, endTime);
    } catch (Exception ex) {
      LOGGER.warn("could start SeedLink client", ex);
    }
  }


  /**
   * Initializes this SLCient.
   *
   * @exception SeedLinkException on error.
   * @exception UnknownHostException if no IP address for the local host could be found.
   *
   */
  private void init(String beginTime, String endTime)
      throws UnknownHostException, SeedLinkException {

    // Make sure a server was specified
    if (slconn.getSLAddress() == null) {
      String message = "no SeedLink server specified";
      throw (new SeedLinkException(message));
    }


    // If no host is given for the SeedLink server, add 'localhost'
    if (slconn.getSLAddress().startsWith(":")) {
      slconn.setSLAddress(InetAddress.getLocalHost().toString() + slconn.getSLAddress());
    }

    // Parse the 'multiselect' string following '-S'
    if (multiselect != null) {
      slconn.parseStreamlist(multiselect, selectors);
    } else {
      slconn.setUniParams(selectors, -1, null);
    }

    // Set begin time for read start in past
    // 20050415 AJL added to support continuous data transfer from a time in the past
    if (beginTime != null) {
      slconn.setBeginTime(beginTime);
    }
    // Set end time for for reading windowed data
    // 20071204 AJL added
    if (endTime != null) {
      slconn.setEndTime(endTime);
    }
  }


  /**
   * Determine if the client has been killed.
   * 
   * @return true if the client has been killed.
   */
  protected boolean isKilled() {
    return thread == null;
  }

  /**
   * Kill the client.
   */
  protected void kill() {
    final Thread t = thread;
    if (t != null) {
      thread = null;
      t.interrupt();
    }
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
    if (isKilled()) {
      return true;
    }

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

    // station list should refresh on demand, not on a schedule.
    // // send an in-line INFO request here
    // long currTime = System.currentTimeMillis();
    // if (currTime - lastInfoRequestTime > INFO_REQUEST_INTERVAL
    // && !slconn.getState().expect_info) {
    // LOGGER.debug("requesting INFO level ID");
    // slconn.requestInfo("ID");
    // lastInfoRequestTime = currTime;
    // }

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

  /*
   * taken from Robert Casey's PDCC seed code.
   */
  private float getSampleRate(double factor, double multiplier) {
    float sampleRate = (float) 10000.0; // default (impossible) value;
    if ((factor * multiplier) != 0.0) { // in the case of log records
      sampleRate = (float) (java.lang.Math.pow(java.lang.Math.abs(factor),
          (factor / java.lang.Math.abs(factor)))
          * java.lang.Math.pow(java.lang.Math.abs(multiplier),
              (multiplier / java.lang.Math.abs(multiplier))));
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
   * Start this SeedLinkClient.
   */
  public void run() {
    try {
      if (infolevel != null) {
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

  }

  /**
   * Start the client.
   */
  public void start() {
    final Thread t = thread;
    if (t != null) {
      t.start();
    }
  }
}
