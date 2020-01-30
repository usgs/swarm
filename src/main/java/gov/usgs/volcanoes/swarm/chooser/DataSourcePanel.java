package gov.usgs.volcanoes.swarm.chooser;

import gov.usgs.volcanoes.core.util.ResourceReader;
import gov.usgs.volcanoes.swarm.Swarm;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Data Source Panel.
 * 
 * @author Dan Cervelli
 */
public abstract class DataSourcePanel {
  protected JPanel panel;
  protected String code;
  protected String name;
  protected String source;
  protected static final JFrame applicationFrame = Swarm.getApplicationFrame();


  public DataSourcePanel(String c, String n) {
    code = c;
    name = n;
  }

  public void setSource(String s) {
    source = s;
  }

  public abstract void resetSource(String s);

  public String getCode() {
    return code;
  }

  public String getName() {
    return name;
  }

  /**
   * Get panel.
   * @return panel
   */
  public JPanel getPanel() {
    if (panel == null) {
      createPanel();
    }
    return panel;
  }

  /**
   * Creates a resource reader for the given resource. If the resource has
   * has a local filename then it is read otherwise the class loader is used.
   * 
   * @param name the resource name
   * @return resource reader
   */
  protected ResourceReader getResourceReader(String name) {
    return ResourceReader.getResourceReader(getClass(), name);
  }

  protected abstract void createPanel();

  public abstract boolean allowOk(boolean edit);

  public abstract String wasOk();
}
