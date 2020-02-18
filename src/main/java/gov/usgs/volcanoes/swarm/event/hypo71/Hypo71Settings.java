package gov.usgs.volcanoes.swarm.event.hypo71;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

/**
 * Hypo71 Test Variable Settings.
 * 
 * @author Diana Norgaard
 *
 */
public class Hypo71Settings extends Properties {

  private static final long serialVersionUID = 2145784685211194449L;
  private static final String SETTINGS_FILENAME = "Hypo71.config";
  private static final String COMMENTS = "Hypo71 Settings";
  public static final String KSING = "KSING";
  public static final String ksingDefault = "0";
  public static final String TEST = "TEST";
  public static final double[] testDefault =
      { 0.10f, 10.0f, 2.0f, 0.05f, 5.0f, 4.0f, -0.87f, 2.00f, 0.0035f, 100.0f, 8.0f, 0.5f, 1.0f };

  /**
   * Default constructor.
   */
  public Hypo71Settings() {
    readSettings();
  }

  /**
   * Read pick settings file.
   */
  private void readSettings() {
    loadDefaultProperties();
    try {
      load(new FileReader(SETTINGS_FILENAME));
    } catch (IOException e) {
      // just use default settings
    }

  }

  /**
   * Load default properties in cases where settings file does not exist.
   */
  private void loadDefaultProperties() {
    // KSING
    this.setProperty(KSING, ksingDefault);

    // Test Variables
    int i = 1;
    for (double t : testDefault) {
      String propertyName = String.format(TEST + "%02d", i);
      this.setProperty(propertyName, String.format("%.2f", t));
      i++;
    }
  }

  /**
   * Save pick settings properties to file.
   * 
   * @throws IOException IOException
   */
  protected void save() throws IOException {
    FileWriter writer = new FileWriter(SETTINGS_FILENAME);
    store(writer, COMMENTS);
  }

  /**
   * Get KSING value for Control Card input.
   * 
   * @return 0 if using original SINGLE subroutine; 1 if using modified SINGLE subroutine
   */
  protected int getKsing() {
    int ksing = Integer.valueOf(this.getProperty(KSING));
    return ksing;
  }

  /**
   * Get array of Hypo71 test variables.
   * 
   * @return hypo71 test variables
   */
  protected double[] getTestValues() {
    double[] test = new double[testDefault.length];
    for (int i = 1; i <= test.length; i++) {
      String propertyName = String.format(TEST + "%02d", i);
      test[i - 1] = Double.valueOf(this.getProperty(propertyName));
    }
    return test;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Hashtable#keys()
   */
  public Enumeration keys() {
    Enumeration<Object> keysEnum = super.keys();
    Vector<String> keyList = new Vector<String>();
    while (keysEnum.hasMoreElements()) {
      keyList.add((String) keysEnum.nextElement());
    }
    Collections.sort(keyList);
    return keyList.elements();
  }
}
