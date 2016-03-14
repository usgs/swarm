package gov.usgs.volcanoes.swarm.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import gov.usgs.volcanoes.swarm.picker.EventObserver;

public class Event {
  private static final Logger LOGGER = LoggerFactory.getLogger(Event.class);

  public final String publicId;
  private final Map<String, Origin> origins;
  private final Map<String, Magnitude> magnitudes;
  private final Map<String, Pick> picks;
  private final List<EventObserver> observers;

  private Origin preferredOrigin;
  private Magnitude preferredMagnitude;
  private String description;
  private String eventSource;
  private String evid;

  public Event(String publicId) {
    this.publicId = publicId;
    LOGGER.debug("New event ({}}", publicId);

    origins = new HashMap<String, Origin>();
    magnitudes = new HashMap<String, Magnitude>();
    picks = new HashMap<String, Pick>();
    observers = new ArrayList<EventObserver>();
  }

  public Event(Element event) {
    this(event.getAttribute("publicID"));

    parseEvent(event);
  }

  public void addObserver(EventObserver observer) {
    observers.add(observer);
  }

  private void parseEvent(Element event) {

    // order matters.
    parsePicks(event.getElementsByTagName("pick"));

    parseOrigins(event.getElementsByTagName("origin"));
    preferredOrigin = origins.get(event.getAttribute("preferredOriginID"));
    if (preferredOrigin == null && origins.size() > 0) {
      preferredOrigin = (Origin) origins.values().toArray()[0];
    }

    parseMagnitudes(event.getElementsByTagName("magnitude"));
    preferredMagnitude = magnitudes.get(event.getAttribute("preferredMagnitudeID"));
    if (preferredMagnitude == null && magnitudes.size() > 0) {
      preferredMagnitude = (Magnitude) magnitudes.values().toArray()[0];
    }

    eventSource = event.getAttribute("catalog:eventsource");
    evid = event.getAttribute("catalog:eventid");
  }

  private void parsePicks(NodeList pickElements) {
    int pickCount = pickElements.getLength();
    for (int idx = 0; idx < pickCount; idx++) {
      Pick pick = new Pick((Element) pickElements.item(idx));
      picks.put(pick.publicId, pick);
    }
  }


  private void parseOrigins(NodeList originElements) {
    int originCount = originElements.getLength();
    for (int idx = 0; idx < originCount; idx++) {
      Origin origin = new Origin((Element) originElements.item(idx), picks);
      origins.put(origin.publicId, origin);
    }
  }

  public Origin getPreferredOrigin() {

    return preferredOrigin;
  }

  private void parseMagnitudes(NodeList magnitudeElements) {
    int originCount = magnitudeElements.getLength();
    for (int idx = 0; idx < originCount; idx++) {
      Magnitude magnitude = new Magnitude((Element) magnitudeElements.item(idx));
      magnitudes.put(magnitude.publicId, magnitude);
    }
  }

  public Magnitude getPerferredMagnitude() {
    return preferredMagnitude;
  }

  public String getEventSource() {
    return eventSource;
  }
  
  public String getEvid() {
    return evid;
  }
  public String getDataid() {
    return null;
  }

}
