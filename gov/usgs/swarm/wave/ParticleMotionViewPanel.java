package gov.usgs.swarm.wave;

import gov.usgs.swarm.wave.WaveClipboardFrame.WavePlotInfo;
import gov.usgs.util.Pair;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.math.BigDecimal;
import java.text.DecimalFormat;

import javax.swing.JPanel;


/**
 * A Panel that holds a single particle motion plot between two sets of component data
 * <br />
 * Component data could be any of the following :
 * <ul>
 * <li>Z component</li>
 * <li>E component</li>
 * <li>N component</li>
 * </ul>
 * 
 * @author Chirag Patel
 * @author Joel Shellman
 */
@SuppressWarnings("serial")
public class ParticleMotionViewPanel extends JPanel {
	private WavePlotInfo xWave;
	private WavePlotInfo yWave;

	private double[] xData;
	private double[] yData;

	private double minX;
	private double maxX;
	private double minY;
	private double maxY;

	private String xLabel;
	private String yLabel;

	private int PLOT_TO_TICK_DIST = 30;

	private int TICKS_TO_LABEL_DIST = 20;

	private int LABEL_TO_SCREEN_DIST = 5;

	private int plotBegin = PLOT_TO_TICK_DIST + TICKS_TO_LABEL_DIST
			+ LABEL_TO_SCREEN_DIST;

	public ParticleMotionViewPanel(WavePlotInfo xWave, WavePlotInfo yWave) {
		this.xWave = xWave;
		this.yWave = yWave;
		this.xLabel = xWave.label;
		this.yLabel = yWave.label;
		this.xData = xWave.data;
		this.yData = yWave.data;
//		maxMagnitude = Math.max(Math.abs(extent.item1), Math.abs(extent.item2));
//		minX = -maxMagnitude;
//		maxX = maxMagnitude;
//		minY = -maxMagnitude;
//		maxY = maxMagnitude;
		
//		minX = xWave.minMax.item1;
//		maxX = xWave.minMax.item2;
//		minY = yWave.minMax.item1;
//		maxY = yWave.minMax.item2;
		
		Pair<Double, Double> xExtent = ParticleMotionFrame.extent(this.xData);
		Pair<Double, Double> yExtent = ParticleMotionFrame.extent(this.yData);
//		double xMaxMag = Math.max(Math.abs(xExtent.item1 - xWave.bias), Math.abs(xExtent.item2 - xWave.bias));
//		minX = -xMaxMag;
//		maxX = xMaxMag;
//		double yMaxMag = Math.max(Math.abs(yExtent.item1 - yWave.bias), Math.abs(yExtent.item2 - yWave.bias));
//		minY = -yMaxMag;
//		maxY = yMaxMag;
		
		Pair<Double, Double> extent = ParticleMotionFrame.extent(add(xExtent, -xWave.bias), add(yExtent, -yWave.bias));
		double maxMagnitude = Math.max(Math.abs(extent.item1), Math.abs(extent.item2));
		minX = -maxMagnitude;
		maxX = maxMagnitude;
		minY = -maxMagnitude;
		maxY = maxMagnitude;
	
	}

	private Pair<Double, Double> add(Pair<Double, Double> pair, double add) {
		return new Pair<Double, Double>(pair.item1 + add, pair.item2 + add);
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setColor(Color.white);
		g2.fillRect(0, 0, getWidth(), getHeight());

		g2.setColor(Color.red);
		Font font = new Font("Serif", Font.PLAIN, 11);
		g2.setFont(font);

		drawXAndYAxis(g2);
		drawAxisMarks(g2);

		drawPlot(g2);

		drawYLabel(g2);
		drawXLabel(g2);
	}
	
	public double getXPixel(double x) {
		return ((x - minX) * getXScale()) + plotBegin;
	}

	public double getYPixel(double y) {
		return (getHeight() - plotBegin) - ((y - minY) * getYScale());
	}

	public double getYScale() {
		return (getHeight() - (plotBegin + 20)) / (maxY - minY);
	}

