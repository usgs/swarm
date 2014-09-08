package gov.usgs.swarm.database.util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;
import javax.swing.Timer;

/**
 * GUI help for running database tasks.
 * 
 * @author Chirag Patel
 *
 * @param <T> result type for the task
 * @param <V> type for intermediate results
 */
public class DataAccessSwingWorker<T, V> extends SwingWorker<T, V> implements PropertyChangeListener
{
	private LongRunningTaskDialog taskDialog;
	private Timer timer = null;
	private DataAccessTask<T, V> backgroudTask;
	private boolean withProgressBar;

	public DataAccessSwingWorker(DataAccessTask<T, V> backgroudTask, boolean withProgressBar)
	{
		super();
		this.backgroudTask = backgroudTask;
		this.withProgressBar = withProgressBar;
		taskDialog = new LongRunningTaskDialog(this);
		if (withProgressBar)
			super.addPropertyChangeListener(this);
		super.execute();
		if (withProgressBar) {
			timer = new Timer(130, new ActionListener()
			{
				@Override
				public void actionPerformed(ActionEvent e)
				{
					showTaskDialog();
				}
			});
			timer.setRepeats(false);
		}
	}

	@Override
	protected T doInBackground() throws Exception
	{
		return backgroudTask.doInBackground(this);
	}

	@Override
	protected void done()
	{
		if (withProgressBar) {
			timer.stop();
			hideTaskDialog();
		}
		try {
			if (!isCancelled()) {
				backgroudTask.done(get());
			} else {
				backgroudTask.cancel();
			}
		} catch (InterruptedException e) {
			String exceptionMessage = readException(e);
			if (exceptionMessage == null) {
				exceptionMessage = "Exception is unknown";
			}
			MessageManager.popupErrorMessage(exceptionMessage, taskDialog);
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void propertyChange(PropertyChangeEvent evt)
	{
		String propertyName = evt.getPropertyName();
		if (propertyName.equals("progress")) {
			// this property name indicates how much job is done.
			// When method setProgress is called
			// "progress" property is updated and therefore PropertyChangeEvent
			// is created and passed to registered
			// PropertyChangeListener
			taskDialog.getProgressBar().setValue((Integer) evt.getNewValue());
		} else if (propertyName.equals("state")) {
			switch ((StateValue) evt.getNewValue()) {
			case STARTED:
				timer.start();
				break;
			case DONE:
			case PENDING:
			default:
				break;
			}
		}
	}

	public T getTaskReturnValue()
	{
		try {
			return super.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void failIfInterrupted() throws InterruptedException
	{
		if (Thread.currentThread().isInterrupted()) {
			throw new InterruptedException("Interrupted while accessing database");
		}
	}

	public void setProgressValue(int progress)
	{
		super.setProgress(progress);
	}

	private void showTaskDialog()
	{
		System.out.println("taskDialog is made visible!!!!!");
		if (!taskDialog.isVisible())
			taskDialog.setVisible(true);
	}

	private void hideTaskDialog()
	{
		if (taskDialog.isVisible())
			taskDialog.setVisible(false);
	}

	private String readException(Throwable e)
	{
		StringBuilder builder = new StringBuilder("<html><div style='line-height:18px;font-size:14px;'>");
		for (StackTraceElement ste : e.getStackTrace()) {
			builder.append(ste.toString()).append("<br/>");
		}
		builder.append("</div></html>");
		return builder.toString();
	}
}