package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.time.Time;
import java.awt.Color;
import java.text.ParseException;
import java.util.Comparator;

public class TagData implements Comparator<TagData> {

  public String channel;
  public double startTime;
  public String classification;
  public Color color;

  /**
   * Default constructor.
   */
  public TagData() {
    // TODO Auto-generated constructor stub
  }

  /**
   * Constructor that reads line from event classification file.
   * 
   * @param line string in format of "channel, yyyy-MM-dd HH:mm:ss, classification"
   * @throws ParseException parse exception
   */
  public TagData(String line) throws ParseException {
    parse(line);
  }

  /**
   * Constructor with parameters.
   * 
   * @param channel - channel name
   * @param startTime - start time in j2k
   * @param classification - classification string
   */
  public TagData(String channel, double startTime, String classification) {
    this.channel = channel;
    this.startTime = startTime;
    this.classification = classification;
    setColor();

  }

  private void parse(String line) throws ParseException {
    String[] data = line.split(",");
    startTime = J2kSec.parse(Time.STANDARD_TIME_FORMAT, data[0].trim());
    channel = data[1].trim();
    classification = data[2].trim();
    setColor();
  }

  private void setColor() {
    this.color = TagMenu.colors.get(classification);
    if (this.color == null) {
      this.color = TagMenu.defaultColor;
    } else {
      this.color =
          new Color(color.getRed(), color.getGreen(), color.getBlue(), TagMenu.transparency);
    }
  }

  public String toString() {
    return J2kSec.format(Time.STANDARD_TIME_FORMAT, startTime) + "," + channel + ","
        + classification;
  }

  public String getTimeString() {
    return J2kSec.format(Time.STANDARD_TIME_FORMAT, startTime);
  }

  public int compare(TagData d1, TagData d2) {
    return Double.compare(d1.startTime, d2.startTime);
  }
}
