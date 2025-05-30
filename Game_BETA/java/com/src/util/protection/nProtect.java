/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package com.src.util.protection;

import java.lang.reflect.Method;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.L2GameClient;
import com.src.gameserver.network.serverpackets.GameGuardQuery;

public class nProtect
{
	private final static Log _log = LogFactory.getLog(nProtect.class);

	public static enum RestrictionType
	{
		RESTRICT_ENTER,RESTRICT_EVENT,RESTRICT_OLYMPIAD,RESTRICT_SIEGE
	}

	public class nProtectAccessor
	{
		public nProtectAccessor()
		{}

		public void setCheckGameGuardQuery(Method m)
		{
			nProtect.this._checkGameGuardQuery = m;
		}

		public void setStartTask(Method m)
		{
			nProtect.this._startTask = m;
		}

		public void setCheckRestriction(Method m)
		{
			nProtect.this._checkRestriction = m;
		}

		public void setSendRequest(Method m)
		{
			nProtect.this._sendRequest = m;
		}

		public void setCloseSession(Method m)
		{
			nProtect.this._closeSession = m;
		}

		public void setSendGGQuery(Method m)
		{
			nProtect.this._sendGGQuery = m;
		}
	}
	protected Method _checkGameGuardQuery = null;
	protected Method _startTask = null;
	protected Method _checkRestriction = null;
	protected Method _sendRequest = null;
	protected Method _closeSession = null;
	protected Method _sendGGQuery = null;
	private static nProtect _instance = null;
	public static nProtect getInstance()
	{
		if(_instance == null)
		{
			_instance = new nProtect();
		}
		return _instance;
	}

	private nProtect()
	{
		Class<?> clazz=null;
		try
		{
			try
			{
				clazz = Class.forName("com.net.protection.main");
			}
			catch(ClassNotFoundException e)
			{}
			if(clazz!=null)
			{
				Method m = clazz.getMethod("init", nProtectAccessor.class);
				if(m!=null)
				{
					m.invoke(null, new nProtectAccessor());
				}
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public void sendGameGuardQuery(GameGuardQuery pkt)
	{
		try
		{
			if(_sendGGQuery!=null)
			{
				_sendGGQuery.invoke(pkt);
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public boolean checkGameGuardRepy(L2GameClient cl, int [] reply)
	{
		try
		{
			if(_checkGameGuardQuery!=null)
			{
				return (Boolean)_checkGameGuardQuery.invoke(null, cl,reply);
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		return true;
	}

	public ScheduledFuture<?> startTask(L2GameClient client)
	{
		try
		{
			if(_startTask != null)
			{
				return (ScheduledFuture<?>)_startTask.invoke(null, client);
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		return null;
	}

	public void sendRequest(L2GameClient cl)
	{
		if(_sendRequest!=null)
		{
			try
			{
				_sendRequest.invoke(null, cl);
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
		}
	}

	public void closeSession(L2GameClient cl)
	{
		if(_closeSession!=null)
		{
			try
			{
				_closeSession.invoke(null, cl);
			}
			catch(Exception e)
			{}
		}
	}

	public boolean checkRestriction(L2PcInstance player, RestrictionType type, Object... params)
	{
		try
		{
			if(_checkRestriction!=null)
			{
				return (Boolean)_checkRestriction.invoke(null,player,type,params);
			}
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		return true;
	}

}