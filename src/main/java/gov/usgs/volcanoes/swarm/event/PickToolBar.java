package gov.usgs.volcanoes.swarm.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;

import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.wave.WaveViewToolBar;

public class PickToolBar extends JToolBar implements PickBoxListener {
  private static final long serialVersionUID = 1L;
  private final static Logger LOGGER = LoggerFactory.getLogger(PickToolBar.class);

  private final JButton sizeButton;
  private final JButton captureButton;
  private final JButton histButton;

  private final JButton compXButton;
  private final JButton expXButton;
  private final JButton forwardButton;
  private final JButton backButton;
  private final Throbber throbber;
  private final WaveViewToolBar waveViewToolBar;
  private JPopupMenu popup;
  private int waveHeight;

  private final PickToolBarListener listener;

  public PickToolBar(PickToolBarListener listener) {
    this.listener = listener;
    setFloatable(false);
    setRollover(true);
    setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));

    sizeButton = createSizeButton();
    captureButton = createCaptureButton();
    backButton = createBackButton();
    compXButton = createCompXButton();
    expXButton = createExpXButton();
    forwardButton = createForwardButton();
    histButton = createHistButton();
    placeButtons();

    waveViewToolBar =  new WaveViewToolBar(null, this, listener);
    selectCountChanged(0);
    add(Box.createHorizontalGlue());
    throbber = new Throbber();
    add(throbber);
  }

  public void placeButtons() {
    add(sizeButton);
    addSeparator();
    add(captureButton);
    addSeparator();
    add(backButton);
    add(forwardButton);
    add(compXButton);
    add(expXButton);
    add(histButton);
    addSeparator();
  }

  public void selectCountChanged(int count) {
    LOGGER.debug("New select count {}", count);
    boolean enable = count > 0;
    histButton.setEnabled(enable);
    compXButton.setEnabled(enable);
    expXButton.setEnabled(enable);
    forwardButton.setEnabled(enable);
    backButton.setEnabled(enable);
    waveViewToolBar.setEnabled(enable);
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
          if (waveHeight == size)
            mi.setSelected(true);
          group.add(mi);
          popup.add(mi);
        } else
          popup.addSeparator();
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