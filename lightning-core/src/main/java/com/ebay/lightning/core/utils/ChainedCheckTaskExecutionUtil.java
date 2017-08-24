package com.ebay.lightning.core.utils;

import java.util.ArrayList;
import java.util.List;

import com.ebay.lightning.core.beans.ChainedURLTask;
import com.ebay.lightning.core.beans.Task;
import com.ebay.lightning.core.constants.LightningCoreConstants.TaskStatus;

/**
 * Helper class to execute a list of {@link ChainedURLTask}.
 * 
 * @author shashukla
 * @see ChainedURLTask
 */
public class ChainedCheckTaskExecutionUtil {
	List<ChainedURLTask> tasks;
	int completedTasks = 0;

	/**
	 * Construct the {@code ChainedURLTask} list from the task list.
	 * @param tasks the task list
	 */
	public ChainedCheckTaskExecutionUtil(List<Task> tasks) {
		List<ChainedURLTask> typeCastedTaskList = new ArrayList<ChainedURLTask>();
		for(Task task : tasks) {
			typeCastedTaskList.add((ChainedURLTask) task);
		}
		this.tasks = typeCastedTaskList;
	}

	/**
	 * Check if the tasks are chained.
	 * @param tasks the task list
	 * @return true if all tasks are {@code ChainedURLTask}; return false otherwise
	 */
	public static boolean areChainedCheckTasks(List<Task> tasks) {
		if (tasks != null && !tasks.isEmpty()) {
			for (Task task : tasks) {
				if (!(task instanceof ChainedURLTask)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Get the next sequence of tasks to be executed.
	 * @return the next sequence of tasks
	 */
	public List<Task> getSubNextTasks() {
		List<Task> nextSubTasks = new ArrayList<Task>();
		for(ChainedURLTask task : tasks) {
			if (!task.hasFailed()) {
				if (task.hasNext()) {
					task.moveToNext();
					nextSubTasks.add(task);
				} else {
					task.setStatus(TaskStatus.SUCCESS);
					completedTasks++;
				}
			} else {
				task.setStatus(TaskStatus.FAILED);
				completedTasks++;
			}
		}
		return nextSubTasks;
	}

	/**
	 * Check for more tasks to be executed in the sequence chain.
	 * @return true if there are more tasks to be executed; false otherwise
	 */
	public boolean hasMoreSubTasks() {
		if (tasks != null) {
			if (completedTasks >= tasks.size()) {
				return false;
			}
		}
		return true;
	}
}
