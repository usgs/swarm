package gov.usgs.volcanoes.swarm.event;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.FormLayout;

import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmModalDialog;

import java.awt.BorderLayout;
import java.io.IOException;

import javax.swing.InputVerifier;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 * Pick Settings Dialog.
 * 
 * @author Diana Norgaard
 */
public class PickSettingsDialog extends SwarmModalDialog {

  private static final long serialVersionUID = 4870724789310886277L;
  private static PickSettingsDialog dialog;
  private PickSettings settings = new PickSettings();
  private JTextField[] weight;
  private static final String weightMessage = "Value must be a non-negative integer.";


  /**
   * Default constructor.
   */
  private PickSettingsDialog() {
    super(Swarm.getApplicationFrame(), "Pick Settings");
    setSizeAndLocation();
  }

  /**
   * Get instance of pick settings dialog.
   * @return pick settings dialog
   */
  public static PickSettingsDialog getInstance() {
    if (dialog == null) {
      dialog = new PickSettingsDialog();
    }
    return dialog;
  }

  /**
   * Create UI.
   */
  protected void createUi() {
    
    super.createUi();
    settings = new PickSettings();
    
    FormLayout layout = new FormLayout("center:40dlu, 10dlu, 50dlu");
    DefaultFormBuilder builder = new DefaultFormBuilder(layout).border(Borders.DIALOG);
    
    // Uncertainty weights
    builder.appendSeparator("Uncertainty");
    builder.nextLine();
    builder.append("Weight");
    builder.append("# of Samples");
    builder.nextLine();
    weight = new JTextField[settings.getNumWeight()];
    for (int i = 0; i < settings.getNumWeight(); i++) {
      String uncertaintyValue = settings.getProperty(PickSettings.WEIGHT + "." + i);
      weight[i] = new JTextField(uncertaintyValue);
      weight[i].setInputVerifier(new WeightInputVerifier());
      weight[i].setHorizontalAlignment(SwingConstants.RIGHT);
      weight[i].setToolTipText(weightMessage);
      builder.append(Integer.toString(i), weight[i], true);
    }

    mainPanel.add(builder.getPanel(), BorderLayout.CENTER);
  }
  
  public static void main(String[] args) {
    PickSettingsDialog psd = PickSettingsDialog.getInstance();
    psd.setVisible(true);
  }


  /**
   * Validate input values.
   * @see gov.usgs.volcanoes.swarm.SwarmModalDialog#allowOk()
   */
  protected boolean allowOk() {
    boolean ok = true;
    // verify weight input values
    for (int i = 0; i < settings.getNumWeight(); i++) {
      try {
        int value = Integer.valueOf(weight[i].getText()); // check to see if non-negative number
        if (value < 0) {
          ok = false;
        } else {
          weight[i].setText(Integer.toString(value)); // modify text field value to integer value
        }
      } catch (NumberFormatException e) {
        ok = false;
      }
      if (!ok) {
        JOptionPane.showMessageDialog(this, weightMessage, "Weight " + i,
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
    // set weight settings
    for (int i = 0; i < settings.getNumWeight(); i++) {
      settings.setProperty(PickSettings.WEIGHT + "." + i, weight[i].getText());
    }
    // save settings to file
    try {
      settings.save();
    } catch (IOException e) {
      if (e.getMessage().equals("")) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Error storing pick settings",
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
    // revert weight settings
    for (int i = 0; i < settings.getNumWeight(); i++) {
      String text = settings.getProperty(PickSettings.WEIGHT + "." + i);
      weight[i].setText(text);
    }
  }

  /**
   * Verify weight input.
   */
  public class WeightInputVerifier extends InputVerifier {
    /**
     * @see javax.swing.InputVerifier#verify(javax.swing.JComponent)
     */
    public boolean verify(JComponent input) {
      String text = ((JTextField) input).getText();
      try {
        int value = Integer.valueOf(text);
        if (value >= 0) {
          return true;
        } else {
          return false;
        }
      } catch (NumberFormatException e) {
        return false;
      }
    }
  }
  
  /**
   * Get pick settings.
   * @return pick settings
   */
  public PickSettings getSettings() {
    return settings;
  }
}
