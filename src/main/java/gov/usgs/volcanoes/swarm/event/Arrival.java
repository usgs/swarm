package gov.usgs.volcanoes.swarm.event;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class Arrival {
  Logger LOGGER = LoggerFactory.getLogger(Arrival.class);
  
public final String publicId;
private Pick pick;
private String phase;

public Arrival(Element arrivalElement, Map<String, Pick> picks) {
  this.publicId = arrivalElement.getAttribute("publicID");
  LOGGER.debug("new arrival {}", publicId);
  
  this.phase = arrivalElement.getAttribute("phase");
  this.pick = picks.get(arrivalElement.getAttribute("pickID"));
}
  
}
