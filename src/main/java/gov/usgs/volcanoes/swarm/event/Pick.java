/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.text.ParseException;

/**
 * Holder for QuakeML pick.
 * 
 * @author Tom Parker
 *
 */
public class Pick {
  Logger LOGGER = LoggerFactory.getLogger(Pick.class);

  public final String publicId;
  private long time;
  private String channel;
  private Onset onset;
  private Polarity polarity;


  public static enum Onset {
    EMERGENT, IMPULSIVE, QUESTIONABLE;

    public static Onset parse(String string) throws ParseException {
      if ("emergent".equals(string)) {
        return EMERGENT;
      } else if ("impulsive".equals(string)) {
        return IMPULSIVE;
      } else if ("questionable".equals(string)) {
        return QUESTIONABLE;
      } else {
        throw new ParseException("Cannot parse " + string, 12);
      }

    }
  }

  public static enum Polarity {
    POSITIVE, NEGATIVE, UNDECIDABLE;

    public static Polarity parse(String string) throws ParseException {
      if ("positive".equals(string)) {
        return POSITIVE;
      } else if ("negative".equals(string)) {
        return NEGATIVE;
      } else if ("undecidable".equals(string)) {
        return UNDECIDABLE;
      } else {
        throw new ParseException("Cannot parse " + string, 12);
      }
    }
  }

  public Pick(Element pickElement) {
    publicId = pickElement.getAttribute("publicID");
    LOGGER.debug("new PIck {}", publicId);

    Element timeElement = (Element) pickElement.getElementsByTagName("time").item(0);
    time =
        QuakeMlUtils.parseTime(timeElement.getElementsByTagName("value").item(0).getTextContent());

    NodeList onsetList = pickElement.getElementsByTagName("onset");
    if (onsetList != null && onsetList.getLength() > 0) {
      try {
        onset = Onset.parse(onsetList.item(0).getTextContent());
      } catch (DOMException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }

    NodeList polarityList = pickElement.getElementsByTagName("polarity");
    if (polarityList != null && polarityList.getLength() > 0) {
      try {
        polarity =
            Polarity.parse(pickElement.getElementsByTagName("polarity").item(0).getTextContent());
      } catch (DOMException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      } catch (ParseException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
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

  public Onset getOnset() {
    return onset;
  }

  public Polarity getPolarity() {
    return polarity;
  }
}
