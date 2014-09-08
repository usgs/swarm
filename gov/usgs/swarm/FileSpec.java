package gov.usgs.swarm;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class that represents an xml representation of file spec saved for a wave file with complete station-component info that have been loaded before that can then be 
 * reused when loading files with the same number of channels as with the saved wave file spec
 * 
 * @author Chirag Patel
 *
 */
@XmlRootElement
public class FileSpec {
	String fileName;

	List<Component> components = new ArrayList<Component>();

	@XmlElement
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@XmlElements(value = { @XmlElement })
	public List<Component> getComponents() {
		return components;
	}

	public void setComponents(List<Component> components) {
		this.components = components;
	}

	//This inner class represents the component info per channel saved for this file spec
	@XmlRootElement
	public
	static class Component{
		
		Integer index;
		
		String componentCode;
		
		String lastComponentCode;
		
		String stationCode;
		
		
		String networkCode;
		
		@XmlElement
		public String getComponentCode() {
			return componentCode;
		}
		public void setComponentCode(String componentCode) {
			this.componentCode = componentCode;
		}
		
		@XmlElement
		public String getStationCode() {
			return stationCode;
		}
		public void setStationCode(String stationCode) {
			this.stationCode = stationCode;
		}
		
		@XmlElement
		public String getNetworkCode() {
			return networkCode;
		}
		public void setNetworkCode(String networkCode) {
			this.networkCode = networkCode;
		}
		
		@XmlElement
		public Integer getIndex() {
			return index;
		}
		public void setIndex(Integer index) {
			this.index = index;
		}
		
		@XmlElement
		public String getLastComponentCode() {
			return lastComponentCode;
		}
		
		public void setLastComponentCode(String lastComponentCode) {
			this.lastComponentCode = lastComponentCode;
		}
		
		
		
		
		
	}
	
	@Override
	public String toString(){
		return this.fileName;
	}
	
	
	/**
	 * Get the component info for a particular channel from a wave data saved for this filespec
	 * 
	 * @param index
	 * @return
	 */
	public Component getComponent(int index){
		for(Component comp : components){
			if(comp.getIndex() == index){
				return comp;
			}
		}
		return null;
	}
}
