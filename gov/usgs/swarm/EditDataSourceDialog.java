package gov.usgs.swarm;

import gov.usgs.util.GridBagHelper;
import gov.usgs.util.ui.BaseDialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

/**
 * $Log: not supported by cvs2svn $
 * Revision 1.4  2005/03/24 23:44:26  cervelli
 * Added compression checkbox for WWS.
 *
 * Revision 1.3  2005/03/24 22:05:24  cervelli
 * Removed Winston Server, added WWS.
 *
 * Revision 1.2  2004/10/23 19:34:46  cvs
 * Added support for SAC files.
 *
 * @author Dan Cervelli
 */
public class EditDataSourceDialog extends BaseDialog
{
	private static final long serialVersionUID = -1;
	private static final int WIDTH = 400;
	private static final int HEIGHT = 300;
	
	private JTextField name;
	
	private JTabbedPane tabPane;
	
	private JPanel seedPanel;
	private JTextField seedFilename;
	private JButton seedButton;
	
	private JPanel sacPanel;
	private JTextField sacFilename;
	private JButton sacButton;
	
	private JPanel waveServerPanel;
	private JTextField wsHost;
	private JTextField wsPort;
	private JTextField wsTimeout;
	
	private JPanel wwsPanel;
	private JTextField wwsHost;
	private JTextField wwsPort;
	private JTextField wwsTimeout;
	private JCheckBox wwsCompress;
	
	private String result;
	private boolean edit;
	
	private int tabIndex = 0;
	
	public EditDataSourceDialog(String source)
	{
		super(Swarm.getParentFrame(), "", true, WIDTH, HEIGHT);
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
		createDataSourceUI(source);	
	}
	
	public void createDataSourceUI(String source)
	{
		JPanel dsPanel = new JPanel(new BorderLayout());
		createSACPanel(source);
		createSeedPanel(source);
		createWaveServerPanel(source);
//		createWinstonPanel(source);
		createWWSPanel(source);
		
		tabPane = new JTabbedPane();
		tabPane.add("Wave Server", waveServerPanel);
		tabPane.add("WWS", wwsPanel);
//		tabPane.add("Winston", winstonPanel);
		tabPane.add("SEED", seedPanel);
		tabPane.add("SAC", sacPanel);
		//tabPane.setBorder(new TitledBorder(new EtchedBorder(), "Data Source Type"));
		dsPanel.add(tabPane, BorderLayout.CENTER);
		
		tabPane.setSelectedIndex(tabIndex);
		
		Box namePanel = new Box(BoxLayout.X_AXIS);
		namePanel.add(new JLabel("Data Source Name:"));
		namePanel.add(Box.createHorizontalStrut(10));
		String n = "";
		if (source != null)
			n = source.substring(0, source.indexOf(';'));
		name = new JTextField(30);
		namePanel.add(name);
		name.setText(n);
		if (edit)
			name.setEnabled(false);
		dsPanel.add(namePanel, BorderLayout.NORTH);
		dsPanel.setBorder(new EmptyBorder(new Insets(10,10,10,10)));
		mainPanel.add(dsPanel, BorderLayout.CENTER);
	}
	
