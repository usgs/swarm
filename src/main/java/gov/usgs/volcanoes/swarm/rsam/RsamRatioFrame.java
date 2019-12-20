package gov.usgs.volcanoes.swarm.rsam;

import gov.usgs.volcanoes.core.data.RSAMData;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.chooser.DataChooser;
import gov.usgs.volcanoes.swarm.data.RsamSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.rsam.RsamViewSettings.ViewType;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

public class RsamRatioFrame extends JInternalFrame implements Runnable, SettingsListener {

  private static final long serialVersionUID = 6845450797054744223L;

  private static final int H_TO_S = 60 * 60;
  private static final int D_TO_S = 24 * H_TO_S;
  private static final int W_TO_S = 7 * D_TO_S;
  private long intervalMs = 5 * 1000;
  private static final int[] SPANS_S = new int[] {1 * H_TO_S, 12 * H_TO_S, 1 * D_TO_S, 2 * D_TO_S,
      1 * W_TO_S, 2 * W_TO_S, 4 * W_TO_S};
  private JToolBar toolBar;
  private JPanel mainPanel;
  private JPanel rsamPanel;
  private RsamRatioPanel viewPanel;
  private int spanIndex;
  private Thread updateThread;
  private boolean run;
  private Throbber throbber;
  
  private RsamViewSettings settings;
  
  // channel 1
  private String channel1;
  private SeismicDataSource ds1;
  
  // channel 2
  private String channel2;
  private SeismicDataSource ds2;
  
  /**
   * Constructor.
   * 
   * @param channel1 channel of first RSAM
   * @param ds1 data source of first RSAM
   * @param channel2 channel of second RSAM
   * @param ds2 data source of second RSAM
   */
  public RsamRatioFrame(String channel1, SeismicDataSource ds1,
      String channel2, SeismicDataSource ds2) {
    super("RSAM Ratio: " + channel1 + "[" + ds1 + "]/" + channel2 + "[" + ds2 + "]", true, true,
        false, true);
    this.channel1 = channel1;
    this.ds1 = ds1;
    this.channel2 = channel2;
    this.ds2 = ds2;
    init();
    createUi();
    settings.setSpanLength(2 * D_TO_S);
  }
  
  private void init() {
    run = true;
    updateThread =
        new Thread(this, "RsamRatioFrame-" + channel1 + "-" + ds1 + "-" + channel2 + "-" + ds2);
    settings = new RsamViewSettings();
    settings.addListener(this);
  }
  
  private void createUi() {

    this.setFrameIcon(Icons.rsam_values);
    mainPanel = new JPanel(new BorderLayout());
    viewPanel = new RsamRatioPanel(settings);
    viewPanel.setChannel(channel1);
    rsamPanel = new JPanel(new BorderLayout());
    rsamPanel.add(viewPanel, BorderLayout.CENTER);

    Border border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 2, 3, 3),
        LineBorder.createGrayLineBorder());
    rsamPanel.setBorder(border);

    mainPanel.add(rsamPanel, BorderLayout.CENTER);

    toolBar = SwarmUtil.createToolBar();

    JButton compXButton = SwarmUtil.createToolBarButton(Icons.xminus,
        "Shrink time axis (Alt-left arrow)", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (spanIndex > 0) {
              settings.setSpanLength(SPANS_S[spanIndex - 1]);
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
    toolBar.add(compXButton);

    JButton expXButton = SwarmUtil.createToolBarButton(Icons.xplus,
        "Expand time axis (Alt-right arrow)", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            if (spanIndex < SPANS_S.length - 1) {
              settings.setSpanLength(SPANS_S[spanIndex + 1]);
            }
          }
        });
    UiUtils.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
    toolBar.add(expXButton);

    toolBar.addSeparator();

    new RsamViewSettingsToolbar(settings, toolBar, this);
    
    toolBar.addSeparator();

    toolBar.add(Box.createHorizontalGlue());

    throbber = new Throbber();
    toolBar.add(throbber);

    mainPanel.add(toolBar, BorderLayout.NORTH);

    this.addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameActivated(InternalFrameEvent e) {
        if (channel1 != null) {
          DataChooser.getInstance().setNearest(channel1);
        }
      }

      public void internalFrameClosing(InternalFrameEvent e) {
        throbber.close();
        pause();
        SwarmInternalFrames.remove(RsamRatioFrame.this);
        ds1.close();
        ds2.close();
      }
    });

    this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
    this.setContentPane(mainPanel);
    this.setSize(750, 280);
    this.setVisible(true);

    updateThread.start();
  }

  /**
   * Get RSAM ratio and set it in view panel.
   */
  public synchronized void getRsamRatio() {
    throbber.increment();

    viewPanel.setWorking(true);
    double now = J2kSec.now();
    double st = now - settings.getSpanLength();

    int period;
    if (settings.getType() == ViewType.VALUES) {
      period = settings.valuesPeriodS;
    } else {
      period = settings.countsPeriodS;
    }
    st -= st % period;

    double et = now;
    et += period - (et % period);
    RSAMData data1 = ((RsamSource) ds1).getRsam(channel1, st, et, period);
    RSAMData data2 = ((RsamSource) ds2).getRsam(channel2, st, et, period);
    RSAMData ratData = data1.getRatSAM(data2);
    viewPanel.setData(ratData, now - settings.getSpanLength(), now);
    viewPanel.setChannel(channel1);
    viewPanel.setWorking(false);
    viewPanel.repaint();
    throbber.decrement();
  }
  
  public void pause() {
    run = false;
    updateThread.interrupt();
  }
  
  /**
   * @see gov.usgs.volcanoes.swarm.rsam.SettingsListener#settingsChanged()
   */
  public void settingsChanged() {
    int i = 0;

    int spanLength = settings.getSpanLength();
    while (i < SPANS_S.length && SPANS_S[i] < spanLength) {
      i++;
    }

    spanIndex = i;
    getRsamRatio();
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while (run) {
      try {
        getRsamRatio();
        Thread.sleep(intervalMs);
      } catch (InterruptedException e) {
        //
      }
    }
  }

}
