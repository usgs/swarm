package gov.usgs.swarm.map;

import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.database.model.HypoResults;
import gov.usgs.swarm.wave.WaveViewPanel;
import gov.usgs.util.ui.ExtensionFileFilter;
import gov.usgs.vdx.calc.data.HypoArchiveOutput;
import gov.usgs.vdx.calc.data.Hypocenter;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A Jframe that holds the Hypooutput Map panel
 * 
 * @author Chirag Patel
 */
public class HypoOuputMapFrame extends JFrame {

	private JPanel mainPanel;

	private Throbber throbber;

	JTextArea dataArea;

	private Border border;

	private JTextField hypoOutput;
	private JButton hypoSaveButton;

	HypoArchiveOutput hy;

	public HypoOuputMapFrame() {
		setTitle("Hypo Output");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		createUI();
	}

	private void createUI() {
		setSize(550, 530);
		throbber = new Throbber();

		mainPanel = new JPanel(new BorderLayout());

		dataArea = new JTextArea();
		dataArea.setText("");
		dataArea.setEditable(false);
		dataArea.setFont(Font.getFont(Font.MONOSPACED));
		JScrollPane scrollPane = new JScrollPane(dataArea);

		JPanel jp = new JPanel();

		jp.setSize(mainPanel.getWidth(), 30);

		hypoSaveButton = new JButton("Save All Hypo Inputs To A file");
		hypoOutput = new JTextField(12);
		jp.add(hypoOutput);
		jp.add(hypoSaveButton);

		mainPanel.add(scrollPane, BorderLayout.CENTER);
		mainPanel.add(jp, BorderLayout.SOUTH);

		setContentPane(mainPanel);

		hypoSaveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = getCustomFileChooser();
				int result = chooser.showSaveDialog(Swarm.getApplication());
				if (result == JFileChooser.APPROVE_OPTION) {
					File propertiesFile = chooser.getSelectedFile();
					String filePath = propertiesFile.getAbsolutePath();
					if (!filePath.endsWith(".xml")) {
						filePath = filePath + ".xml";
					}

					hypoOutput.setText(filePath);
					if (Swarm.getSelectedAttempt() != null) {
						Swarm.getSelectedAttempt().setHypoInputArchiveFilePath(
								filePath);
						Swarm.getSelectedAttempt().persist();
					}
					try {
						File file = new File(filePath);
						JAXBContext jaxbContext = JAXBContext
								.newInstance(HypoArchiveOutput.class);
						Marshaller jaxbMarshaller = jaxbContext
								.createMarshaller();
						jaxbMarshaller.setProperty(
								Marshaller.JAXB_FORMATTED_OUTPUT, true);
						jaxbMarshaller.marshal(hy, file);
					} catch (PropertyException e1) {
						e1.printStackTrace();
					} catch (JAXBException e1) {
						e1.printStackTrace();
					}
				}
			}
		});

		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentShown(ComponentEvent e) {
				// if (!mapPanel.imageValid())
				// mapPanel.resetImage();
			}
		});
	}

	private List<ClickableGeoLabel> centersToMarkers(List<Hypocenter> centers) {
		List<ClickableGeoLabel> result = new ArrayList<ClickableGeoLabel>();
		for (Hypocenter h : centers) {
			gov.usgs.swarm.map.Hypocenter marker = new gov.usgs.swarm.map.Hypocenter();
			double lat = h.getLAT1() + (h.getLAT2() / 60.0);
			double lon = h.getLON1() + (h.getLON2() / 60.0);
			marker.text = WaveViewPanel.roundTo(lat, 1000) + "," + WaveViewPanel.roundTo(lon, 1000);
			marker.location = new Point2D.Double(lon, lat);
			result.add(marker);
		}
		return result;
	}

	private JFileChooser getCustomFileChooser() {
		JFileChooser customFileChoser = new JFileChooser() {
			@Override
			public void approveSelection() {
				File f = getSelectedFile();
				if (f.exists() && getDialogType() == SAVE_DIALOG) {

					if (!f.getAbsolutePath().endsWith(".xml")) {
						JOptionPane.showMessageDialog(null,
								"Please select an xml file", "Error",
								JOptionPane.ERROR_MESSAGE);
						return;
					} else {
						int result = JOptionPane.showConfirmDialog(this,
								"The file exists, overwrite?", "Existing file",
								JOptionPane.YES_NO_CANCEL_OPTION);
						switch (result) {
						case JOptionPane.YES_OPTION:
							super.approveSelection();
							return;
						case JOptionPane.NO_OPTION:
							return;
						case JOptionPane.CLOSED_OPTION:
							return;
						case JOptionPane.CANCEL_OPTION:
							cancelSelection();
							return;
						}
					}

				}
				super.approveSelection();
			}
		};

		customFileChoser.resetChoosableFileFilters();
		ExtensionFileFilter propsExt = new ExtensionFileFilter(".xml",
				"Xml file");
		customFileChoser.addChoosableFileFilter(propsExt);
		customFileChoser.setDialogTitle("Save All Hypo Inputs To xml File");
		customFileChoser.setFileFilter(customFileChoser
				.getAcceptAllFileFilter());
		File lastPath = new File(Swarm.config.lastPath);
		customFileChoser.setCurrentDirectory(lastPath);
		customFileChoser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		return customFileChoser;
	}

	public void setStatusText(final String t) {

	}

	public void setResultText(String text) {
		dataArea.setText(text);
	}

	public Throbber getThrobber() {
		return throbber;
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

	public HypoArchiveOutput getHy() {
		return hy;
	}

	public void setHy(HypoArchiveOutput hy) {
		this.hy = hy;
		
		HypoResults results = Swarm.getSelectedAttempt().getHypoResultsAsObject();
		List<Hypocenter> centers = results.getHypocenterOuput();

		// Test location
//		if (centers.size() == 0) {
//			gov.usgs.swarm.map.Hypocenter h1 = new gov.usgs.swarm.map.Hypocenter();
//			h1.text = "0.00,0.00";
//			h1.location = new Point2D.Double(0, 0);
//
//			MapFrame mapFrame = Swarm.getApplication().getMapFrame();
//			MapPanel mapPanel = mapFrame.getMapPanel();
//			mapFrame.setVisible(true);
//			mapFrame.moveToFront();
//
//			List<ClickableGeoLabel> markers = Arrays.asList((ClickableGeoLabel)h1);
//			mapPanel.addMarkers(markers);
//			mapPanel.moveToMarker(markers.get(0));
//		}
		
		if (centers != null && centers.size() > 0) {
			MapFrame mapFrame = Swarm.getApplication().getMapFrame();
			MapPanel mapPanel = mapFrame.getMapPanel();
			mapFrame.setVisible(true);
			mapFrame.moveToFront();

			List<ClickableGeoLabel> markers = centersToMarkers(centers);
			mapPanel.addMarkers(markers);
			mapPanel.moveToMarker(markers.get(0));
		}
	}

	public void setHypoOutput(String text) {
		hypoOutput.setText(text);
	}
}