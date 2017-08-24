/**
 * 
 */
package com.ebay.lightning.core.beans;

import java.io.Serializable;

import com.ebay.lightning.core.controllers.LightningController;


/**
 * The {@code ReservationReceipt} class holds information about the reservation made by the client with the seed or core.
 * The seeds might either return ACCEPTED, DENIED or BUSY based on the load of the seed.
 * 
 * @author shashukla
 * @see LightningController#reserve()
 *
 */
public class ReservationReceipt implements Serializable{
	private static final long serialVersionUID = 1L;
	public static enum State {
		ACCEPTED, DENIED, BUSY
	}

	private String id;
	private State state;
	private int load;
	private int busyWithLoad;

	public ReservationReceipt(State accepted, String id, int load) {
		this.state = accepted;
		this.id = id;
		this.load = load;
	}

	/**
	 * Get the state returned by the seed.
	 * @return might return ACCEPTED, DENIED or BUSY based on the load of the seed.
	 */
	public State getState() {
		return state;
	}

	/**
	 * Set the state for the request.
	 * @param state ACCEPTED, DENIED or BUSY
	 */
	public void setState(State state) {
		this.state = state;
	}

	/**
	 * Get the request id for which reservation is made.
	 * @return the request id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the request id for which reservation is made.
	 * @param id the request id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/* (non-Javadoc)
	 * @see {@link Object#hashCode()}
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((state == null) ? 0 : state.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see {@link Object#equals(Object)}
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ReservationReceipt other = (ReservationReceipt) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		if (state != other.state) {
			return false;
		}
		return true;
	}

	/**
	 * Get the load of the request.
	 * @return the load of the request
	 */
	public int getLoad() {
		return load;
	}

	/**
	 * Set the load of the request.
	 * @param load the load of the request
	 */
	public void setLoad(int load) {
		this.load = load;
	}

	/* (non-Javadoc)
	 * @see {@link Object#toString()}
	 */
	@Override
	public String toString() {
		return "ReservationReciept [id=" + id + ", state=" + state + ", load=" + load + ", busyWithLoad=" + busyWithLoad + "]";
	}

	/**
	 * Get the load of the seed while making reservation for the request.
	 * @return the load of the seed while making reservation for the request
	 */
	public int getBusyWithLoad() {
		return busyWithLoad;
	}

	/**
	 * Set the load of the seed while making reservation for the request.
	 * @param busyWithLoad the load of the seed while making reservation for the request
	 */
	public void setBusyWithLoad(int busyWithLoad) {
		this.busyWithLoad = busyWithLoad;
	}
	
	
}