	public double getXScale() {
		return (getWidth() - (plotBegin + 20)) / (maxX - minX);
	}

	
	private void drawAxisMarks(Graphics2D g2) {
		// drawing Y -marks
		double Ydiff = maxY - minY;
		double yunit = Ydiff / 4;
		long label = (long) minY;

		// draw for Y -axis
		for (int i = 0; i <= 5; i++) {
			label = (long) minY + (long) (yunit * i);
			
			
			String labelString = Long.toString(label);
			if(label != 0){
			BigDecimal bd = new BigDecimal(labelString);
			DecimalFormat format = new DecimalFormat("0.0E0");
			labelString = format.format(bd);
			}
			FontMetrics fm = g2.getFontMetrics(g2.getFont());
			g2.drawString(
					labelString,
					((LABEL_TO_SCREEN_DIST + TICKS_TO_LABEL_DIST) - fm
							.stringWidth(labelString))
							+ ((TICKS_TO_LABEL_DIST)),
					(float) (getYPixel(label)));
			g2.drawLine(plotBegin, (int) (getYPixel(label)), (plotBegin + 10),
					(int) (getYPixel(label)));
		}

		// draw for x-axis
		g2.drawLine((int) (getXPixel(0)), (getHeight() - plotBegin),
				(int) (getXPixel(0)), getHeight() - (plotBegin + 10));
		g2.drawString(
				"0",
				(int) (getXPixel(0)),
				(float) ((getHeight() - (LABEL_TO_SCREEN_DIST + PLOT_TO_TICK_DIST))));
		
		
		BigDecimal bd = new BigDecimal(Integer.toString((int) minX));
		DecimalFormat format = new DecimalFormat("0.0E0");
		String minString = format.format(bd);
		
		g2.drawString(
				minString,
				(int) (getXPixel(minX)),
				(float) ((getHeight() - (LABEL_TO_SCREEN_DIST + PLOT_TO_TICK_DIST))));

		FontMetrics fm = g2.getFontMetrics(g2.getFont());

		
		bd = new BigDecimal(Integer.toString((int) maxX));
		
		String maxString = format.format(bd);
		
		g2.drawString(
				maxString,
				((int) (getXPixel(maxX)) - fm.stringWidth(maxString)),
				(float) ((getHeight() - (LABEL_TO_SCREEN_DIST + PLOT_TO_TICK_DIST))));

	}

	private void drawPlot(Graphics2D g2) {
		Color origColor = g2.getColor();
		g2.setColor(Color.blue);
		GeneralPath gp = new GeneralPath();
		gp.moveTo((float) getXPixel(xData[0] - xWave.bias), (float) (getYPixel(yData[0] - yWave.bias)));
		for (int i = 1; i < xData.length; i++) {
			try{
			gp.lineTo((float) getXPixel(xData[i] - xWave.bias), (float) getYPixel(yData[i] - yWave.bias));
			}catch(Exception e){
				System.out.println(e.getMessage());
			}
		}
		g2.draw(gp);
		g2.setColor(origColor);
	}

	private void drawXAndYAxis(Graphics2D g2) {
		g2.drawRect(plotBegin, 20, getWidth() - (plotBegin + 20), getHeight()
				- (plotBegin + 20));
	}

	private void drawYLabel(Graphics2D g2) {
		// Create a rotation transformation for the font.
		AffineTransform fontAT = new AffineTransform();

		// get the current font
		Font theFont = g2.getFont();

		// Derive a new font using a rotatation transform
		fontAT.rotate(3.0 * java.lang.Math.PI / 2.0);
		Font theDerivedFont = theFont.deriveFont(fontAT);

		FontMetrics fm = g2.getFontMetrics(theDerivedFont);
		java.awt.geom.Rectangle2D rect = fm.getStringBounds(xLabel, g2);

		int textHeight = (int) (rect.getHeight());
		int panelHeight = this.getHeight();

		// Center text horizontally and vertically
		int y = (panelHeight - textHeight) / 2 + fm.getAscent();

		// set the derived font in the Graphics2D context
		g2.setFont(theDerivedFont);

		// Render a string using the derived font
		g2.drawString(yLabel, 2 * LABEL_TO_SCREEN_DIST, y);

		// put the original font back
		g2.setFont(theFont);
	}

	private void drawXLabel(Graphics2D g2) {
		// get the current font
		Font f = g2.getFont();

		FontMetrics fm = g2.getFontMetrics(f);
		java.awt.geom.Rectangle2D rect = fm.getStringBounds(xLabel, g2);

		int textWidth = (int) (rect.getWidth());
		int panelWidth = this.getWidth();

		// Center text horizontally and vertically
		int x = (panelWidth - textWidth) / 2;

		// Draw the string. in center
		g2.drawString(xLabel, x, (getHeight() - LABEL_TO_SCREEN_DIST));
	}

	
	// Getter and setter for plot properties
	
	public double[] getxData() {
		return xData;
	}

	public void setxData(double[] xData) {
		this.xData = xData;
	}

	public double[] getyData() {
		return yData;
	}

	public void setyData(double[] yData) {
		this.yData = yData;
	}

	public String getxLabel() {
		return xLabel;
	}

	public void setxLabel(String xLabel) {
		this.xLabel = xLabel;
	}

	public String getyLabel() {
		return yLabel;
	}

	public void setyLabel(String yLabel) {
		this.yLabel = yLabel;
	}
}
