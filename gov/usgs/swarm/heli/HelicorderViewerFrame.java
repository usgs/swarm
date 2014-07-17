package gov.usgs.swarm.heli;

import gov.usgs.plot.Plot;
import gov.usgs.plot.PlotException;
import gov.usgs.plot.data.HelicorderData;
import gov.usgs.plot.data.Wave;
import gov.usgs.plot.render.HelicorderRenderer;
import gov.usgs.swarm.Icons;
import gov.usgs.swarm.Kioskable;
import gov.usgs.swarm.Swarm;
import gov.usgs.swarm.SwarmFrame;
import gov.usgs.swarm.SwarmUtil;
import gov.usgs.swarm.SwingWorker;
import gov.usgs.swarm.Throbber;
import gov.usgs.swarm.TimeListener;
import gov.usgs.swarm.WaveViewTime;
import gov.usgs.swarm.chooser.DataChooser;
import gov.usgs.swarm.data.GulperListener;
import gov.usgs.swarm.data.SeismicDataSource;
import gov.usgs.swarm.data.SeismicDataSourceListener;
import gov.usgs.swarm.map.MapFrame;
import gov.usgs.swarm.wave.WaveClipboardFrame;
import gov.usgs.swarm.wave.WaveViewSettings;
import gov.usgs.swarm.wave.WaveViewSettingsToolbar;
import gov.usgs.util.ConfigFile;
import gov.usgs.util.CurrentTime;
import gov.usgs.util.GridBagHelper;
import gov.usgs.util.Util;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

/**
 * <code>JInternalFrame</code> that holds a helicorder.
 * 
 * @author Dan Cervelli
 */
public class HelicorderViewerFrame extends SwarmFrame implements Kioskable {
	public static final long serialVersionUID = -1;

	// minutes * 60 = seconds
	public static final int[] chunkValues = new int[] { 10 * 60, 15 * 60, 20 * 60, 30 * 60, 60 * 60, 120 * 60,
			180 * 60, 360 * 60 };

	// hours * 60 = minutes
	public static final int[] spanValues = new int[] { 2 * 60, 4 * 60, 6 * 60, 12 * 60, 24 * 60, 48 * 60, 72 * 60,
			96 * 60, 120 * 60, 144 * 60, 168 * 60, 192 * 60, 216 * 60, 240 * 60, 264 * 60, 288 * 60, 312 * 60, 336 * 60 };

	// seconds
	public static final int[] zoomValues = new int[] { 1, 2, 5, 10, 20, 30, 60, 120, 300, 600 };

	private RefreshThread refreshThread;
	private SeismicDataSource dataSource;
	private JPanel mainPanel;
	private JToolBar toolBar;
	private JToggleButton pinButton;
	private JButton settingsButton;
	private JButton backButton;
	private JButton forwardButton;
	private JButton compX;
	private JButton expX;
	private JButton compY;
	private JButton expY;
	private JButton clipboard;
	private JButton removeWave;
	private JButton capture;
	private JFileChooser chooser;
	private JButton scaleButton;
	private boolean scaleClipState;
	protected JToggleButton autoScaleSliderButton;
	protected int autoScaleSliderButtonState;
	protected JSlider autoScaleSlider;

	private HelicorderViewPanel helicorderViewPanel;

	private WaveViewSettings waveViewSettings;
	private HelicorderViewerSettings settings;

	private boolean gulperWorking;
	private boolean working;
	private JLabel statusLabel;

	private JPanel heliPanel;

	protected long lastRefreshTime;

	private Border border;
	private Border thinBorder;

	protected Throbber throbber;
	protected JProgressBar progressBar;

	private boolean noData = false;

	private TimeListener timeListener;

	public GulperListener gulperListener;

	private SeismicDataSourceListener dataListener;

	public HelicorderViewerFrame(ConfigFile cf) {
		super("<layout>", true, true, true, true);
		Swarm.getApplication().touchUITime();
		String channel = cf.getString("channel");
		SeismicDataSource sds = swarmConfig.getSource(cf.getString("source"));
		dataSource = sds.getCopy();
		setTitle(channel + ", [" + dataSource + "]");
		settings = new HelicorderViewerSettings(channel);
		settings.set(cf);
		waveViewSettings = new WaveViewSettings();
		waveViewSettings.set(cf.getSubConfig("wave"));

		createUI();
		boolean pinned = Boolean.parseBoolean(cf.getString("pinned"));
		setPinned(pinned);
		processStandardLayout(cf);
		setVisible(true);
		getHelicorder();
		refreshThread = new RefreshThread();
	}

