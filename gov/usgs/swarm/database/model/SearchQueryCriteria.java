package gov.usgs.swarm.database.model;

import java.sql.Timestamp;

/**
 * Represents a query for events/attempts.
 * 
 * @author Chirag Patel
 */
public class SearchQueryCriteria
{
	private String userLabel = null;
	private Timestamp startTime = null;
	private Timestamp endTime = null;
	private Long durationMin = null;
	private Long durationMax = null;

	public SearchQueryCriteria(String userLabel, Timestamp startTime, Timestamp endTime, Long durationMin,
			Long durationMax)
	{
		this.userLabel = userLabel;
		this.startTime = startTime;
		this.endTime = endTime;
		this.durationMin = durationMin;
		this.durationMax = durationMax;
	}

	public String getUserLabel()
	{
		return userLabel;
	}

	public void setUserLabel(String userLabel)
	{
		this.userLabel = userLabel;
	}

	public Timestamp getStartTime()
	{
		return startTime;
	}

	public void setStartTime(Timestamp startTime)
	{
		this.startTime = startTime;
	}

	public Timestamp getEndTime()
	{
		return endTime;
	}

	public void setEndTime(Timestamp endTime)
	{
		this.endTime = endTime;
	}

	public Long getDurationMin()
	{
		return durationMin;
	}

	public void setDurationMin(Long durationMin)
	{
		this.durationMin = durationMin;
	}

	public Long getDurationMax()
	{
		return durationMax;
	}

	public void setDurationMax(Long durationMax)
	{
		this.durationMax = durationMax;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((durationMax == null) ? 0 : durationMax.hashCode());
		result = prime * result + ((durationMin == null) ? 0 : durationMin.hashCode());
		result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
		result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
		result = prime * result + ((userLabel == null) ? 0 : userLabel.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchQueryCriteria other = (SearchQueryCriteria) obj;
		if (durationMax == null) {
			if (other.durationMax != null)
				return false;
		} else if (!durationMax.equals(other.durationMax))
			return false;
		if (durationMin == null) {
			if (other.durationMin != null)
				return false;
		} else if (!durationMin.equals(other.durationMin))
			return false;
		if (endTime == null) {
			if (other.endTime != null)
				return false;
		} else if (!endTime.equals(other.endTime))
			return false;
		if (startTime == null) {
			if (other.startTime != null)
				return false;
		} else if (!startTime.equals(other.startTime))
			return false;
		if (userLabel == null) {
			if (other.userLabel != null)
				return false;
		} else if (!userLabel.equals(other.userLabel))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "SearchQueryCriteria [userLabel=" + userLabel + ", startTime=" + startTime + ", endTime=" + endTime
				+ ", durationMin=" + durationMin + ", durationMax=" + durationMax + "]";
	}
}