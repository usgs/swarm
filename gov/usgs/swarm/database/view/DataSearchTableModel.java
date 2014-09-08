package gov.usgs.swarm.database.view;

import gov.usgs.swarm.database.managers.DataSearchManager;
import gov.usgs.swarm.database.model.Attempt;
import gov.usgs.swarm.database.model.Event;
import gov.usgs.swarm.database.model.ResultSetModel;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.table.DefaultTableModel;

/**
 * Model for data search.
 * 
 * @author Chirag Patel
 */
public class DataSearchTableModel extends DefaultTableModel
{
	private ResultSetModel resultSetModel;
	private DataSearchManager dataSearchManager;
	private Map<Integer, Integer[]> preferredRowColByEventIds;

	public DataSearchTableModel(ResultSetModel resultSetModel, DataSearchManager dataSearchManager)
	{
		this.resultSetModel = resultSetModel;
		this.dataSearchManager = dataSearchManager;
		updateModel(this.resultSetModel, false);
	}
	
	
	public DataSearchTableModel(List<Attempt> attempts)
	{
//		this.resultSetModel = resultSetModel;
//		this.dataSearchManager = dataSearchManager;
		this.setRowCount(0);
		this.setColumnCount(6);
		this.setRowCount(attempts.size());
		for(int i = 0; i < attempts.size(); i++ ){
			Event e  = Event.find(attempts.get(i).getEvent());
			this.setValueAt(e.getEventLabel(), i, 0);
			this.setValueAt(attempts.get(i).getId(), i, 1);
			this.setValueAt(attempts.get(i).getPreferred() == null?Boolean.FALSE:attempts.get(i).getPreferred(), i, 2);
			System.out.println(attempts.get(i).getPreferred());
		}
		
//		updateModel(attempts);
	
	}

	public void updateModel(ResultSetModel resultSetModel, boolean fireTableDataChanged)
	{
		this.resultSetModel = resultSetModel;
		Object[][] data = resultSetModel.getData();
		int preferredIndex = getPreferredIndex();
		preferredRowColByEventIds = new HashMap<Integer, Integer[]>();
		for (int i = 0; i < data.length; i++) {
			if ((Boolean) data[i][preferredIndex]) {
				Integer eventId = resultSetModel.getEventIdByRow(i);
				preferredRowColByEventIds.put(eventId, new Integer[] { i, preferredIndex });
			}
		}
		if (fireTableDataChanged) {
			super.fireTableDataChanged();
		}
	}
	
	
	public void updateModel(List<Attempt> attempts)
	{
		this.resultSetModel = resultSetModel;
		Object[][] data = resultSetModel.getData();
		int preferredIndex = getPreferredIndex();
		preferredRowColByEventIds = new HashMap<Integer, Integer[]>();
		for (int i = 0; i < data.length; i++) {
			if ((Boolean) data[i][preferredIndex]) {
				Integer eventId = resultSetModel.getEventIdByRow(i);
				preferredRowColByEventIds.put(eventId, new Integer[] { i, preferredIndex });
			}
		}
			super.fireTableDataChanged();
		
	}

//	@Override
//	public int getRowCount()
//	{
////		return resultSetModel.getRowCount();
//		return this.getRowCount();
//	}

//	@Override
//	public int getColumnCount()
//	{
//		return resultSetModel.getColumnCount();
//	}

//	@Override
//	public String getColumnName(int columnIndex)
//	{
//		return resultSetModel.getColumnNames()[columnIndex];
//	}
//
//	@Override
//	public Class<?> getColumnClass(int columnIndex)
//	{
//		return resultSetModel.getColumnClasses()[columnIndex];
//	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex)
	{
		if (isPreferred(columnIndex)) {
			return true;
		}
		return false;
	}

	
	 /*@Override
    public Class getColumnClass(int column) {
    return getValueAt(0, column).getClass();
    }*/
    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case 0:
                return String.class;
            case 1:
                return String.class;
            case 2:
                return Integer.class;
            case 3:
                return Double.class;
            default:
                return Boolean.class;
        }
    }
	
	public Attempt getAttemptByRow(int rowIndex)
	{
		Integer attemptId = (Integer) getValueAt(rowIndex, 1);
		Boolean preferred = (Boolean) getValueAt(rowIndex, 2);
		Long duration = (Long) getValueAt(rowIndex, 3);
		Double longitude = (Double) getValueAt(rowIndex, 4);
		Double latitude = (Double) getValueAt(rowIndex, 5);

		return new Attempt(attemptId, preferred, longitude, latitude, duration);
	}

	public int getPreferredIndex()
	{
		return 2;
	}

	public boolean isPreferred(int col)
	{
		return col == getPreferredIndex();
	}

	public int getAttemptIdColIndex()
	{
		return 1;
	}
	
	
	
}