package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

/**
 * Pick menu bar on clipboard toolbar.
 * 
 * @author Diana Norgaard
 */
public class PickMenuBar extends JMenuBar {

  private static final long serialVersionUID = 8681764007165352268L;

  private WaveClipboardFrame clipboard;
  private PickSettingsDialog settingsDialog;
  private JMenu menu;
  
  /**
   * Constructor.
   */
  public PickMenuBar(WaveClipboardFrame clipboard) {
    super();
    this.menu = new JMenu("Pick Menu");
    this.add(menu);
    this.setLayout(new GridLayout(1, 1));
    this.clipboard = clipboard;
    this.settingsDialog = PickSettingsDialog.getInstance();
    this.createMenu();
  }
  
  /**
   * Create right click menu for pick.
   */
  private void createMenu() {
    createSettingsMenu();
    menu.addSeparator();
    createEventMenu();
  }
 
  /**
   * Create import/export event menu items.
   */
  private void createEventMenu() { 
    JMenuItem createEventMenu = new JMenuItem("Open Event Dialog");
    createEventMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.CTRL_MASK));
    createEventMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        openEventDialog();
      }
    });
    menu.add(createEventMenu);   
  }
    
  /**
   * Open event dialog for export to file.
   */
  private void openEventDialog() {
    EventDialog eventDialog = EventDialog.getInstance();
    eventDialog.checkForPicks();
    eventDialog.setSizeAndLocation();
    eventDialog.setVisible(true);   
    eventDialog.toFront();
    eventDialog.requestFocus();

    // if event dialog is already in frame it throws an IllegalArgumentException.
    // catch and try again.
    try {
      SwarmInternalFrames.add(eventDialog);
    } catch (IllegalArgumentException e) {
      openEventDialog();
    }

  }
  
  /**
   * Create settings menu item.
   */
  private void createSettingsMenu() {
    JMenuItem settingsMenu = new JMenuItem("Settings");
    settingsMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
    settingsMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        settingsDialog.setVisible(true);
      }
    });
    menu.add(settingsMenu);
    
    JMenuItem clearMenu = new JMenuItem("Clear All Picks");
    clearMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, ActionEvent.CTRL_MASK));
    clearMenu.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        String message = "Are you sure you want to remove all picks from clipboard?";
        message += "\nThis cannot be undone.";
        int result = JOptionPane.showConfirmDialog(Swarm.getApplicationFrame(), message,
            "Remove Picks", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
          for (WaveViewPanel wvp : clipboard.getWaves()) {
            wvp.getPickData().clearAllPicks(wvp);
            wvp.repaint();
          }
        }
      }
    });
    menu.add(clearMenu);
  }
  
}
