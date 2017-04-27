package gov.usgs.volcanoes.swarm.chooser;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.core.util.StringUtils;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Winston Wave Server Panel.
 * @author Dan Cervelli
 */
public class WWSPanel extends DataSourcePanel {
  private JTextField wwsHost;
  private JTextField wwsPort;
  private JTextField wwsTimeout;
  private JCheckBox wwsCompress;

  public WWSPanel() {
    super("wws", "Winston Wave Server");
  }

  private void createFields() {
    wwsHost = new JTextField();
    wwsPort = new JTextField();
    wwsTimeout = new JTextField();
    wwsCompress = new JCheckBox();
    resetSource(source);
  }

  /**
   * @see gov.usgs.volcanoes.swarm.chooser.DataSourcePanel#resetSource(java.lang.String)
   */
  public void resetSource(String source) {
    this.source = source;
    String h = "";
    String p = "16022";
    String t = "15.0";
    boolean wscomp = true;
    if (source != null && source.indexOf(";wws:") != -1) {
      String[] ss = source.substring(source.indexOf(";wws:") + 5).split(":");
      h = ss[0];
      p = ss[1];
      t = String.format("%.1f", Integer.parseInt(ss[2]) / 1000.0);
      wscomp = ss[3].equals("1");
    }
    wwsHost.setText(h);
    wwsPort.setText(p);
    wwsTimeout.setText(t);
    wwsCompress.setSelected(wscomp);
  }
  
  @Override
  protected void createPanel() {
    createFields();
    FormLayout layout = new FormLayout("right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 126dlu", "");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);
    builder.append(new JLabel("Use this data source to connect to a Winston Wave Server (WWS)."),
        5);
    builder.nextLine();
    builder.appendSeparator();
    builder.append("IP Address or Host Name:");
    builder.append(wwsHost, 3);
    builder.nextLine();
    builder.append("Port:");
    builder.append(wwsPort);
    builder.append(" Winston default: 16022");
    builder.nextLine();

    builder.append("Timeout:");
    builder.append(wwsTimeout);
    builder.append(" seconds");
    builder.nextLine();

    builder.append("Use Compression:");
    builder.append(wwsCompress);
    builder.nextLine();

    panel = builder.getPanel();
  }

  @Override
  public boolean allowOk(boolean edit) {
    String host = wwsHost.getText();
    String message = null;

    if (host == null || host.length() == 0 || host.indexOf(';') != -1 || host.indexOf(':') != -1) {
      message = "There is an error with the WWS IP address or host name.";
    }
    
    int ip = StringUtils.stringToInt(wwsPort.getText(), -1);
    if (ip < 0 || ip > 65535) {
      message = "There is an error with the WWS port.";
    }

    double to = StringUtils.stringToDouble(wwsTimeout.getText(), -1);
    if (to <= 0) {
      message = "There is an error with the WWS time out (must be > 0).";
    }
    
    if (message != null) {
      JOptionPane.showMessageDialog(applicationFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
      return false;
    } else {
      return true;
    }
  }

  @Override
  public String wasOk() {
    int timeout = (int) (Double.parseDouble(wwsTimeout.getText()) * 1000);
    String result = String.format("wws:%s:%s:%d:%s", wwsHost.getText(), wwsPort.getText(), timeout,
        (wwsCompress.isSelected() ? "1" : "0"));
    return result;
  }

}