	public HelicorderViewerFrame(SeismicDataSource sds, String ch, double bt) {
		super(ch + ", [" + sds + "]", true, true, true, true);
		Swarm.getApplication().touchUITime();
		settings = new HelicorderViewerSettings(ch);
		settings.setBottomTime(bt);
		waveViewSettings = new WaveViewSettings();
		dataSource = sds.getCopy();

		createUI();
		setVisible(true);
		getHelicorder();
		refreshThread = new RefreshThread();
	}

	public void saveLayout(ConfigFile cf, String prefix) {
		super.saveLayout(cf, prefix);
		cf.put("helicorder", prefix);
		cf.put(prefix + ".source", dataSource.getName());
		cf.put(prefix + ".pinned", Boolean.toString(pinButton.isSelected()));
		settings.save(cf, prefix);
		waveViewSettings.save(cf, prefix + ".wave");
	}

	public void createUI() {
		mainPanel = new JPanel(new BorderLayout());
		createHeliPanel();
		createToolBar();
		createStatusLabel();
		createListeners();

		setFrameIcon(Icons.heli);
		setDefaultCloseOperation(JInternalFrame.DISPOSE_ON_CLOSE);
		setSize(800, 750);
		setContentPane(mainPanel);
	}

	public void addLinkListeners() {
		helicorderViewPanel.addListener(WaveClipboardFrame.getInstance().getLinkListener());
		helicorderViewPanel.addListener(MapFrame.getInstance().getLinkListener());
	}

