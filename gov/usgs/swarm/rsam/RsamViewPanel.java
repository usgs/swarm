package gov.usgs.swarm.rsam;

import gov.usgs.math.BinSize;
import gov.usgs.math.Butterworth;
import gov.usgs.math.Butterworth.FilterType;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.GenericDataMatrix;
import gov.usgs.plot.data.RSAMData;
import gov.usgs.plot.decorate.FrameDecorator;
import gov.usgs.plot.decorate.SmartTick;
import gov.usgs.plot.render.AxisRenderer;
import gov.usgs.plot.render.HistogramRenderer;
import gov.usgs.plot.render.MatrixRenderer;
import gov.usgs.plot.render.ShapeRenderer;
import gov.usgs.plot.render.TextRenderer;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.SwarmConfig;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.time.UiTime;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import cern.colt.matrix.DoubleMatrix2D;

/**
 * A component that renders a RSAM plot.
 * 
 * 
 * @author Tom Parker
 */
public class RsamViewPanel extends JComponent {
    public static final long serialVersionUID = -1;

    private static final String DISPLAY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static SwarmConfig swarmConfig;

    /**
     * X pixel location of where the main plot axis should be located on the
     * component.
     */
    private int xOffset = 60;

    /**
     * Y pixel location of where the main plot axis should be located on the
     * component.
     */
    private int yOffset = 20;

    /** The amount of padding space on the right side. */
    private int rightWidth = 40;

    /** The amount of padding space on the bottom. */
    private int bottomHeight = 20;

    private RSAMData data;

    private double startTime;
    private double endTime;
    private RsamViewSettings settings;
    private int bias;

    private boolean timeSeries;
    private String channel;

    /**
     * The data source to use for zoom drags. This should probably be moved from
     * this class to follow a stricter interpretation of MVC.
     */
    private SeismicDataSource source;

    /**
     * A flag to indicate wheter the plot should display a title. Currently used
     * when the plot is on the clipboard or monitor.
     */
    private boolean displayTitle;

    private Color backgroundColor;
    private Color bottomBorderColor;
    private JLabel statusLabel;

    private boolean allowDragging;
    private boolean dragging;
    private double j2k1;
    private double j2k2;
    private int highlightX1;
    private int highlightX2;

    private static Image closeImg;
    private boolean allowClose;

    private EventListenerList listeners = new EventListenerList();

    /**
     * A flag that indicates whether data are being loaded for this panel.
     */
    private boolean working;

    /**
     * The wave is rendered to an image that is only updated when the settings
     * change for repaint efficiency.
     */
    private BufferedImage image;

    private double mark1 = Double.NaN;
    private double mark2 = Double.NaN;

    private double cursorMark = Double.NaN;

    private boolean useFilterLabel = true;

    private Color borderColor;

    private double[] translation;

    /**
     * Constructs a WaveViewPanel with default settings.
     */
    public RsamViewPanel() {
        this(new RsamViewSettings());
    }

    /**
     * Constructs a WaveViewPanel with specified settings.
     * 
     * @param s
     *            the settings
     */
    public RsamViewPanel(RsamViewSettings s) {
        swarmConfig = SwarmConfig.getInstance();
        settings = s;
        s.view = this;

        backgroundColor = new Color(0xf7, 0xf7, 0xf7);
        setupMouseHandler();
    }

    /**
     * Constructs a WaveViewPanel set up the same as a source WaveViewPanel.
     * Used when copying a waveform to the clipboard.
     * 
     * @param p
     *            the source WaveViewPanel
     */
    public RsamViewPanel(RsamViewPanel p) {
        swarmConfig = SwarmConfig.getInstance();
        channel = p.channel;
        source = p.source;
        startTime = p.startTime;
        endTime = p.endTime;
        bias = p.bias;
        timeSeries = p.timeSeries;
        allowDragging = p.allowDragging;
        settings = new RsamViewSettings(p.settings);
        settings.view = this;
        data = p.data;
        displayTitle = p.displayTitle;
        backgroundColor = p.backgroundColor;
        translation = new double[8];
        if (p.translation != null)
            System.arraycopy(p.translation, 0, translation, 0, 8);

        processSettings();
        setupMouseHandler();
    }

    public void setOffsets(int xo, int yo, int rw, int bh) {
        xOffset = xo;
        yOffset = yo;
        rightWidth = rw;
        bottomHeight = bh;
    }

