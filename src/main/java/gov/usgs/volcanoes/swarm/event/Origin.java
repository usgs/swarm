package gov.usgs.volcanoes.swarm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Origin {
  private static final Logger LOGGER = LoggerFactory.getLogger(Origin.class);
  private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSX";

  public final String publicId;
  private double latitude;
  private double longitude;
  private double depth;
  private long time;

  private Map<String, Arrival> arrivals;

  public Origin(Element originElement, Map<String, Pick> picks) {
    this.publicId = originElement.getAttribute("publicId");
    LOGGER.debug("new origin {}", publicId);

    this.longitude = Double
        .parseDouble(originElement.getElementsByTagName("longitude").item(0).getTextContent());
    this.latitude =
        Double.parseDouble(originElement.getElementsByTagName("latitude").item(0).getTextContent());

    Element depthElement = (Element) originElement.getElementsByTagName("depth").item(0);
    depth = Double.parseDouble(depthElement.getElementsByTagName("value").item(0).getTextContent());

    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    
    Element timeElement = (Element) originElement.getElementsByTagName("time").item(0);
    try {
      time = dateFormat.parse(timeElement.getElementsByTagName("value").item(0).getTextContent()).getTime();
    } catch (DOMException e) {
      LOGGER.debug("DOMException parsing origin");
    } catch (ParseException e) {
      LOGGER.debug("{ParseException parsing origin");
    }
    
    

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
}
