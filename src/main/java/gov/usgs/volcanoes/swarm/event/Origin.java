package gov.usgs.volcanoes.swarm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class Origin {
  private static final Logger LOGGER = LoggerFactory.getLogger(Origin.class);

  public final String publicId;
  private double latitude;
  private double longitude;
  private double depth;
  private long time;

  private Map<String, Arrival> arrivals;

  public Origin(Element originElement, Map<String, Pick> picks) {
    this.publicId = originElement.getAttribute("publicId");
    arrivals = new HashMap<String, Arrival>();
    
    LOGGER.debug("new origin {}", publicId);

    Element lonElement = (Element) originElement.getElementsByTagName("longitude").item(0);
    longitude = Double.parseDouble(lonElement.getElementsByTagName("value").item(0).getTextContent());

    Element latElement = (Element) originElement.getElementsByTagName("latitude").item(0);
    latitude = Double.parseDouble(latElement.getElementsByTagName("value").item(0).getTextContent());

    Element depthElement = (Element) originElement.getElementsByTagName("depth").item(0);
    depth = Double.parseDouble(depthElement.getElementsByTagName("value").item(0).getTextContent());


    Element timeElement = (Element) originElement.getElementsByTagName("time").item(0);
    time = 0;
    time = QuakeMlUtils.parseTime(timeElement.getElementsByTagName("value").item(0).getTextContent());
    
    parseArrivals(originElement.getElementsByTagName("arrival"), picks);
  }

  private void parseArrivals(NodeList arrivalElements, Map<String, Pick> picks) {
    int arrivalCount = arrivalElements.getLength();
    for (int idx = 0; idx < arrivalCount; idx++) {
      Arrival arrival = new Arrival((Element) arrivalElements.item(idx), picks);
      arrivals.put(arrival.publicId, arrival);
    }
  }

  public double getLongitude() {
    return longitude;
  }

  public double getLatitude() {
    return latitude;
  }

  public double getDepth() {
    return depth;
  }

  public long getTime() {
    return time;
  }

  public Collection<Arrival> getArrivals() {
    return arrivals.values();
  }
}
