package gov.usgs.volcanoes.swarm.wave;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.swarm.SwarmModalDialog;

import java.awt.BorderLayout;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A dialog for Monitor Mode Settings.
 * 
 * 
 * @author Dan Cervelli
 */
public class MultiMonitorSettingsDialog extends SwarmModalDialog {
  public static final long serialVersionUID = -1;

  private MultiMonitor monitor;

  private JPanel dialogPanel;

  private JComboBox<String> spanList;
  private JTextField refreshInterval;
  private JTextField slideInterval;

  private static MultiMonitorSettingsDialog dialog;

  private MultiMonitorSettingsDialog() {
    super(applicationFrame, "Monitor Settings");
    createUi();
    setSizeAndLocation();
  }

  private void createFields() {
    int[] values = MultiMonitor.SPANS;
    String[] spans = new String[values.length];
    for (int i = 0; i < spans.length; i++) {
      spans[i] = Integer.toString(values[i]);
    }
    spanList = new JComboBox<String>(spans);
    spanList.setEditable(true);
    refreshInterval = new JTextField();
    slideInterval = new JTextField();
  }

  protected void createUi() {
    super.createUi();
    createFields();

    FormLayout layout = new FormLayout("right:max(30dlu;pref), 3dlu, 40dlu, 3dlu, 40dlu", "");

    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);

    builder.append("Time Span:");
    builder.append(spanList);
    builder.append(" seconds");
    builder.nextLine();
    builder.append("Refresh Interval:");
    builder.append(refreshInterval);
    builder.append(" seconds");
    builder.nextLine();
    builder.append("Redraw Interval:");
    builder.append(slideInterval);
    builder.append(" seconds");
    builder.nextLine();

    dialogPanel = builder.getPanel();
    mainPanel.add(dialogPanel, BorderLayout.CENTER);
  }

  /**
   * Get instance of multi-monitor settings dialog.
   * @param mm multi-monitor
   * @return multi-monitor settings dialog
   */
  public static MultiMonitorSettingsDialog getInstance(MultiMonitor mm) {
    if (dialog == null) {
      dialog = new MultiMonitorSettingsDialog();
    }

    dialog.setMonitor(mm);
    dialog.setToCurrent();
    return dialog;
  }

  /**
   * @see java.awt.Dialog#setVisible(boolean)
   */
  public void setVisible(boolean b) {
    if (b) {
      this.getRootPane().setDefaultButton(okButton);
    }
    super.setVisible(b);
  }

  public void setMonitor(MultiMonitor mm) {
    monitor = mm;
    setToCurrent();
  }

  /**
   * Set to current.
   */
  public void setToCurrent() {
    slideInterval.setText(Double.toString(monitor.getSlideInterval() / 1000.0));
    refreshInterval.setText(Double.toString(monitor.getRefreshInterval() / 1000.0));
    String span = Integer.toString(monitor.getSpan());
    spanList.setSelectedItem(span);
  }

  protected void wasOk() {
    try {
      monitor.setSpan(Integer.parseInt(spanList.getSelectedItem().toString()));
      monitor.setRefreshInterval(Math.round(Double.parseDouble(refreshInterval.getText()) * 1000));
      monitor.setSlideInterval(Math.round(Double.parseDouble(slideInterval.getText()) * 1000));
    } catch (Exception e) {
      e.printStackTrace();
      // don't do anything here since all validation should occur in
      // allowOK() -- this is just worst case.
    }
  }

  protected boolean allowOk() {
    String message = null;
    try {
      message =
        "Invalid refresh interval; legal values are between 0 and 3600, 0 to refresh continuously.";
      double ri = Double.parseDouble(refreshInterval.getText());
      if (ri < 0 || ri > 3600) {
        throw new NumberFormatException();
      }

      message =
        "Invalid redraw interval; legal values are between 0 and 3600, 0 to refresh continuously.";
      ri = Double.parseDouble(refreshInterval.getText());
      if (ri < 0 || ri > 3600) {
        throw new NumberFormatException();
      }

      message = "Invalid time span.";
      ri = Integer.parseInt(spanList.getSelectedItem().toString());

      return true;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }
}
