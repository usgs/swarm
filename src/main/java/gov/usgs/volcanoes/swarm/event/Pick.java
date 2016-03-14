package gov.usgs.volcanoes.swarm.event;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public class Pick {
  Logger LOGGER = LoggerFactory.getLogger(Pick.class);
  private static final List<String> DATE_FORMATS = new ArrayList<String>();

  static {
    DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    DATE_FORMATS.add("yyyy-MM-dd'T'HH:mm:ss.SS");
  }

  public final String publicId;
  private long time;
  private String channel;
  
  public Pick(Element pickElement) {
    publicId = pickElement.getAttribute("publicID");
    LOGGER.debug("new PIck {}", publicId);
    
//    DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
//    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    Element timeElement = (Element) pickElement.getElementsByTagName("time").item(0);
    time = 0;
    Iterator<String> it = DATE_FORMATS.iterator();
    while (it.hasNext() && time < 1) {
      String format = it.next();
      DateFormat dateFormat = new SimpleDateFormat(format);
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      try {
        time = dateFormat.parse(timeElement.getElementsByTagName("value").item(0).getTextContent())
            .getTime();
      } catch (DOMException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ParseException e) {
        System.out.println(timeElement.getElementsByTagName("value").item(0).getTextContent()
            + " didn't match " + format);
      }
    }

    
    
    
    
    
    
//    try {
//      time = dateFormat.parse(timeElement.getElementsByTagName("value").item(0).getTextContent())
//          .getTime();
//    } catch (DOMException e) {
//      LOGGER.debug("DOMException parsing origin");
//    } catch (ParseException e) {
//      LOGGER.debug("{ParseException parsing origin");
//    }

    
    Element waveformId = (Element) pickElement.getElementsByTagName("waveformID").item(0);
    String station = waveformId.getAttribute("stationCode");
    String chan =  waveformId.getAttribute("channelCode");
    String net =  waveformId.getAttribute("networkCode");
    String loc =  waveformId.getAttribute("locationCode");
  
    channel = station + "$" + chan + "$" + net + "$" + loc;
  }
  
  public long getTime() {
    return time;
  }
  
  public String getChannel() {
    return channel;
  }
}
