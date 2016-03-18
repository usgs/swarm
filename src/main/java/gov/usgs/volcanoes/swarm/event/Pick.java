package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

public class Pick {
  Logger LOGGER = LoggerFactory.getLogger(Pick.class);

  public final String publicId;
  private long time;
  private String channel;

  public Pick(Element pickElement) {
    publicId = pickElement.getAttribute("publicID");
    LOGGER.debug("new PIck {}", publicId);

    Element timeElement = (Element) pickElement.getElementsByTagName("time").item(0);
    time =
        QuakeMlUtils.parseTime(timeElement.getElementsByTagName("value").item(0).getTextContent());

    Element waveformId = (Element) pickElement.getElementsByTagName("waveformID").item(0);
    String station = waveformId.getAttribute("stationCode");
    String chan = waveformId.getAttribute("channelCode");
    String net = waveformId.getAttribute("networkCode");
    String loc = waveformId.getAttribute("locationCode");

    channel = station + "$" + chan + "$" + net + "$" + loc;
  }

  public long getTime() {
    return time;
  }

  public String getChannel() {
    return channel;
  }
}
