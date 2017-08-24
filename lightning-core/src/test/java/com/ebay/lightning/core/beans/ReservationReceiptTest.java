package com.ebay.lightning.core.beans;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.ebay.lightning.core.beans.ReservationReceipt.State;

public class ReservationReceiptTest {

	private ReservationReceipt receipt;
	private String id = "id100";
	private int load = 110;
	
	@Before
	public void setup(){
		receipt = new ReservationReceipt(State.ACCEPTED, id, load);
	}
	
	@Test
	public void testEquals(){
		Assert.assertTrue(receipt.equals(receipt));
		Assert.assertFalse(receipt.equals(null));
		Assert.assertFalse(receipt.equals(new Object()));
		ReservationReceipt newReceipt = new ReservationReceipt(State.DENIED, "test", 5);
		Assert.assertFalse(receipt.equals(newReceipt));
		
		receipt.setId(null);
		Assert.assertFalse(receipt.equals(newReceipt));
		Assert.assertFalse(receipt.hashCode() == newReceipt.hashCode());
		receipt.setId(id);
		newReceipt.setId(id);
		Assert.assertFalse(receipt.equals(newReceipt));
		
		newReceipt.setState(null);
		Assert.assertFalse(receipt.equals(newReceipt));
		Assert.assertFalse(receipt.hashCode() == newReceipt.hashCode());
		newReceipt.setState(State.ACCEPTED);
		Assert.assertTrue(receipt.equals(newReceipt));
		Assert.assertEquals(receipt.hashCode(), newReceipt.hashCode());
		
		newReceipt.setLoad(Integer.MAX_VALUE);
		Assert.assertTrue(receipt.equals(newReceipt));
		Assert.assertEquals(receipt.hashCode(), newReceipt.hashCode());
	}
	
}
