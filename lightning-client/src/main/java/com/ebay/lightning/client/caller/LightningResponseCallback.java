package com.ebay.lightning.client.caller;

import com.ebay.lightning.core.beans.LightningResponse;

public interface LightningResponseCallback {

	public void onComplete(LightningResponse response);
	public void onTimeout(LightningResponse response);
}
