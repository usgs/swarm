package gov.usgs.volcanoes.swarm.rsam;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import gov.usgs.volcanoes.core.contrib.PngEncoder;
import gov.usgs.volcanoes.core.contrib.PngEncoderB;
import gov.usgs.volcanoes.core.data.RSAMData;
import gov.usgs.volcanoes.core.time.J2kSec;
import gov.usgs.volcanoes.core.util.UiUtils;
import gov.usgs.volcanoes.swarm.Icons;
import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwarmUtil;
import gov.usgs.volcanoes.swarm.Throbber;
import gov.usgs.volcanoes.swarm.chooser.DataChooser;
import gov.usgs.volcanoes.swarm.data.RsamSource;
import gov.usgs.volcanoes.swarm.data.SeismicDataSource;
import gov.usgs.volcanoes.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.volcanoes.swarm.rsam.RsamViewSettings.ViewType;

/**
 * RSAM Viewer Frame.
 * @author Tom Parker
 */
public class RsamViewerFrame extends JInternalFrame implements Runnable, SettingsListener {
  public static final long serialVersionUID = -1;
  private static final int H_TO_S = 60 * 60;
  private static final int D_TO_S = 24 * H_TO_S;
  private static final int W_TO_S = 7 * D_TO_S;

  private long intervalMs = 5 * 1000;
  private static final int[] SPANS_S = new int[] {1 * H_TO_S, 12 * H_TO_S, 1 * D_TO_S, 2 * D_TO_S,
      1 * W_TO_S, 2 * W_TO_S, 4 * W_TO_S, 6 * W_TO_S, 8 * W_TO_S};
  private int spanIndex;
  private SeismicDataSource dataSource;
  private String channel;
  private Thread updateThread;
  private boolean run;
  private JToolBar toolBar;
  private JButton captureButton;

  private RsamViewSettings settings;
  private RsamViewPanel viewPanel;

  private JPanel mainPanel;
  private JPanel rsamPanel;

  private Throbber throbber;

  /**
   * RSAM viewer frame constructor.
   * @param sds seismic data source
   * @param ch channel
   */
  public RsamViewerFrame(SeismicDataSource sds, String ch) {
    super(ch + ", [" + sds + "]", true, true, false, true);
    dataSource = sds;
    channel = ch;
    settings = new RsamViewSettings();
    settings.addListener(this);
    run = true;
    updateThread = new Thread(this, "RsamViewerFrame-" + sds + "-" + ch);
    createUi();
    settings.setSpanLength(2 * D_TO_S);
  }

