package gov.usgs.swarm.data.seedLink;

import edu.iris.Fissures.seed.container.Blockette;
import edu.iris.Fissures.seed.container.Btime;
import edu.iris.Fissures.seed.container.Waveform;
import edu.iris.Fissures.seed.exception.SeedException;
import gov.usgs.plot.data.Wave;
import gov.usgs.swarm.ChannelInfo;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.seedLink.orfeus.BaseSLClient;
import gov.usgs.util.Util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import nl.knmi.orfeus.seedlink.SLPacket;
import nl.knmi.orfeus.seedlink.SeedLinkException;

/**
 * SeedLink client.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkClient extends BaseSLClient
{
	/** SeedLink client counter */
	private static int _seedLinkClientCounter;

	/** SeedLinkClient class name */
	private static final String CLASS_NAME = "gov.usgs.swarm.data.SeedLinkClient";

	/** Data type. */
	private static final String DATA_TYPE = SeedLinkChannelInfo.DATA_TYPE;

	/** The interval for sending info requests in milliseconds. */
	// each hour
	private static final long INFO_REQUEST_INTERVAL = 1000 * 60 * 60;

	/** The SeedLink log. */
	private static final SeedLinkLog sllog = new SeedLinkLog();

	/**
	 * Get the Btime value from the specified blockette field.
	 * 
	 * @param blockette the blockette.
	 * @param fieldNum the field number.
	 * @return the Btime value.
	 * @throws SeedException if error.
	 */
	private static Btime getBtime(Blockette blockette, int fieldNum)
			throws SeedException
	{
		Object obj = blockette.getFieldVal(fieldNum);
		if (obj instanceof Btime)
			return (Btime) obj;
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
			throws SeedException
	{
		Object obj = blockette.getFieldVal(fieldNum);
		if (obj instanceof Number)
			return ((Number) obj).doubleValue();
		return Double.parseDouble(obj.toString());
	}

	/**
	 * Get the next SeedLink client counter value.
	 * 
	 * @return the value.
	 */
	private static int getNextSeedLinkClientCounter()
	{
		final int counter = ++_seedLinkClientCounter;
		if (counter == 1)
		{
			sllog.setLevel(Level.INFO);
		}
		return counter;
	}

	/**
	 * Converts a j2ksec to a SeedLink date string
	 * ("year,month,day,hour,minute,second").
	 * 
	 * @param j the j2ksec or NaN if none.
	 * @return a SeedLink date string or null if none.
	 */
	private static String j2kToSeedLinkDateString(double j)
	{
		if (Double.isNaN(j))
		{
			return null;
		}
		return Util.j2KToDateString(j, "yyyy,MM,dd,HH,mm,ss");
	}

	/**
	 * 
	 * main method
	 * 
	 */
	public static void main(String[] args)
	{
		SeedLinkClient slClient = null;

		try
		{
			slClient = new SeedLinkClient();
			int rval = slClient.parseCmdLineArgs(args);
			if (rval != 0)
			{
				System.exit(rval);
			}
			slClient.init();
			slClient.run();
			SeedLinkChannelInfo.writeString(new java.io.File("streams.xml"),
					slClient.slconn.getInfoString());
		}
		catch (SeedLinkException sle)
		{
			sllog.log(true, 0, sle.getMessage());
		}
		catch (Exception e)
		{
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Gets a <code>String</code> representation of the object.
	 * 
	 * @return the <code>String</code> representation of the object
	 */
	public static String toString(Object obj)
	{
		return SeedLinkSource.toString(obj);
	}

	/** The last info request time. */
	private long lastInfoRequestTime;

	/** The logger. */
	private final Logger logger;

	/** Log prefix text. */
	private final String logPrefixText;

	/** The SCNL or null if none. */
	private String scnl;

	/** The start and end time. */
	private final StartEndTime startEndTime;

	/** The thread or null if none. */
	private Thread thread;

	/** Thread synchronization object or null if none. */
	private final Object threadSyncObj;

	/** The wave list or null if none. */
	private List<Wave> waveList;

	private SeedLinkClient()
	{
		super(sllog);
		logPrefixText = CLASS_NAME + "(" + getNextSeedLinkClientCounter() + ","
				+ Thread.currentThread().getName() + "): ";
		logger = SeedLinkLog.logger;
		startEndTime = new StartEndTime();
		threadSyncObj = null; // logPrefixText;
		// create thread so that it is not killed by default
		thread = new Thread(this);
	}

	/**
	 * Create the SeedLink client.
	 * 
	 * @param server the server host.
	 * @param port the server port.
	 */
	public SeedLinkClient(String host, int port)
	{
		this();
		final String sladdr = host + ":" + port;
		slconn.setSLAddress(sladdr);
	}

	/**
	 * Cache the wave.
	 * 
	 * @param scnl the SCNL.
	 * @param wave the wave.
	 */
	protected void cacheWave(String scnl, Wave wave)
	{
		if (scnl == null || wave == null)
		{
			return;
		}
		if (logger.isLoggable(Level.FINEST))
		{
			logger.finest(logPrefixText + "putting wave in cache (" + scnl
					+ "): " + toString(wave));
		}
		CachedDataSource.getInstance().putWave(scnl, wave);
		CachedDataSource.getInstance().cacheWaveAsHelicorder(scnl, wave);
	}

	/**
	 * Close the SeedLink connection.
	 */
	public void close()
	{
		logger.fine(logPrefixText + "close the SeedLinkConnection");
		slconn.terminate();
		kill();
	}

	/**
	 * Get the SeedLink information string.
	 * 
	 * @return the SeedLink information string or null if error.
	 */
	public String getInfoString()
	{
		try
		{
			infolevel = "STREAMS";
			init();
			runAndWait();
			return slconn.clearInfoString();
		}
		catch (Exception ex)
		{
			logger.log(Level.WARNING, logPrefixText + "could not get channels",
					ex);
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
	private String getMultiSelect(ChannelInfo channelInfo)
	{
		return channelInfo.getNetwork() + "_" + channelInfo.getStation() + ":"
				+ channelInfo.getLocation() + channelInfo.getChannel() + "."
				+ DATA_TYPE;
	}

	/**
	 * Get the start and end time and clears the value for the next call.
	 * 
	 * @param o the start end time to set or null to return a new copy.
	 * 
	 * @return the start and end time.
	 */
	public StartEndTime getStartEndTime(StartEndTime o)
	{
		synchronized (startEndTime)
		{
			if (o == null)
			{
				o = new StartEndTime(startEndTime);
			}
			else
			{
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
	public Wave getWave(String scnl, double t1, double t2)
	{
		waveList = new ArrayList<Wave>();
		init(scnl, t1, t2);
		runAndWait();
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
	public void init(String scnl, double t1, double t2)
	{
		this.scnl = scnl;
		final ChannelInfo channelInfo = new ChannelInfo(scnl);
		infolevel = null;
		// selectors = channelInfo.getFormattedSCNL();
		multiselect = getMultiSelect(channelInfo);
		begin_time = j2kToSeedLinkDateString(t1);
		end_time = j2kToSeedLinkDateString(t2);
		StringBuilder sb = new StringBuilder(slconn.getSLAddress());
		if (selectors != null)
		{
			sb.append(" -s " + selectors);
		}
		if (multiselect != null)
		{
			sb.append(" -S " + multiselect);
		}
		if (begin_time != null)
		{
			sb.append(" -t " + begin_time);
		}
		if (end_time != null)
		{
			sb.append(" -e " + end_time);
		}
		logger.info(logPrefixText + sb.toString());
		try
		{
			init();
		}
		catch (Exception ex)
		{
			logger.log(Level.WARNING, logPrefixText
					+ "could start SeedLink client", ex);
		}
	}

	/**
	 * Determine if the client has been killed.
	 * 
	 * @return true if the client has been killed.
	 */
	protected boolean isKilled()
	{
		return thread == null;
	}

	/**
	 * Kill the client.
	 */
	protected void kill()
	{
		final Thread t = thread;
		if (t != null)
		{
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
	public boolean packetHandler(int count, SLPacket slpack) throws Exception
	{
		if (isKilled()) // if killed
		{
			return true; // close the connection
		}

		if (count % 10000 == 0)
		{
			Runtime.getRuntime().gc();
			logger.finer(logPrefixText + "Packet count reached limit of "
					+ 10000 + ", garbage collection performed.");
		}

		// may not be on AWT-Event Thread, so do not call any GUI methods

		// check if not a complete packet
		if (slpack == null || slpack == SLPacket.SLNOPACKET
				|| slpack == SLPacket.SLERROR)
		{
			return false; // do not close the connection
		}

		// get basic packet info
		final int type = slpack.getType();

		// process INFO packets here
		// return if unterminated
		if (type == SLPacket.TYPE_SLINF)
		{
			return false; // do not close the connection
		}
		// process message and return if terminated
		if (type == SLPacket.TYPE_SLINFT)
		{
			logger.finer(logPrefixText + "received INFO packet:\n"
					+ slconn.getInfoString());
			if (infolevel != null)
			{
				return true; // close the connection
			}
			else
			{
				return false; // do not close the connection
			}
		}

		// send an in-line INFO request here
		long currTime = System.currentTimeMillis();
		if (currTime - lastInfoRequestTime > INFO_REQUEST_INTERVAL
				&& !slconn.getState().expect_info)
		{
			logger.finer(logPrefixText + "requesting INFO level ID");
			String infostr = "ID";
			slconn.requestInfo(infostr);
			lastInfoRequestTime = currTime;
		}

		// if here, must be a blockette
		final Blockette blockette = slpack.getBlockette();
		logger.finest(logPrefixText + "packet seqnum="
				+ slpack.getSequenceNumber() + ", packet type=" + type
				+ ", blockette type=" + blockette.getType() + ", blockette="
				+ blockette);

		final Waveform waveform = blockette.getWaveform();
		// if waveform and FSDH
		if (waveform != null && blockette.getType() == 999
				&& Swarm.getApplication() != null)
		{
			// convert waveform to wave (also done in
			// gov.usgs.swarm.data.FileDataSource)
			try
			{
				final Btime bTime = getBtime(blockette, 8);
				final double factor = getDouble(blockette, 10);
				final double multiplier = getDouble(blockette, 11);
				final double startTime = Util
						.dateToJ2K(btimeToDate(bTime));
				final double samplingRate = getSampleRate(factor,
						multiplier);
				final Wave wave = new Wave();
				wave.setSamplingRate(samplingRate);
				wave.setStartTime(startTime);
				wave.buffer = waveform.getDecodedIntegers();
				wave.register();
				cacheWave(scnl, wave);
				if (waveList != null)
				{
					waveList.add(wave);
				}
				else
				{
					final double endTime = wave.getEndTime();
					synchronized (startEndTime)
					{
						startEndTime.update(startTime, endTime);
					}
				}
			}
			catch (Exception ex)
			{
				logger.log(Level.WARNING, logPrefixText
						+ "packetHandler: could create wave", ex);
				return true; // close the connection
			}
		}
		return false; // do not close the connection
	}

	/*
	 * taken from Robert Casey's PDCC seed code.
	 */
	private float getSampleRate(double factor, double multiplier) {
        float sampleRate = (float) 10000.0;  // default (impossible) value;
        if ((factor * multiplier) != 0.0) {  // in the case of log records
            sampleRate = (float) (java.lang.Math.pow
                                      (java.lang.Math.abs(factor),
                                           (factor/java.lang.Math.abs(factor)))
                                      * java.lang.Math.pow
                                      (java.lang.Math.abs(multiplier),
                                           (multiplier/java.lang.Math.abs(multiplier))));
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
	public void run()
	{
		super.run();
		if (threadSyncObj != null)
		{
			synchronized (threadSyncObj)
			{
				threadSyncObj.notifyAll();
			}
		}
	}

	/**
	 * Run and wait for it to complete.
	 */
	protected void runAndWait()
	{
		if (threadSyncObj != null)
		{
			start();
			synchronized (threadSyncObj)
			{
				try
				{
					threadSyncObj.wait();
				}
				catch (InterruptedException ex)
				{
					kill();
				}
			}
		}
		else
		{
			run();
		}
	}

	/**
	 * Start the client.
	 */
	public void start()
	{
		final Thread t = thread;
		if (t != null)
		{
			t.start();
		}
	}
}