    private void setupMouseHandler() {
        Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
        this.setCursor(crosshair);

        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                UiTime.touchTime();
                if (SwingUtilities.isRightMouseButton(e))
                    settings.cycleType();
                fireMousePressed(e);
            }
        });
    }

    public void addListener(RsamViewPanelListener listener) {
        listeners.add(RsamViewPanelListener.class, listener);
    }

    public void removeListener(RsamViewPanelListener listener) {
        listeners.remove(RsamViewPanelListener.class, listener);
    }

    public void fireZoomed(MouseEvent e, double oldST, double oldET, double newST, double newET) {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == RsamViewPanelListener.class)
                ((RsamViewPanelListener) ls[i + 1]).waveZoomed(this, oldST, oldET, newST, newET);
    }

    public void fireTimePressed(MouseEvent e, double j2k) {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == RsamViewPanelListener.class)
                ((RsamViewPanelListener) ls[i + 1]).waveTimePressed(this, e, j2k);
    }

    public void fireMousePressed(MouseEvent e) {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == RsamViewPanelListener.class)
                ((RsamViewPanelListener) ls[i + 1]).mousePressed(this, e, dragging);
    }

    public void fireClose() {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == RsamViewPanelListener.class)
                ((RsamViewPanelListener) ls[i + 1]).waveClosed(this);
    }

    public void setAllowClose(boolean b) {
        allowClose = b;
    }

    public void zoom(final double st, final double et) {
        final SwingWorker worker = new SwingWorker() {
            public Object construct() {
                RSAMData data = null;
                data = source.getRsam(channel, st, et);
                setData(data, st, et);
                return null;
            }

            public void finished() {

                repaint();
            }
        };
        worker.start();
    }

    /**
     * Set the working flag. This flag indicates whether data are being loaded
     * for this panel.
     * 
     * @param b
     *            the working flag state
     */
    public void setWorking(boolean b) {
        working = b;
    }

    /**
     * Set the allow dragging flag. This flag enables zoom dragging. Currently
     * only allowed on the clipboard, but could be implemented within the
     * helicorder view.
     * 
     * @param b
     *            the allow dragging flag state
     */
    public void setAllowDragging(boolean b) {
        allowDragging = b;
    }

    public void setStatusLabel(JLabel l) {
        statusLabel = l;
    }

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public RsamViewSettings getSettings() {
        return settings;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public RSAMData getData() {
        return data;
    }

    public RsamViewSettings getRsamViewSettings() {
        return settings;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String c) {
        channel = c;
    }

    public void setSettings(RsamViewSettings s) {
        settings = s;
        processSettings();
    }

    public SeismicDataSource getDataSource() {
        return source;
    }

    public void setDataSource(SeismicDataSource s) {
        source = s;
    }

    public void setDisplayTitle(boolean b) {
        displayTitle = b;
    }

    public void settingsChanged() {
        processSettings();
    }

    public boolean isTimeSeries() {
        return timeSeries;
    }

    
    /**
     * Gets the translation info for this panel. The translation info is used to
     * convert from pixel coordinates on the panel into time or data
     * coordinates.
     * 
     * @return the transformation information
     */
    public double[] getTranslation() {
        return translation;
    }

    /**
     * Set the background color of the panel.
     * 
     * @param c
     *            the background color
     */
    public void setBackgroundColor(Color c) {
        backgroundColor = c;
    }

    public void setBottomBorderColor(Color c) {
        bottomBorderColor = c;
    }

    public void setBorderColor(Color c) {
        borderColor = c;
    }

    public void setData(RSAMData data, double st, double et) {
        this.data = data;
        startTime = st;
        endTime = et;
        processSettings();
    }

    private synchronized void setImage(BufferedImage bi) {
        image = bi;
    }

    private synchronized BufferedImage getImage() {
        return image;
    }

    private void createImage() {
        final Runnable r = new Runnable() {
            public void run() {
                if (getWidth() > 0 && getHeight() > 0) {
                    BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics2D ig = (Graphics2D) bi.getGraphics();
                    constructPlot(ig);
                    setImage(bi);
                }
            }
        };

        if (SwingUtilities.isEventDispatchThread()) {
            SwingWorker worker = new SwingWorker() {
                public Object construct() {
                    r.run();
                    return null;
                }

                public void finished() {
                    repaint();
                }
            };
            worker.start();
        } else
            r.run();
    }

    /**
     * Does NOT call repaint for efficiency purposes, that is left to the
     * container.
     */
    private void processSettings() {
        if (data == null || data.getData() == null || data.getData().rows() == 0)
            return;

        createImage();
    }

    /**
     * Paints the component on the specified graphics context.
     * 
     * @param g
     *            the graphics context
     */
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Dimension dim = this.getSize();
        if (data == null) {
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, dim.width, dim.height);
            g2.setColor(Color.black);
            if (working)
                g2.drawString("Retrieving data...", dim.width / 2 - 50, dim.height / 2);
            else {
                String error = "No RSAM data.";
                if (channel != null)
                    error = "No RSAM data for " + channel + ".";
                int w = g2.getFontMetrics().stringWidth(error);
                g2.drawString(error, dim.width / 2 - w / 2, dim.height / 2);
            }
        } else {
            BufferedImage bi = getImage();
            if (bi != null)
                g2.drawImage(bi, 0, 0, null);

            if (dragging)
                paintDragBox(g2);

            if (!Double.isNaN(mark1))
                paintMark(g2, mark1);

            if (!Double.isNaN(mark2))
                paintMark(g2, mark2);

            if (!Double.isNaN(cursorMark))
                paintCursor(g2);
        }

        if (allowClose) {
            if (closeImg == null)
                closeImg = Icons.close_view.getImage();

            g2.drawImage(closeImg, dim.width - 17, 3, null);
        }
        if (bottomBorderColor != null) {
            g2.setColor(bottomBorderColor);
            g2.drawLine(0, dim.height - 1, dim.width, dim.height - 1);
        }
        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRect(0, 0, dim.width - 1, dim.height - 2);
        }
    }

    public void setUseFilterLabel(boolean b) {
        useFilterLabel = b;
    }

    public TextRenderer getFilterLabel() {
        return getFilterLabel(xOffset + 5, 148, TextRenderer.NONE, TextRenderer.NONE);
    }

    public TextRenderer getFilterLabel(int x, int y, int horizJustification, int vertJustification) {
        String ft = "";
        switch (settings.filter.getType()) {
        case BANDPASS:
            ft = "Band pass [" + settings.filter.getCorner1() + "-" + settings.filter.getCorner2() + " Hz]";
            break;
        case HIGHPASS:
            ft = "High pass [" + settings.filter.getCorner1() + " Hz]";
            break;
        case LOWPASS:
            ft = "Low pass [" + settings.filter.getCorner1() + " Hz]";
            break;
        }
        TextRenderer tr = new TextRenderer(x, y, ft);
        tr.horizJustification = horizJustification;
        tr.vertJustification = vertJustification;
        tr.color = Color.red;
        return tr;
    }

    /**
     * Constructs the plot on the specified graphics context.
     * 
     * @param g2
     *            the graphics context
     */
    private synchronized void constructPlot(Graphics2D g2) {
        Dimension dim = this.getSize();

        Plot plot = new Plot();
        plot.setBackgroundColor(backgroundColor);
        plot.setSize(dim);

        switch (settings.viewType) {
        case VALUES:
            plotValues(plot, data);
            break;
        case COUNTS:
            plotCounts(plot, data);
            break;
        }

        try {
            plot.render(g2);
        } catch (PlotException e) {
            e.printStackTrace();
        }
    }

    /**
     * Plots RSAM values.
     * 
     * @param data
     *            the RSAM values to plot
     */
    private void plotValues(Plot plot, RSAMData data) {
        if (data == null || data.getData() == null || data.getData().rows() == 0)
            return;
        
        GenericDataMatrix gdm = new GenericDataMatrix(data.getData().copy());

        if (settings.despike)
            gdm.despike(1, settings.despikePeriod);

        if (settings.detrend)
            gdm.detrend(1);

        if (settings.runningMedian)
            gdm.set2median(1, settings.runningMedianPeriod);

        if (settings.runningMedian)
            gdm.set2mean(1, settings.runningMeanPeriod);

//        if (settings.bandpass) {
//            GenericDataMatrix gdm   = new GenericDataMatrix(data.getData());
//            Butterworth bw = new Butterworth();
//            Double singleBand = 0.0;
//                bw.set(FilterType.BANDPASS, 4, Math.pow(filterPeriod, -1), Math.pow(settings.bandpassMax, -1), Math.pow(settings.bandpassMin, -1));
//            gdm.filter(bw, 1, true);
//
//        }

        MatrixRenderer mr = new MatrixRenderer(gdm.getData(), false);
        double max = gdm.max(1) + gdm.max(1) * .1;
        double min = gdm.min(1) - gdm.max(1) * .1;

        mr.setExtents(startTime, endTime, min, max);
        mr.setLocation(xOffset, yOffset, this.getWidth() - xOffset - rightWidth, this.getHeight() - yOffset
                - bottomHeight);
        mr.createDefaultAxis();
        mr.setXAxisToTime(8, true, true);

        mr.getAxis().setLeftLabelAsText("RSAM Values", -55, Color.BLACK);

        mr.createDefaultLineRenderers(Color.blue);
        plot.addRenderer(mr);
    }

    /**
     * Plots RSAM values.
     * 
     * @param data
     *            the RSAM values to plot
     */
    private void plotCounts(Plot plot, RSAMData data) {
        if (data == null || data.getData() == null || data.getData().rows() == 0)
            return;

        // get the relevant information for this channel
        data.countEvents(settings.eventThreshold, settings.eventRatio, settings.eventMaxLength);

        // setup the histogram renderer with this data
        HistogramRenderer hr = new HistogramRenderer(data.getCountsHistogram(settings.binSize));
        hr.setLocation(xOffset, yOffset, this.getWidth() - xOffset - rightWidth, this.getHeight() - yOffset
                - bottomHeight);
        hr.setDefaultExtents();
        hr.setMinX(startTime);
        hr.setMaxX(endTime);

        // x axis decorations
        hr.createDefaultAxis(8, 8, true, true, false, true, true, true);
        hr.setXAxisToTime(8, true, true);

        hr.getAxis().setLeftLabelAsText("Events per " + settings.binSize, -55, Color.BLACK);

        DoubleMatrix2D countsData = data.getCumulativeCounts();
        if (countsData != null && countsData.rows() > 0) {

            double cmin = countsData.get(0, 1);
            double cmax = countsData.get(countsData.rows() - 1, 1);

            MatrixRenderer mr = new MatrixRenderer(countsData, false);
            mr.setAllVisible(true);
            mr.setLocation(xOffset, yOffset, this.getWidth() - xOffset - rightWidth, this.getHeight() - yOffset
                    - bottomHeight);
            mr.setExtents(startTime, endTime, cmin, cmax + 1);
            mr.createDefaultLineRenderers(Color.RED);
            ShapeRenderer[] r = mr.getLineRenderers();
            ((ShapeRenderer) r[0]).color = Color.RED;
            ((ShapeRenderer) r[0]).stroke = new BasicStroke(2.0f);

            // create the axis for the right hand side
            AxisRenderer ar = new AxisRenderer(mr);
            ar.createRightTickLabels(SmartTick.autoTick(cmin, cmax, 8, false), null);
            mr.setAxis(ar);

            hr.addRenderer(mr);
            hr.getAxis().setRightLabelAsText("Cumulative Counts");

        }
        plot.addRenderer(hr);

    }

    /**
     * Paints the zoom drag box.
     * 
     * @param g2
     *            the graphics context
     */
    private void paintDragBox(Graphics2D g2) {
        int x1 = Math.min(highlightX1, highlightX2);
        int x2 = Math.max(highlightX1, highlightX2);
        int width = x2 - x1 + 1;
        Paint pnt = g2.getPaint();
        g2.setPaint(new Color(255, 255, 0, 128));
        g2.fillRect(x1, yOffset + 1, width, getSize().height - bottomHeight - yOffset);
        g2.setPaint(pnt);
    }

    private static final Color DARK_RED = new Color(168, 0, 0);
    private static final Color DARK_GREEN = new Color(0, 168, 0);

    public void setCursorMark(double j2k) {
        cursorMark = j2k;
        repaint();
    }

    private void paintCursor(Graphics2D g2) {
        if (Double.isNaN(cursorMark) || cursorMark < startTime || cursorMark > endTime)
            return;

        double[] t = getTranslation();
        if (t == null)
            return;
        double x = (cursorMark - t[1]) / t[0];
        g2.setColor(DARK_RED);
        g2.draw(new Line2D.Double(x, yOffset + 1, x, getHeight() - bottomHeight - 1));
    }

    private void paintMark(Graphics2D g2, double j2k) {
        if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime)
            return;

        double[] t = getTranslation();
        if (t == null)
            return;

        double x = (j2k - t[1]) / t[0];
        g2.setColor(DARK_GREEN);
        g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

        GeneralPath gp = new GeneralPath();
        gp.moveTo((float) x, yOffset);
        gp.lineTo((float) x - 5, yOffset - 7);
        gp.lineTo((float) x + 5, yOffset - 7);
        gp.closePath();
        g2.setPaint(Color.GREEN);
        g2.fill(gp);
        g2.setColor(DARK_GREEN);
        g2.draw(gp);
    }

    /**
     * Overload of Component. Always returns the developer-specified size.
     * 
     * @return the size of the component
     */
    public Dimension getPreferredSize() {
        return getSize();
    }

    /**
     * Overload of Component. Always returns the developer-specified size.
     * 
     * @return the size of the component
     */
    public Dimension getMinimumSize() {
        return getSize();
    }

    /**
     * Overload of Component. Always returns the developer-specified size.
     * 
     * @return the size of the component
     */
    public Dimension getMaximumSize() {
        return getSize();
    }

    public void setMarks(double m1, double m2) {
        mark1 = m1;
        mark2 = m2;
    }
}
