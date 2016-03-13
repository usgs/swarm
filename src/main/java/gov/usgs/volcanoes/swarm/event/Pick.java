package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class Pick {
  Logger LOGGER = LoggerFactory.getLogger(Pick.class);
  
  public final String publicId;

  public Pick(Element pickElement) {
    publicId = pickElement.getAttribute("publicID");
    LOGGER.debug("new PIck {}", publicId);
  }
}
