package gov.usgs.swarm.wave;

import gov.usgs.math.Filter;
import gov.usgs.plot.FrameDecorator;
import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.TextRenderer;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Metadata;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmMenu;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.calculation.AzimuthCalculator;
import gov.usgs.swarm.data.CachedDataSource;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.database.model.Attempt;
import gov.usgs.swarm.database.model.Marker;
import gov.usgs.swarm.wave.WaveViewSettings.ViewType;
import gov.usgs.util.Time;
import gov.usgs.util.Util;
import gov.usgs.vdx.data.wave.SeisanChannel.SimpleChannel;
import gov.usgs.vdx.data.wave.SliceWave;
import gov.usgs.vdx.data.wave.Wave;
import gov.usgs.vdx.data.wave.plot.SliceWaveRenderer;
import gov.usgs.vdx.data.wave.plot.SpectraRenderer;
import gov.usgs.vdx.data.wave.plot.SpectrogramRenderer;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TimeZone;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;


/**
 * A component that renders a wave in either a standard wave view, a frequency spectra, or spectrogram. Relies heavily
 * on the Valve plotting package.
 * 
 * TODO: move filter method
 * 
 * 
 * @author Dan Cervelli
 * @author Jamil Shehzad
 * @author Joel Shellman
 */
public class WaveViewPanel extends JComponent {
    public static final long serialVersionUID = -1;

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * X pixel location of where the main plot axis should be located on the component.
     */
    private int xOffset = 60;

    /**
     * Y pixel location of where the main plot axis should be located on the component.
     */
    private int yOffset = 20;

    /** The amount of padding space on the right side. */
    private int rightWidth = 20;

    /** The amount of padding space on the bottom. */
    private int bottomHeight = 20;

    private FrameDecorator decorator;
    private SliceWaveRenderer waveRenderer;
    private SpectrogramRenderer spectrogramRenderer;
    private SpectraRenderer spectraRenderer;

    public SliceWaveRenderer getWaveRenderer() {
        return waveRenderer;
    }

    private Wave wave;

    private double startTime;
    private double endTime;
    private WaveViewSettings settings;
    private int bias;

    private double minAmp = 1E300;
    private double maxAmp = -1E300;
    private double maxSpectraPower = -1E300;
    private double maxSpectrogramPower = -1E300;
    private double[] translation;

    private boolean timeSeries;
    private SimpleChannel channel;
    private String fileName;
    private String filePath;
    private String fileType;
    private int fileIndex;
    public boolean paintnow = false;
    private double lineX;
    private int plotHeight;

    private ArrayList<Marker> markers = new ArrayList<Marker>();
    private Marker selectedMarker;

    /**
     * The data source to use for zoom drags. This should probably be moved from this class to follow a stricter
     * interpretation of MVC.
     */
    private SeismicDataSource source;

    /**
     * A flag to indicate wheter the plot should display a title. Currently used when the plot is on the clipboard or
     * monitor.
     */
    private boolean displayTitle;

    private Color backgroundColor;
    private Color bottomBorderColor;
    private JLabel statusLabel;

    private boolean allowDragging;
    private boolean dragging;
    private boolean draggingMarker;
    private double j2k1;
    private double j2k2;
    private int highlightX1;
    private int highlightX2;

    private static Image closeImg;
    private boolean allowClose;

    private EventListenerList listeners = new EventListenerList();

    public EventListenerList getListerners() {
        return listeners;
    }

    /**
     * A flag that indicates whether data are being loaded for this panel.
     */
    private boolean working;

    /**
     * The wave is rendered to an image that is only updated when the settings change for repaint efficiency.
     */
    private BufferedImage image;

    private double mark1 = Double.NaN;
    private double mark2 = Double.NaN;

    private double cursorMark = Double.NaN;

    private boolean useFilterLabel = true;

    private Color borderColor;

    /**
     * Constructs a WaveViewPanel with default settings.
     */
    public WaveViewPanel() {
        this(new WaveViewSettings());
    }

    /**
     * Constructs a WaveViewPanel with specified settings.
     * 
     * @param s
     *            the settings
     */
    public WaveViewPanel(WaveViewSettings s) {
        settings = s;
        s.view = this;

        backgroundColor = new Color(0xf7, 0xf7, 0xf7);
        setupMouseHandler();
    }

    /**
     * Constructs a WaveViewPanel set up the same as a source WaveViewPanel. Used when copying a waveform to the
     * clipboard.
     * 
     * @param p
     *            the source WaveViewPanel
     */
    public WaveViewPanel(WaveViewPanel p) {
        channel = p.channel;
        source = p.source;
        startTime = p.startTime;
        endTime = p.endTime;
        bias = p.bias;
        maxSpectraPower = p.maxSpectraPower;
        maxSpectrogramPower = p.maxSpectrogramPower;
        translation = new double[8];
        if (p.translation != null) System.arraycopy(p.translation, 0, translation, 0, 8);
        timeSeries = p.timeSeries;
        allowDragging = p.allowDragging;
        settings = new WaveViewSettings(p.settings);
        settings.view = this;
        wave = p.wave;
        displayTitle = p.displayTitle;
        backgroundColor = p.backgroundColor;
        fileName = p.fileName;
        filePath = p.filePath;
        fileType = p.fileType;
        fileIndex = p.fileIndex;
        setupMouseHandler();
        processSettings();
    }

    public void setStationInfo(String code, String comp, String network, String lastComponent) {
        channel = new SimpleChannel(null, network, code, comp + lastComponent);
    }

    public String getStationCode() {
        return channel != null ? channel.stationCode : null;
    }

    public String getNetwork() {
        return channel != null ? channel.networkName : null;
    }

    public String getFirstComp() {
        return channel != null ? channel.firstTwoComponentCode : null;
    }

    public String getFileName() {
        return fileName;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setFileIndex(int fileIndex) {
        this.fileIndex = fileIndex;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setOffsets(int xo, int yo, int rw, int bh) {
        xOffset = xo;
        yOffset = yo;
        rightWidth = rw;
        bottomHeight = bh;
    }

    public void addListener(WaveViewPanelListener listener) {
        listeners.add(WaveViewPanelListener.class, listener);
    }

    public void removeListener(WaveViewPanelListener listener) {
        listeners.remove(WaveViewPanelListener.class, listener);
    }

    public void fireZoomed(MouseEvent e, double oldST, double oldET, double newST, double newET) {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == WaveViewPanelListener.class)
                ((WaveViewPanelListener)ls[i + 1]).waveZoomed(this, oldST, oldET, newST, newET);
    }

    public void fireTimePressed(MouseEvent e, double j2k) {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == WaveViewPanelListener.class) ((WaveViewPanelListener)ls[i + 1]).waveTimePressed(this, e, j2k);
    }

