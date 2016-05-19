/**
 * I waive copyright and related rights in the this work worldwide
 * through the CC0 1.0 Universal public domain dedication.
 * https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import org.w3c.dom.Element;

/**
 * Holder for QuakeML magnitude.
 * 
 * @author Tom Parker
 *
 */
public class Magnitude {

  public final String publicId;
  
  private double mag;
  private String type;
  private String uncertainty;
  
  public Magnitude(Element magnitudeElement) {
    publicId = magnitudeElement.getAttribute("publicId");
    type = magnitudeElement.getElementsByTagName("type").item(0).getTextContent();
    
    Element magElement = (Element) magnitudeElement.getElementsByTagName("mag").item(0);
    mag = Double.parseDouble(magElement.getElementsByTagName("value").item(0).getTextContent());
    
    Element  uncertaintyElement = (Element) magElement.getElementsByTagName("uncertainty").item(0);
    if (uncertaintyElement != null)
    uncertainty = "\u00B1" + uncertaintyElement.getTextContent();

  }

  public double getMag() {
    return mag;
  }
  
  public String getType() {
    return type;
  }

  public String getUncertainty() {
    return uncertainty;
  }
}
