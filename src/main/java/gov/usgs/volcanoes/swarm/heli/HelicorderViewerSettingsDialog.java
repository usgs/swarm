package gov.usgs.volcanoes.swarm.heli;

import gov.usgs.volcanoes.core.configfile.ConfigFile;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.ui.GridBagHelper;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.internalframe.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettings;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettingsDialog;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

/**
 * Helicorder viewer settings dialog.
 * 
 * @author Dan Cervelli
 */
public class HelicorderViewerSettingsDialog extends SwarmModalDialog {
  public static final long serialVersionUID = -1;

  private HelicorderViewerSettings settings;
  private WaveViewSettings waveSettings;

  private JPanel dialogPanel;

  private JComboBox<String> chunkList;
  private JComboBox<String> spanList;
  private JTextField bottomTime;
  private JComboBox<String> zoomList;
  private JTextField refreshInterval;
  private JTextField scrollSize;
  private DateFormat utcDateFormat;
  private JCheckBox removeDrift;

  private JCheckBox showClip;
  private JCheckBox alertClip;
  private JTextField alertClipTimeout;
  private JLabel alertClipTimeoutLabel;
  private JTextField clipValue;
  private JCheckBox autoScale;
  private JTextField barRange;
  private JCheckBox applyToAll;

  private static HelicorderViewerSettingsDialog dialog;

  private HelicorderViewerSettingsDialog() {
    // super(Swarm.getApplication(), "Helicorder View Settings", true,
    // WIDTH, HEIGHT);
    super(Swarm.getApplicationFrame(), "Helicorder View Settings");
    utcDateFormat = new SimpleDateFormat("yyyyMMddHHmm");
    utcDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

    createUi();
    setSizeAndLocation();
  }

  /**
   * Get instance of helicorder viewer settins dialog.
   * 
   * @param s helicorder viewer settings
   * @param s2 wave view settings
   * @return helicorder viewer settings dialog
   */
  public static HelicorderViewerSettingsDialog getInstance(HelicorderViewerSettings s,
      WaveViewSettings s2) {
    if (dialog == null) {
      dialog = new HelicorderViewerSettingsDialog();
    }

    dialog.setSettings(s, s2);
    // dialog.setToCurrent();
    return dialog;
  }

  /**
   * Set visible.
   * 
   * @see java.awt.Dialog#setVisible(boolean)
   */
  public void setVisible(boolean b) {
    if (b) {
      this.getRootPane().setDefaultButton(okButton);
    }
    super.setVisible(b);
  }

  /**
   * Set settings.
   * 
   * @param s helicorder viewer settings
   * @param s2 wave view settings
   */
  public void setSettings(HelicorderViewerSettings s, WaveViewSettings s2) {
    settings = s;
    waveSettings = s2;
    setToCurrent();
  }

  private void createComponents() {
    int[] values = HelicorderViewerFrame.chunkValues;
    String[] chunks = new String[values.length];
    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = Integer.toString(values[i] / 60);
    }
    chunkList = new JComboBox<String>(chunks);

    values = HelicorderViewerFrame.spanValues;
    String[] spans = new String[values.length];
    for (int i = 0; i < spans.length; i++) {
      spans[i] = Integer.toString(values[i] / 60);
    }

