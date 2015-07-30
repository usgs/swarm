package gov.usgs.swarm.rsam;

import gov.usgs.plot.data.RSAMData;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.chooser.DataChooser;
import gov.usgs.swarm.data.RsamSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.internalFrame.SwarmInternalFrames;
import gov.usgs.swarm.rsam.RsamViewSettings.ViewType;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.Util;

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

/**
 * 
 * @author Tom Parker
 */
public class RsamViewerFrame extends JInternalFrame implements Runnable, SettingsListener {
    public static final long serialVersionUID = -1;
    private static final int H_TO_S = 60 * 60;
    private static final int D_TO_S = 24 * H_TO_S;
    private static final int W_TO_S = 7 * D_TO_S;

    private long intervalMs = 5 * 1000;
    private final static int[] SPANS_S = new int[] { 1 * H_TO_S, 12 * H_TO_S, 1 * D_TO_S, 2 * D_TO_S, 1 * W_TO_S,
            2 * W_TO_S, 4 * W_TO_S };
    private int spanIndex;
    private SeismicDataSource dataSource;
    private String channel;
    private Thread updateThread;
    private boolean run;
    private JToolBar toolBar;

    private RsamViewSettings settings;
    private RsamViewPanel viewPanel;

    private JPanel mainPanel;
    private JPanel rsamPanel;

    private Throbber throbber;

    public RsamViewerFrame(SeismicDataSource sds, String ch) {
        super(ch + ", [" + sds + "]", true, true, false, true);
        dataSource = sds;
        channel = ch;
        settings = new RsamViewSettings();
        settings.addListener(this);
        run = true;
        updateThread = new Thread(this, "RsamViewerFrame-" + sds + "-" + ch);
        createUI();
        settings.setSpanLength(2 * D_TO_S);
    }

    private void createUI() {
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

        JButton compXButton = SwarmUtil.createToolBarButton(Icons.xminus, "Shrink time axis (Alt-left arrow)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (spanIndex > 0)
                            settings.setSpanLength(SPANS_S[spanIndex - 1]);
                    }
                });
        Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compXButton);
        toolBar.add(compXButton);

        JButton expXButton = SwarmUtil.createToolBarButton(Icons.xplus, "Expand time axis (Alt-right arrow)",
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (spanIndex < SPANS_S.length - 1)
                            settings.setSpanLength(SPANS_S[spanIndex + 1]);
                    }
                });
        Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expXButton);
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
                if (channel != null)
                    DataChooser.getInstance().setNearest(channel);
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

    public synchronized void getRsam() {
        throbber.increment();
        
        viewPanel.setWorking(true);
        double now = CurrentTime.getInstance().nowJ2K();
        double st = now - settings.getSpanLength();

        int period;
        if (settings.getType() == ViewType.VALUES)
            period = settings.valuesPeriodS;
        else
            period = settings.countsPeriodS;

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

    public void run() {
        while (run) {
            try {
                getRsam();
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
            }
        }
        dataSource.close();
    }

    public void settingsChanged() {
        int i = 0;

        int spanLength = settings.getSpanLength();
        while (i < SPANS_S.length && SPANS_S[i] < spanLength)
            i++;

        spanIndex = i;
        getRsam();
    }
}