package gov.usgs.volcanoes.swarm.chooser;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import gov.usgs.volcanoes.swarm.Swarm;
import gov.usgs.volcanoes.swarm.SwarmConfig;
import gov.usgs.volcanoes.swarm.SwarmDialog;

/**
 *
 * @author Dan Cervelli
 */
public class EditDataSourceDialog extends SwarmDialog
{
	private static final long serialVersionUID = 1L;
    private static final JFrame applicationFrame = Swarm.getApplicationFrame();

	private String source;
	private boolean edit;
	
	private JTextField name;
	
	private JTabbedPane tabPane;
	
	private List<DataSourcePanel> panels;
	
	private String result;
	
	public EditDataSourceDialog(String s)
	{
		super(applicationFrame, "", true);
		createPanels();
		source = s;
		if (source == null)
		{
			this.setTitle("New Data Source");
			edit = false;
		}
		else
		{
			this.setTitle("Edit Data Source");
			edit = true;
		}
		createDataSourceUI();
		setSizeAndLocation();
	}
	
	private void createPanels()
	{
		panels = new ArrayList<DataSourcePanel>();
		panels.add(new WWSPanel());
		panels.add(new WaveServerPanel());
//		panels.add(new DHIPanel());
		panels.add(new WebServicesPanel());
		panels.add(new SeedLinkPanel());
	}
	
	protected void createDataSourceUI()
	{
		JPanel dsPanel = new JPanel(new BorderLayout());
		
		String src = null;
		if (source != null)
			src = source.substring(source.indexOf(';') + 1, source.indexOf(':'));

		tabPane = new JTabbedPane();
		for (DataSourcePanel dsp : panels)
		{
			dsp.setSource(source);
			JPanel p = dsp.getPanel();
			tabPane.add(dsp.getName(), p);
			if (src != null && src.equals(dsp.getCode()))
				tabPane.setSelectedComponent(p);
		}
		
		dsPanel.add(tabPane, BorderLayout.CENTER);
		
		Box namePanel = new Box(BoxLayout.X_AXIS);
		namePanel.add(new JLabel("Data Source Name:"));
		namePanel.add(Box.createHorizontalStrut(10));
		String n = "";
		if (source != null)
			n = source.substring(0, source.indexOf(';'));
		name = new JTextField(30);
		namePanel.add(name);
		name.setText(n);
		dsPanel.add(namePanel, BorderLayout.NORTH);
		dsPanel.setBorder(new EmptyBorder(new Insets(10,10,10,10)));
		mainPanel.add(dsPanel, BorderLayout.CENTER);
	}

	protected boolean allowOK()
	{
		String n = name.getText();
		String message = null;
		if (n == null || n.length() <= 0)
			message = "You must specify a name for this data source.";
		else if (!edit && SwarmConfig.getInstance().sourceExists(n))
			message = "A data source by that name already exists.";
		
		if (message != null)
		{
			JOptionPane.showMessageDialog(applicationFrame, message, "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		
		DataSourcePanel p = panels.get(tabPane.getSelectedIndex());
		return p.allowOK(edit);
	}

	protected void wasOK()
	{
		DataSourcePanel p = panels.get(tabPane.getSelectedIndex());
		result = name.getText() + ";" + p.wasOK();
	}
	
	public String getResult()
	{
		return result;
	}
}
