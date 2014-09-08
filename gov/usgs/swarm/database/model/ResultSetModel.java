package gov.usgs.swarm.database.model;

/**
 * Handles results of searches for events.
 * 
 * @author Chirag Patel
 */
public final class ResultSetModel
{
	private String[] columnNames;
	private Class<?>[] columnClasses;
	private Object[][] data;
	private int rowCount = 0;
	private int columnCount = 0;

	public ResultSetModel(String[] columnNames, Class<?>[] columnClasses, Object[][] data, int columnCount)
	{
		super();
		this.columnNames = columnNames;
		this.columnClasses = columnClasses;
		this.data = data;
		this.rowCount = data.length;
		this.columnCount = columnCount;
	}

	public String[] getColumnNames()
	{
		return columnNames;
	}

	public Class<?>[] getColumnClasses()
	{
		return columnClasses;
	}

	public Object[][] getData()
	{
		return data;
	}

	public int getRowCount()
	{
		return rowCount;
	}

	public int getColumnCount()
	{
		return columnCount;
	}

	public Integer getEventIdByRow(int row)
	{
		String event = (String) data[row][0];
		int openingBracket = event.lastIndexOf('(');
		String number = event.substring(openingBracket + 1, event.length() - 1);
		return Integer.valueOf(number);
	}
}