/**
 * I waive copyright and related rights in the this work worldwide through the CC0 1.0 Universal
 * public domain dedication. https://creativecommons.org/publicdomain/zero/1.0/legalcode
 */

package gov.usgs.volcanoes.swarm.event;

import gov.usgs.volcanoes.core.quakeml.Event;
import gov.usgs.volcanoes.core.quakeml.Pick;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.wave.WaveClipboardFrame;
import gov.usgs.volcanoes.swarm.wave.WaveViewPanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewToolBar;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Toolbar used in event frame.
 * 
 * @author Tom Parker
 *
 */
public class PickToolBar extends JToolBar implements PickBoxListener {
  private static final long serialVersionUID = 1L;
  private static final Logger LOGGER = LoggerFactory.getLogger(PickToolBar.class);

  private final JButton sizeButton;
  private final JButton sortButton;
  private final JButton captureButton;
  private final JButton clipboardButton;
  private final JButton histButton;

  private final JButton compXButton;
  private final JButton expXButton;
  private final JButton forwardButton;
  private final JButton backButton;
  private final Throbber throbber;
  private final WaveViewToolBar waveViewToolBar;
  private JPopupMenu popup;
  private int waveHeight;
  private Event event;

  private final PickToolBarListener listener;

  /**
   * Constructor.
   * @param listener Pick tool bar listener
   */
  public PickToolBar(PickToolBarListener listener, Event event) {
    this.listener = listener;
    this.event = event;
    setFloatable(false);
    setRollover(true);
    setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

    sizeButton = createSizeButton();
    sortButton = createSortButton();
    captureButton = createCaptureButton();
    clipboardButton = createClipboardButton();
    backButton = createBackButton();
    compXButton = createCompXButton();
    expXButton = createExpXButton();
    forwardButton = createForwardButton();
    histButton = createHistButton();
    placeButtons();

    waveViewToolBar = new WaveViewToolBar(null, this, listener);
    selectCountChanged(0);
    add(Box.createHorizontalGlue());
    throbber = new Throbber();
    add(throbber);
  }

  /**
   * Add buttons to tool bar.
   */
  public void placeButtons() {
    add(sizeButton);
    add(sortButton);
    addSeparator();
    add(clipboardButton);
    add(captureButton);
    addSeparator();
    add(backButton);
    add(forwardButton);
    add(compXButton);
    add(expXButton);
    add(histButton);
    addSeparator();
  }

  /** 
   * @see gov.usgs.volcanoes.swarm.event.PickBoxListener#selectCountChanged(int)
   */
  public void selectCountChanged(int count) {
    LOGGER.debug("New select count {}", count);
    boolean enable = count > 0;
    histButton.setEnabled(enable);
    compXButton.setEnabled(enable);
    expXButton.setEnabled(enable);
    forwardButton.setEnabled(enable);
    backButton.setEnabled(enable);
    waveViewToolBar.setEnabled(enable);

    sortButton.setEnabled(count == 1);
  }

