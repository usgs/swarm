package gov.usgs.swarm.database.model;

import gov.usgs.vdx.calc.data.AdjustmentIteration;
import gov.usgs.vdx.calc.data.Hypocenter;
import gov.usgs.vdx.calc.data.Station;
import gov.usgs.vdx.calc.data.Stats;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents the results of an hypo calculation
 *  
 * @author Chirag Patel
 */
@SuppressWarnings("serial")
public class HypoResults implements Serializable{
	private List<AdjustmentIteration> adjustmentsOutput = new LinkedList<AdjustmentIteration>();
	private List<Hypocenter> hypocenterOuput = new LinkedList<Hypocenter>();
	private List<Station> missingStationsList = new LinkedList<Station>();
	private List<Station> stationsResultList = new LinkedList<Station>();
	private List<Station> summaryList = new LinkedList<Station>();
	private List<String> deletedStationsList = new LinkedList<String>();
	private Stats stats;
	private String printOutput = new String();
	private String punchOutput = new String();
	public List<AdjustmentIteration> getAdjustmentsOutput() {
		return adjustmentsOutput;
	}
	public void setAdjustmentsOutput(List<AdjustmentIteration> adjustmentsOutput) {
		this.adjustmentsOutput = adjustmentsOutput;
	}
	public List<Hypocenter> getHypocenterOuput() {
		return hypocenterOuput;
	}
	public void setHypocenterOuput(List<Hypocenter> hypocenterOuput) {
		this.hypocenterOuput = hypocenterOuput;
	}
	public List<Station> getMissingStationsList() {
		return missingStationsList;
	}
	public void setMissingStationsList(List<Station> missingStationsList) {
		this.missingStationsList = missingStationsList;
	}
	public List<Station> getStationsResultList() {
		return stationsResultList;
	}
	public void setStationsResultList(List<Station> stationsResultList) {
		this.stationsResultList = stationsResultList;
	}
	public List<Station> getSummaryList() {
		return summaryList;
	}
	public void setSummaryList(List<Station> summaryList) {
		this.summaryList = summaryList;
	}
	public List<String> getDeletedStationsList() {
		return deletedStationsList;
	}
	public void setDeletedStationsList(List<String> deletedStationsList) {
		this.deletedStationsList = deletedStationsList;
	}
	public Stats getStats() {
		return stats;
	}
	public void setStats(Stats stats) {
		this.stats = stats;
	}
	public String getPrintOutput() {
		return printOutput;
	}
	public void setPrintOutput(String printOutput) {
		this.printOutput = printOutput;
	}
	public String getPunchOutput() {
		return punchOutput;
	}
	public void setPunchOutput(String punchOutput) {
		this.punchOutput = punchOutput;
	}
	
	
	
}
