package gov.usgs.swarm.data;

/**
 * Stores the start and end time.
 * 
 * @author Kevin Frechette (ISTI)
 */
public class StartEndTime
{
	/** The end time. */
	private double endTime;

	/** The start time. */
	private double startTime;

	/**
	 * Create an empty start end time.
	 */
	public StartEndTime()
	{
		this(Double.NaN, Double.NaN);
	}

	/**
	 * Create a start and end time.
	 * 
	 * @param startTime the start time.
	 * @param endTime the end time.
	 */
	public StartEndTime(double startTime, double endTime)
	{
		this.startTime = startTime;
		this.endTime = endTime;
	}

	/**
	 * Create a copy of a start and end time.
	 * 
	 * @param o the start end time.
	 */
	public StartEndTime(StartEndTime o)
	{
		this(o.getStartTime(), o.getEndTime());
	}

	/**
	 * Clear the start and end time.
	 */
	public void clear()
	{
		startTime = Double.NaN;
		endTime = Double.NaN;
	}

	/**
	 * Gets the end time.
	 * 
	 * @return the end time
	 */
	public double getEndTime()
	{
		return endTime;
	}

	/**
	 * Gets the start time.
	 * 
	 * @return the start time
	 */
	public double getStartTime()
	{
		return startTime;
	}

	/**
	 * Determines if the start and end time is empty.
	 * 
	 * @return true if the start and end time is empty, false otherwise.
	 */
	public boolean isEmpty()
	{
		return Double.isNaN(startTime);
	}

	/**
	 * Set this start end time to the specified start end time.
	 * 
	 * @param o the start end time to set this one.
	 */
	public void set(StartEndTime o)
	{
		this.startTime = o.startTime;
		this.endTime = o.endTime;
	}

	/**
	 * Set the end time.
	 * 
	 * @param d the end time
	 */
	public void setEndTime(double d)
	{
		endTime = d;
	}

	/**
	 * Set the start time.
	 * 
	 * @param d the start time
	 */
	public void setStartTime(double d)
	{
		startTime = d;
	}

	/**
	 * Update the start and end time.
	 * 
	 * @param startTime the start time.
	 * @param endTime the end time.
	 */
	public void update(double startTime, double endTime)
	{
		if (isEmpty())
		{
			this.startTime = startTime;
			this.endTime = endTime;
		}
		else
		{
			if (startTime < this.startTime)
			{
				this.startTime = startTime;
			}
			if (endTime > this.endTime)
			{
				this.endTime = endTime;
			}
		}
	}
}