	private void createSeedPanel(String source)
	{
		String fn = "";
		
		if (source != null && source.indexOf(";seed:") != -1)
		{
			tabIndex = 2;
			fn = source.substring(source.indexOf(";seed:") + 6);
		}

		seedPanel = new JPanel(new GridBagLayout());
		seedFilename = new JTextField(fn);
		seedButton = new JButton("Browse...");
		
		GridBagConstraints c = new GridBagConstraints();
		JLabel info = new JLabel("<html>Swarm can read either full SEED volumes or miniSEED.</html>");
		seedPanel.add(info, GridBagHelper.set(c, "x=0;y=0;w=3;h=1;ix=4;iy=4;wy=0.3;f=b"));
		seedPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=3;h=1;ix=4;iy=4;wy=0;wx=1.0;f=h"));
		
		JLabel seedLabel = new JLabel("SEED file:");
		seedLabel.setHorizontalAlignment(JLabel.RIGHT);
		
		seedFilename.setMinimumSize(new Dimension(180, 20));
		seedPanel.add(seedLabel, GridBagHelper.set(c, "x=0;y=2;a=ne;w=1;h=1;wx=0;wy=0;f=n;i=0,0,0,8;wy=1"));
		seedPanel.add(seedFilename, GridBagHelper.set(c, "x=1;y=2;a=nw;w=1;h=1;wx=1;i=0,0,0,0;f=h"));
		
		seedPanel.add(seedButton, GridBagHelper.set(c, "x=2;y=2;a=ne;w=1;h=1;wx=0;i=0,0,0,0"));
		
		seedButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						JFileChooser chooser = Swarm.getParentFrame().getFileChooser();
						int result = chooser.showOpenDialog(Swarm.getParentFrame());
						if (result == JFileChooser.APPROVE_OPTION) 
						{						            
							File f = chooser.getSelectedFile();
							seedFilename.setText(f.getPath());
						}
					}
				});
	}
	
	private void createSACPanel(String source)
	{
		String fn = "";
		
		if (source != null && source.indexOf(";sac:") != -1)
		{
			tabIndex = 3;
			fn = source.substring(source.indexOf(";sac:") + 5);
		}

		sacPanel = new JPanel(new GridBagLayout());
		sacFilename = new JTextField(fn);
		sacButton = new JButton("Browse...");
		
		GridBagConstraints c = new GridBagConstraints();
		JLabel info = new JLabel("<html>Swarm can read binary SAC files.</html>");
		sacPanel.add(info, GridBagHelper.set(c, "x=0;y=0;w=3;h=1;ix=4;iy=4;wy=0.3;f=b"));
		sacPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=3;h=1;ix=4;iy=4;wy=0;wx=1.0;f=h"));
		
		JLabel sacLabel = new JLabel("SAC file:");
		sacLabel.setHorizontalAlignment(JLabel.RIGHT);
		
		sacFilename.setMinimumSize(new Dimension(180, 20));
		sacPanel.add(sacLabel, GridBagHelper.set(c, "x=0;y=2;a=ne;w=1;h=1;wx=0;wy=0;f=n;i=0,0,0,8;wy=1"));
		sacPanel.add(sacFilename, GridBagHelper.set(c, "x=1;y=2;a=nw;w=1;h=1;wx=1;i=0,0,0,0;f=h"));
		
		sacPanel.add(sacButton, GridBagHelper.set(c, "x=2;y=2;a=ne;w=1;h=1;wx=0;i=0,0,0,0"));
		
		sacButton.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						JFileChooser chooser = Swarm.getParentFrame().getFileChooser();
						int result = chooser.showOpenDialog(Swarm.getParentFrame());
						if (result == JFileChooser.APPROVE_OPTION) 
						{						            
							File f = chooser.getSelectedFile();
							sacFilename.setText(f.getPath());
						}
					}
				});
	}
	
	private void createWWSPanel(String source)
	{
		String wsh = "";
		String wsp = "16022";
		String wsto = "10";
		boolean wscomp = false;
		if (source != null && source.indexOf(";wws:") != -1)
		{
			tabIndex = 1;
			String s = source.substring(source.indexOf(";wws:") + 4);
//			String[] ss = Util.splitString(s, ":");
			String[] ss = s.split(":");
//			wsh = source.substring(source.indexOf(";ws:") + 4, source.indexOf(':', source.indexOf("ws:") + 4));
			wsh = ss[0];
			wsp = ss[1];
			wsto = Double.toString(Double.parseDouble(ss[2]) / 1000);
			wscomp = ss[3].equals("1");
		}
		wwsPanel = new JPanel(new GridBagLayout());
		wwsPanel.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
		GridBagConstraints c = new GridBagConstraints();
		
		wwsHost = new JTextField();
		wwsHost.setMinimumSize(new Dimension(180, 20));
		wwsHost.setText(wsh);
		wwsPort = new JTextField("16022");
		wwsPort.setMinimumSize(new Dimension(50, 20));
		wwsPort.setText(wsp);
		
		wwsTimeout = new JTextField("10");
		wwsTimeout.setMinimumSize(new Dimension(50, 20));
		wwsTimeout.setText(wsto);

		wwsCompress = new JCheckBox();
		wwsCompress.setSelected(wscomp);
		
		JLabel info = new JLabel("<html>Use this type of data source to connect to a Winston Wave Server (WWS). The default WWS port is 16022.</html>");
		wwsPanel.add(info, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;ix=4;iy=4;wy=0.3;f=b"));
		
		wwsPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=2;h=1;ix=4;iy=4;wy=0;wx=1.0;f=h"));
		JLabel ipLabel = new JLabel("IP address or host name:");
		ipLabel.setHorizontalAlignment(JLabel.RIGHT);
		wwsPanel.add(ipLabel, GridBagHelper.set(c, "x=0;y=2;a=e;w=1;h=1;wx=0;wy=0;f=h;i=0,0,0,8"));
		wwsPanel.add(wwsHost, GridBagHelper.set(c, "x=1;y=2;a=w;w=1;h=1;wx=1;i=0,0,0,0;f=n"));
		
		JLabel portLabel = new JLabel("Port:");
		portLabel.setHorizontalAlignment(JLabel.RIGHT);
		wwsPanel.add(portLabel, GridBagHelper.set(c, "x=0;y=3;a=ne;w=1;h=1;wx=0;wy=0.0;i=0,0,0,8"));
		wwsPanel.add(wwsPort, GridBagHelper.set(c, "x=1;y=3;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
		
		JLabel toLabel = new JLabel("Timeout, seconds:");
		toLabel.setHorizontalAlignment(JLabel.RIGHT);
		wwsPanel.add(toLabel, GridBagHelper.set(c, "x=0;y=4;a=ne;w=1;h=1;wx=0;wy=0.7;i=0,0,0,8"));
		wwsPanel.add(wwsTimeout, GridBagHelper.set(c, "x=1;y=4;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
		
		JLabel compLabel = new JLabel("Use compression:");
		compLabel.setHorizontalAlignment(JLabel.RIGHT);
		wwsPanel.add(compLabel, GridBagHelper.set(c, "x=0;y=5;a=ne;w=1;h=1;wx=0;wy=0.7;i=0,0,0,8"));
		wwsPanel.add(wwsCompress, GridBagHelper.set(c, "x=1;y=5;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
	}
	
	private void createWaveServerPanel(String source)
	{
		String wsh = "";
		String wsp = "16022";
		String wsto = "2";
		if (source != null && source.indexOf(";ws:") != -1)
		{
			tabIndex = 0;
			String s = source.substring(source.indexOf(";ws:") + 4);
//			String[] ss = Util.splitString(s, ":");
			String[] ss = s.split(":");
//			wsh = source.substring(source.indexOf(";ws:") + 4, source.indexOf(':', source.indexOf("ws:") + 4));
			wsh = ss[0];
			wsp = ss[1];
			wsto = Double.toString(Double.parseDouble(ss[2]) / 1000);
		}
		waveServerPanel = new JPanel(new GridBagLayout());
		waveServerPanel.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
		GridBagConstraints c = new GridBagConstraints();
		
		wsHost = new JTextField();
		wsHost.setMinimumSize(new Dimension(180, 20));
		wsHost.setText(wsh);
		wsPort = new JTextField("16022");
		wsPort.setMinimumSize(new Dimension(50, 20));
		wsPort.setText(wsp);
		
		wsTimeout = new JTextField("2");
		wsTimeout.setMinimumSize(new Dimension(50, 20));
		wsTimeout.setText(wsto);
		
		JLabel info = new JLabel("<html>Use this type of data source to connect to an Earthworm Wave Server. The default Wave Server port is 16022.</html>");
		waveServerPanel.add(info, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;ix=4;iy=4;wy=0.3;f=b"));
		
		waveServerPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=2;h=1;ix=4;iy=4;wy=0;wx=1.0;f=h"));
		JLabel ipLabel = new JLabel("IP address or host name:");
		ipLabel.setHorizontalAlignment(JLabel.RIGHT);
		waveServerPanel.add(ipLabel, GridBagHelper.set(c, "x=0;y=2;a=e;w=1;h=1;wx=0;wy=0;f=h;i=0,0,0,8"));
		waveServerPanel.add(wsHost, GridBagHelper.set(c, "x=1;y=2;a=w;w=1;h=1;wx=1;i=0,0,0,0;f=n"));
		
		JLabel portLabel = new JLabel("Port:");
		portLabel.setHorizontalAlignment(JLabel.RIGHT);
		waveServerPanel.add(portLabel, GridBagHelper.set(c, "x=0;y=3;a=ne;w=1;h=1;wx=0;wy=0.0;i=0,0,0,8"));
		waveServerPanel.add(wsPort, GridBagHelper.set(c, "x=1;y=3;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
		
		JLabel toLabel = new JLabel("Timeout, seconds:");
		toLabel.setHorizontalAlignment(JLabel.RIGHT);
		waveServerPanel.add(toLabel, GridBagHelper.set(c, "x=0;y=4;a=ne;w=1;h=1;wx=0;wy=0.7;i=0,0,0,8"));
		waveServerPanel.add(wsTimeout, GridBagHelper.set(c, "x=1;y=4;a=nw;w=1;h=1;wx=1;i=0,0,0,0"));
	}
	
	/*
	private void createWinstonPanel(String source)
	{
		String wh = "";
		String wp = "";
		String wu = "";
		
		if (source != null && source.indexOf(";winstonserver:") != -1)
		{
			tabIndex = 1;
			String[] s = Util.splitString(source, ":");
			wh = s[1];
			wp = s[2];
			wu = s[3];
		}
		winstonPanel = new JPanel(new GridBagLayout());
		
		winstonPanel.setBorder(new EmptyBorder(new Insets(5,5,5,5)));
		GridBagConstraints c = new GridBagConstraints();
		
		JLabel info = new JLabel("<html>Use this type of data source to connect to a Winston Server. The default Winston Server port is 6369.</html>");
		winstonPanel.add(info, GridBagHelper.set(c, "x=0;y=0;w=2;h=1;ix=4;iy=4;wy=0.3;f=b"));
		winstonPanel.add(new JSeparator(), GridBagHelper.set(c, "x=0;y=1;w=2;h=1;ix=4;iy=4;wy=0;wx=1.0;f=h"));
		
		JLabel hostLabel = new JLabel("IP address or host name:");
		hostLabel.setHorizontalAlignment(JLabel.RIGHT);
		
		winstonHost = new JTextField(wh);
		winstonHost.setMinimumSize(new Dimension(180, 20));
		winstonPanel.add(hostLabel, GridBagHelper.set(c, "x=0;y=2;a=e;w=1;h=1;wx=0;wy=0;f=n;i=0,0,0,8"));
		winstonPanel.add(winstonHost, GridBagHelper.set(c, "x=1;y=2;a=w;w=1;h=1;wx=1;i=0,0,0,0;f=n"));
		
		JLabel portLabel = new JLabel("Port:");
		portLabel.setHorizontalAlignment(JLabel.RIGHT);
		winstonPort = new JTextField(wp);
		winstonPort.setMinimumSize(new Dimension(50, 20));
		winstonPanel.add(portLabel, GridBagHelper.set(c, "x=0;y=3;a=e;w=1;h=1;wx=0;wy=0.0;i=0,0,0,8"));
		winstonPanel.add(winstonPort, GridBagHelper.set(c, "x=1;y=3;a=nw;w=1;h=1;wx=1;i=0,0,0,0;f=n"));
		
		JLabel uidLabel = new JLabel("User ID:");
		uidLabel.setHorizontalAlignment(JLabel.RIGHT);
		winstonUser = new JTextField(wu);
		winstonUser.setMinimumSize(new Dimension(180, 20));
		winstonPanel.add(uidLabel, GridBagHelper.set(c, "x=0;y=4;a=e;w=1;h=1;wx=0;wy=0.0;i=0,0,0,8"));
		winstonPanel.add(winstonUser, GridBagHelper.set(c, "x=1;y=4;a=nw;w=1;h=1;wx=1;i=0,0,0,0;f=n"));

		JLabel pwLabel = new JLabel("Password:");
		pwLabel.setHorizontalAlignment(JLabel.RIGHT);
		winstonPassword = new JPasswordField();
		winstonPassword.setMinimumSize(new Dimension(180, 20));
		winstonPanel.add(pwLabel, GridBagHelper.set(c, "x=0;y=5;a=ne;w=1;h=1;wx=0;wy=0.7;i=0,0,0,8"));
		winstonPanel.add(winstonPassword, GridBagHelper.set(c, "x=1;y=5;a=nw;w=1;h=1;wx=1;i=0,0,0,0;f=n"));
	}
	*/
	
	protected boolean allowOK()
	{
		String message = null;
		
		String n = name.getText();
		// check name
		if (n == null || n.length() <= 0)
			message = "You must specify a name for this data source.";
		else if (!edit && Swarm.getParentFrame().sourceExists(n))
			message = "A data source by that name already exists.";
		
		if (message == null)
		{
			int type = tabPane.getSelectedIndex();
			if (type == 0) // Wave Server
			{
				String host = wsHost.getText();
				if (host == null || host.length() == 0 || host.indexOf(';') != -1 || host.indexOf(':') !=-1)
					message = "There is an error with the Wave Server IP address or host name.";
				int ip = -1;
				try { ip = Integer.parseInt(wsPort.getText()); } catch (Exception e) {}
				if (ip < 0 || ip > 65535)
					message = "There is an error with the Wave Server port.";
				
				double to = -1;
				try { to = Double.parseDouble(wsTimeout.getText()); } catch (Exception e) {}
				if (to <= 0)
					message = "There is an error with the Wave Server time out (must be > 0).";
			}
			else if (type == 1) // WWS
			{
				String host = wwsHost.getText();
				if (host == null || host.length() == 0 || host.indexOf(';') != -1 || host.indexOf(':') !=-1)
					message = "There is an error with the WWS IP address or host name.";
				int ip = -1;
				try { ip = Integer.parseInt(wwsPort.getText()); } catch (Exception e) {}
				if (ip < 0 || ip > 65535)
					message = "There is an error with the WWS port.";
				
				double to = -1;
				try { to = Double.parseDouble(wwsTimeout.getText()); } catch (Exception e) {}
				if (to <= 0)
					message = "There is an error with the WWS time out (must be > 0).";
			}
			/*
			else if (type == 1) // Winston
			{
				String host = winstonHost.getText();
				if (host == null || host.length() == 0 || host.indexOf(';') != -1 || host.indexOf(':') !=-1)
					message = "There is an error with the Winston Server IP address or host name.";
				int ip = -1;
				try { ip = Integer.parseInt(winstonPort.getText()); } catch (Exception e) {}
				if (ip < 0 || ip > 65535)
					message = "There is an error with the Winston Server port.";
				
				String user = winstonUser.getText();
				if (user == null || user.length() == 0 || user.length() > 20 || user.indexOf(';') != -1 
						|| user.indexOf(':') != -1)
					message = "There is an error with the Winston user ID.";
				
				String password = new String(winstonPassword.getPassword());
				if (password == null || password.length() == 0)
					message = "There is an error with the Winston password.";
			}
			*/
			else if (type == 2) // SEED
			{
				if (seedFilename.getText().length() == 0)
					message = "You must specify a filename.";
			}
			else if (type == 3) // SAC
			{
				if (sacFilename.getText().length() == 0)
					message = "You must specify a filename.";
			}
			
		}
		
		if (message != null)
		{
			JOptionPane.showMessageDialog(Swarm.getParentFrame(), message, "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		else
			return true;	
	}
	
	protected void wasOK()
	{
		int type = tabPane.getSelectedIndex();
		if (type == 0)  // Earthworm Wave Server
		{
			int timeout = (int)(Double.parseDouble(wsTimeout.getText()) * 1000);
			result = name.getText() + ";ws:" + wsHost.getText() + ":" + wsPort.getText() + ":" + timeout;
		}
		/*
		else if (type == 1)  // Winston Server 
		{
			result = name.getText() + ";winston:" + winstonHost.getText() + ":" + winstonPort.getText() + ":" +
					winstonUser.getText() + ":" + Util.md5(new String(winstonPassword.getPassword()));
		}
		*/
		else if (type == 1)  // WWS
		{
		    int timeout = (int)(Double.parseDouble(wwsTimeout.getText()) * 1000);
			result = name.getText() + ";wws:" + wwsHost.getText() + ":" + wwsPort.getText() + ":" + timeout + ":" + (wwsCompress.isSelected() ? "1" : "0");
		}
		else if (type == 2)  // SEED
		{
			if (seedFilename.getText().length() > 1)
			{
				result = name.getText() + ";seed:" + seedFilename.getText();
			}
		}
		else if (type == 3)  // SAC
		{
			if (sacFilename.getText().length() > 1)
			{
				result = name.getText() + ";sac:" + sacFilename.getText();
			}
		}
	}
	
	public String getResult()
	{
		return result;
	}
}
