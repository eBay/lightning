package com.ebay.lightning.core.async;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import junit.framework.Assert;

public class ReminderTest {

	@Test
	public void testReminder()throws Exception{
		final AtomicInteger value1 = new AtomicInteger(0);
		final AtomicInteger value2 = new AtomicInteger(0);
		final Callback<Long> oneTimeCallback = new Callback<Long>() {
			@Override
			public void notify(Long data) {
				value1.incrementAndGet();
			}
		};
		final Callback<Long> repetitiveCallback = new Callback<Long>() {
			@Override
			public void notify(Long data) {
				value2.incrementAndGet();
			}
		};
		final Reminder oneTimeReminder = new Reminder("oneTimeReminder", oneTimeCallback, 1L, false);
		final Reminder repetitiveReminder = new Reminder("repetitiveReminder", repetitiveCallback, 1L, true);
		Assert.assertEquals("oneTimeReminder", oneTimeReminder.getReminderName());
		Assert.assertEquals("repetitiveReminder", repetitiveReminder.getReminderName());
		Thread.sleep(2000);

		Assert.assertTrue(value2.longValue() >= 1);
		repetitiveReminder.cancel();
		final int valueAfterCancel = value2.intValue();
		Thread.sleep(1000);

		Assert.assertEquals(1, value1.intValue());
		Assert.assertEquals(valueAfterCancel, value2.intValue());
	}

	@Test
	public void testReminderException(){
		final Callback<Long> callback = new Callback<Long>() {
			@Override
			public void notify(Long data) {
				throw new RuntimeException();
			}
		};
		try{
			new Reminder("reminder", callback, 1L, false);
			Thread.sleep(1000);
			Assert.assertTrue(true);
		}catch(final Exception e){
			Assert.assertFalse(true);
		}

	}


}
