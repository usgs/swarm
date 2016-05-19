/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import java.util.Comparator;
import java.util.Map;

import gov.usgs.volcanoes.swarm.event.Pick.Onset;
import gov.usgs.volcanoes.swarm.event.Pick.Polarity;

/**
 * Holder for QuakeML arrival.
 * 
 * @author Tom Parker
 *
 */
public class Arrival {
  Logger LOGGER = LoggerFactory.getLogger(Arrival.class);

  public final String publicId;
  private Pick pick;
  private String phase;
  private double timeResidual;
  private double distance;

  public Arrival(Element arrivalElement, Map<String, Pick> picks) {
    this.publicId = arrivalElement.getAttribute("publicID");
    LOGGER.debug("new arrival {}", publicId);

    // this.phase = arrivalElement.getAttribute("phase");
    pick = picks.get(arrivalElement.getElementsByTagName("pickID").item(0).getTextContent());
    phase = arrivalElement.getElementsByTagName("phase").item(0).getTextContent();
    timeResidual = Double
        .parseDouble(arrivalElement.getElementsByTagName("timeResidual").item(0).getTextContent());
    distance = Double
        .parseDouble(arrivalElement.getElementsByTagName("distance").item(0).getTextContent());
 }

  public Pick getPick() {
    return pick;
  }

  public String getPhase() {
    return phase;
  }

  public double getTimeResidual() {
    return timeResidual;
  }

  public String getTag() {
    StringBuilder sb = new StringBuilder();
    
    Onset onset = pick.getOnset();
    if (onset == Pick.Onset.EMERGENT) {
      sb.append("e");
    } else if (onset == Pick.Onset.IMPULSIVE) {
      sb.append("i");
    }
    
    sb.append(phase.charAt(0));
    
    Polarity polarity = pick.getPolarity();
    if (polarity == Pick.Polarity.NEGATIVE) {
      sb.append("-");
    } else if (polarity == Pick.Polarity.POSITIVE) {
      sb.append("+");
    }
    
    return sb.toString();
  }
  
  public static Comparator<Arrival> distanceComparator() {
    return new Comparator<Arrival>() {
      public int compare(final Arrival e1, final Arrival e2) {
        return Double.compare(e1.distance, e2.distance);
      }
    };
  }

}
