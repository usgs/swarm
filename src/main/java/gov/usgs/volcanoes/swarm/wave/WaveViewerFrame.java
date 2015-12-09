package gov.usgs.volcanoes.swarm.wave;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import gov.usgs.plot.data.Wave;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.chooser.DataChooser;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;

/**
 *
 * @author Dan Cervelli
 */
public class WaveViewerFrame extends JInternalFrame implements Runnable {
  public static final long serialVersionUID = -1;

  private final long interval = 2000;
  private final static int[] SPANS = new int[] {15, 30, 60, 120, 180, 240, 300};
  private int spanIndex;
  private final SeismicDataSource dataSource;
  private final String channel;
  private final Thread updateThread;
  private boolean kill;
  private JToolBar toolBar;

  private final WaveViewSettings settings;
  private WaveViewPanel waveViewPanel;

  private JPanel mainPanel;
  private JPanel wavePanel;

  private Throbber throbber;

  public WaveViewerFrame(final SeismicDataSource sds, final String ch) {
    super(ch + ", [" + sds + "]", true, true, false, true);
    dataSource = sds;
    channel = ch;
    settings = new WaveViewSettings();
    spanIndex = 3;
    kill = false;
    updateThread = new Thread(this, "WaveViewerFrame-" + sds + "-" + ch);
    createUI();
  }

  public void createUI() {
    this.setFrameIcon(Icons.wave);
    mainPanel = new JPanel(new BorderLayout());
    waveViewPanel = new WaveViewPanel(settings);
    wavePanel = new JPanel(new BorderLayout());
    wavePanel.add(waveViewPanel, BorderLayout.CENTER);
    final Border border = BorderFactory.createCompoundBorder(
        BorderFactory.createEmptyBorder(0, 2, 3, 3), LineBorder.createGrayLineBorder());
    wavePanel.setBorder(border);

    mainPanel.add(wavePanel, BorderLayout.CENTER);

    toolBar = SwarmUtil.createToolBar();

    final JButton compXButton = SwarmUtil.createToolBarButton(Icons.xminus,
        "Shrink time axis (Alt-left arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            if (spanIndex != 0)
              spanIndex--;
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
    toolBar.add(compXButton);

    final JButton expXButton = SwarmUtil.createToolBarButton(Icons.xplus,
        "Expand time axis (Alt-right arrow)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            if (spanIndex < SPANS.length - 1)
              spanIndex++;
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
    toolBar.add(expXButton);

    toolBar.addSeparator();

    new WaveViewSettingsToolbar(settings, toolBar, this);

    final JButton clipboard = SwarmUtil.createToolBarButton(Icons.clipboard,
        "Copy wave to clipboard (C or Ctrl-C)", new ActionListener() {
          public void actionPerformed(final ActionEvent e) {
            if (waveViewPanel != null) {
              final WaveClipboardFrame cb = WaveClipboardFrame.getInstance();
              cb.setVisible(true);
              cb.addWave(new WaveViewPanel(waveViewPanel));
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "C", "clipboard1", clipboard);
    UiUtils.mapKeyStrokeToButton(this, "control C", "clipboard2", clipboard);
    toolBar.add(clipboard);

    toolBar.addSeparator();

    toolBar.add(Box.createHorizontalGlue());

    throbber = new Throbber();
    toolBar.add(throbber);

    mainPanel.add(toolBar, BorderLayout.NORTH);

    this.addInternalFrameListener(new InternalFrameAdapter() {
      @Override
      public void internalFrameActivated(final InternalFrameEvent e) {
        if (channel != null)
          DataChooser.getInstance().setNearest(channel);
      }

      @Override
      public void internalFrameClosing(final InternalFrameEvent e) {
        throbber.close();
        kill();
        SwarmInternalFrames.remove(WaveViewerFrame.this);
        dataSource.close();
      }
    });

    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.setContentPane(mainPanel);
    this.setSize(750, 280);
    this.setVisible(true);

    updateThread.start();
  }

  public void getWave() {
    throbber.increment();
    final double now = J2kSec.now();
    final Wave sw = dataSource.getWave(channel, now - SPANS[spanIndex], now);
    // System.out.println(sw);
    waveViewPanel.setWorking(true);
    waveViewPanel.setWave(sw, now - SPANS[spanIndex], now);
    waveViewPanel.setChannel(channel);
    waveViewPanel.setDataSource(dataSource);
    waveViewPanel.setWorking(false);
    waveViewPanel.repaint();
    throbber.decrement();
  }

  public void kill() {
    kill = true;
    updateThread.interrupt();
  }

  public void run() {
    while (!kill) {
      try {
        getWave();
        Thread.sleep(interval);
      } catch (final InterruptedException e) {
      }
    }
    dataSource.close();
  }
}
