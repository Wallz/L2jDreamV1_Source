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
package com.src.gameserver.model;

import javolution.util.FastList;

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.RadarControl;
import com.src.gameserver.thread.ThreadPoolManager;

public final class L2Radar
{
	private L2PcInstance _player;
	private FastList<RadarMarker> _markers;

	public L2Radar(L2PcInstance player)
	{
		_player = player;
		_markers = new FastList<RadarMarker>();
	}

	public void addMarker(int x, int y, int z)
	{
		RadarMarker newMarker = new RadarMarker(x, y, z);

		_markers.add(newMarker);
		_player.sendPacket(new RadarControl(2, 2, x, y, z));
		_player.sendPacket(new RadarControl(0, 1, x, y, z));

		newMarker = null;
	}

	public void removeMarker(int x, int y, int z)
	{
		RadarMarker newMarker = new RadarMarker(x, y, z);

		_markers.remove(newMarker);
		_player.sendPacket(new RadarControl(1, 1, x, y, z));

		newMarker = null;
	}

	public void removeAllMarkers()
	{
		for(RadarMarker tempMarker : _markers)
		{
			_player.sendPacket(new RadarControl(2, 2, tempMarker._x, tempMarker._y, tempMarker._z));
		}

		_markers.clear();
	}

	public void loadMarkers()
	{
		_player.sendPacket(new RadarControl(2, 2, _player.getX(), _player.getY(), _player.getZ()));
		for(RadarMarker tempMarker : _markers)
			_player.sendPacket(new RadarControl(0, 1, tempMarker._x, tempMarker._y, tempMarker._z));
	}

	public class RadarOnPlayer implements Runnable
	{
		private final L2PcInstance _myTarget, _me;

		public RadarOnPlayer(L2PcInstance target, L2PcInstance me)
		{
			_me = me;
			_myTarget = target;
		}

		@Override
		public void run()
		{
			try
			{
				if(_me == null || _me.isOnline()==0)
				{
					return;
				}

				_me.sendPacket(new RadarControl(1, 1, _me.getX(), _me.getY(), _me.getZ()));
				if(_myTarget == null || _myTarget.isOnline()==0)
				{
					return;
				}

				_me.sendPacket(new RadarControl(0, 1, _myTarget.getX(), _myTarget.getY(), _myTarget.getZ()));
				ThreadPoolManager.getInstance().scheduleGeneral(new RadarOnPlayer(_myTarget,_me), 15000);
			}
			catch(Throwable t)
			{
			}
		}
	}

	public static class RadarMarker
	{
		public int _type, _x, _y, _z;

		public RadarMarker(int type, int x, int y, int z)
		{
			_type = type;
			_x = x;
			_y = y;
			_z = z;
		}

		public RadarMarker(int x, int y, int z)
		{
			_type = 1;
			_x = x;
			_y = y;
			_z = z;
		}

		/** 
		 * @see java.lang.Object#hashCode() 
		 */ 
		@Override 
		public int hashCode() 
		{ 
			final int prime = 31; 
			int result = 1; 
			result = prime * result + _type; 
			result = prime * result + _x; 
			result = prime * result + _y; 
			result = prime * result + _z; 
			return result; 
		} 

		/**
		 * @see java.lang.Object#equals(java.lang.Object) 
		 */
		@Override
		public boolean equals(Object obj)
		{
			if(this == obj) 
				return true; 

			if(obj == null) 
				return false; 

			if(!(obj instanceof RadarMarker)) 
				return false; 

			final RadarMarker other = (RadarMarker) obj; 
			if(_type != other._type) 
				return false; 

			if(_x != other._x) 
				return false; 

			if(_y != other._y) 
				return false; 

			if(_z != other._z) 
				return false; 

			return true;
 	       }
	}

}