  private JButton createSizeButton() {
    JButton sizeButton =
        SwarmUtil.createToolBarButton(Icons.resize, "Set wave height", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            doSizePopup();
          }
        });

    return sizeButton;
  }

  private JButton createCaptureButton() {
    JButton captureButton = SwarmUtil.createToolBarButton(Icons.camera, "Save pick image (P)",
        new CaptureActionListener());
    listener.mapKeyStroke("P", "capture", captureButton);

    return captureButton;
  }

  private JButton createClipboardButton() {
    JButton clipboardButton = SwarmUtil.createToolBarButton(Icons.clipboard,
        "Send picks to clipboard", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            //WaveClipboardFrame.getInstance().importEvent(event);
            
            // ask if user wants to clear clipboard first
            WaveClipboardFrame clipboard = WaveClipboardFrame.getInstance();
            int result = JOptionPane.showConfirmDialog(Swarm.getApplicationFrame(),
                "Clear clipboard first?", "Clear clipboard", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
              clipboard.removeWaves();
            }
            
            // update event dialog 
            EventDialog.getInstance().setEventDetails(event);
            
            // Add panels to clipbard
            
            PickBox pickBox = (PickBox)listener;
            List<PickWavePanel> panels = pickBox.getPanels();
            for (WaveViewPanel wvp : panels) {
              clipboard.addWave(new WaveViewPanel(wvp));
            }

            for (WaveViewPanel wvp : clipboard.getWaves()) {
              wvp.getSettings().pickEnabled = true;
              PickMenu pickMenu = wvp.getPickMenu();
              for (String phase : new String[] {PickMenu.P, PickMenu.S}) {
                Pick pick = pickMenu.getPick(phase);
                if (pick != null && pickMenu.isPickChannel(phase)) {
                  pickMenu.propagatePick(phase, pick);
                }
              }
            }
            
            clipboard.getPickButton().setSelected(true);
            clipboard.getPickMenuBar().setVisible(true);
            clipboard.setVisible(true);
          }
        });
    return clipboardButton;
  }

  private JButton createSortButton() {
    JButton sortButton = SwarmUtil.createToolBarButton(Icons.geosort,
        "Sort waves by nearest to selected wave", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            listener.sortChannelsByNearest();
          }
        });

    return sortButton;
  }

  private JButton createBackButton() {
    JButton backButton = SwarmUtil.createToolBarButton(Icons.left,
        "Scroll back time 20% (Left arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            shiftTime(-0.20);
          }
        });
    listener.mapKeyStroke("LEFT", "backward1", backButton);

    return backButton;
  }

  private JButton createForwardButton() {
    JButton forwardButton = SwarmUtil.createToolBarButton(Icons.right,
        "Scroll forward time 20% (Right arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            shiftTime(0.20);
          }
        });
    listener.mapKeyStroke("RIGHT", "forward1", forwardButton);

    return forwardButton;
  }

  private JButton createCompXButton() {
    JButton compXButton = SwarmUtil.createToolBarButton(Icons.xminus,
        "Shrink sample time 20% (Alt-left arrow, +)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            scaleTime(0.20);
          }
        });
    listener.mapKeyStroke("alt LEFT", "compx", compXButton);
    listener.mapKeyStroke("EQUALS", "compx2", compXButton);
    listener.mapKeyStroke("shift EQUALS", "compx2", compXButton);

    return compXButton;
  }

  private JButton createExpXButton() {
    JButton expXButton = SwarmUtil.createToolBarButton(Icons.xplus,
        "Expand sample time 20% (Alt-right arrow, -)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            scaleTime(-0.20);
          }
        });
    listener.mapKeyStroke("alt RIGHT", "expx", expXButton);
    listener.mapKeyStroke("MINUS", "expx", expXButton);

    return expXButton;
  }

  private JButton createHistButton() {
    JButton histButton = SwarmUtil.createToolBarButton(Icons.timeback,
        "Last time settings (Backspace)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            back();
          }
        });
    listener.mapKeyStroke("BACK_SPACE", "back", histButton);

    return histButton;
  }

  public void scaleTime(final double pct) {
    listener.scaleTime(pct);
  }

  public void back() {
    listener.back();
  }

  public void shiftTime(final double pct) {
    listener.shiftTime(pct);
  }

  class CaptureActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      listener.writeImage();
    }
  }

  private void doSizePopup() {
    if (popup == null) {
      final String[] labels = new String[] {"Auto", null, "Tiny", "Small", "Medium", "Large"};
      final int[] sizes = new int[] {-1, -1, 50, 100, 160, 230};
      popup = new JPopupMenu();
      final ButtonGroup group = new ButtonGroup();
      for (int i = 0; i < labels.length; i++) {
        if (labels[i] != null) {
          final int size = sizes[i];
          final JRadioButtonMenuItem mi = new JRadioButtonMenuItem(labels[i]);
          mi.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
              listener.setWaveHeight(size);
            }
          });
          if (waveHeight == size) {
            mi.setSelected(true);
          }
          group.add(mi);
          popup.add(mi);
        } else {
          popup.addSeparator();
        }
      }
    }
    popup.show(sizeButton.getParent(), sizeButton.getX(), sizeButton.getY());
  }

  public void incrementThrobber() {
    throbber.increment();
  }

  public void decrementThrobber() {
    throbber.decrement();
  }
}
