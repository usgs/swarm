package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;

import java.text.ParseException;

public class TagData {
  
  public String channel;
  public double startTime;
  public String classification;

  /**
   * Default constructor.
   */
  public TagData() {
    // TODO Auto-generated constructor stub
  }
  
  /**
   * Constructor that reads line from event classification file.
   * @param line string in format of "channel, yyyy-MM-dd HH:mm:ss, classification"
   * @throws ParseException 
   */
  public TagData(String line) throws ParseException {
    parse(line);
  }

  /**
   * Constructor with parameters.
   * @param channel - channel name
   * @param startTime - start time in j2k
   * @param classification - classification string
   */
  public TagData(String channel, double startTime, String classification) {
    this.channel = channel;
    this.startTime = startTime;
    this.classification = classification;
  }
  
  private void parse(String line) throws ParseException {
    String[] data = line.split(",");
    channel = data[0].trim();
    startTime = J2kSec.parse(Time.STANDARD_TIME_FORMAT, data[1].trim());
    classification = data[2].trim();
  }

  public String toString() {
    return channel + "," + J2kSec.format(Time.STANDARD_TIME_FORMAT, startTime) + ","
        + classification;
  }
}
