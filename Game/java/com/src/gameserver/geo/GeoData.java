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
package com.src.gameserver.geo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.geo.pathfinding.Node;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.util.object.Point3D;


public class GeoData
{
	protected static final Log _log = LogFactory.getLog(GeoData.class);

	private static final class SingletonHolder
	{
		static
		{
			_log.info("Geodata Engine: Disabled.");
		}

		private static final GeoData INSTANCE = new GeoData();
	}

	protected GeoData()
	{
	}

	public static GeoData getInstance()
	{
		if(Config.GEODATA > 0)
		{
			return GeoEngine.getInstance();
		}
		else
		{
			return SingletonHolder.INSTANCE;
		}
	}

	public short getType(int x, int y)
	{
		return 0;
	}

	public short getHeight(int x, int y, int z)
	{
		return (short) z;
	}

	public short getSpawnHeight(int x, int y, int zmin, int zmax, L2Spawn spawn)
	{
		return (short) zmin;
	}

	public String geoPosition(int x, int y)
	{
		return "";
	}

	public boolean canSeeTarget(L2Object cha, L2Object target)
	{
		return Math.abs(target.getZ() - cha.getZ()) < 1000;
	}

	public boolean canSeeTarget(L2Object cha, Point3D worldPosition)
	{
		return Math.abs(worldPosition.getZ() - cha.getZ()) < 1000;
	}

	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return (Math.abs(z - tz) < 1000);
	}

	public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
	{
		return true;
	}

	public short getNSWE(int x, int y, int z)
	{
		return 15;
	}

	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz)
	{
		return new Location(tx, ty, tz);
	}

	public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return true;
	}

	public void addGeoDataBug(L2PcInstance gm, String comment)
	{
	}

	public void unloadGeodata(byte rx, byte ry)
	{
	}

	public boolean loadGeodataFile(byte rx, byte ry)
	{
		return false;
	}

	public boolean hasGeo(int x, int y)
	{
		return false;
	}

	public Node[] getNeighbors(Node n)
	{
		return null;
	}

}