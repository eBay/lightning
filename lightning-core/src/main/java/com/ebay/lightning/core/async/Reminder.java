package com.ebay.lightning.core.async;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

/**
 * The {@code Reminder} class provides the helper functions to create one time or repeating reminders.
 * The tasks for {@code Reminder} are defined by {@link Callback}
 * 
 * @author shashukla
 * @see Callback
 */
public class Reminder {
	private static final Logger log = Logger.getLogger(Reminder.class);
	
	private Timer timer;
	private String reminderName;
	
	/**
	 * Construct an one-time or repeating reminder.
	 * @param reminderName the name of the reminder.
	 * @param callback the method to call for every execution of the reminder
	 * @param freqInSeconds frequency of the reminder execution
	 * @param repeatForever one-time  or repeating task.
	 */
	public Reminder(String reminderName, Callback<?> callback, Long freqInSeconds, boolean repeatForever) {
		this.reminderName = reminderName;
		timer = new Timer(reminderName + "-- sleeping seconds:" + freqInSeconds);
		RemindTask remindTask = new RemindTask();
		remindTask.setPM(callback);
		remindTask.setRepetition(repeatForever);

		if (repeatForever) {
			timer.schedule(remindTask, freqInSeconds * 1000, freqInSeconds * 1000);
		} else {
			timer.schedule(remindTask, freqInSeconds * 1000);
		}
	}

	class RemindTask extends TimerTask {
		boolean repeatForever = false;
		Callback<?> pm = null;

		/**
		 * Set the callback method.
		 * @param pm the callback method
		 */
		public void setPM(Callback<?> pm) {
			this.pm = pm;
		}

		/**
		 * Set to repeat reminder.
		 * @param repeatForever true to repeat reminder
		 */
		public void setRepetition(boolean repeatForever) {
			this.repeatForever = repeatForever;
		}

		/* (non-Javadoc)
		 * @see {@link TimerTask#run()}
		 */
		@Override
		public void run() {
			try{
				if (!repeatForever) {
					// Not necessary after this call
					timer.cancel();
				}
				pm.notify(null);
			}catch(Throwable t){
				log.warn(t);
			}
		}
	}

	/**
	 * Cancel the reminder
	 */
	public void cancel() {
		timer.cancel();
	}
	
	/**
	 * Get the reminder name.
	 * @return the reminder name
	 */
	public String getReminderName() {
		return reminderName;
	}

}
