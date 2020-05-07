package gov.usgs.volcanoes.swarm.chooser;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;
import gov.usgs.volcanoes.core.util.StringUtils;
import java.util.Arrays;
import java.util.TimeZone;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;

/**
 * Wave Server Panel.
 * 
 * @author Dan Cervelli
 */
public class WaveServerPanel extends DataSourcePanel {
  private JTextField wsHost;
  private JTextField wsPort;
  private JTextField wsTimeout;
  private JTextField gulperSize;
  private JTextField gulperDelay;
  private JComboBox<String> wsOffset;

  public WaveServerPanel() {
    super("ws", "Earthworm Wave Server");
  }

  private void createFields() {
    wsHost = new JTextField();
    wsPort = new JTextField();
    wsTimeout = new JTextField();
    gulperSize = new JTextField();
    gulperDelay = new JTextField();
    String[] tzs = TimeZone.getAvailableIDs();
    Arrays.sort(tzs);
    wsOffset = new JComboBox<String>(tzs);
    resetSource(source);
  }

  /**
   * Reset source.
   * 
   * @see gov.usgs.volcanoes.swarm.chooser.DataSourcePanel#resetSource(java.lang.String)
   */
  public void resetSource(String source) {
    this.source = source;
    String h = "";
    String p = "16022";
    String t = "2.0";
    String gs = "30";
    String gd = "1.0";
    wsOffset.setSelectedItem("UTC");

    if (source != null && source.indexOf(";ws:") != -1) {
      String[] ss = source.substring(source.indexOf(";ws:") + 4).split(":");
      h = ss[0];
      p = ss[1];
      t = String.format("%.1f", Integer.parseInt(ss[2]) / 1000.0);
      gs = String.format("%.0f", Integer.parseInt(ss[3]) / 60.0);
      gd = String.format("%.1f", Integer.parseInt(ss[4]) / 1000.0);
      if (ss.length >= 6) {
        wsOffset.setSelectedItem(ss[5]);
      }
    }
    wsHost.setText(h);
    wsPort.setText(p);
    wsTimeout.setText(t);
    gulperSize.setText(gs);
    gulperDelay.setText(gd);

  }

  @Override
  protected void createPanel() {
    createFields();
    FormLayout layout = new FormLayout("right:max(20dlu;pref), 3dlu, 40dlu, 0dlu, 126dlu", "");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);
    builder.append(new JLabel("Use this data source to connect to an Earthworm Wave Server (WSV)."),
        5);
    builder.nextLine();
    builder.appendSeparator();
    builder.append("IP address or host name:");
    builder.append(wsHost, 3);
    builder.nextLine();
    builder.append("Port:");
    builder.append(wsPort);
    builder.append(" Earthworm default: 16022");
    builder.nextLine();

    builder.append("Timeout:");
    builder.append(wsTimeout);
    builder.append(" seconds");
    builder.nextLine();

    builder.append("Gulp size:");
    builder.append(gulperSize);
    builder.append(" minutes");
    builder.nextLine();

    builder.append("Gulp delay:");
    builder.append(gulperDelay);
    builder.append(" seconds");

    builder.append("Tank file time zone:");
    builder.append(wsOffset, 3);
    builder.nextLine();
    // builder.append(" minutes");

    panel = builder.getPanel();
  }

  @Override
  public boolean allowOk(boolean edit) {
    String message = null;
    String host = wsHost.getText();
    if (host == null || host.length() == 0 || host.indexOf(';') != -1 || host.indexOf(':') != -1) {
      message = "There is an error with the Wave Server IP address or host name. ";
    }

    int ip = StringUtils.stringToInt(wsPort.getText(), -1);
    if (ip < 0 || ip > 65535) {
      message = "There is an error with the Wave Server port. ";
    }

    double to = StringUtils.stringToDouble(wsTimeout.getText(), -1);
    if (to <= 0) {
      message = "There is an error with the Wave Server time out (must be > 0). ";
    }

    double gs = StringUtils.stringToDouble(gulperSize.getText(), -1);
    if (gs <= 0) {
      message = "The gulper size must be greater than 0 minutes. ";
    }

    double gd = StringUtils.stringToDouble(gulperDelay.getText(), -1);
    if (gd < 0) {
      message = "The gulper delay must be greater than or equal to 0 seconds. ";
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
    int timeout = (int) (Double.parseDouble(wsTimeout.getText()) * 1000);
    int gs = (int) (Double.parseDouble(gulperSize.getText()) * 60);
    int gd = (int) (Double.parseDouble(gulperDelay.getText()) * 1000);
    String result = String.format("ws:%s:%s:%d:%d:%d:%s", wsHost.getText(), wsPort.getText(),
        timeout, gs, gd, wsOffset.getSelectedItem());
    return result;
  }

}
