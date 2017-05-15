package gov.usgs.volcanoes.swarm.event;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JOptionPane;

/**
 * Pick Settings.
 * @author Diana Norgaard
 *
 */
public class PickSettings extends Properties {
  private static final long serialVersionUID = 897150893674347168L;
  private static final String SETTINGS_FILENAME = "PickSettings.config";
  public static final String WEIGHT = "weight";
  public static final String WEIGHT_UNIT = "weight_unit";
  private final int numWeight = 5;
  private static final String COMMENTS = "Swarm Pick Settings Configuration";
  
  public enum WeightUnit { SAMPLES, MILLISECONDS }
  
  
  /**
   * Default constructor.
   */
  public PickSettings() {
    readSettings();
  }

  /**
   * Read pick settings file.
   */
  private void readSettings() {
    loadDefaultProperties(); 
    try {
      load(new FileReader(SETTINGS_FILENAME));
    } catch (FileNotFoundException e) {
      JOptionPane.showMessageDialog(null, e.getMessage(), "Error loading pick settings.",
          JOptionPane.ERROR_MESSAGE);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  /**
   * Load default properties in cases where settings file does not exist.
   */
  private void loadDefaultProperties() {
    this.setProperty(WEIGHT_UNIT, WeightUnit.SAMPLES.toString());
    this.setProperty(WEIGHT + ".0", "1");
    this.setProperty(WEIGHT + ".1", "2");
    this.setProperty(WEIGHT + ".2", "5");
    this.setProperty(WEIGHT + ".3", "10");
    this.setProperty(WEIGHT + ".4", "20");
  }
  
  /**
   * Save pick settings properties to file.
   * @throws IOException IOException
   */
  protected void save() throws IOException {
    FileWriter writer = new FileWriter(SETTINGS_FILENAME);
    store(writer, COMMENTS);
  }
   
  /**
   * Convert weight to time. In case where unit is in samples, 
   * the sample rate is required.
   * @param weight weight
   * @param sampleRate sample rate to use if weights are in samples
   * @return milliseconds
   */
  public long getWeightToTime(int weight, double sampleRate) {
    if (weight > numWeight) {
      String message = "Weight must be between 0 and " + numWeight;
      throw new IllegalArgumentException(message);
    }
    String prop = getProperty(WEIGHT + "." + weight);
    long value = Long.valueOf(prop);
    if (getWeightUnit().equals(WeightUnit.SAMPLES)) {
      long millis = (long) (1000 * value / sampleRate);
      return millis;
    } else {
      return value; // already in millis
    }
  }

  /**
   * Number of weights used.
   * @return number of weights
   */
  public int getNumWeight() {
    return numWeight;
  }
  
  /**
   * Type of unit mapped to weight.
   * @return WeightUnit
   */
  public WeightUnit getWeightUnit() {
    return WeightUnit.valueOf(getProperty(WEIGHT_UNIT));
  }
}
