package gov.usgs.volcanoes.swarm.event;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Magnitude {

  public final String publicId;
  
  private double mag;
  private String type;
  private String uncertainty;
  
  public Magnitude(Element magnitudeElement) {
    publicId = magnitudeElement.getAttribute("publicId");
//    mag = Double.parseDouble(magnitudeElement.getElementsByTagName("mag").item(0).getTextContent());
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
