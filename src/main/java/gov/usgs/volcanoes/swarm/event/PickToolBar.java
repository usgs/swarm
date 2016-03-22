package gov.usgs.volcanoes.swarm.event;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JToolBar;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.SwingWorker;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.data.CachedDataSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.wave.AbstractWavePanel;
import gov.usgs.volcanoes.swarm.wave.WaveViewSettingsToolbar;

public class PickToolBar extends JToolBar implements PickBoxListener {

  private final static SwarmConfig swarmConfig = SwarmConfig.getInstance();
  private final static Component applicationFrame = Swarm.getApplicationFrame();
  
  private JButton sizeButton;
  private JButton saveButton;
  private JButton captureButton;
  private JButton histButton;

  private WaveViewSettingsToolbar waveToolbar;

  private JButton compXButton;
  private JButton expXButton;
  private JButton forwardButton;
  private JButton backButton;
  private JButton gotoButton;
  private Throbber throbber;
  private JPopupMenu popup;
  private int waveHeight;

  private PickPanel pickBox;
  private PickToolBarListener listener;

  public PickToolBar() {
    setFloatable(false);
    setRollover(true);
    setBorder(BorderFactory.createEmptyBorder(1, 0, 0, 0));
  }

  public void setPickBox(PickPanel pickBox) {
    this.pickBox = pickBox;
  }

  public void selectCountChanged(int count) {
    boolean enable = count > 0;
    histButton.setEnabled(enable);
    compXButton.setEnabled(enable);
    expXButton.setEnabled(enable);
    forwardButton.setEnabled(enable);
    backButton.setEnabled(enable);
    gotoButton.setEnabled(enable);
  }

  private void createMainButtons() {
    sizeButton =
        SwarmUtil.createToolBarButton(Icons.resize, "Set wave height", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            doSizePopup();
          }
        });
    add(sizeButton);

    addSeparator();
    captureButton = SwarmUtil.createToolBarButton(Icons.camera, "Save pick image (P)",
        new CaptureActionListener());
    UiUtils.mapKeyStrokeToButton(this, "P", "capture", captureButton);
    add(captureButton);
  }


  private void createWaveButtons() {
    addSeparator();

    backButton = SwarmUtil.createToolBarButton(Icons.left, "Scroll back time 20% (Left arrow)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            shiftTime(-0.20);
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
    add(backButton);
    backButton.setEnabled(false);

    forwardButton = SwarmUtil.createToolBarButton(Icons.right,
        "Scroll forward time 20% (Right arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            shiftTime(0.20);
          }
        });
    add(forwardButton);
    UiUtils.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);
    forwardButton.setEnabled(false);
    // gotoButton =
    // SwarmUtil.createToolBarButton(Icons.gototime, "Go to time (Ctrl-G)", new ActionListener() {
    // public void actionPerformed(final ActionEvent e) {
    // final String t = JOptionPane.showInputDialog(applicationFrame,
    // "Input time in 'YYYYMMDDhhmm[ss]' format:", "Go to Time",
    // JOptionPane.PLAIN_MESSAGE);
    // if (t != null)
    // gotoTime(t);
    // }
    // });
    // toolbar.add(gotoButton);
    // UiUtils.mapKeyStrokeToButton(this, "ctrl G", "goto", gotoButton);

    compXButton = SwarmUtil.createToolBarButton(Icons.xminus,
        "Shrink sample time 20% (Alt-left arrow, +)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            scaleTime(0.20);
          }
        });
    add(compXButton);
    UiUtils.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
    UiUtils.mapKeyStrokeToButton(this, "EQUALS", "compx2", compXButton);
    UiUtils.mapKeyStrokeToButton(this, "shift EQUALS", "compx2", compXButton);
    compXButton.setEnabled(false);

    expXButton = SwarmUtil.createToolBarButton(Icons.xplus,
        "Expand sample time 20% (Alt-right arrow, -)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            scaleTime(-0.20);
          }
        });
    add(expXButton);
    UiUtils.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
    UiUtils.mapKeyStrokeToButton(this, "MINUS", "expx", expXButton);
    expXButton.setEnabled(false);

    histButton = SwarmUtil.createToolBarButton(Icons.timeback, "Last time settings (Backspace)",
        new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            back();
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "BACK_SPACE", "back", histButton);
    add(histButton);
    addSeparator();
    histButton.setEnabled(false);
    waveToolbar = new WaveViewSettingsToolbar(null, this, listener);

    addSeparator();

    add(Box.createHorizontalGlue());

    throbber = new Throbber();
    add(throbber);

    // UiUtils.mapKeyStrokeToAction(this, "control A", "selectAll", new AbstractAction() {
    // private static final long serialVersionUID = 1L;
    //
    // public void actionPerformed(final ActionEvent e) {
    // for (final PickerWavePanel wave : waves)
    // select(wave);
    // }
    // });
  }

//  private void addHistory(final AbstractWavePanel wvp, final double[] t) {
//    Stack<double[]> history = histories.get(wvp);
//    if (history == null) {
//      history = new Stack<double[]>();
//      histories.put(wvp, history);
//    }
//    history.push(t);
//  }

//  public void gotoTime(final AbstractWavePanel wvp, String t) {
//    double j2k = Double.NaN;
//    try {
//      if (t.length() == 12)
//        t = t + "30";
//
//      j2k = J2kSec.parse("yyyyMMddHHmmss", t);
//    } catch (final Exception e) {
//      JOptionPane.showMessageDialog(applicationFrame, "Illegal time value.", "Error",
//          JOptionPane.ERROR_MESSAGE);
//    }
//
//    if (!Double.isNaN(j2k)) {
//      double dt = 60;
//      if (wvp.getWave() != null) {
//        final double st = wvp.getStartTime();
//        final double et = wvp.getEndTime();
//        final double[] ts = new double[] {st, et};
//        addHistory(wvp, ts);
//        dt = (et - st);
//      }
//
//      final double tzo =
//          swarmConfig.getTimeZone(wvp.getChannel()).getOffset(System.currentTimeMillis()) / 1000;
//
//      final double nst = j2k - tzo - dt / 2;
//      final double net = nst + dt;
//
//      fetchNewWave(wvp, nst, net);
//    }
//  }

  // TODO: This isn't right, this should be a method of waveviewpanel
  private void fetchNewWave(final AbstractWavePanel wvp, final double nst, final double net) {
    final SwingWorker worker = new SwingWorker() {
      @Override
      public Object construct() {
        throbber.increment();
        final SeismicDataSource sds = wvp.getDataSource();
        // Hacky fix for bug #84
        Wave sw = null;
        if (sds instanceof CachedDataSource)
          sw = ((CachedDataSource) sds).getBestWave(wvp.getChannel(), nst, net);
        else
          sw = sds.getWave(wvp.getChannel(), nst, net);
        wvp.setWave(sw, nst, net);
        wvp.repaint();
        return null;
      }

      @Override
      public void finished() {
        throbber.decrement();
        repaint();
      }
    };
    worker.start();
  }


  public void gotoTime(final String t) {
      listener.gotoTime(t);
  }

  public void scaleTime(final double pct) {
    pickBox.scaleTime(pct);
  }

  public void back() {
    pickBox.back();
  }

  public void shiftTime(final double pct) {
    pickBox.shiftTime(pct);
  }

  class CaptureActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      pickBox.writeImage();
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
              pickBox.setWaveHeight(size);
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
    throbber.increment();;
  }

  public void decrementThrobber() {
    throbber.decrement();;
  }

}
