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
    try {
      load(new FileReader(SETTINGS_FILENAME));
    } catch (FileNotFoundException e) {
      JOptionPane.showMessageDialog(null, e.getMessage(), "Error loading pick settings.",
          JOptionPane.ERROR_MESSAGE);
      loadDefaultProperties();
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
  
  protected void save() throws IOException {
    FileWriter writer = new FileWriter(SETTINGS_FILENAME);
    store(writer, COMMENTS);
  }
 
  /**
   * Convert weight to time.
   * @param weight 0-3
   * @param sampleRate sample rate to use in converting weight to time
   * @return milliseconds
   */
  public long getWeightToTime(int weight, double sampleRate) {
    if (weight > numWeight) {
      String message = "Weight must be between 0 and " + numWeight;
      throw new IllegalArgumentException(message);
    }
    String prop = getProperty(WEIGHT + "." + weight);
    int numSample = Integer.valueOf(prop);
    long millis =  (long) (1000 * numSample / sampleRate);
    return millis;
  }

  public int getNumWeight() {
    return numWeight;
  }
}
