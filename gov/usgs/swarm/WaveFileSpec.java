package gov.usgs.swarm;



import java.io.File;
import java.util.ArrayList;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
/**
 * 
 * This class is used to load/save wavefile specs for waves that have been loaded to the wave clipboard frame
 * 
 * @author Chirag Patel
 *
 */
@XmlRootElement
public class WaveFileSpec {
	

	private ArrayList<FileSpec> specs = new ArrayList<FileSpec>();
	
	@XmlElements(value = { @XmlElement })
	public ArrayList<FileSpec> getSpecs() {
		return specs;
	}
	
	public void setSpecs(ArrayList<FileSpec> specs) {
		this.specs = specs;
	}

	public WaveFileSpec(){
	
	}
	
	/**
	 * Load the files spec from the file wave_file_spec.xml in the uer home directory
	 * 
	 * @throws JAXBException
	 */
	public void loadFileSpec() throws JAXBException{
		String FILE_SPEC_PATH = System.getProperty("user.home") + File.separatorChar +"wave_file_spec.xml";
		File file = new File(FILE_SPEC_PATH);
		if(!file.exists()){
			saveFileSpec();
		}
		JAXBContext jaxbContext = JAXBContext.newInstance(WaveFileSpec.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
        WaveFileSpec wfc = (WaveFileSpec)jaxbUnmarshaller.unmarshal(file);
        this.specs = wfc.specs;
	}
	
	
	/**
	 * 
	 * Saves a file spec for wave files in a file wave_file_spec.xml in the user home directory
	 * 
	 * @throws JAXBException
	 */
	public  void saveFileSpec() throws JAXBException{
		String FILE_SPEC_PATH = System.getProperty("user.home") + File.separatorChar +"wave_file_spec.xml";
        File file = new File(FILE_SPEC_PATH);
		JAXBContext jaxbContext = JAXBContext.newInstance(WaveFileSpec.class);
	    Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
	    jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
	    jaxbMarshaller.marshal(this, file);
		
	}
	
	/**
	 * Get a file spec for wave file with the specified file name.
	 * 
	 * @param fileName : file name of wave file
	 * @return
	 */
	public FileSpec getFileSpec(String fileName){
		try {
			loadFileSpec();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		if(specs != null && specs.size() > 0){
			for(FileSpec spec : specs){
				if(fileName != null && fileName.equals(spec.getFileName())){
					return spec;
				}
			}
		}
		return null;
	}
	
	
	/**
	 * Gets all file specs that have the same channel count as the supplied one
	 * 
	 * @param channelCount : supplied channel count
	 * @return
	 */
	public ArrayList<FileSpec> getFileSpecs(int  channelCount){
		try {
			loadFileSpec();
		} catch (JAXBException e) {
			e.printStackTrace();
		}
		ArrayList<FileSpec> fileSpecs = new ArrayList<FileSpec>();
		if(specs != null && specs.size() > 0){
			for(FileSpec spec : specs){
				
				if(spec.getComponents().size() == channelCount){
					fileSpecs.add(spec);
				}
			}
		}
		return fileSpecs;
	}
	

}
