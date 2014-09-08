package gov.usgs.swarm.database.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/**
 * Dialog for long running tasks.
 * 
 * @author Chirag Patel
 */
public class LongRunningTaskDialog extends JDialog implements ActionListener
{
	private JProgressBar progressBar;
	private JButton cancelButton;
	private SwingWorker<?, ?> worker;

	public LongRunningTaskDialog(SwingWorker<?, ?> worker)
	{
		super((Frame) null, "Wait...", true);
		this.worker = worker;

//		setUndecorated(true);
		super.setResizable(false);
		// set no border for this dialog
		JPanel contentPane = (JPanel) super.getContentPane();
		contentPane.setBorder(BorderFactory.createEmptyBorder());
		contentPane.setBackground(Color.white);
		progressBar = new JProgressBar();
		progressBar.setBorderPainted(false);
		progressBar.setStringPainted(true);
		progressBar.setIndeterminate(true);
		contentPane.add(progressBar,BorderLayout.CENTER);
		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		contentPane.add(cancelButton, BorderLayout.SOUTH);
		super.setAlwaysOnTop(true);
		super.pack();
		super.setLocationRelativeTo(null);
		// all swing components except JFrame are visible by default but I need it to be hidden
		super.setVisible(false);
	}

	public JProgressBar getProgressBar()
	{
		return progressBar;
	}

	@Override
	public void actionPerformed(ActionEvent e)
	{
		worker.cancel(true);
		// here I have problems canceling current query because it is not clear
		// which is current statement
		// try {
		// AbstractConnectionManager.getInstance().cancelCurrentQuery();
		// } catch (SQLException e1) {
		// MessageManager.popupErrorMessage("Sorry, but this query cannot be cancelled!",
		// this);
		// e1.printStackTrace();
		// }
	}
}