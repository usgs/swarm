/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Holder for QuakeML origin.
 * 
 * @author Tom Parker
 *
 */
public class Origin {
  private static final Logger LOGGER = LoggerFactory.getLogger(Origin.class);

  public static enum EvaluationMode {
    AUTOMATIC, MANUAL;
  }
  
  public static enum EvaluationStatus {
    PRELIMINARY, CONFIRMED, REVIEWED, FINAL, REJECTED
  }

  public final String publicId;
  private double latitude;
  private double longitude;
  private double depth;
  private long time;
  private EvaluationMode evaluationMode;
  private EvaluationStatus evaluationStatus;
  private double standardError;
  private double azimuthalGap;
  private Map<String, Arrival> arrivals;
  private int phaseCount;
  private double minimumDistance;

  public Origin(Element originElement, Map<String, Pick> picks) {
    this.publicId = originElement.getAttribute("publicId");
    arrivals = new HashMap<String, Arrival>();

    LOGGER.debug("new origin {}", publicId);
    
    Element lonElement = (Element) originElement.getElementsByTagName("longitude").item(0);
    longitude =
        Double.parseDouble(lonElement.getElementsByTagName("value").item(0).getTextContent());

    Element latElement = (Element) originElement.getElementsByTagName("latitude").item(0);
    latitude =
        Double.parseDouble(latElement.getElementsByTagName("value").item(0).getTextContent());

    Element depthElement = (Element) originElement.getElementsByTagName("depth").item(0);
    depth = Double.parseDouble(depthElement.getElementsByTagName("value").item(0).getTextContent());

    Element timeElement = (Element) originElement.getElementsByTagName("time").item(0);
    time = 0;
    time =
        QuakeMlUtils.parseTime(timeElement.getElementsByTagName("value").item(0).getTextContent());

    Element evaluationElement = (Element) originElement.getElementsByTagName("evaluationStatus").item(0);
    if (evaluationElement != null) {
      evaluationStatus = EvaluationStatus.valueOf(evaluationElement.getTextContent().toUpperCase());
    }

    evaluationElement = (Element) originElement.getElementsByTagName("evaluationMode").item(0);
    if (evaluationElement != null) {
      evaluationMode = EvaluationMode.valueOf(evaluationElement.getTextContent().toUpperCase());
    }

    Element qualityElement = (Element) originElement.getElementsByTagName("quality").item(0);
    if (qualityElement != null) {
      Element errorElement = (Element) qualityElement.getElementsByTagName("standardError").item(0);
      if (errorElement != null) {
        standardError = Double.parseDouble(errorElement.getTextContent());        
      } else {
        standardError = Double.NaN;
      }
      
      Element gapElement = (Element) qualityElement.getElementsByTagName("azimuthalGap").item(0);
      if (gapElement != null) {
        LOGGER.debug("GAP: {}", gapElement.getTextContent());
        azimuthalGap = Double.parseDouble(gapElement.getTextContent());
      } else {
        azimuthalGap = Double.NaN;
      }

      Element phaseCountElement = (Element) qualityElement.getElementsByTagName("usedPhaseCount").item(0);
      if (gapElement != null) {
        phaseCount = Integer.parseInt(phaseCountElement.getTextContent());
      } else {
        phaseCount = -1;
      }

      Element distanceElement = (Element) qualityElement.getElementsByTagName("minimumDistance").item(0);
      if (distanceElement != null) {
        minimumDistance = Double.parseDouble(distanceElement.getTextContent());
      } else {
        minimumDistance = Double.NaN;
      }

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

  public Collection<Arrival> getArrivals() {
    return arrivals.values();
  }

  public EvaluationStatus getEvaluationStatus() {
    return evaluationStatus;
  }

  public EvaluationMode getEvaluationMode() {
    return evaluationMode;
  }
  
  public double getStandardError() {
    return standardError;
  }
  
  public double getAzimuthalGap() {
    return azimuthalGap;
  }
  
  public int getPhaseCount() {
    return phaseCount;
  }

  public double getMinimumDistance() {
    return minimumDistance;
  }
}
