/*
Copyright 2013-2014 eBay Software Foundation  
*/
package com.ebay.lightning.core.async;


/**
 * The {@code Callback} class defines the callback handler for {@link Reminder} thread.
 * 
 * @author shashukla
 * @see Reminder
 */
public interface Callback<T> {
	
	/**
	 * This method is called by the Reminder task whenever it gets executed.
	 * @param data the input data
	 */
	public void notify(T data);
}
