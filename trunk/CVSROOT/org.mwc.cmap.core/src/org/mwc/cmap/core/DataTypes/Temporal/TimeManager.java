/**
 * 
 */
package org.mwc.cmap.core.DataTypes.Temporal;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.eclipse.core.runtime.Status;
import org.mwc.cmap.core.CorePlugin;

import MWC.GenericData.HiResDate;
import MWC.GenericData.TimePeriod;

/**
 * @author ian.mayo
 */
public class TimeManager implements ControllableTime, TimeProvider
{

	/**
	 * manage all of the listeners, etc.
	 */
	private PropertyChangeSupport _pSupport;

	/**
	 * the current time
	 */
	private HiResDate _currentTime;

	/**
	 * the time period covered by the data
	 */
	private TimePeriod _timePeriod;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mwc.cmap.core.DataTypes.Temporal.ControllableTime#setTime(java.lang.Object,
	 *      MWC.GenericData.HiResDate)
	 */
	public void setTime(Object origin, HiResDate newDate, boolean fireUpdate)
	{
		// are we managing our own time period?
		if (_timePeriod != null)
		{
			if (!_timePeriod.contains(newDate))
			{
				// bugger. it's outside the time period
				// throw a wobbly
				CorePlugin.logError(Status.ERROR, "New time outside data period", null);
			}
		}

		// ok. remember the old time (if we have one)
		HiResDate oldTime = null;
		if (_currentTime != null)
			oldTime = new HiResDate(_currentTime);

		// store the new time
		_currentTime = newDate;

		// do we want to fire the update?
		if (fireUpdate)
		{
			// do we have any listeners?
			if (_pSupport != null)
			{
				_pSupport.firePropertyChange(TIME_CHANGED_PROPERTY_NAME, oldTime, _currentTime);
			}
		}

		// done.
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mwc.cmap.core.DataTypes.Temporal.TimeProvider#getPeriod()
	 */
	public TimePeriod getPeriod()
	{
		return _timePeriod;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mwc.cmap.core.DataTypes.Temporal.TimeProvider#getTime()
	 */
	public HiResDate getTime()
	{
		return _currentTime;
	}

	/**
	 * let somebody start listening to our changes
	 * 
	 * @param listener
	 *          the new listener
	 * @param propertyType
	 *          the (optional) property to listen to. Use null if you don't mind
	 */
	public void addListener(PropertyChangeListener listener, String propertyType)
	{
		if (_pSupport == null)
			_pSupport = new PropertyChangeSupport(this);

		_pSupport.addPropertyChangeListener(propertyType, listener);
	}

	/**
	 * let somebody stop listening to our changes
	 * 
	 * @param listener
	 *          the old listener
	 * @param propertyType
	 *          the (optional) property to stop listening to. Use null if you
	 *          don't mind
	 */
	public void removeListener(PropertyChangeListener listener, String propertyType)
	{
		_pSupport.removePropertyChangeListener(propertyType, listener);
	}

	/**
	 * let somebody specify the time period we're managing.
	 * 
	 * @param origin -
	 *          whoever is setting the time period (so that they can optionally
	 *          ignore changes they triggered)
	 * @param period -
	 *          the new time period
	 */
	public void setPeriod(Object origin, TimePeriod period)
	{
		TimePeriod oldTime = null;

		// ok. remember the old period
		// right, do we have an old period?
		if (_timePeriod != null)
			oldTime = new TimePeriod.BaseTimePeriod(_timePeriod.getStartDTG(), _timePeriod
					.getEndDTG());

		// store the new time
		_timePeriod = period;

		// do we have any listeners?
		if (_pSupport != null)
		{
			_pSupport.firePropertyChange(PERIOD_CHANGED_PROPERTY_NAME, oldTime, _currentTime);
		}

	}

}