    public void fireMousePressed(MouseEvent e) {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == WaveViewPanelListener.class)
                ((WaveViewPanelListener)ls[i + 1]).mousePressed(this, e, dragging);
    }

    public void fireClose() {
        Object[] ls = listeners.getListenerList();
        for (int i = ls.length - 2; i >= 0; i -= 2)
            if (ls[i] == WaveViewPanelListener.class) ((WaveViewPanelListener)ls[i + 1]).waveClosed(this);
    }

    public void setAllowClose(boolean b) {
        allowClose = b;
    }

    // added code
    public void drawMarker(Graphics g, double xPoint) {
        super.paintComponent(g);
        double[] t = getTranslation();
        new Color(0);
        g.setColor(Color.green);
        if (t != null) {
            int x = (int)((xPoint - t[1]) / t[0]);
            g.drawLine(x, plotHeight, x, -plotHeight);
        }
    }

    /**
     * Draw all markers that have been placed on this WavePanel
     * 
     * @param g
     *            : {@link Graphics} Object
     */
    public void paintMarker(Graphics g) {
        super.paintComponent(g);
        Attempt selectedAttempt = Swarm.getSelectedAttempt();
        if (null != Swarm.allMarkers) {
            Swarm.allMarkers.clear();
        }
        if (selectedAttempt != null) {
			Swarm.allMarkers = Marker.listByAttempt(selectedAttempt.getId());
	        double[] t = getTranslation();
	        Collection<Marker> markerCollection = Swarm.allMarkers;
	        for (Marker marker : markerCollection) {
	            if (t != null && this.getStationCode().equals(marker.getStation())) {
	                double j2k = Util.dateToJ2K(marker.getMarkerTime());
	                int x = (int)((j2k - t[1]) / t[0]);
	
	                if (marker.getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)) {
	                    g.setColor(Marker.P_MARKER_COLOR);
	                } else if (marker.getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)) {
	                    g.setColor(Marker.S_MARKER_COLOR);
	                } else if (marker.getMarkerType().equalsIgnoreCase(Marker.CODA_MARKER_LABEL)) {
	                    g.setColor(Marker.CODA_MARKER_COLOR);
	                } else if (marker.getMarkerType().equalsIgnoreCase(Marker.AZIMUTH_MARKER_LABEL)) {
	                    g.setColor(Marker.AZIMUTH_MARKER_COLOR);
	                } else if (marker.getMarkerType().equalsIgnoreCase(Marker.PARTICLE_MARKER_LABEL)) {
	                    g.setColor(Marker.PARTICLE_MARKER_COLOR);
	                }
	                g.drawLine(x, plotHeight, x, -plotHeight);
	                paintMarkerLabel(g, marker, x);
	            }
	        }
        }
    }

    /**
     * Draw marker label next to marker line for easy identification of markers as well as making dragging of markers
     * easy also
     * 
     * 
     * @param g
     *            : {@link Graphics} object
     * @param marker
     *            : {@link Marker} object
     * @param x
     *            : x-Position Marker label should be drawn
     */
    private void paintMarkerLabel(Graphics g, Marker marker, int x) {
        // setting marker label and background color
        String information = "";
        Color backgroundColor = null;
        if (marker.getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)) {
            information = marker.getIs_es();
            backgroundColor = Marker.S_MARKER_COLOR;
        } else if (marker.getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)) {
            information = marker.getIp_ep();
            backgroundColor = Marker.P_MARKER_COLOR;
        } else if (marker.getMarkerType().equalsIgnoreCase(Marker.CODA_MARKER_LABEL)) {
            information = "C";
            backgroundColor = Marker.CODA_MARKER_COLOR;
        } else if (marker.getMarkerType().equalsIgnoreCase(Marker.AZIMUTH_MARKER_LABEL)) {
            information = "A";
            backgroundColor = Marker.AZIMUTH_MARKER_COLOR;
        } else if (marker.getMarkerType().equalsIgnoreCase(Marker.PARTICLE_MARKER_LABEL)) {
            information = "P";
            backgroundColor = Marker.PARTICLE_MARKER_COLOR;
        }

        int dim = 15;
        // initialise rectangle.
        g.setColor(backgroundColor);
        g.fillRect(x, 9, dim, dim);

        Font font = g.getFont();
        Rectangle2D fontRec = font.getStringBounds(information, g.getFontMetrics().getFontRenderContext());

        while (fontRec.getWidth() >= dim * 0.95f || fontRec.getHeight() >= dim * 0.95f) {
            Font smallerFont = font.deriveFont((float)(font.getSize() - 2));
            font = smallerFont;
            g.setFont(smallerFont);
            fontRec = smallerFont.getStringBounds(information, g.getFontMetrics().getFontRenderContext());
        }

        new Color(0);
        // center string and draw text
        g.setColor(Color.white);
        FontMetrics fm = g.getFontMetrics();
        float stringWidth = fm.stringWidth(information);
        int fontX = (int)(x + dim / 2 - stringWidth / 2);
        int fontY = (int)(9 + dim / 2);
        g.drawString(information, fontX, fontY);

    }

    /**
     * Gets A Marker at the specified position on the WavePanel. Returns null if none exist. Since markers are
     * distinguished by their RGB values on the WavePanel, The RGB value at the specified location is used to check if a
     * marker exist at that point
     * 
     * 
     * @param x
     *            : X-position
     * @param y
     *            : Y-position
     * @return
     */
    private Marker getMarkerAtPosition(int x, int y) {
        double[] t = getTranslation();
        double j2k = x * t[0] + t[1];
        // Timestamp time = new Timestamp((long)j2k);
        Timestamp time = new Timestamp(Util.j2KToDate(j2k).getTime());

        BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        paint(bi.getGraphics());

        int rgb = bi.getRGB(x, y);
        Color c = new Color(rgb);
        if (c.equals(Marker.P_MARKER_COLOR)) {
            return this.getMarkerByType(Marker.P_MARKER_LABEL);
        } else if (c.equals(Marker.S_MARKER_COLOR)) {
            return this.getMarkerByType(Marker.S_MARKER_LABEL);
        } else if (c.equals(Marker.CODA_MARKER_COLOR)) {
            return this.getMarkerByType(Marker.CODA_MARKER_LABEL);
        } else if (c.equals(Marker.AZIMUTH_MARKER_COLOR)) {
            ArrayList<Marker> markers = getMarkersByType(Marker.AZIMUTH_MARKER_LABEL);
            if (markers.size() == 2) {
                long diff1 = Math.abs(markers.get(0).getMarkerTime().getTime() - time.getTime());
                long diff2 = Math.abs(markers.get(1).getMarkerTime().getTime() - time.getTime());
                return diff1 < diff2 ? markers.get(0) : markers.get(1);
            } else if (markers.size() == 1) {
                return markers.get(0);
            } else {
                return null;
            }
        } else if (c.equals(Marker.PARTICLE_MARKER_COLOR)) {
            ArrayList<Marker> markers = getMarkersByType(Marker.PARTICLE_MARKER_LABEL);
            if (markers.size() == 2) {
                long diff1 = Math.abs(markers.get(0).getMarkerTime().getTime() - time.getTime());
                long diff2 = Math.abs(markers.get(1).getMarkerTime().getTime() - time.getTime());
                return diff1 < diff2 ? markers.get(0) : markers.get(1);
            } else if (markers.size() == 1) {
                return markers.get(0);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    /**
     * Gets the time boundaries for only markers that are placed twiced on this wave panel. Such markers include Azimuth
     * and Particle Motion markers.
     * 
     * 
     * @param markerType
     *            : Specified Marker types
     * @return
     */
    public double[] getMarkerTimeBoundaries(String markerType) {
        ArrayList<Marker> markers = getMarkersByType(markerType);
        if (markers != null && markers.size() == 2) {
            Marker marker1 = markers.get(0);
            Marker marker2 = markers.get(1);

            double marker1Time = Util.dateToJ2K(marker1.getMarkerTime());
            double marker2Time = Util.dateToJ2K(marker2.getMarkerTime());

            double t1 = marker1Time > marker2Time ? marker2Time : marker1Time;
            double t2 = marker1Time < marker2Time ? marker2Time : marker1Time;

            double[] t = { t1, t2 };
            return t;
        } else {
            return null;
        }
    }

    public static String roundTo(double value, int ths) {
        return Float.toString((float)Math.round(value * ths) / ths);
    }

    public void setEventCalculations() {
        if (SwarmMenu.DataRecordState()) {
            if (channel.stationCode != null && (!channel.stationCode.isEmpty())) {

                SwarmMenu.getDataRecord().getEventCalculationPanel().clearUIFields();
                List<Marker> origMarkers = new ArrayList<Marker>();
                origMarkers.addAll(markers);
                applyConstraints1(channel.stationCode, selectedMarker.getMarkerType(), WaveViewPanel.this);
                Marker sMarker = getMarkerByTypes(Marker.S_MARKER_LABEL);
                Marker pMarker = getMarkerByTypes(Marker.P_MARKER_LABEL);
                Marker codaMarker = getMarkerByTypes(Marker.CODA_MARKER_LABEL);
                markers.clear();
                markers.addAll(origMarkers);

                int stationCount =
                        Swarm.getApplication().getWaveClipboard().getStationComponentCount(channel.stationCode);

                SwarmMenu.getDataRecord().getEventCalculationPanel().setStationValue(channel.stationCode);

                if (sMarker != null && pMarker != null) {
                    long sMarkerTime = sMarker.getMarkerTime().getTime();
                    long pMarkerTime = pMarker.getMarkerTime().getTime();
                    long timeDiffFromSToP = Math.abs(sMarkerTime - pMarkerTime);
                    double timeDiffFromSToPInSec = (double)timeDiffFromSToP / 1000;
                    SwarmMenu.getDataRecord().getEventCalculationPanel()
                            .setPToSTimeValue(roundTo(timeDiffFromSToPInSec, 100) + " s");

                    double ansv = Swarm.config.ansv;
                    if (ansv != 0 && ansv != Double.NaN) {
                        /*double distanceFromPToS = 1.366 * ansv
                                * timeDiffFromSToPInSec;*/
                        double distanceFromPToS = Swarm.config.ansv * timeDiffFromSToPInSec;
                        SwarmMenu.getDataRecord().getEventCalculationPanel()
                                .setPToSDistanceValue(roundTo(distanceFromPToS, 100) + " km");
                    }
                }
                if (stationCount == 3) {
                    double[] markerBoundaries = getMarkerTimeBoundaries(Marker.AZIMUTH_MARKER_LABEL);
                    if (markerBoundaries != null) {
                        Wave[] waves =
                                Swarm.getApplication()
                                        .getWaveClipboard()
                                        .getWaveDataSectionFromStationComponents(channel.stationCode,
                                                markerBoundaries[0], markerBoundaries[1]);

                        if (waves != null) {
                            AzimuthCalculator azimuthCalculator = new AzimuthCalculator();
                            double[][] dataMatrix = Swarm.getApplication().getWaveClipboard().generateDataMatrix(waves);

                            double azimuth =
                                    azimuthCalculator.calculate(dataMatrix, 0, dataMatrix[0].length,
                                            Swarm.config.azimuthPvel);
                            // Azimuth: integer, velocity: tenths, coherence: tenths
                            SwarmMenu
                                    .getDataRecord()
                                    .getEventCalculationPanel()
                                    .setAzimuthValue(
                                            Integer.toString((int)Math.round(azimuth)) + ", velocity: " +
                                                    roundTo(azimuthCalculator.getVelocity(), 10) + ", coherence: " +
                                                    roundTo(azimuthCalculator.getCoherence(), 10));
                        } else {
                            System.out.println("Cannot get waves for azimuth");
                        }
                    }
                }

                if (codaMarker != null && pMarker != null) {
                    long codaMarkerTime = codaMarker.getMarkerTime().getTime();
                    long pMarkerTime = pMarker.getMarkerTime().getTime();
                    long timeDiffFromCodaToP = Math.abs(codaMarkerTime - pMarkerTime);
                    long timeDiffFromCodaToPInSec = timeDiffFromCodaToP / 1000;
                    SwarmMenu.getDataRecord().getEventCalculationPanel()
                            .setCodaValue(Long.toString(timeDiffFromCodaToPInSec) + " s");

                    double durationMagnitude = Swarm.config.getDurationMagnitude(timeDiffFromCodaToPInSec);

                    SwarmMenu.getDataRecord().getEventCalculationPanel()
                            .setDurationMagnitudeValue(Double.toString(durationMagnitude));
                }

            }
        }
    }

    private void setupMouseHandler() {
        Cursor crosshair = new Cursor(Cursor.CROSSHAIR_CURSOR);
        this.setCursor(crosshair);
        this.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Swarm.getApplication().touchUITime();

                double[] t = getTranslation();
                Dimension size = getSize();
                if (wave != null && t != null && e.getY() > yOffset && e.getY() < (size.height - bottomHeight) &&
                    e.getX() > xOffset && e.getX() < size.width - rightWidth) {

                    String markerType = Swarm.getApplication().getWaveClipboard().getSelectedMarkerType();

                    if (t != null) {
                        boolean placeMarker = true;
                        // Applying constraints for placement of CODA and
                        // Particle
                        // Motion Markers.(They can only be placed on a station
                        // that
                        // has three component)

                        if (Swarm.getApplication().getWaveClipboard().isMakerPlacementEnabled()) {
                            int componentCount =
                                    Swarm.getApplication().getWaveClipboard()
                                            .getStationComponentCount(channel.stationCode);

                            if (markerType.equalsIgnoreCase(Marker.AZIMUTH_MARKER_LABEL) ||
                                markerType.equalsIgnoreCase(Marker.PARTICLE_MARKER_LABEL)) {
                                if (componentCount < 3) {
                                    placeMarker = false;
                                }

                            }

                        } else {
                            placeMarker = false;
                        }

                        int x = e.getX();
                        double j2k = x * t[0] + t[1];
                        Timestamp time = new Timestamp(Util.j2KToDate(j2k).getTime());

                        // Timestamp time = new Timestamp((long)j2k);

                        // Try to get a marker at the specified location
                        Marker marker = getMarkerAtPosition(e.getX(), e.getY());

                        if (marker != null) {
                            selectedMarker = marker;
                            selectedMarker.setFileIndex(WaveViewPanel.this.fileIndex);
                            selectedMarker.setStation(channel.stationCode);
                            draggingMarker = true;
                            if (lineX == j2k) {
                                lineX = Double.NaN;
                            }
                        } else {
                            lineX = j2k;
                            if (placeMarker) {
                                selectedMarker = new Marker();
                                selectedMarker.setStation(channel.stationCode);
                                selectedMarker.setFileIndex(WaveViewPanel.this.fileIndex);
                                selectedMarker.setFilePath(WaveViewPanel.this.filePath);
                                selectedMarker.setFileType(WaveViewPanel.this.fileType);
                                selectedMarker.setMarkerTime(time);
                                selectedMarker.setMarkerType(markerType);

                                if (selectedMarker.getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)) {
                                    selectedMarker.setIs_es("IS");
                                    selectedMarker.setUpDownUnknown("Up");
                                }
                                if (selectedMarker.getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)) {
                                    selectedMarker.setIp_ep("IP");
                                    selectedMarker.setUpDownUnknown("Up");
                                }
                                if (Swarm.getApplication().getWaveClipboard().isMakerPlacementEnabled()) {

                                    Swarm.getApplication()
                                            .getWaveClipboard()
                                            .applyConstraints(channel.stationCode, selectedMarker.getMarkerType(),
                                                    WaveViewPanel.this);

                                    applyConstraints(selectedMarker);
                                    /*// markers.put(time, selectedMarker);
                                    applyConstraints1(channel.stationCode, selectedMarker.getMarkerType(),
                                            WaveViewPanel.this);*/
                                    markers.add(selectedMarker);
                                }
                            } else {
                                selectedMarker = null;
                            }

                        }

                        if (placeMarker) {
                            selectedMarker.setAttempt(Swarm.getSelectedAttempt().getId());
                            setEventCalculations();
                            selectedMarker.setFileName(channel.toString());
                            selectedMarker.persist();
                            System.out.println("maker id : " + selectedMarker.getId() + " , marker time : " +
                                               Time.format(DATE_FORMAT, selectedMarker.getMarkerTime()));
                        }

                        if (SwarmMenu.DataRecordState()) {
                            SwarmMenu.getDataRecord().getMarkerPanel().setViewPanel(WaveViewPanel.this);
                        }

                        if (timeSeries)
                        /* System.out.printf("%s UTC: %s j2k: %.3f ew: %.3f\n",
                                channel,
                                Time.format(DATE_FORMAT, Util.j2KToDate(j2k)),
                                j2k, Util.j2KToEW(j2k)); */

                        if (SwingUtilities.isRightMouseButton(e)) {
                            settings.cycleType();
                        }

                        if (timeSeries && j2k >= startTime && j2k <= endTime) fireTimePressed(e, j2k);

                        if (timeSeries && allowDragging && SwingUtilities.isLeftMouseButton(e)) {
                            // Dimension size = getSize();
                            int y = e.getY();
                            if (t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset &&
                                x < size.width - rightWidth) {
                                j2k1 = j2k2 = j2k;
                                if (e.isControlDown()) {
                                    System.out.println(channel + ": " + Time.format(DATE_FORMAT, Util.j2KToDate(j2k1)));
                                } else if (!e.isShiftDown()) {
                                    highlightX1 = highlightX2 = x;
                                    dragging = true;
                                }
                            }
                        }
                    }
                    paintnow = true;
                    fireMousePressed(e);
                }
            }

            public void mouseReleased(MouseEvent e) {
                Swarm.getApplication().touchUITime();
                if (SwingUtilities.isLeftMouseButton(e) && dragging) {
                    dragging = false;
                    if (j2k1 != j2k2 && source != null && (!draggingMarker)) {
                        double st = Math.min(j2k1, j2k2);
                        double et = Math.max(j2k1, j2k2);
                        zoom(st, et);
                        fireZoomed(e, getStartTime(), getEndTime(), st, et);
                    }
                    repaint();
                }

                int mx = e.getX();
                int my = e.getY();
                if (allowClose && SwingUtilities.isLeftMouseButton(e) && mx > WaveViewPanel.this.getWidth() - 17 &&
                    mx < WaveViewPanel.this.getWidth() - 3 && my > 2 && my < 17) {
                    fireClose();
                }

                draggingMarker = false;
                if (selectedMarker != null) {
                    selectedMarker.persist();
                }
            }

            public void mouseExited(MouseEvent e) {
                Swarm.getApplication().fireTimeChanged(Double.NaN);
                dragging = false;
                repaint();
            }
        });

        this.addMouseMotionListener(new MouseMotionListener() {
            public void mouseMoved(MouseEvent e) {
                Swarm.getApplication().touchUITime();
                processMousePosition(e.getX(), e.getY());
            }

            public void mouseDragged(MouseEvent e) {
                Swarm.getApplication().touchUITime();
                /*
                 * // This used to be the launcher for the microview. // It was
                 * removed because it wasn't very useful, but this // stub is
                 * left here in case something like it ever gets // put in if
                 * (SwingUtilities.isLeftMouseButton(e) && e.isControlDown() &&
                 * settings.type != WaveViewSettings.SPECTRA) { Dimension size =
                 * getSize(); double[] t = getTranslation(); int x = e.getX();
                 * int y = e.getY(); if (t != null && y > Y_OFFSET && y <
                 * (size.height - BOTTOM_HEIGHT) && x > X_OFFSET && x <
                 * size.width - RIGHT_WIDTH) { double j2k = x * t[0] + t[1];
                 * createMicroView(j2k); } }
                 */

                processMousePosition(e.getX(), e.getY());
                if (SwingUtilities.isLeftMouseButton(e) && !e.isControlDown() && dragging) {
                    double[] t = getTranslation();
                    int x = e.getX();
                    int y = e.getY();
                    Dimension size = getSize();
                    if (t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset &&
                        x < size.width - rightWidth) {
                        j2k2 = x * t[0] + t[1];
                        if (draggingMarker) {


                            // Timestamp time = new Timestamp(Util.j2KToDate(j2k2)
                            // .getTime());

                            // Timestamp time = new Timestamp((long)j2k2);
                            Timestamp time = new Timestamp(Util.j2KToDate(j2k2).getTime());
                            // markers.remove(time);
                            selectedMarker.setMarkerTime(time);

                            lineX = Double.NaN;

                            if (Swarm.getApplication().getWaveClipboard().isMakerPlacementEnabled()) {
                                // Set<Timestamp> keys = markers.keySet();
                                //
                                // for (Timestamp key : keys) {
                                // if (markers.get(key).equals(selectedMarker))
                                // {
                                // System.out.println(markers.get(key).getMarkerType()
                                // + "   " + selectedMarker.getMarkerType());
                                // markers.remove(key);
                                // markers.put(time, selectedMarker);
                                // break;
                                // }
                                // }

                            }
                            // selectedMarker.persist();
                            WaveViewPanel.this.setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));

                            if (SwarmMenu.DataRecordState()) {
                                SwarmMenu.getDataRecord().getMarkerPanel().setViewPanel(WaveViewPanel.this);
                            }
                            setEventCalculations();
                        } else {
                            WaveViewPanel.this.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
                            highlightX2 = x;
                        }
                        repaint();
                    }
                }
            }
        });
    }

    public void zoom(final double st, final double et) {
        final SwingWorker worker = new SwingWorker() {
            public Object construct() {
                Wave sw = null;
                if (source instanceof CachedDataSource) sw =
                        ((CachedDataSource)source).getBestWave(channel.toString, st, et);
                else sw = source.getWave(channel.toString, st, et);
                setWave(sw, st, et);
                return null;
            }

            public void finished() {

                repaint();
            }
        };
        worker.start();
    }

    /**
     * Set the working flag. This flag indicates whether data are being loaded for this panel.
     * 
     * @param b
     *            the working flag state
     */
    public void setWorking(boolean b) {
        working = b;
    }

    /**
     * Set the allow dragging flag. This flag enables zoom dragging. Currently only allowed on the clipboard, but could
     * be implemented within the helicorder view.
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

    public WaveViewSettings getSettings() {
        return settings;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public Wave getWave() {
        return wave;
    }

    public WaveViewSettings getWaveViewSettings() {
        return settings;
    }

    public SimpleChannel getChannel() {
        return channel;
    }

    public void setChannel(SimpleChannel c) {
        channel = c;
    }

    public void setSettings(WaveViewSettings s) {
        settings = s;
        processSettings();
    }

    public SeismicDataSource getDataSource() {
        return source;
    }

    public void setDataSource(SeismicDataSource s) {
        source = s;
    }

    public void setFrameDecorator(FrameDecorator fd) {
        decorator = fd;
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
     * Gets the translation info for this panel. The translation info is used to convert from pixel coordinates on the
     * panel into time or data coordinates.
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

    /**
     * Processes the mouse position variables when the cursor is over the panel. Currently, the only thing this does is
     * set the status bar text.
     * 
     * @param x
     *            the mouse x position
     * @param y
     *            the mouse y position
     */
    public boolean processMousePosition(int x, int y) {
        String status = null;
        String unit = null;
        String waveInfo = null;

        Dimension size = getSize();
        double[] t = getTranslation();
        double j2k = Double.NaN;

        if (wave != null && t != null && y > yOffset && y < (size.height - bottomHeight) && x > xOffset &&
            x < size.width - rightWidth) {
            j2k = x * t[0] + t[1];
            double yi = y * -t[2] + t[3];

            int[] dataRange = wave.getDataRange();
            waveInfo =
                    String.format("[%s - %s (UTC), %d samples (%.2f s), %d samples/s, %d, %d]",
                            Time.format(DATE_FORMAT, Util.j2KToDate(wave.getStartTime())),
                            Time.format(DATE_FORMAT, Util.j2KToDate(wave.getEndTime())), wave.numSamples(),
                            wave.numSamples() / wave.getSamplingRate(), (int)wave.getSamplingRate(), dataRange[0],
                            dataRange[1]);

            if (timeSeries) {
                String utc = Time.format(DATE_FORMAT, Util.j2KToDate(j2k));
                TimeZone tz = Swarm.config.getTimeZone(channel.toString);
                double tzo = Time.getTimeZoneOffset(tz, j2k);
                if (tzo != 0) {
                    String tza = tz.getDisplayName(tz.inDaylightTime(Util.j2KToDate(j2k)), TimeZone.SHORT);
                    status = Time.format(DATE_FORMAT, Util.j2KToDate(j2k + tzo)) + " (" + tza + "), " + utc + " (UTC)";
                } else status = utc;

                double offset = 0;
                double multiplier = 1;

                if (settings.viewType == ViewType.SPECTROGRAM) unit = "Frequency (Hz)";
                else {
                    Metadata md = Swarm.config.getMetadata(channel.toString);
                    if (md != null) {
                        offset = md.getOffset();
                        multiplier = md.getMultiplier();
                        unit = md.getUnit();
                    }

                    if (unit == null) unit = "Counts";
                }

                // System.out.printf("Multipler: %f, Offset: %f\n", offset,
                // multiplier);
                status = String.format("%s, %s: %.3f, %s", status, unit, multiplier * yi + offset, waveInfo);

            }

            else {
                double xi = j2k;
                if (settings.viewType == ViewType.SPECTRA && settings.logFreq) xi = Math.pow(10.0, xi);
                if (settings.viewType == ViewType.SPECTRA && settings.logPower) yi = Math.pow(10.0, yi);
                status = String.format("%s, Frequency (Hz): %.3f, Power: %.3f", waveInfo, xi, yi);
            }
        } else {
            status = " ";
        }

        Swarm.getApplication().fireTimeChanged(j2k);

        if (status == null) status = " ";

        if (!Double.isNaN(mark1) && !Double.isNaN(mark2)) {
            double dur = Math.abs(mark1 - mark2);
            String pre = String.format("Duration: %.2fs (Md: %.2f)", dur, Swarm.config.getDurationMagnitude(dur));
            if (status.length() > 2) status = pre + ", " + status;
            else status = pre;
        }

        if (status != null && statusLabel != null) {
            final String st = status;
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    statusLabel.setText(st);
                }
            });
        }

        return !status.equals(" ");
    }

    public void setWave(Wave sw, double st, double et) {
        wave = sw;
        startTime = st;
        endTime = et;
        processSettings();
    }

    public void resetAutoScaleMemory() {
        minAmp = 1E300;
        maxAmp = -1E300;
        maxSpectraPower = -1E300;
        maxSpectrogramPower = -1E300;
        settings.autoScaleAmp = true;
        settings.autoScalePower = true;
        processSettings();
    }

    public void adjustScale(double pct) {
        double maxa = settings.autoScaleAmp ? maxAmp : settings.maxAmp;
        double mina = settings.autoScaleAmp ? minAmp : settings.minAmp;
        settings.autoScaleAmp = false;
        double range = maxa - mina;
        double center = range / 2 + mina;
        double newRange = range * pct;
        settings.minAmp = center - newRange / 2;
        settings.maxAmp = center + newRange / 2;

        if (settings.viewType == ViewType.SPECTROGRAM) {
            double maxf = settings.maxFreq * pct;
            System.out.printf("WaveViewPanel(804): maxf = %f\n", maxf);
            settings.maxFreq = (maxf > wave.getSamplingRate() / 2) ? wave.getSamplingRate() / 2 : maxf;
            System.out.printf("WaveViewPanel(806): settings.maxFreq = %f\n", settings.maxFreq);

        }

        processSettings();
    }

    private synchronized void setImage(BufferedImage bi) {
        image = bi;
    }

    private synchronized BufferedImage getImage() {
        return image;
    }

    public void createImage() {
        final Runnable r = new Runnable() {
            public void run() {
                if (getWidth() > 0 && getHeight() > 0) {
                    BufferedImage bi = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
                    Graphics2D ig = (Graphics2D)bi.getGraphics();
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
        } else r.run();
    }

    /**
     * Does NOT call repaint for efficiency purposes, that is left to the container.
     */
    private void processSettings() {
        if (wave == null || wave.buffer == null || wave.buffer.length == 0) return;

        if (settings.maxFreq > wave.getNyquist()) settings.maxFreq = wave.getNyquist();

        timeSeries = !(settings.viewType == ViewType.SPECTRA);

        createImage();
    }

    private void filter(Wave w) {
        double mean = w.mean();

        double[] dBuf = new double[w.buffer.length + (int)(w.buffer.length * 0.5)];
        Arrays.fill(dBuf, mean);
        int trueStart = (int)(w.buffer.length * 0.25);
        for (int i = 0; i < w.buffer.length; i++) {
            if (w.buffer[i] != Wave.NO_DATA) dBuf[i + trueStart] = w.buffer[i];
        }

        settings.filter.setSamplingRate(w.getSamplingRate());
        settings.filter.create();
        Filter.filter(dBuf, settings.filter.getSize(), settings.filter.getXCoeffs(), settings.filter.getYCoeffs(),
                settings.filter.getGain(), 0, 0);
        if (settings.zeroPhaseShift) {
            double[] dBuf2 = new double[dBuf.length];
            for (int i = 0, j = dBuf.length - 1; i < dBuf.length; i++, j--)
                dBuf2[j] = dBuf[i];

            Filter.filter(dBuf2, settings.filter.getSize(), settings.filter.getXCoeffs(), settings.filter.getYCoeffs(),
                    settings.filter.getGain(), 0, 0);

            for (int i = 0, j = dBuf2.length - 1 - trueStart; i < w.buffer.length; i++, j--)
                w.buffer[i] = (int)Math.round(dBuf2[j]);
        } else {
            for (int i = 0; i < w.buffer.length; i++)
                w.buffer[i] = (int)Math.round(dBuf[i + trueStart]);
        }
        w.invalidateStatistics();
    }

    /**
     * Paints the component on the specified graphics context.
     * 
     * @param g
     *            the graphics context
     */
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        Dimension dim = this.getSize();
        if (wave == null) {
            g2.setColor(backgroundColor);
            g2.fillRect(0, 0, dim.width, dim.height);
            g2.setColor(Color.black);
            if (working) g2.drawString("Retrieving data...", dim.width / 2 - 50, dim.height / 2);
            else {
                String error = "No wave data.";
                if (channel != null) error = "No wave data for " + channel + ".";
                int w = g2.getFontMetrics().stringWidth(error);
                g2.drawString(error, dim.width / 2 - w / 2, dim.height / 2);
            }
        } else {
            BufferedImage bi = getImage();
            if (bi != null) g2.drawImage(bi, 0, 0, null);

            if (dragging) paintDragBox(g2);

            if (!Double.isNaN(mark1)) paintMark(g2, mark1);

            if (!Double.isNaN(mark2)) paintMark(g2, mark2);

            if (!Double.isNaN(cursorMark)) paintCursor(g2);
        }

        if (allowClose) {
            if (closeImg == null) closeImg = Icons.close_view.getImage();

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

        if (paintnow) {
            if (lineX != Double.NaN) {
                drawMarker(g2, lineX);
            }
            paintMarker(g2);
        }
    }

    public void setUseFilterLabel(boolean b) {
        useFilterLabel = b;
    }

    public TextRenderer getFilterLabel() {
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
        TextRenderer tr = new TextRenderer(xOffset + 5, 148, ft);
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
        Wave renderWave = wave;
        if (settings.filterOn) {
            renderWave = new Wave(wave);
            filter(renderWave);
            if (settings.removeBias) bias = (int)Math.round(renderWave.mean());
        }

        switch (settings.viewType) {
        case WAVE:
            plotWave(plot, renderWave);
            plotHeight = plot.getHeight();
            break;
        case SPECTRA:
            plotSpectra(plot, renderWave);
            break;
        case SPECTROGRAM:
            plotSpectrogram(plot, renderWave);
            break;
        }

        try {
            plot.render(g2);
        } catch (PlotException e) {
            e.printStackTrace();
        }
    }

    /**
     * Plots a wave.
     * 
     * @param renderWave
     *            the wave to plot
     */
    private void plotWave(Plot plot, Wave renderWave) {
        plot.getRenderers().clear();
        if (renderWave == null || renderWave.numSamples() == 0) return;

        SliceWave wv = new SliceWave(renderWave);
        wv.setSlice(startTime, endTime);

        double offset = 0;
        double multiplier = 1;
        Metadata md = Swarm.config.getMetadata(channel.toString);

        if (settings.useUnits && md != null) {
            offset = md.getOffset();
            multiplier = md.getMultiplier();
        }

        double bias = 0;
        if (settings.removeBias) bias = wv.mean();

        double minY = (settings.minAmp - offset) / multiplier;
        double maxY = (settings.maxAmp - offset) / multiplier;

        if (settings.autoScaleAmp) {
            double[] dr = new double[] { wv.min(), wv.max() };
            if (settings.autoScaleAmpMemory) {
                minY = Math.min(minAmp, dr[0] - bias);
                maxY = Math.max(maxAmp, dr[1] - bias);
                minAmp = Math.min(minY, minAmp);
                maxAmp = Math.max(maxY, maxAmp);
            } else {
                minY = dr[0] - bias;
                maxY = dr[1] - bias;
            }
        }

        if (waveRenderer == null) waveRenderer = new SliceWaveRenderer();

        if (decorator != null) waveRenderer.setFrameDecorator(decorator);

        if (settings.useUnits && md != null && md.getUnit() != null) waveRenderer.setYLabelText(md.getUnit());
        else waveRenderer.setYLabelText("Counts");

        waveRenderer.setYAxisCoefficients(multiplier, offset);
        waveRenderer.setLocation(xOffset, yOffset, this.getWidth() - xOffset - rightWidth, this.getHeight() - yOffset -
                                                                                           bottomHeight);
        waveRenderer.setYLimits(minY, maxY);
        waveRenderer.setViewTimes(startTime, endTime, "");
        waveRenderer.setWave(wv);
        waveRenderer.setRemoveBias(settings.removeBias);
        if (channel != null && displayTitle) waveRenderer.setTitle(channel.toString);

        waveRenderer.update();
        plot.addRenderer(waveRenderer);
        if (useFilterLabel && settings.filterOn) plot.addRenderer(getFilterLabel());
        translation = waveRenderer.getDefaultTranslation();
    }

    /**
     * Plots frequency spectra.
     * 
     * @param renderWave
     *            the wave to plot
     */
    private void plotSpectra(Plot plot, Wave renderWave) {
        if (renderWave == null || renderWave.numSamples() == 0) return;

        SliceWave wv = new SliceWave(renderWave);
        wv.setSlice(startTime, endTime);

        if (spectraRenderer == null) spectraRenderer = new SpectraRenderer();

        if (decorator != null) spectraRenderer.setFrameDecorator(decorator);

        spectraRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset, this.getHeight() -
                                                                                              bottomHeight - yOffset);
        spectraRenderer.setWave(wv);

        spectraRenderer.setAutoScale(settings.autoScalePower);
        spectraRenderer.setLogPower(settings.logPower);
        spectraRenderer.setLogFreq(settings.logFreq);
        spectraRenderer.setMaxFreq(settings.maxFreq);
        spectraRenderer.setMinFreq(settings.minFreq);
        spectraRenderer.setYUnitText("Power");
        if (channel != null && displayTitle) spectraRenderer.setTitle(channel.toString);

        spectraRenderer.update();
        if (useFilterLabel && settings.filterOn) plot.addRenderer(getFilterLabel());

        translation = spectraRenderer.getDefaultTranslation();
        plot.addRenderer(spectraRenderer);
    }

    /**
     * Plots a spectrogram. TODO: Fix logPower.
     * 
     * @param renderWave
     *            the wave to plot
     */
    private void plotSpectrogram(Plot plot, Wave renderWave) {
        if (renderWave == null || renderWave.numSamples() == 0) return;

        SliceWave wv = new SliceWave(renderWave);
        wv.setSlice(startTime, endTime);

        if (spectrogramRenderer == null) spectrogramRenderer = new SpectrogramRenderer();

        if (decorator != null) spectrogramRenderer.setFrameDecorator(decorator);

        spectrogramRenderer.setLocation(xOffset, yOffset, this.getWidth() - rightWidth - xOffset, this.getHeight() -
                                                                                                  bottomHeight -
                                                                                                  yOffset);
        spectrogramRenderer.setWave(wv);

        spectrogramRenderer.setViewStartTime(startTime);
        spectrogramRenderer.setViewEndTime(endTime);
        spectrogramRenderer.setAutoScale(settings.autoScalePower);
        spectrogramRenderer.setLogPower(settings.logPower);

        spectrogramRenderer.setOverlap(settings.spectrogramOverlap);
        spectrogramRenderer.setMaxFreq(settings.maxFreq);
        spectrogramRenderer.setMinFreq(settings.minFreq);

        spectrogramRenderer.setMaxPower(settings.maxPower);
        spectrogramRenderer.setMinPower(settings.minPower);

        spectrogramRenderer.setBinSize((int)Math.pow(2,
                Math.ceil(Math.log(settings.binSize * wave.getSamplingRate()) / Math.log(2))));

        if (channel != null && displayTitle) spectrogramRenderer.setTitle(channel.toString);

        spectrogramRenderer.setYUnitText("Frequency (Hz)");

        spectrogramRenderer.setNfft(settings.nfft);

        double Power[] = spectrogramRenderer.update();

        settings.minPower = Power[0];
        settings.maxPower = Power[1];

        plot.addRenderer(spectrogramRenderer);
        if (useFilterLabel && settings.filterOn) plot.addRenderer(getFilterLabel());
        translation = spectrogramRenderer.getDefaultTranslation();
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
        if (Double.isNaN(cursorMark) || cursorMark < startTime || cursorMark > endTime) return;

        double[] t = getTranslation();
        if (t == null) return;
        double x = (cursorMark - t[1]) / t[0];
        g2.setColor(DARK_RED);
        g2.draw(new Line2D.Double(x, yOffset + 1, x, getHeight() - bottomHeight - 1));
    }

    private void paintMark(Graphics2D g2, double j2k) {
        if (Double.isNaN(j2k) || j2k < startTime || j2k > endTime) return;

        double[] t = getTranslation();
        if (t == null) return;

        double x = (j2k - t[1]) / t[0];
        g2.setColor(DARK_GREEN);
        g2.draw(new Line2D.Double(x, yOffset, x, getHeight() - bottomHeight - 1));

        GeneralPath gp = new GeneralPath();
        gp.moveTo((float)x, yOffset);
        gp.lineTo((float)x - 5, yOffset - 7);
        gp.lineTo((float)x + 5, yOffset - 7);
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

    /**
     * Apply Constraints for placing markers at the time a marker is to be placed on the Wave Panel <br />
     * <ul>
     * <li><b>Phase (p)</b> : Only one can be placed</li>
     * <li><b>Phase (s)</b> : Only one can be placed</li>
     * <li><b>Coda</b> : Only one can be placed</li>
     * <li><b>Azimuth</b> : Only Two can be placed</li>
     * <li><b>Particle Motion</b> : Only Two can be placed</li>
     * </ul>
     * 
     * @param marker
     *            : {@link Marker} object that needs to be placed on this WavePanel
     */
    public void applyConstraints(Marker marker) {
        ArrayList<Timestamp> existingMarkers = getMarkerKeys(marker.getMarkerType());
        if (marker.getMarkerType().equalsIgnoreCase(Marker.P_MARKER_LABEL)) {
            if (existingMarkers.size() == 1) {
                Marker m = getMarker(existingMarkers.get(0));
                removeMarker(existingMarkers.get(0));
                if (m != null) {
                    m.delete();
                }
            }

        } else if (marker.getMarkerType().equalsIgnoreCase(Marker.S_MARKER_LABEL)) {
            if (existingMarkers.size() == 1) {
                Marker m = getMarker(existingMarkers.get(0));
                removeMarker(existingMarkers.get(0));
                if (m != null) {
                    m.delete();
                }
            }

        } else if (marker.getMarkerType().equalsIgnoreCase(Marker.CODA_MARKER_LABEL)) {
            if (existingMarkers.size() == 1) {
                Marker m = getMarker(existingMarkers.get(0));
                removeMarker(existingMarkers.get(0));
                if (m != null) {
                    m.delete();
                }
            }
        } else {
            if (existingMarkers.size() == 2) {
                Marker m = getMarker(existingMarkers.get(1));
                removeMarker(existingMarkers.get(1));
                if (m != null) {
                    m.delete();
                }
            }
        }
    }

    /**
     * Add a Marker at a particular time to thie WavePanel
     * 
     * @param key
     *            : timestamp that marker is to be placed on the WavePanel
     * @param marker
     *            : Marker to be placed on this WavePanel
     */
    public void addMarker(Timestamp key, Marker marker) {
        markers.add(marker);
    }

    /**
     * Get the timestamp that markers of a paticular type was placed on this WavePanel
     * 
     * @param markerType
     *            : type of Marker
     * @return
     */
    public ArrayList<Timestamp> getMarkerKeys(String markerType) {
        ArrayList<Timestamp> markerKeys = new ArrayList<Timestamp>();
        for (Marker m : markers) {
            if (m.getMarkerType().equalsIgnoreCase(markerType)) {
                markerKeys.add(m.getMarkerTime());
            }
        }
        return markerKeys;
    }

    /**
     * Get Marker of specified type placed on this WavePanel
     * 
     * @param markerType
     *            : type of Marker needed to be returned
     * @return
     */
    public Marker getMarkerByType(String markerType) {
        for (Marker m : markers) {
            if (m.getMarkerType().equalsIgnoreCase(markerType)) {
                return m;
            }
        }
        return null;
    }

    public Marker getMarkerByTypes(String markerType) {
        for (Marker m : markers) {
            if (m.getMarkerType().equalsIgnoreCase(markerType)) {
                return m;
            }
        }
        return null;
    }

    private void applyConstraints1(String stationCode, String markerType, WaveViewPanel wv) {
        ArrayList<WaveViewPanel> waves =
                Swarm.getApplication().getWaveClipboard().getStationComponentMap().get(stationCode);
        String[] mType = new String[3];
        mType[0] = Marker.P_MARKER_LABEL;
        mType[1] = Marker.S_MARKER_LABEL;
        mType[2] = Marker.CODA_MARKER_LABEL;
        if (waves != null) {
            for (WaveViewPanel wvp : waves) {
                if (!wvp.equals(wv)) {
                    if (markerType.equalsIgnoreCase(Marker.P_MARKER_LABEL) ||
                        markerType.equalsIgnoreCase(Marker.S_MARKER_LABEL) ||
                        markerType.equalsIgnoreCase(Marker.CODA_MARKER_LABEL)) {
                        for (int i = 0; i < mType.length; i++) {
                            if (!mType[i].equalsIgnoreCase(markerType)) {
                                if (null != wvp.getMarkerByType(mType[i])) markers.add(wvp.getMarkerByType(mType[i]));
                            }
                        }
                    }
                }
            }

        }
    }

    /**
     * 
     * Get all markers of a specified type placed on this WavePanel. This is only used to return Azimuth and Particle
     * motion markers as they are the only markers that can be placed twice on a WavePanel
     * 
     * @param markerType
     *            : type of Marker needed to be returned(Mainly Azimuth and Particle Motion markers)
     * @return
     */
    public ArrayList<Marker> getMarkersByType(String markerType) {
        ArrayList<Marker> selectedMarkers = new ArrayList<Marker>();
        for (Marker m : markers) {
            if (m.getMarkerType().equalsIgnoreCase(markerType)) {
                selectedMarkers.add(m);
            }
        }
        return selectedMarkers;
    }

    /**
     * Removes marker from the WavePanel that has been placed at specified timestamp
     * 
     * @param key
     *            : timestamp of Marker
     */
    public void removeMarker(Timestamp key) {
        for (Marker m : markers) {
            if (m.getMarkerTime().equals(key)) {

                markers.remove(m);
                break;
            }
        }
    }


    /**
     * Get Marker on the WavePanel placed at the specified time stamp
     * 
     * @param key
     *            : timestamp of Marker
     * @return
     */
    public Marker getMarker(Timestamp key) {
        for (Marker m : markers) {
            if (m.getMarkerTime().equals(key)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Remove Specified marker from this WavePanel
     * 
     * @param marker
     *            : Specified {@link Marker} object
     */
    public void removeMarker(Marker marker) {
        if (markers.contains(marker)) {
            markers.remove(marker);
        }

    }

    /**
     * Remove Marker with specified Id from the WavePanel
     * 
     * @param id
     *            : ID of {@link Marker} object
     * @return
     */
    public boolean removeMarker(Integer id) {
        boolean removed = false;
        for (Marker m : markers) {
            if (m.getId().equals(id)) {
                markers.remove(m);
                break;
            }
        }
        return removed;
    }


    /**
     * Remove all {@link Marker} objects from this wave panel
     * 
     */
    public void removeAllMarkersFromView() {
        markers.clear();
    }

    /**
     * Gets the selected {@link Marker} object in use for this wave panel
     * 
     * @return Selected Marker object
     */
    public Marker getSelectedMarker() {
        return selectedMarker;
    }

    /**
     * Sets the specified {@link Marker} object as the current selected marker in use for this wave panel
     * 
     * @param marker
     *            : Specified {@link Marker} object
     */
    public void setSelectedMarker(Marker marker) {
        selectedMarker = marker;
    }

    public String getFileType() {
        return fileType;
    }

}
