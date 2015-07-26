package gov.usgs.swarm.data.seedLink;

import gov.usgs.swarm.data.Gulper;
import gov.usgs.swarm.data.GulperList;

/**
 * SeedLink Gulper.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class SeedLinkGulper extends Gulper
{
	/** The SeedLink client. */
	private final SeedLinkClient client;

	/** The start end time. */
	private final StartEndTime startEndTime;

	public SeedLinkGulper(GulperList gl, String k, SeedLinkSource source,
			String ch, double t1, double t2, int size, int delay)
	{
		super(gl, k, source, ch, t1, t2, size, delay);
		startEndTime = new StartEndTime();
		// start client with no end time to keep getting updates
		client = source.createClient();
		client.init(ch, t1, Double.NaN);
		client.start();
	}

	/**
	 * Determine if this gulper has been killed.
	 * 
	 * @return true if the gulper has been killed or was never started.
	 */
	public boolean isKilled()
	{
		return super.isKilled() || client.isKilled();
	}

	/**
	 * Kill this gulper.
	 */
	protected void kill()
	{
		client.kill();
		super.kill();
	}

	/**
	 * This is the run loop.
	 */
	protected void runLoop()
	{
		while (!isKilled())
		{
			client.getStartEndTime(startEndTime);
			if (!startEndTime.isEmpty())
			{
				fireGulped(startEndTime.getStartTime(),
						startEndTime.getEndTime(), true);
				startEndTime.clear();
			}
			delay();
		}
	}

	/**
	 * Start this gulper.
	 */
	public void start()
	{
		super.start();
	}

	public void update(double t1, double t2)
	{
		// we do not need to do Gulper update since the SeedLink client will
		// keep getting updates and fire them to the listeners
	}
}