	private void createStatusLabel() {
		JPanel statusPanel = new JPanel();
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
		statusLabel = new JLabel(" ");
		statusLabel.setBorder(BorderFactory.createEmptyBorder(1, 5, 0, 0));
		statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 1, 3));
		statusPanel.add(statusLabel);
		statusPanel.add(Box.createHorizontalGlue());
		progressBar = new JProgressBar(0, 100);
		progressBar.setVisible(false);
		progressBar.setStringPainted(true);
		progressBar.setPreferredSize(new Dimension(100, 15));
		progressBar.setSize(new Dimension(100, 15));
		progressBar.setMaximumSize(new Dimension(100, 15));
		statusPanel.add(progressBar);
		mainPanel.add(statusPanel, BorderLayout.SOUTH);
	}

	private void createHeliPanel() {
		helicorderViewPanel = new HelicorderViewPanel(this);
		settings.view = helicorderViewPanel;
		heliPanel = new JPanel(new BorderLayout());
		thinBorder = LineBorder.createGrayLineBorder();
		border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(0, 2, 0, 3), thinBorder);

		heliPanel.setBorder(border);
		heliPanel.add(helicorderViewPanel, BorderLayout.CENTER);
		mainPanel.add(heliPanel, BorderLayout.CENTER);
	}

	private void createToolBar() {
		toolBar = SwarmUtil.createToolBar();

		pinButton = SwarmUtil.createToolBarToggleButton(Icons.pin, "Helicorder always on top", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setPinned(pinButton.isSelected());
			}
		});
		toolBar.add(pinButton);

		settingsButton = SwarmUtil.createToolBarButton(Icons.settings, "Helicorder view settings",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						HelicorderViewerSettingsDialog hvsd = HelicorderViewerSettingsDialog.getInstance(settings,
								waveViewSettings);
						hvsd.setVisible(true);
						noData = false;
						getHelicorder();
					}
				});
		toolBar.add(settingsButton);

		toolBar.addSeparator();

		backButton = SwarmUtil.createToolBarButton(Icons.left, "Scroll back time (A or Left arrow)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (helicorderViewPanel.hasInset())
							helicorderViewPanel.moveInset(-1);
						else
							scroll(-1);
					}
				});
		Util.mapKeyStrokeToButton(this, "LEFT", "backward1", backButton);
		Util.mapKeyStrokeToButton(this, "A", "backward2", backButton);
		toolBar.add(backButton);

		forwardButton = SwarmUtil.createToolBarButton(Icons.right, "Scroll forward time (Z or Right arrow)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (helicorderViewPanel.hasInset())
							helicorderViewPanel.moveInset(1);
						else
							scroll(1);
					}
				});
		Util.mapKeyStrokeToButton(this, "RIGHT", "forward1", forwardButton);
		Util.mapKeyStrokeToButton(this, "Z", "forward2", forwardButton);
		toolBar.add(forwardButton);

		compX = SwarmUtil.createToolBarButton(Icons.xminus, "Compress X-axis (Alt-left arrow)", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				decXAxis();
				getHelicorder();
			}
		});
		Util.mapKeyStrokeToButton(this, "alt LEFT", "compx", compX);
		toolBar.add(compX);

		expX = SwarmUtil.createToolBarButton(Icons.xplus, "Expand X-axis (Alt-right arrow)", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				incXAxis();
				getHelicorder();
			}
		});
		Util.mapKeyStrokeToButton(this, "alt RIGHT", "expx", expX);
		toolBar.add(expX);

		compY = SwarmUtil.createToolBarButton(Icons.yminus, "Compress Y-axis (Alt-down arrow)", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				decYAxis();
				getHelicorder();
			}
		});
		Util.mapKeyStrokeToButton(this, "alt DOWN", "compy", compY);
		toolBar.add(compY);

		expY = SwarmUtil.createToolBarButton(Icons.yplus, "Expand Y-axis (Alt-up arrow)", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				incYAxis();
				getHelicorder();
			}
		});
		Util.mapKeyStrokeToButton(this, "alt UP", "expy", expY);
		toolBar.add(expY);

		toolBar.addSeparator();

		JButton addZoom = SwarmUtil.createToolBarButton(Icons.zoomplus, "Decrease zoom time window (+)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						decZoom();
						settings.notifyView();
					}
				});
		Util.mapKeyStrokeToButton(this, "EQUALS", "addzoom1", addZoom);
		Util.mapKeyStrokeToButton(this, "shift EQUALS", "addzoom2", addZoom);
		toolBar.add(addZoom);

		JButton subZoom = SwarmUtil.createToolBarButton(Icons.zoomminus, "Increase zoom time window (-)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						incZoom();
						settings.notifyView();
					}
				});
		Util.mapKeyStrokeToButton(this, "MINUS", "subzoom", subZoom);
		toolBar.add(subZoom);

		new WaveViewSettingsToolbar(waveViewSettings, toolBar, this);

		clipboard = SwarmUtil.createToolBarButton(Icons.clipboard, "Copy inset to clipboard (C or Ctrl-C)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						helicorderViewPanel.insetToClipboard();
					}
				});
		clipboard.setEnabled(false);
		Util.mapKeyStrokeToButton(this, "control C", "clipboard1", clipboard);
		Util.mapKeyStrokeToButton(this, "C", "clipboard2", clipboard);
		toolBar.add(clipboard);

		removeWave = SwarmUtil.createToolBarButton(Icons.delete, "Remove inset wave (Delete or Escape)",
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						helicorderViewPanel.removeWaveInset();
					}
				});
		removeWave.setEnabled(false);
		Util.mapKeyStrokeToButton(this, "ESCAPE", "removewave", removeWave);
		Util.mapKeyStrokeToButton(this, "DELETE", "removewave", removeWave);
		toolBar.add(removeWave);

		toolBar.addSeparator();

		capture = SwarmUtil.createToolBarButton(Icons.camera, "Save helicorder image (P)", new CaptureActionListener());
		Util.mapKeyStrokeToButton(this, "P", "capture", capture);
		toolBar.add(capture);

		toolBar.addSeparator();

		scaleButton = SwarmUtil.createToolBarButton(Icons.wavezoom,
				"Toggle between adjusting helicoder scale and clip", new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						scaleClipState = !scaleClipState;
						if (scaleClipState) {
							autoScaleSlider.setValue(40 - (new Double(settings.barMult * 4).intValue()));
							scaleButton.setIcon(Icons.waveclip);
							autoScaleSlider.setToolTipText("Adjust helicorder clip");
						} else {
							autoScaleSlider.setValue(settings.clipBars / 3);
							scaleButton.setIcon(Icons.wavezoom);
							autoScaleSlider.setToolTipText("Adjust helicorder scale");
						}

						settings.notifyView();
					}
				});
		scaleButton.setSelected(true);
		toolBar.add(scaleButton);

		autoScaleSlider = new JSlider(1, 39, (int) (10 - settings.barMult) * 4);
		autoScaleSlider.setToolTipText("Adjust helicorder scale");
		autoScaleSlider.setFocusable(false);
		autoScaleSlider.setPreferredSize(new Dimension(80, 20));
		autoScaleSlider.setMaximumSize(new Dimension(80, 20));
		autoScaleSlider.setMinimumSize(new Dimension(80, 20));
		autoScaleSlider.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				settings.autoScale = true;
				if (!autoScaleSlider.getValueIsAdjusting()) {
					if (scaleClipState)
						settings.clipBars = autoScaleSlider.getValue() * 3;
					else
						settings.barMult = 10 - (new Integer(autoScaleSlider.getValue()).doubleValue() / 4);
					repaintHelicorder();
				}
			}
		});
		toolBar.add(autoScaleSlider);

		toolBar.add(Box.createHorizontalGlue());
		throbber = new Throbber();
		toolBar.add(throbber);
		mainPanel.add(toolBar, BorderLayout.NORTH);
	}

	private void createListeners() {
		timeListener = new TimeListener() {
			public void timeChanged(double j2k) {
				helicorderViewPanel.setCursorMark(j2k);
			}
		};
		WaveViewTime.addTimeListener(timeListener);

		this.addInternalFrameListener(new InternalFrameAdapter() {
			public void internalFrameActivated(InternalFrameEvent e) {
				if (settings.channel != null)
					DataChooser.getInstance().setNearest(settings.channel);
			}

			public void internalFrameClosing(InternalFrameEvent e) {
				dispose();
				throbber.close();
				refreshThread.kill();
				Swarm.getApplication().removeInternalFrame(HelicorderViewerFrame.this);
				WaveViewTime.removeTimeListener(timeListener);
				dataSource.notifyDataNotNeeded(settings.channel, helicorderViewPanel.getStartTime(),
						helicorderViewPanel.getEndTime(), gulperListener);
				dataSource.close();
			}

			public void internalFrameDeiconified(InternalFrameEvent e) {
				helicorderViewPanel.setResized(true);
				repaintHelicorder();
				repaint();
			}
		});

		this.addComponentListener(new ComponentAdapter() {
			public void componentResized(ComponentEvent e) {
				if (getWidth() < 530)
					helicorderViewPanel.setMinimal(true);
				else
					helicorderViewPanel.setMinimal(false);

				helicorderViewPanel.setResized(true);
				repaintHelicorder();
				repaint();
			}
		});

		gulperListener = new GulperListener() {
			public void gulperStarted() {
				gulperWorking = true;
				throbber.increment();
			}

			public void gulperStopped(boolean killed) {
				if (killed)
					noData = true;
				else {
					gulperWorking = false;
					throbber.decrement();
					HelicorderData hd = helicorderViewPanel.getData();
					if (hd == null || hd.rows() == 0)
						noData = true;
					repaintHelicorder();
				}
			}

			public void gulperGulped(double t1, double t2, boolean success) {
				if (success)
					getHelicorder();
			}
		};

		dataListener = new SeismicDataSourceListener() {

			public void channelsProgress(String id, double progress) {
			}

			public void channelsUpdated() {
			}

			public void helicorderProgress(String channel, final double progress) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						if (!progressBar.isVisible())
							progressBar.setVisible(true);

						if (progress == -1) {
							progressBar.setIndeterminate(true);
							progressBar.setString("Waiting for server");
						} else if (progress >= 0.0 && progress < 1.0) {
							progressBar.setIndeterminate(false);
							progressBar.setValue((int) (progress * 100));
							progressBar.setString("Downloading");
						}

						if (progress == 1.0)
							progressBar.setVisible(false);
					}
				});

			}
		};
		dataSource.addListener(dataListener);
	}

	public HelicorderViewPanel getHelicorderViewPanel() {
		return helicorderViewPanel;
	}

	public void setPinned(boolean b) {
		pinButton.setSelected(b);
		int layer = b ? JLayeredPane.MODAL_LAYER : JLayeredPane.DEFAULT_LAYER;
		Swarm.getApplication().setFrameLayer(HelicorderViewerFrame.this, layer);
	}

	public void incXAxis() {
		int index = SwarmUtil.linearSearch(chunkValues, settings.timeChunk);
		if (index == -1 || index == chunkValues.length - 1)
			return;
		settings.timeChunk = chunkValues[index + 1];
	}

	public void decXAxis() {
		int index = SwarmUtil.linearSearch(chunkValues, settings.timeChunk);
		if (index == -1 || index == 0)
			return;
		settings.timeChunk = chunkValues[index - 1];
	}

	public void incYAxis() {
		int index = SwarmUtil.linearSearch(spanValues, settings.span);
		if (index == -1 || index == spanValues.length - 1)
			return;
		settings.span = spanValues[index + 1];
	}

	public void decYAxis() {
		int index = SwarmUtil.linearSearch(spanValues, settings.span);
		if (index == -1 || index == 0)
			return;
		settings.span = spanValues[index - 1];
	}

	public void incZoom() {
		int index = SwarmUtil.linearSearch(zoomValues, settings.waveZoomOffset);
		if (index == -1 || index == zoomValues.length - 1)
			return;
		settings.waveZoomOffset = zoomValues[index + 1];
	}

	public void decZoom() {
		int index = SwarmUtil.linearSearch(zoomValues, settings.waveZoomOffset);
		if (index == -1 || index == 0)
			return;
		settings.waveZoomOffset = zoomValues[index - 1];
	}

	public void setInsetButtonsEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				clipboard.setEnabled(b);
				removeWave.setEnabled(b);
			}
		});
	}

	public void setNavigationButtonsEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				compX.setEnabled(b);
				expX.setEnabled(b);
				forwardButton.setEnabled(b);
				backButton.setEnabled(b);
				compY.setEnabled(b);
				expY.setEnabled(b);
				capture.setEnabled(b);
			}
		});
	}

	public void setKioskMode(boolean b) {
		super.setDefaultKioskMode(b);
		helicorderViewPanel.setFullScreen(fullScreen);
		if (fullScreen) {
			mainPanel.remove(toolBar);
			heliPanel.setBorder(null);
		} else {
			mainPanel.add(toolBar, BorderLayout.NORTH);
			heliPanel.setBorder(border);
		}
		if (helicorderViewPanel != null)
			helicorderViewPanel.requestFocus();
	}

	public void setStatus(final String status) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				statusLabel.setText(status);
			}
		});
	}

	public Throbber getThrobber() {
		return throbber;
	}

	public JLabel getStatusLabel() {
		return statusLabel;
	}

	public WaveViewSettings getWaveViewSettings() {
		return waveViewSettings;
	}

	public HelicorderViewerSettings getHelicorderViewerSettings() {
		return settings;
	}

	public void repaintHelicorder() {
		helicorderViewPanel.invalidateImage();
	}

	public void scroll(int units) {
		double bt = settings.getBottomTime();
		if (Double.isNaN(bt))
			bt = CurrentTime.getInstance().nowJ2K();

		settings.setBottomTime(bt + units * settings.scrollSize * settings.timeChunk);
		getHelicorder();
	}

	public boolean isWorking() {
		return working || gulperWorking;
	}

	public void getHelicorder() {
		if (noData)
			return;

		final SwingWorker worker = new SwingWorker() {
			private double end;
			private double before;
			private HelicorderData hd;
			private boolean success = false;

			public Object construct() {
				try {
					setNavigationButtonsEnabled(false);
					throbber.increment();
					working = true;
					end = settings.getBottomTime();
					if (Double.isNaN(end))
						end = CurrentTime.getInstance().nowJ2K();

					before = end - settings.span * 60;
					int tc = 30;
					if (helicorderViewPanel != null)
						tc = settings.timeChunk;

					if (!HelicorderViewerFrame.this.isClosed) {
						hd = dataSource.getHelicorder(settings.channel, before - tc, end + tc, gulperListener);
						success = true;
					} else {
						success = false;
					}
				} catch (Throwable e) {
					e.printStackTrace();
					System.err.println("getHelicorder() Error: " + e.getMessage());
				} finally {
					working = false;
				}
				return null;
			}

			public void finished() {
				lastRefreshTime = System.currentTimeMillis();
				throbber.decrement();
				setNavigationButtonsEnabled(true);
				if (success) {
					if (hd != null && hd.getEndTime() < before && !dataSource.isActiveSource()) {
						// this would get executed if the data source
						// forcably returned a different time than asked
						// for -- like in the case of a miniSEED.
						double dt = end - before;
						before = hd.getEndTime() - dt / 2;
						end = hd.getEndTime() + dt / 2;
						settings.setBottomTime(end);
					}
					helicorderViewPanel.setHelicorder(hd, before, end);
					repaintHelicorder();
				}
			}
		};
		worker.start();
	}

	public Wave getWave(double t1, double t2) {
		return dataSource.getWave(settings.channel, t1, t2);
	}

	public SeismicDataSource getDataSource() {
		return dataSource;
	}

	private class RefreshThread extends Thread {
		private boolean kill = false;

		public RefreshThread() {
			super("HeliRefresh-" + settings.channel);
			this.setPriority(Thread.MIN_PRIORITY);
			start();
		}

		public void kill() {
			kill = true;
			this.interrupt();
		}

		public void run() {
			while (!kill) {
				// enforce a dataSource-specific minimum refresh interval
				int refreshInterval;
				if (settings.refreshInterval == 0 || dataSource.getMinimumRefreshInterval() == 0)
					refreshInterval = 0;
				else
					refreshInterval = Math.max(settings.refreshInterval, dataSource.getMinimumRefreshInterval());

				long lastUI = System.currentTimeMillis() - Swarm.getApplication().getLastUITime();
				boolean reset = swarmConfig.isKiosk() && lastUI > 10 * 60 * 1000;
				// TODO: extract magic number
				if (reset || !Double.isNaN(settings.getBottomTime())
						&& settings.getLastBottomTimeSet() > 10 * 60 * 1000) {
					helicorderViewPanel.removeWaveInset();
					helicorderViewPanel.clearMarks();
					settings.setBottomTime(Double.NaN);
					if (swarmConfig.isKiosk() && !Swarm.getApplication().isFullScreenMode())
						Swarm.getApplication().toggleFullScreenMode();
				}

				try {
					long now = System.currentTimeMillis();
					long sleepTime = Math.min(now - lastRefreshTime, refreshInterval * 1000);

					if (refreshInterval > 0)
						Thread.sleep(sleepTime);
					else
						Thread.sleep(30 * 1000);
				} catch (Exception e) {
				}

				long now = System.currentTimeMillis();

				if (!kill && refreshInterval > 0 && (now - lastRefreshTime) > refreshInterval * 1000) {
					try {
						double bt = settings.getBottomTime();
						if (dataSource.isActiveSource() && Double.isNaN(bt)) {
							if (!working)
								getHelicorder();
						}
					} catch (Exception e) {
						System.err.println("Exception during refresh:");
						e.printStackTrace();
					}
				}
			}
		}
	}

	// TODO: refactor out some functions
	private class CaptureActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			chooser = Swarm.getApplication().getFileChooser();
			chooser.setDialogTitle("Save Helicorder Screen Capture");
			chooser.setSelectedFile(new File("heli.png"));
			File lastPath = new File(swarmConfig.lastPath);
			chooser.setCurrentDirectory(lastPath);

			JPanel imagePanel = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			imagePanel.setBorder(new TitledBorder(new EtchedBorder(), "Image Properties"));

			JLabel heightLabel = new JLabel("Height:");
			JTextField heightTextField = new JTextField(4);
			heightLabel.setLabelFor(heightTextField);
			heightTextField.setText("700");

			JLabel widthLabel = new JLabel("Width:");
			JTextField widthTextField = new JTextField(4);
			widthLabel.setLabelFor(widthTextField);
			widthTextField.setText("900");

			JCheckBox includeChannel = new JCheckBox("Include channel");
			includeChannel.setSelected(true);

			JLabel fileFormatLabel = new JLabel("File format:");
			JComboBox fileFormatCB = new JComboBox();
			fileFormatCB.addItem("PNG");
			fileFormatCB.addItem("PS");

			fileFormatCB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JComboBox source = (JComboBox) e.getSource();
					if (source.getSelectedItem().equals("PS")) {
						String fn = chooser.getSelectedFile().getName().replaceAll("\\..*$", ".ps");
						chooser.setSelectedFile(new File(chooser.getCurrentDirectory().getAbsoluteFile(), fn));
					} else {
						String fn = chooser.getSelectedFile().getName().replaceAll("\\..*$", ".png");
						chooser.setSelectedFile(new File(chooser.getCurrentDirectory().getAbsoluteFile(), fn));
					}
				}
			});

			imagePanel
					.add(heightLabel, GridBagHelper.set(c, "x=0;y=0;w=1;h=1;wx=1;wy=0;ix=12;iy=2;a=nw;f=n;i=0,4,0,4"));
			imagePanel.add(heightTextField, GridBagHelper.set(c, "x=1;y=0;w=1;h=1;wx=1;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(widthLabel, GridBagHelper.set(c, "x=0;y=1;w=1;h=1;wx=1;wy=0;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(widthTextField, GridBagHelper.set(c, "x=1;y=1;w=1;h=1;wx=1;ix=12;iy=2;f=n;i=0,4,0,4"));
			imagePanel.add(fileFormatLabel,
					GridBagHelper.set(c, "x=0;y=2;w=1;h=1;wx=1;wy=0;ix=12;iy=2;a=nw;f=w;i=0,4,0,4"));
			imagePanel.add(fileFormatCB,
					GridBagHelper.set(c, "x=1;y=2;w=1;h=1;wx=1;wy=1;ix=12;iy=2;a=nw;f=w;i=0,4,0,4"));
			imagePanel.add(includeChannel,
					GridBagHelper.set(c, "x=0;y=3;w=2;h=1;wx=1;wy=1;ix=12;iy=2;a=nw;f=w;i=0,4,0,4"));

			chooser.setAccessory(imagePanel);

			String fn = settings.channel.replace(' ', '_') + ".png";
			chooser.setSelectedFile(new File(chooser.getCurrentDirectory().getAbsoluteFile(), fn));

			int result = chooser.showSaveDialog(Swarm.getApplication());
			if (result == JFileChooser.APPROVE_OPTION) {
				File f = chooser.getSelectedFile();

				if (f.exists()) {
					int choice = JOptionPane.showConfirmDialog(Swarm.getApplication(), "File exists, overwrite?",
							"Confirm", JOptionPane.YES_NO_OPTION);
					if (choice != JOptionPane.YES_OPTION)
						return;
				}

				int width = -1;
				int height = -1;
				try {
					width = Integer.parseInt(widthTextField.getText());
					height = Integer.parseInt(heightTextField.getText());
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				if (width <= 0 || height <= 0) {
					JOptionPane.showMessageDialog(HelicorderViewerFrame.this, "Illegal width or height.", "Error",
							JOptionPane.ERROR_MESSAGE);
					return;
				}

				Plot plot = new Plot(width, height);

				Double end = settings.getBottomTime();
				if (Double.isNaN(end))
					end = CurrentTime.getInstance().nowJ2K();

				Double before = end - settings.span * 60;
				int tc = 30;

				// TODO: this should use existing data.
				HelicorderData heliData = dataSource.getHelicorder(settings.channel, before - tc, end + tc, null);
				HelicorderRenderer heliRenderer = new HelicorderRenderer(heliData, settings.timeChunk);
				if (swarmConfig.heliColors != null)
					heliRenderer.setDefaultColors(swarmConfig.heliColors); // DCK:
																			// add
																			// configured
																			// colors

				heliRenderer.setChannel(settings.channel);
				heliRenderer.setLocation(HelicorderViewPanel.X_OFFSET, HelicorderViewPanel.Y_OFFSET, width
						- HelicorderViewPanel.X_OFFSET - HelicorderViewPanel.RIGHT_WIDTH, height
						- HelicorderViewPanel.Y_OFFSET - HelicorderViewPanel.BOTTOM_HEIGHT);
				heliRenderer.setHelicorderExtents(before, end, -1 * Math.abs(settings.barRange),
						Math.abs(settings.barRange));
				heliRenderer.setTimeZone(swarmConfig.getTimeZone(settings.channel));
				heliRenderer.setForceCenter(settings.forceCenter);
				heliRenderer.setClipBars(settings.clipBars);
				heliRenderer.setShowClip(settings.showClip);
				heliRenderer.setClipValue(settings.clipValue);
				heliRenderer.setChannel(settings.channel);
				heliRenderer.setLargeChannelDisplay(includeChannel.isSelected());
				heliRenderer.createDefaultAxis();
				plot.addRenderer(heliRenderer);

				if (fileFormatCB.getSelectedItem().equals("PS"))
					plot.writePS(f.getAbsolutePath());
				else
					try {
						plot.writePNG(f.getAbsolutePath());
					} catch (PlotException e1) {
						e1.printStackTrace();
					}

				swarmConfig.lastPath = f.getParent();
			}

			chooser.setAccessory(null);
		}
	}
}