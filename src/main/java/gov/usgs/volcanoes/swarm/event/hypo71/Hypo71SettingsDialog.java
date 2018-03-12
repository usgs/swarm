package gov.usgs.volcanoes.swarm.event.hypo71;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.ButtonGroup;
import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Hypo71 Test Variable Settings Dialog.
 * 
 * @author Diana Norgaard
 */
public class Hypo71SettingsDialog extends SwarmModalDialog {

  private static final long serialVersionUID = 2107880206447383082L;
  private static Hypo71SettingsDialog dialog;
  private Hypo71Settings settings = new Hypo71Settings();
  private JRadioButton ksing0Button;
  private JRadioButton ksing1Button;
  private JTextField[] test;
  private static final String testMessage = "Value must be a numeric.";

  /**
   * Default constructor.
   */
  private Hypo71SettingsDialog() {
    super(Swarm.getApplicationFrame(), "Hypo71 Settings");
    setSizeAndLocation();
  }

  /**
   * Get instance of pick settings dialog.
   * @return pick settings dialog
   */
  public static Hypo71SettingsDialog getInstance() {
    if (dialog == null) {
      dialog = new Hypo71SettingsDialog();
    }
    return dialog;
  }

  /**
   * Create UI.
   */
  protected void createUi() {
    
    super.createUi();
    settings = new Hypo71Settings();
    
    FormLayout layout = new FormLayout("center:50dlu, 10dlu, 50dlu, 50dlu");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);
    
    // KSING
    builder.appendSeparator("KSING");
    ksing0Button = new JRadioButton("Use original SINGLE subroutine. (KSING=0)");
    ksing0Button.setToolTipText("Locate in-network earthquakes only.");
    ButtonGroup ksingGroup = new ButtonGroup();
    ksingGroup.add(ksing0Button);
    builder.append(ksing0Button, 4);
    builder.nextLine();
    ksing1Button = new JRadioButton("Use modified SINGLE subroutine. (KSING=1)");
    ksing1Button.setToolTipText(
        "Locate earthquakes outside the network using extended distance weighting.");
    ksingGroup.add(ksing1Button);
    builder.append(ksing1Button, 4);
    builder.nextLine();
    if (settings.getKsing() == 0) {
      ksing0Button.setSelected(true);
    } else {
      ksing1Button.setSelected(true);      
    }
    
    // Test variables
    builder.appendSeparator("Test Variables");
    test = new JTextField[Hypo71Settings.testDefault.length];
    for (int i = 1; i <= test.length; i++) {
      String propertyName = String.format(Hypo71Settings.TEST + "%02d", i);
      String testValue = settings.getProperty(propertyName);
      test[i - 1] = new JTextField(testValue);
      test[i - 1].setInputVerifier(new TestVariableVerifier());
      test[i - 1].setHorizontalAlignment(SwingConstants.RIGHT);
      test[i - 1].setToolTipText(testMessage);
      String labelName = String.format(Hypo71Settings.TEST + " %02d", i);
      builder.append(labelName, test[i - 1], true);
    }

    mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
  }

  /**
   * Validate input values.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#allowOk()
   */
  protected boolean allowOk() {
    boolean ok = true;
    // verify test variable values
    for (int i = 0; i < Hypo71Settings.testDefault.length; i++) {
      try {
        double value = Double.valueOf(test[i].getText()); 
      } catch (NumberFormatException e) {
        ok = false;
      }
      if (!ok) {
        JOptionPane.showMessageDialog(this, testMessage, Hypo71Settings.TEST + i,
            JOptionPane.ERROR_MESSAGE);
        return false;
      }
    }
    return true;
  }
  
  /**
   * Save settings if values validated.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#wasOk()
   */
  protected void wasOk() {
    saveSettings();    
  }

  /**
   * Set settings and save to file.
   */
  private void saveSettings() {
    
    // KSING
    if (ksing0Button.isSelected()) {
      settings.setProperty(Hypo71Settings.KSING, "0");
    } else {
      settings.setProperty(Hypo71Settings.KSING, "1");
    }
    
    // Test variables
    for (int i = 1; i <= test.length; i++) {
      String propertyName = String.format(Hypo71Settings.TEST + "%02d", i);
      settings.setProperty(propertyName, test[i - 1].getText());
    }
    
    // save settings to file
    try {
      settings.save();
    } catch (IOException e) {
      if (e.getMessage().equals("")) {
        JOptionPane.showMessageDialog(this, e.getMessage(),
            "Error storing Hypo71 settings",
            JOptionPane.ERROR_MESSAGE);
      } else {
        e.printStackTrace();
      }
    } 
  }
  
  /**
   * Revert settings.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#wasCancelled()
   */
  protected void wasCancelled() {
    String ksing = settings.getProperty(Hypo71Settings.KSING);
    if (ksing.equals("0")) {
      ksing0Button.setSelected(true);
    } else {
      ksing1Button.setSelected(true);
    }
    for (int i = 1; i <= Hypo71Settings.testDefault.length; i++) {
      String propertyName = String.format(Hypo71Settings.TEST + "%02d", i);
      String text = settings.getProperty(propertyName);
      test[i - 1].setText(text);
    }
  }

  /**
   * Verify input.
   */
  public class TestVariableVerifier extends InputVerifier {
    /**
     * @see javax.swing.InputVerifier#verify(javax.swing.JComponent)
     */
    public boolean verify(JComponent input) {
      String text = ((JTextField) input).getText();
      try {
        double value = Double.valueOf(text);
        return true;
      } catch (NumberFormatException e) {
        return false;
      }
    }
  }
  
  /**
   * Get settings.
   * @return settings
   */
  public Hypo71Settings getSettings() {
    return settings;
  }
}
