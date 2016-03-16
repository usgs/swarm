package gov.usgs.volcanoes.swarm.event;

import java.util.Map;

import org.w3c.dom.Element;

public class Magnitude {

  public final String publicId;
  
  private double mag;
  private String type;
  
  public Magnitude(Element magnitudeElement) {
    publicId = magnitudeElement.getAttribute("publicId");
//    mag = Double.parseDouble(magnitudeElement.getElementsByTagName("mag").item(0).getTextContent());
    type = magnitudeElement.getElementsByTagName("type").item(0).getTextContent();
    
    Element magElement = (Element) magnitudeElement.getElementsByTagName("mag").item(0);
    mag = Double.parseDouble(magElement.getElementsByTagName("value").item(0).getTextContent());

  }

  public double getMag() {
    return mag;
  }
  
  public String getType() {
    return type;
  }

}