  private void createUi() {
    this.setFrameIcon(Icons.rsam_values);
    mainPanel = new JPanel(new BorderLayout());
    viewPanel = new RsamViewPanel(settings);
    viewPanel.setChannel(channel);
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
    
    JButton ratioButton = SwarmUtil.createToolBarButton(Icons.phi,
        "RSAM Ratio", new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            openRsamRatio();
          }
        });
    toolBar.add(ratioButton);

    toolBar.addSeparator();    
    
    captureButton = SwarmUtil.createToolBarButton(Icons.camera, "Save RSAM image (P)",
        new CaptureActionListener());
    UiUtils.mapKeyStrokeToButton(this, "P", "capture", captureButton);
    toolBar.add(captureButton);

    toolBar.add(Box.createHorizontalGlue());

    throbber = new Throbber();
    toolBar.add(throbber);

    mainPanel.add(toolBar, BorderLayout.NORTH);

    this.addInternalFrameListener(new InternalFrameAdapter() {
      public void internalFrameActivated(InternalFrameEvent e) {
        if (channel != null) {
          DataChooser.getInstance().setNearest(channel);
        }
      }

      public void internalFrameClosing(InternalFrameEvent e) {
        throbber.close();
        pause();
        SwarmInternalFrames.remove(RsamViewerFrame.this);
        dataSource.close();
      }
    });

    this.setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
    this.setContentPane(mainPanel);
    this.setSize(750, 280);
    this.setVisible(true);

    updateThread.start();
  }
  
  /**
   * Open RSAM Ratio Frame.
   */
  private void openRsamRatio() {
    HashMap<String, RsamViewerFrame> rvfs = new HashMap<String, RsamViewerFrame>();
    List<JInternalFrame> frames = SwarmInternalFrames.getFrames();
    for (JInternalFrame frame : frames) {
      if (frame instanceof RsamViewerFrame) {
        RsamViewerFrame rvf = (RsamViewerFrame) frame;
        if (rvf.channel != this.channel) {
          rvfs.put(rvf.channel, rvf);
        }
      }
    }

    RsamViewerFrame rvf;
    switch (rvfs.size()) {
      case 0: 
        JOptionPane.showMessageDialog(this, "No other RSAM frames are open.");
        return;
      case 1: 
        rvf = (RsamViewerFrame) rvfs.values().toArray()[0];
        break;
      default:
        String c = (String) JOptionPane.showInputDialog(this,
            "Select channel to compare", "RSAM Ratio", JOptionPane.PLAIN_MESSAGE, null,
            rvfs.keySet().toArray(), rvfs.keySet().iterator().next());
        rvf = rvfs.get(c);
    }

    RsamRatioFrame rrf = new RsamRatioFrame(this.channel, this.dataSource, 
                                            rvf.channel, rvf.dataSource);
    SwarmInternalFrames.add(rrf);
  }

  /**
   * Get RSAM data and set it in view panel.
   */
  public synchronized void getRsam() {
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
    RSAMData data = ((RsamSource) dataSource).getRsam(channel, st, et, period);
    viewPanel.setData(data, now - settings.getSpanLength(), now);
    viewPanel.setChannel(channel);
    viewPanel.setWorking(false);
    viewPanel.repaint();
    throbber.decrement();
  }

  public void pause() {
    run = false;
    updateThread.interrupt();
  }

  /**
   * @see java.lang.Runnable#run()
   */
  public void run() {
    while (run) {
      try {
        getRsam();
        Thread.sleep(intervalMs);
      } catch (InterruptedException e) {
        //
      }
    }
    dataSource.close();
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
    getRsam();
  }
  
  class CaptureActionListener implements ActionListener {
    public void actionPerformed(final ActionEvent e) {
      final JFileChooser chooser = new JFileChooser();
      final File lastPath = new File(SwarmConfig.getInstance().lastPath);
      chooser.setCurrentDirectory(lastPath);
      String filename = "rsam_" + channel.trim().replaceAll(" ", "_") + ".png";
      chooser.setSelectedFile(new File(filename));
      chooser.setDialogTitle("Save RSAM Screen Capture");
      final int result = chooser.showSaveDialog(Swarm.getApplicationFrame());
      File f = null;
      if (result == JFileChooser.APPROVE_OPTION) {
        f = chooser.getSelectedFile();

        if (f.exists()) {
          final int choice = JOptionPane.showConfirmDialog(Swarm.getApplicationFrame(),
              "File exists, overwrite?", "Confirm", JOptionPane.YES_NO_OPTION);
          if (choice != JOptionPane.YES_OPTION) {
            return;
          }
        }
        SwarmConfig.getInstance().lastPath = f.getParent();
      }
      if (f == null) {
        return;
      }

      int height = viewPanel.getHeight();
      int width = viewPanel.getWidth();
      
      final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
      final Graphics g = image.getGraphics();
      viewPanel.paint(g);
      g.translate(0, height);
      
      try {
        final PngEncoderB png = new PngEncoderB(image, false, PngEncoder.FILTER_NONE, 7);
        final FileOutputStream out = new FileOutputStream(f);
        final byte[] bytes = png.pngEncode();
        out.write(bytes);
        out.close();
      } catch (final Exception ex) {
        ex.printStackTrace();
      }
    }
  }
}