    spanList = new JComboBox<String>(spans);
  }

  protected void createUi() {
    super.createUi();
    createComponents();

    // AXIS PANEL
    int[] values = HelicorderViewerFrame.chunkValues;
    String[] chunks = new String[values.length];
    for (int i = 0; i < chunks.length; i++) {
      chunks[i] = Integer.toString(values[i] / 60);
    }

    chunkList = new JComboBox<String>(chunks);

    values = HelicorderViewerFrame.spanValues;
    String[] spans = new String[values.length];
    for (int i = 0; i < spans.length; i++) {
      spans[i] = Integer.toString(values[i] / 60);
    }

    spanList = new JComboBox<String>(spans);

    JLabel chunkLabel = new JLabel("X, minutes:");
    chunkLabel.setLabelFor(chunkList);
    JLabel spanLabel = new JLabel("Y, hours:");
    spanLabel.setLabelFor(spanList);
    bottomTime = new JTextField();
    bottomTime.setToolTipText("Format: YYYYMMDDhhmm");
    JLabel bottomLabel = new JLabel("View time:");
    bottomLabel.setLabelFor(bottomTime);
    bottomLabel.setToolTipText("Format: YYYYMMDDhhmm");
    bottomTime.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        bottomTime.selectAll();
      }

      public void focusLost(FocusEvent e) {}
    });
    JPanel axisPanel = new JPanel(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    axisPanel.setBorder(new TitledBorder(new EtchedBorder(), "Axes"));
    axisPanel.add(chunkLabel,
        GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
    axisPanel.add(chunkList, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
    axisPanel.add(spanLabel, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;a=w;f=n"));
    axisPanel.add(spanList, GridBagHelper.set(c, "x=2;y=1;w=1;h=1;f=h;wx=0;a=e"));
    axisPanel.add(bottomLabel, GridBagHelper.set(c, "x=0;y=2;w=1;h=1;wx=0;a=w;f=n"));
    axisPanel.add(bottomTime, GridBagHelper.set(c, "x=1;y=2;w=2;h=1;f=h;wx=1;a=e"));

    // ZOOM PANEL
    values = HelicorderViewerFrame.zoomValues;
    String[] zooms = new String[values.length];
    for (int i = 0; i < zooms.length; i++) {
      zooms[i] = Integer.toString(values[i]);
    }

    zoomList = new JComboBox<String>(zooms);
    JLabel zoomLabel = new JLabel("Zoom, seconds:");
    zoomLabel.setLabelFor(zoomList);
    JButton waveSettingsButton = new JButton("Wave Settings", Icons.wavesettings);
    waveSettingsButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        WaveViewSettingsDialog wvsd = WaveViewSettingsDialog.getInstance(waveSettings);
        wvsd.setVisible(true);
      }
    });

    JPanel zoomPanel = new JPanel(new GridBagLayout());
    c = new GridBagConstraints();
    zoomPanel.setBorder(new TitledBorder(new EtchedBorder(), "Zoom"));
    zoomPanel.add(zoomLabel,
        GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
    zoomPanel.add(zoomList, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
    zoomPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=3;h=1;f=h;i=5,0,5,0"));
    zoomPanel.add(waveSettingsButton, GridBagHelper.set(c, "x=0;y=2;w=3;h=1;a=e;f=n;i=0,4,0,4"));

    // CLIPPING PANEL
    showClip = new JCheckBox("Show clip");
    showClip.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setShowClipEnabled(showClip.isSelected());
      }
    });
    alertClip = new JCheckBox("Audible clipping");
    alertClip.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setAlertClipEnabled(alertClip.isSelected());
      }
    });

    alertClipTimeoutLabel = new JLabel("Alert frequency, minutes: ");
    alertClipTimeout = new JTextField(4);
    alertClipTimeoutLabel.setLabelFor(alertClipTimeout);

    JPanel clippingPanel = new JPanel(new GridBagLayout());
    c = new GridBagConstraints();
    clippingPanel.setBorder(new TitledBorder(new EtchedBorder(), "Clipping"));
    clippingPanel.add(showClip,
        GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
    clippingPanel.add(alertClip,
        GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
    clippingPanel.add(alertClipTimeoutLabel, GridBagHelper.set(c, "x=0;y=2;w=1;h=1;f=h;wx=0;a=w"));
    clippingPanel.add(alertClipTimeout, GridBagHelper.set(c, "x=1;y=2;w=1;h=1;f=h;wx=1;a=e"));

    // OTHER PANEL
    JLabel cvLabel = new JLabel("Clip threshold:");
    clipValue = new JTextField();
    cvLabel.setLabelFor(clipValue);

    refreshInterval = new JTextField(4);
    JLabel refreshLabel = new JLabel("Refresh, seconds: ");
    refreshLabel.setLabelFor(refreshInterval);
    JLabel scrollLabel = new JLabel("Scroll size, rows: ");
    scrollSize = new JTextField(4);
    scrollLabel.setLabelFor(scrollSize);

    autoScale = new JCheckBox("Auto-scale");
    autoScale.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setAutoScaleEnabled(autoScale.isSelected());
      }
    });
    JLabel obrLabel = new JLabel("One bar range:");
    barRange = new JTextField();
    obrLabel.setLabelFor(barRange);

    JPanel otherPanel = new JPanel(new GridBagLayout());
    c = new GridBagConstraints();
    otherPanel.setBorder(new TitledBorder(new EtchedBorder(), "Other"));

    otherPanel.add(refreshLabel,
        GridBagHelper.set(c, "x=0;y=0;w=2;h=1;wx=1;ix=12;iy=2;a=w;f=n;i=0,4,0,4"));
    otherPanel.add(refreshInterval, GridBagHelper.set(c, "x=2;y=0;w=1;h=1;f=h;wx=0;a=e"));
    otherPanel.add(scrollLabel, GridBagHelper.set(c, "x=0;y=1;w=2;h=1;wx=1;a=w;f=n"));
    otherPanel.add(scrollSize, GridBagHelper.set(c, "x=2;y=1;w=1;h=1;f=h;wx=0;a=e"));
    removeDrift = new JCheckBox("Force center");
    otherPanel.add(removeDrift, GridBagHelper.set(c, "x=0;y=2;w=3;h=1;f=h;wx=0;a=w"));

    otherPanel.add(autoScale, GridBagHelper.set(c, "x=0;y=6;w=3;h=1;f=h;wx=0;a=w"));
    otherPanel.add(obrLabel, GridBagHelper.set(c, "x=0;y=7;w=2;h=1;wx=1;a=w;f=n"));
    otherPanel.add(barRange, GridBagHelper.set(c, "x=2;y=7;w=1;h=1;f=h;wx=0;a=e"));

    otherPanel.add(cvLabel, GridBagHelper.set(c, "x=0;y=8;w=2;h=1;wx=1;a=w;f=n"));
    otherPanel.add(clipValue, GridBagHelper.set(c, "x=2;y=8;w=1;h=1;f=h;wx=0;a=e"));

    applyToAll = new JCheckBox("Apply to all open helicorders");

    // CENTER PANEL
    JPanel centerPanel = new JPanel();
    BoxLayout bl = new BoxLayout(centerPanel, BoxLayout.Y_AXIS);
    centerPanel.setLayout(bl);

    centerPanel.add(axisPanel);
    centerPanel.add(zoomPanel);
    centerPanel.add(clippingPanel);
    centerPanel.add(otherPanel);
    centerPanel.add(applyToAll);

    // DIALOG PANEL
    dialogPanel = new JPanel(new BorderLayout());
    dialogPanel.add(centerPanel, BorderLayout.CENTER);


    mainPanel.add(dialogPanel, BorderLayout.CENTER);
  }

  private void setShowClipEnabled(final boolean b) {
    SwingWorker worker = new SwingWorker() {
      public Object construct() {
        alertClip.setEnabled(b);
        setAlertClipEnabled(b);
        return null;
      }

      public void finished() {}
    };
    worker.start();
  }

  private void setAlertClipEnabled(final boolean b) {
    SwingWorker worker = new SwingWorker() {
      public Object construct() {
        boolean state = showClip.isSelected() && alertClip.isSelected();
        alertClipTimeout.setEnabled(state);
        alertClipTimeoutLabel.setEnabled(state);
        return null;
      }

      public void finished() {}
    };
    worker.start();
  }

  private void setAutoScaleEnabled(final boolean b) {
    SwingWorker worker = new SwingWorker() {
      public Object construct() {
        clipValue.setEnabled(!b);
        barRange.setEnabled(!b);
        return null;
      }

      public void finished() {}
    };
    worker.start();
  }

  /**
   * Set to current.
   */
  public void setToCurrent() {
    if (settings == null || waveSettings == null) {
      return;
    }

    String tc = Integer.toString((int) (settings.timeChunk / 60.0));
    chunkList.setSelectedItem(tc);

    String span = Integer.toString((int) (settings.span / 60.0));
    spanList.setSelectedItem(span);

    String wzo = Integer.toString(settings.waveZoomOffset);
    zoomList.setSelectedItem(wzo);

    double bt = settings.getBottomTime();
    if (Double.isNaN(bt)) {
      bottomTime.setText("Now");
    } else {
      double tzo =
          swarmConfig.getTimeZone(settings.channel).getOffset(System.currentTimeMillis()) / 1000;

      bottomTime.setText(utcDateFormat.format(J2kSec.asDate(bt + tzo)));
    }

    refreshInterval.setText(Integer.toString(settings.refreshInterval));
    scrollSize.setText(Integer.toString(settings.scrollSize));

    removeDrift.setSelected(settings.forceCenter);

    clipValue.setText(Double.toString(settings.clipValue));
    barRange.setText(Double.toString(settings.barRange));
    autoScale.setSelected(settings.autoScale);
    showClip.setSelected(settings.showClip);
    alertClip.setSelected(settings.alertClip);

    String alertTimeOut = Integer.toString(settings.alertClipTimeout / 60);
    alertClipTimeout.setText(alertTimeOut);

    setAutoScaleEnabled(settings.autoScale);
  }

  protected void wasOk() {
    try {
      settings.timeChunk = Integer.parseInt(chunkList.getSelectedItem().toString()) * 60;
      settings.span = Integer.parseInt(spanList.getSelectedItem().toString()) * 60;
      settings.waveZoomOffset = Integer.parseInt(zoomList.getSelectedItem().toString());
      settings.refreshInterval = Integer.parseInt(refreshInterval.getText());
      settings.scrollSize = Integer.parseInt(scrollSize.getText());;
      if (bottomTime.getText().toLowerCase().equals("now")) {
        settings.setBottomTime(Double.NaN);
      } else {
        String t = bottomTime.getText();
        if (t.length() == 8) {
          t = t + "2359";
        }
        Date bt = utcDateFormat.parse(t);
        double tzo = swarmConfig.getTimeZone(settings.channel).getOffset(bt.getTime()) / 1000;

        settings.setBottomTime(J2kSec.fromDate(bt) - tzo);
      }
      settings.forceCenter = removeDrift.isSelected();
      settings.showClip = showClip.isSelected();
      settings.alertClip = alertClip.isSelected();
      settings.alertClipTimeout = Integer.parseInt(alertClipTimeout.getText()) * 60;

      // Calibration cb = Swarm.getParentFrame().getCalibration(channel);

      settings.clipValue = Double.parseDouble(clipValue.getText());
      settings.autoScale = autoScale.isSelected();
      settings.barRange = Double.parseDouble(barRange.getText());
      settings.notifyView();

      // If option to apply to all helicorders is selected
      if (applyToAll.isSelected()) {
        // heli config
        ConfigFile cfg = new ConfigFile();
        settings.save(cfg, "heli");
        ConfigFile hcf = cfg.getSubConfig("heli");
        // wave config
        ConfigFile wcfg = new ConfigFile();
        waveSettings.save(wcfg, "wave");
        ConfigFile wcf = wcfg.getSubConfig("wave");
        List<JInternalFrame> frames = SwarmInternalFrames.getFrames();
        for (JInternalFrame frame : frames) {
          if (frame instanceof HelicorderViewerFrame) {
            HelicorderViewerFrame hvf = (HelicorderViewerFrame) frame;
            hvf.getHelicorderViewerSettings().set(hcf);
            hvf.getHelicorderViewerSettings().notifyView();
            hvf.getWaveViewSettings().set(wcf);
            hvf.getWaveViewSettings().notifyView();
          }
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      // don't do anything here since all validation should occur in
      // allowOK() -- this is just worst case.
    }
  }

  protected boolean allowOk() {
    String message = null;
    try {
      message = "Invalid time; legal format is 'YYYYMMDD' or 'YYYYMMDDhhmm' or 'Now'.";
      // no bottom time
      if (!bottomTime.getText().toLowerCase().equals("now")) {
        Date bt = null;
        String t = bottomTime.getText();
        if (t.length() == 8) {
          t = t + "2359";
        }
        bt = utcDateFormat.parse(t);
        if (bt == null) {
          throw new ParseException(null, 0);
        }
      }

      // validate
      message = "Invalid refresh interval; legal values are between 0 and 3600, 0 for no refresh.";
      int ri = Integer.parseInt(refreshInterval.getText());
      if (ri < 0 || ri > 3600) {
        throw new NumberFormatException();
      }

      message = "Invalid scroll size; legal values are between 1 and 48.";
      int ss = Integer.parseInt(scrollSize.getText());
      if (ss < 1 || ss > 48) {
        throw new NumberFormatException();
      }

      message = "Invalid one bar range.";
      Double.parseDouble(barRange.getText());

      message = "Invalid clip value.";
      Double.parseDouble(clipValue.getText());

      return true;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(this, message, "Options Error", JOptionPane.ERROR_MESSAGE);
    }
    return false;
  }
}
