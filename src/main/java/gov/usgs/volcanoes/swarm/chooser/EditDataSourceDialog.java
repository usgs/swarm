package gov.usgs.volcanoes.swarm.chooser;

import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * Edit data source dialog.
 * @author Dan Cervelli
 */
public class EditDataSourceDialog extends SwarmModalDialog {
  private static final long serialVersionUID = 1L;
  private static final JFrame applicationFrame = Swarm.getApplicationFrame();

  private String source;
  private boolean edit;

  private JTextField name;

  private JTabbedPane tabPane;

  private List<DataSourcePanel> panels;

  private String result;

  /**
   * Edit data source dialog.
   * @param s source name
   */
  public EditDataSourceDialog(String s) {
    super(applicationFrame, "");
    createPanels();
    source = s;
    if (source == null) {
      this.setTitle("New Data Source");
      edit = false;
    } else {
      this.setTitle("Edit Data Source");
      edit = true;
    }
    createDataSourceUi();
    setSizeAndLocation();
  }

  private void createPanels() {
    panels = new ArrayList<DataSourcePanel>();
    panels.add(new WWSPanel());
    panels.add(new WaveServerPanel());
    // panels.add(new DHIPanel());
    panels.add(new WebServicesPanel());
    panels.add(new SeedLinkPanel());
  }

  protected void createDataSourceUi() {

    String src = null;
    if (source != null) {
      src = source.substring(source.indexOf(';') + 1, source.indexOf(':'));
    }

    tabPane = new JTabbedPane();
    JPanel dsPanel = new JPanel(new BorderLayout());
    for (DataSourcePanel dsp : panels) {
      dsp.setSource(source);
      JPanel p = dsp.getPanel();
      tabPane.add(dsp.getName(), p);
      if (src != null && src.equals(dsp.getCode())) {
        tabPane.setSelectedComponent(p);
      }
    }

    dsPanel.add(tabPane, BorderLayout.CENTER);

    Box namePanel = new Box(BoxLayout.X_AXIS);
    namePanel.add(new JLabel("Data Source Name:"));
    namePanel.add(Box.createHorizontalStrut(10));
    String n = "";
    if (source != null) {
      n = source.substring(0, source.indexOf(';'));
    }
    name = new JTextField(30);
    namePanel.add(name);
    name.setText(n);
    dsPanel.add(namePanel, BorderLayout.NORTH);
    dsPanel.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
    mainPanel.add(dsPanel, BorderLayout.CENTER);
  }

  /**
   * Reset data source.
   * @param src source name
   */
  public void resetSource(String src) {
    if (src != null) {
      source = src;
      String s = source.substring(source.indexOf(';') + 1, source.indexOf(':'));
      for (DataSourcePanel dsp : panels) {
        dsp.resetSource(source);
        JPanel p = dsp.getPanel();
        if (s.equals(dsp.getCode())) {
          tabPane.setSelectedComponent(p);
        }
      }
      name.setText(source.substring(0, source.indexOf(';')));
    }
  }

  protected boolean allowOk() {
    String n = name.getText().trim();
    String message = null;
    if (n == null || n.length() <= 0) {
      message = "You must specify a name for this data source.";
    } else if (!edit && SwarmConfig.getInstance().sourceExists(n)) {
      message = "A data source by that name already exists.";
    } else if (n.contains(";")) {
      message = "Data source name cannot contain ';'";
    }

    if (message != null) {
      JOptionPane.showMessageDialog(applicationFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    }

    DataSourcePanel p = panels.get(tabPane.getSelectedIndex());
    return p.allowOk(edit);
  }

  /**
   * @see java.awt.Dialog#setVisible(boolean)
   */
  public void setVisible(boolean b) {
    if (b) {
      if (source == null) {
        name.setText("");
      }
      result = null;
    }
    super.setVisible(b);
  }

  protected void wasOk() {
    DataSourcePanel p = panels.get(tabPane.getSelectedIndex());
    result = name.getText().trim() + ";" + p.wasOk();
  }

  public String getResult() {
    return result;
  }
}
