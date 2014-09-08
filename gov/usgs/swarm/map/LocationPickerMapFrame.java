package gov.usgs.swarm.map;

import gov.usgs.swarm.Throbber;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * Frame for picking a location.
 * 
 * @author Chirag Patel
 */
public class LocationPickerMapFrame extends JDialog {

	private JPanel mainPanel;

	private LocationPickerMapPanel mapPanel;

	private Throbber throbber;

	JTextField minVal;

	JTextField maxVal;

	JButton searchButton;

	Double minD;

	Double maxD;

	Point2D.Double longLat;

	boolean searchClicked;

	public boolean isSearchClicked() {
		return searchClicked;
	}

	public Double getMinD() {
		return minD;
	}

	public Double getMaxD() {
		return maxD;
	}

	public Point2D.Double getLongLat() {
		return longLat;
	}

	private Border border;

	public LocationPickerMapPanel getMapPanel() {
		return mapPanel;
	}

	public LocationPickerMapFrame() {
		setAlwaysOnTop(true);
		setTitle("Search By Location");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		createUI();
		this.setModal(true);

	}

	private void createUI() {
		// setFrameIcon(Icons.earth);
		setSize(550, 400);
		throbber = new Throbber();

		mainPanel = new JPanel(new BorderLayout());

		mapPanel = new LocationPickerMapPanel();

		border = BorderFactory.createCompoundBorder(
				BorderFactory.createEmptyBorder(0, 2, 0, 3),
				LineBorder.createGrayLineBorder());
		mapPanel.setBorder(border);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(new JLabel("Min Depth"));
		minVal = new JTextField(8);
		buttonPanel.add(minVal);

		buttonPanel.add(new JLabel("Max Depth"));
		maxVal = new JTextField(8);
		buttonPanel.add(maxVal);

		buttonPanel.add(createSearchButton());

		// JSplitPane splitPane = createSplitPane();
		//
		// splitPane.setDividerLocation(300);
		// splitPane.setTopComponent(mapPanel);
		// splitPane.setBottomComponent(buttonPanel);
		// mainPanel.add(splitPane, BorderLayout.CENTER);

		mainPanel.add(mapPanel, BorderLayout.CENTER);
		mainPanel.add(buttonPanel, BorderLayout.SOUTH);

		setContentPane(mainPanel);

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				if (!mapPanel.imageValid())
					mapPanel.resetImage();
			}
		});
	}

	public void setStatusText(final String t) {

	}

	public Throbber getThrobber() {
		return throbber;
	}

	private JButton createSearchButton() {
		searchButton = new JButton("Search");
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean errorExist = false;
				searchClicked = false;
				longLat = mapPanel.getPickedLongAndLatitude();

				if (minVal.getText() != null
						&& !minVal.getText().trim().isEmpty()) {
					try {
						minD = Double.parseDouble(minVal.getText());
					} catch (Exception ex) {
						errorExist = true;
						JOptionPane.showMessageDialog(null,
								"Enter number for min depth", "Error",
								JOptionPane.ERROR_MESSAGE);
					}

				}

				if (minVal.getText() != null
						&& !minVal.getText().trim().isEmpty()) {
					try {
						maxD = Double.parseDouble(maxVal.getText());
					} catch (Exception ex) {
						errorExist = true;
						JOptionPane.showMessageDialog(null,
								"Enter number for max depth", "Error",
								JOptionPane.ERROR_MESSAGE);
					}

				}

				if (minD != null && maxD != null) {
					if (minD > maxD) {
						errorExist = true;
						JOptionPane.showMessageDialog(null,
								"Min Depth cannot be more than Max Depth",
								"Error", JOptionPane.ERROR_MESSAGE);
					}
				}

				if (!errorExist) {
					searchClicked = true;
					setVisible(false);
				}

			}
		});
		return searchButton;
	}

	private JSplitPane createSplitPane() {
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitPane.setDividerSize(10);
		splitPane.setOneTouchExpandable(true);
		final SplitPaneUI ui = splitPane.getUI();
		if (ui instanceof BasicSplitPaneUI) {
			((BasicSplitPaneUI) ui).getDivider().setBorder(null);
			splitPane
					.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
					.put(KeyStroke.getKeyStroke(KeyEvent.VK_U,
							KeyEvent.CTRL_DOWN_MASK), "jumpToTop");
			splitPane.getActionMap().put("jumpToTop", new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					((JButton) ((BasicSplitPaneUI) ui).getDivider()
							.getComponent(0)).doClick();
				}
			});
			splitPane
					.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
					.put(KeyStroke.getKeyStroke(KeyEvent.VK_D,
							KeyEvent.CTRL_DOWN_MASK), "jumpToBottom");
			splitPane.getActionMap().put("jumpToBottom", new AbstractAction() {
				@Override
				public void actionPerformed(ActionEvent e) {
					((JButton) ((BasicSplitPaneUI) ui).getDivider()
							.getComponent(1)).doClick();
				}
			});
		}
		return splitPane;
	}

}