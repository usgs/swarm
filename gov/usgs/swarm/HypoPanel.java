package gov.usgs.swarm;

import gov.usgs.swarm.database.model.HypoResults;
import gov.usgs.swarm.database.model.Marker;
import gov.usgs.swarm.map.HypoOuputMapFrame;
import gov.usgs.util.ui.ExtensionFileFilter;
import gov.usgs.vdx.calc.Hypo71;
import gov.usgs.vdx.calc.Hypo71.Results;
import gov.usgs.vdx.calc.data.ControlCard;
import gov.usgs.vdx.calc.data.CrustalModel;
import gov.usgs.vdx.calc.data.HypoArchiveOutput;
import gov.usgs.vdx.calc.data.Hypocenter;
import gov.usgs.vdx.calc.data.PhaseRecord;
import gov.usgs.vdx.calc.data.Station;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Panel that is used to do hypo calculation
 * 
 * @author Joel Shellman
 */
public class HypoPanel extends JPanel {

	private JTextField hypoInputPath;
	private JButton browseHypoInputButton;

	private JTextField hypoTotalInputPath;
	private JButton browseHypoTotalInputButton;

	private JButton runHypoButton;

	private JRadioButton inputCheck;
	private JRadioButton archiveCheck;

	LinkedList<Station> stationsList = new LinkedList<Station>();
	LinkedList<CrustalModel> crustalModelList = new LinkedList<CrustalModel>();
	LinkedList<PhaseRecord> phaseRecordsList = new LinkedList<PhaseRecord>();
	ControlCard controlCard;
	HypoArchiveOutput hy;

	public HypoPanel() {
		this.setLayout(new BorderLayout());

		FormLayout layout = new FormLayout(
				"80dlu, 3dlu, right:95dlu, 3dlu, pref:grow",
				"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref");

		DefaultFormBuilder builder = new DefaultFormBuilder(layout);
		builder.setDefaultDialogBorder();

		builder.addSeparator("Hypo", "1,1,5,1,FILL,FILL");

		inputCheck = new JRadioButton("Use Input properties file to run hypo");
		builder.add(inputCheck, "1,3,3,1,FILL,FILL");

		hypoInputPath = new JTextField();
		builder.add(hypoInputPath, "1,5,1,1,FILL,FILL");

		browseHypoInputButton = new JButton("Browse");
		builder.add(browseHypoInputButton, "3,5,1,1,LEFT,FILL");

		archiveCheck = new JRadioButton(
				"Use Input Archived xml file to run hypo");
		builder.add(archiveCheck, "1,7,3,1,FILL,FILL");

		hypoTotalInputPath = new JTextField();
		builder.add(hypoTotalInputPath, "1,9,1,1,FILL,FILL");

		browseHypoTotalInputButton = new JButton("Browse");
		builder.add(browseHypoTotalInputButton, "3,9,1,1,LEFT,FILL");

		runHypoButton = new JButton("Run Hypo");
		builder.add(runHypoButton, "3,11,1,1,LEFT,FILL");

		this.add(builder.getPanel(), BorderLayout.CENTER);

		disableFields();
		inputCheck.setSelected(true);

		inputCheck.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				inputCheck.setSelected(true);
				hypoInputPath.setEnabled(true);
				browseHypoInputButton.setEnabled(true);

				archiveCheck.setSelected(false);
				hypoTotalInputPath.setEnabled(false);
				browseHypoTotalInputButton.setEnabled(false);

			}

		});

		archiveCheck.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				archiveCheck.setSelected(true);
				hypoTotalInputPath.setEnabled(true);
				browseHypoTotalInputButton.setEnabled(true);

				inputCheck.setSelected(false);
				hypoInputPath.setEnabled(false);
				browseHypoInputButton.setEnabled(false);
			}

		});

		runHypoButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				String error = null;
				if (inputCheck.isSelected()) {
					if (hypoInputPath.getText() == null
							|| hypoInputPath.getText().trim().length() == 0) {
						error = "Please select a properties file for input to Hypo";
					} else {
						File f = new File(hypoInputPath.getText());
						if (!f.exists()) {
							error = "properties file does not exist";
						}
					}
				} else {
					if (hypoTotalInputPath.getText() == null
							|| hypoTotalInputPath.getText().trim().length() == 0) {
						error = "Please select an xml file for input archive to Hypo";
					} else {
						File f = new File(hypoTotalInputPath.getText());
						if (!f.exists()) {
							error = "xml file does not exist";
						}
					}
				}

				if (error == null) {
					try {
						generateHypoInputs(inputCheck.isSelected() ? null
								: hypoTotalInputPath.getText());
						if (stationsList.size() == 0) {
							error = "No Station record associated with selected attempt was found in props file";
						} else if (phaseRecordsList.size() == 0) {
							error = "Incomplete Phase record for attempt";
						} else if (crustalModelList.size() == 0) {
							error = "Incomplete crustal model for attempt";
						}
					} catch (Exception ex) {
						ex.printStackTrace();
						error = "Cannot run hypo please ensure input file has correct data";
					}

				}

				if (error == null) {
					try {
						Results hypoResult = null;
						hypoResult = runHypo();

						HypoResults hr = new HypoResults();
						hr.setAdjustmentsOutput(hypoResult
								.getAdjustmentIterations());
						hr.setDeletedStationsList(hypoResult
								.getDeletedStationsList()); 
						hr.setHypocenterOuput(hypoResult.getHypocenterOutput());
						hr.setMissingStationsList(hypoResult
								.getMissingStationsList());
						hr.setPrintOutput(hypoResult.getPrintOutput());
						hr.setPunchOutput(hypoResult.getPunchOutput());
						hr.setStationsResultList(hypoResult
								.getStationsResultList());
						hr.setStats(hypoResult.getStats());
						hr.setSummaryList(hypoResult.getSummaryList());

						if (hr.getHypocenterOuput() != null
								&& hr.getHypocenterOuput().size() > 0) {

							List<Hypocenter> centers = hr.getHypocenterOuput();
							if (centers.size() > 0) {
								Swarm.getSelectedAttempt().setLatitude(
										(double) centers.get(0).getLAT1());
								Swarm.getSelectedAttempt().setLongitude(
										(double) centers.get(0).getLON1());
								Swarm.getSelectedAttempt().setDepth(
										centers.get(0).getZ());

							}
						}

						if (Swarm.getSelectedAttempt() != null) {
							Swarm.getSelectedAttempt()
									.setHypoResultsAsBytes(hr);
							if (archiveCheck.isSelected()) {
								Swarm.getSelectedAttempt()
										.setHypoInputArchiveFilePath(
												hypoTotalInputPath.getText());
							}
							Swarm.getSelectedAttempt().persist();
						}

						if (Swarm.getApplication().getHypoOuputMapFrame() == null) {
							Swarm.getApplication().setHypoOuputMapFrame(
									new HypoOuputMapFrame());
						}

						Swarm.getApplication().getHypoOuputMapFrame().setHy(hy);
						Swarm.getApplication().getHypoOuputMapFrame()
								.setHypoOutput(hypoTotalInputPath.getText());
						Swarm.getApplication().getHypoOuputMapFrame()
								.setResultText(hypoResult.getOutput());
						Swarm.getApplication().getHypoOuputMapFrame()
								.setVisible(true);

					} catch (Exception e1) {
						e1.printStackTrace();
						JOptionPane.showMessageDialog(null,
								"Cannot run hypo, please verify hypo has all neccessary inputs: "
										+ e1.getMessage(), "Error",
								JOptionPane.ERROR_MESSAGE);
					}

				} else {
					JOptionPane.showMessageDialog(null, error, "Error",
							JOptionPane.ERROR_MESSAGE);
				}

			}

		});

		browseHypoInputButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = Swarm.getApplication().getFileChooser();
				chooser.resetChoosableFileFilters();
				ExtensionFileFilter propsExt = new ExtensionFileFilter(
						".properties", "Properties file");
				chooser.addChoosableFileFilter(propsExt);
				chooser.setDialogTitle("Select Hypo Inputs");
				chooser.setFileFilter(chooser.getAcceptAllFileFilter());
				File lastPath = new File(Swarm.config.lastPath);
				chooser.setCurrentDirectory(lastPath);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int result = chooser.showOpenDialog(Swarm.getApplication());
				if (result == JFileChooser.APPROVE_OPTION) {
					File propertiesFile = chooser.getSelectedFile();
					String filePath = propertiesFile.getAbsolutePath();
					if (filePath.endsWith(".properties")) {
						hypoInputPath.setText(filePath);
						if (Swarm.getSelectedAttempt() != null) {
							Swarm.getSelectedAttempt().setHypoInputFilePath(
									filePath);
							Swarm.getSelectedAttempt().persist();
						}
					} else {
						JOptionPane.showMessageDialog(null,
								"Please select a properties file", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}

		});

		browseHypoTotalInputButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {

				JFileChooser chooser = Swarm.getApplication().getFileChooser();
				chooser.resetChoosableFileFilters();
				ExtensionFileFilter propsExt = new ExtensionFileFilter(".xml",
						"Xml file");
				chooser.addChoosableFileFilter(propsExt);
				chooser.setDialogTitle("Select All Hypo Input Archive");
				chooser.setFileFilter(chooser.getAcceptAllFileFilter());
				File lastPath = new File(Swarm.config.lastPath);
				chooser.setCurrentDirectory(lastPath);
				chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int result = chooser.showOpenDialog(Swarm.getApplication());
				if (result == JFileChooser.APPROVE_OPTION) {
					File propertiesFile = chooser.getSelectedFile();
					String filePath = propertiesFile.getAbsolutePath();
					if (filePath.endsWith(".xml")) {
						hypoTotalInputPath.setText(filePath);
					} else {
						JOptionPane.showMessageDialog(null,
								"Please select an xml file", "Error",
								JOptionPane.ERROR_MESSAGE);
					}
				}
			}

		});

	}

	/**
	 * Generates hypo inputs either from an archived file or from objects in
	 * memory
	 */
	private void generateHypoInputs(String archiveFile)
			throws FileNotFoundException, IOException, JAXBException, Exception {
		if (archiveFile == null) {
			Properties props = new Properties();
			props.load(new FileInputStream(hypoInputPath.getText()));
			controlCard = new ControlCard(props);
			loadCrustalModelList();
			List<String> stations = Marker.listStationByAttempt(Swarm
					.getSelectedAttempt().getId());

			loadStationList(stations);
			phaseRecordsList.clear();
			for (String st : stations) {
				List<Marker> pmarkers = Marker.listByStationAndTypeAndAttempt(
						Swarm.getSelectedAttempt().getId(), st,
						Marker.P_MARKER_LABEL);

				List<Marker> smarkers = Marker.listByStationAndTypeAndAttempt(
						Swarm.getSelectedAttempt().getId(), st,
						Marker.S_MARKER_LABEL);

				List<Marker> codaMarkers = Marker
						.listByStationAndTypeAndAttempt(Swarm
								.getSelectedAttempt().getId(), st,
								Marker.CODA_MARKER_LABEL);

				if (pmarkers.size() == 0 && smarkers.size() == 0) {
					System.out
							.println("WARNING: Marker (p or s) not found for station "
									+ st
									+ " using file "
									+ hypoInputPath.getText()
									+ ". Skipping this station.");
					continue;
				}

				if (codaMarkers.size() > 0 && pmarkers.size() == 0) {
					throw new Exception(
							"Must have a p marker with coda marker. No p marker found for coda marker for station: "
									+ st
									+ " using file "
									+ hypoInputPath.getText());
				}

				String prmk = null;
				float pkDate = 0f;
				int pHour = 0;
				int pMin = 0;
				float pSec = 0f;

				String smrk = null;
				float sSec = 0f;

				float timeDiffFromCodaToPInSec = 0;

				if (pmarkers.size() > 0) {
					Marker pMarker = pmarkers.get(0);
					prmk = pMarker.getIp_ep()
							+ (pMarker.getUpDownUnknown().equals("Up") ? "U" : (pMarker.getUpDownUnknown().equals("Down") ? "D" : ""))
							+ (pMarker.getWeight() == null ? "0" : pMarker
									.getWeight().toString());
					Calendar c = Calendar.getInstance();
					c.setTimeInMillis(pMarker.getMarkerTime().getTime());
					int pmonth = c.get(Calendar.MONTH);
					int pyear = c.get(Calendar.YEAR);
					int pday = c.get(Calendar.DAY_OF_MONTH);
					pHour = c.get(Calendar.HOUR_OF_DAY);
					pMin = c.get(Calendar.MINUTE);
					pSec = c.get(Calendar.SECOND) + c.get(Calendar.MILLISECOND)/1000f;

					pkDate = Float.parseFloat(Integer.toString(pyear - 1900)
							+ (pmonth < 10 ? "0" + Integer.toString(pmonth)
									: Integer.toString(pmonth))
							+ (pday < 10 ? "0" + Integer.toString(pday)
									: Integer.toString(pday)));

					if (codaMarkers.size() > 0) {
						Marker codaMarker = codaMarkers.get(0);
						long codaMarkerTime = codaMarker.getMarkerTime()
								.getTime();
						long pMarkerTime = pMarker.getMarkerTime().getTime();
						long timeDiffFromCodaToP = Math.abs(codaMarkerTime
								- pMarkerTime);
						timeDiffFromCodaToPInSec = timeDiffFromCodaToP / 1000;
					}
				}

				if (smarkers.size() > 0) {
					Marker sMarker = smarkers.get(0);
					Calendar c = Calendar.getInstance();
					c.setTimeInMillis(sMarker.getMarkerTime().getTime());

					smrk = sMarker.getIs_es()
							+ sMarker.getUpDownUnknown()
							+ (sMarker.getWeight() == null ? "0" : sMarker
									.getWeight().toString());

					sSec = c.get(Calendar.SECOND) + c.get(Calendar.MILLISECOND)/1000f;
				}

				phaseRecordsList.add(new PhaseRecord(st,
						prmk, // PRMK
						(prmk != null && prmk.length() > 3) ? Float.parseFloat(prmk.substring(3, 4)) : 0,
						(int) pkDate, pMin, pSec,
						sSec, smrk, // SMRK
						0.0f, // WS
						0.0f, // AMX TODO: calc this
						0.0f, // PRX
						0.0f, // CALC
						0.0f, // CALX
						"", // RMK
						0.0f, // DT
						timeDiffFromCodaToPInSec, // FMP
						"", // "1.22",
						'D', smrk != null ? "1" : "", "", // "SR01IPD0 691005120651.22",
						' ', "" // "IPD0"
				));
			}
			PhaseRecord lastRecordIndicator = new PhaseRecord();
			lastRecordIndicator.setMSTA("");
			phaseRecordsList.add(lastRecordIndicator);
		} else {
			File file = new File(archiveFile);
			JAXBContext jaxbContext = JAXBContext
					.newInstance(HypoArchiveOutput.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
			HypoArchiveOutput h = (HypoArchiveOutput) jaxbUnmarshaller
					.unmarshal(file);

			controlCard = h.getControlCard();
			crustalModelList = h.getCrustalModel();
			phaseRecordsList = h.getPhaseRecords();
			stationsList = h.getStations();
		}
	}

	/**
	 * 
	 * Enable fields required for hypo calculation
	 * 
	 */
	public void enableFields() {
		runHypoButton.setEnabled(true);
		if (inputCheck.isSelected()) {
			hypoInputPath.setEnabled(true);
			browseHypoInputButton.setEnabled(true);
			hypoTotalInputPath.setEnabled(false);
			browseHypoTotalInputButton.setEnabled(false);
		} else {
			hypoInputPath.setEnabled(false);
			browseHypoInputButton.setEnabled(false);
			hypoTotalInputPath.setEnabled(true);
			browseHypoTotalInputButton.setEnabled(true);
		}

		if (Swarm.getSelectedAttempt() != null) {
			if (Swarm.getSelectedAttempt().getHypoInputFilePath() != null
					&& !Swarm.getSelectedAttempt().getHypoInputFilePath()
							.isEmpty()) {
				hypoInputPath.setText(Swarm.getSelectedAttempt()
						.getHypoInputFilePath());
			}

			if (Swarm.getSelectedAttempt().getHypoInputArchiveFilePath() != null
					&& !Swarm.getSelectedAttempt()
							.getHypoInputArchiveFilePath().isEmpty()) {
				hypoTotalInputPath.setText(Swarm.getSelectedAttempt()
						.getHypoInputArchiveFilePath());
			}
		}
	}

	/**
	 * Disable all fields. This is required when all inputs to hypo calculation
	 * is not complete
	 */
	public void disableFields() {
		hypoInputPath.setEnabled(false);
		browseHypoInputButton.setEnabled(false);
		hypoInputPath.setText("");
		runHypoButton.setEnabled(false);
		hypoTotalInputPath.setEnabled(false);
		browseHypoTotalInputButton.setEnabled(false);
		hypoTotalInputPath.setText("");
	}

	/**
	 * Runs hypo with all the set hypo inputs.
	 * 
	 * @return
	 * @throws IOException
	 * @throws ParseException
	 */
	private Results runHypo() throws IOException, ParseException {
		Hypo71 hypoCalculator = new Hypo71();

		hy = new HypoArchiveOutput();
		hy.setControlCard(controlCard);
		for (PhaseRecord o : phaseRecordsList) {
			hy.getPhaseRecords().add(o);
		}

		for (Station o : stationsList) {
			hy.getStations().add(o);
		}

		for (CrustalModel o : crustalModelList) {
			hy.getCrustalModel().add(o);
		}

		hypoCalculator.calculateHypo71("", null, stationsList,
				crustalModelList, controlCard, phaseRecordsList, null);
		Results result = hypoCalculator.getResults();

		return result;

	}

	/**
	 * Loads the station record hypo input using the specified list of strings
	 * 
	 * @param stations
	 *            : List of strings holding station codes
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void loadStationList(List<String> stations)
			throws FileNotFoundException, IOException {
		stationsList.clear();
		List<Integer> indexes = getIndexes("station");
		Properties props = new Properties();
		props.load(new FileInputStream(hypoInputPath.getText()));

		if (indexes.size() > 0) {
			for (Integer index : indexes) {
				Station st = new Station();
				if (props.get("station[" + index + "].IW") != null
						&& !props.get("station[" + index + "].IW").toString()
								.isEmpty())
					st.setIW(props.get("station[" + index + "].IW").toString()
							.charAt(0));
				if (props.get("station[" + index + "].NSTA") != null
						&& !props.get("station[" + index + "].NSTA").toString()
								.isEmpty())
					st.setNSTA(props.get("station[" + index + "].NSTA")
							.toString());
				if (props.get("station[" + index + "].LAT1") != null
						&& !props.get("station[" + index + "].LAT1").toString()
								.isEmpty())
					st.setLAT1(Integer.parseInt((String) props.get("station["
							+ index + "].LAT1")));
				if (props.get("station[" + index + "].LAT2") != null
						&& !props.get("station[" + index + "].LAT2").toString()
								.isEmpty())
					st.setLAT2(Float.parseFloat((String) props.get("station["
							+ index + "].LAT2")));
				if (props.get("station[" + index + "].INS") != null
						&& !props.get("station[" + index + "].INS").toString()
								.isEmpty())
					st.setINS(props.get("station[" + index + "].INS")
							.toString().charAt(0));
				if (props.get("station[" + index + "].LON1") != null
						&& !props.get("station[" + index + "].LON1").toString()
								.isEmpty())
					st.setLON1(Integer.parseInt((String) props.get("station["
							+ index + "].LON1")));
				if (props.get("station[" + index + "].LON2") != null
						&& !props.get("station[" + index + "].LON2").toString()
								.isEmpty())
					st.setLON2(Float.parseFloat((String) props.get("station["
							+ index + "].LON2")));
				if (props.get("station[" + index + "].IEW") != null
						&& !props.get("station[" + index + "].IEW").toString()
								.isEmpty())
					st.setIEW(props.get("station[" + index + "].IEW")
							.toString().charAt(0));
				if (props.get("station[" + index + "].IELV") != null
						&& !props.get("station[" + index + "].IELV").toString()
								.isEmpty())
					st.setIELV(Integer.parseInt((String) props.get("station["
							+ index + "].IELV")));
				if (props.get("station[" + index + "].dly") != null
						&& !props.get("station[" + index + "].dly").toString()
								.isEmpty())
					st.setDly(Float.parseFloat((String) props.get("station["
							+ index + "].dly")));
				if (props.get("station[" + index + "].FMGC") != null
						&& !props.get("station[" + index + "].FMGC").toString()
								.isEmpty())
					st.setFMGC(Float.parseFloat((String) props.get("station["
							+ index + "].FMGC")));
				if (props.get("station[" + index + "].XMGC") != null
						&& !props.get("station[" + index + "].XMGC").toString()
								.isEmpty())
					st.setXMGC(Float.parseFloat((String) props.get("station["
							+ index + "].XMGC")));
				if (props.get("station[" + index + "].KLAS") != null
						&& !props.get("station[" + index + "].KLAS").toString()
								.isEmpty())
					st.setKLAS(Integer.parseInt((String) props.get("station["
							+ index + "].KLAS")));
				if (props.get("station[" + index + "].PRR") != null
						&& !props.get("station[" + index + "].PRR").toString()
								.isEmpty())
					st.setPRR(Float.parseFloat((String) props.get("station["
							+ index + "].PRR")));
				if (props.get("station[" + index + "].CALR") != null
						&& !props.get("station[" + index + "].CALR").toString()
								.isEmpty())
					st.setCALR(Float.parseFloat((String) props.get("station["
							+ index + "].CALR")));
				if (props.get("station[" + index + "].ICAL") != null
						&& !props.get("station[" + index + "].ICAL").toString()
								.isEmpty())
					st.setICAL(Integer.parseInt((String) props.get("station["
							+ index + "].ICAL")));
				if (props.get("station[" + index + "].NDATE") != null
						&& !props.get("station[" + index + "].NDATE")
								.toString().isEmpty())
					st.setNDATE(Integer.parseInt((String) props.get("station["
							+ index + "].NDATE")));
				if (props.get("station[" + index + "].NHRMN") != null
						&& !props.get("station[" + index + "].NHRMN")
								.toString().isEmpty())
					st.setNHRMN(Integer.parseInt((String) props.get("station["
							+ index + "].NHRMN")));
				if (stations.contains(st.getNSTA())) {
					stationsList.add(st);
				}
			}
		}
	}

	/**
	 * Loads the crustal model list from the input properties file to hypo
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void loadCrustalModelList() throws FileNotFoundException,
			IOException {
		crustalModelList.clear();
		List<Integer> indexes = getIndexes("crustal");
		Properties props = new Properties();
		props.load(new FileInputStream(hypoInputPath.getText()));

		if (indexes.size() > 0) {
			for (Integer index : indexes) {
				CrustalModel cm = new CrustalModel();
				if (props.get("crustal[" + index + "].V") != null
						&& !props.get("crustal[" + index + "].V").toString()
								.isEmpty())
					cm.setV(Float.parseFloat(props.get(
							"crustal[" + index + "].V").toString()));
				if (props.get("crustal[" + index + "].D") != null
						&& !props.get("crustal[" + index + "].D").toString()
								.isEmpty())
					cm.setD(Float.parseFloat(props.get(
							"crustal[" + index + "].D").toString()));
				crustalModelList.add(cm);
			}
		}
	}

	/**
	 * 
	 * Gets all indexes of a particular object list definition in the hypo input
	 * property file for key values that are represented as list of objects. <br />
	 * <br />
	 * An example is : <b>object[index].property = value</b> <br />
	 * <br />
	 * This function returns all index of that list of object
	 * 
	 * @param property
	 *            : Object definition
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private List<Integer> getIndexes(String property)
			throws FileNotFoundException, IOException {
		Properties props = new Properties();
		props.load(new FileInputStream(hypoInputPath.getText()));
		Set<Object> keys = props.keySet();
		List<Integer> indexes = new ArrayList<Integer>();

		for (Object k : keys) {
			String keyAsString = k.toString();
			if (keyAsString.toLowerCase().startsWith(property)) {
				try {
					int index = Integer.parseInt(keyAsString.substring(
							property.length() + 1, keyAsString.indexOf(']')));
					if (!indexes.contains(index)) {
						indexes.add(index);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		Collections.sort(indexes);
		return indexes;
	}
}
