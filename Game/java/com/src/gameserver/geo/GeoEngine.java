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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.StringTokenizer;

import com.src.Config;
import com.src.gameserver.geo.pathfinding.Node;
import com.src.gameserver.geo.pathfinding.cellnodes.CellPathFinding;
import com.src.gameserver.geo.util.Door;
import com.src.gameserver.geo.util.L2Arrays;
import com.src.gameserver.geo.util.LookupTable;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.Location;
import com.src.gameserver.model.actor.instance.L2DoorInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.actor.instance.L2SiegeGuardInstance;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.util.object.Point3D;


public final class GeoEngine extends GeoData
{
	private final static byte _e = 1;
	private final static byte _w = 2;
	private final static byte _s = 4;
	private final static byte _n = 8;

	private static final class SingletonHolder
	{
		private static final GeoEngine INSTANCE = new GeoEngine();
	}

	public static GeoEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}

	private final LookupTable<MappedByteBuffer> _geodata = new LookupTable<MappedByteBuffer>();
	private final LookupTable<IntBuffer> _geodataIndex = new LookupTable<IntBuffer>();
	private BufferedOutputStream _geoBugsOut;
	private static short count;

	private GeoEngine()
	{
		nInitGeodata();
	}

	@Override
	public short getType(int x, int y)
	{
		return nGetType((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4);
	}

	@Override
	public short getHeight(int x, int y, int z)
	{
		return nGetHeight((x - L2World.MAP_MIN_X) >> 4,(y - L2World.MAP_MIN_Y) >> 4, z);
	}

	@Override
	public short getSpawnHeight(int x, int y, int zmin, int zmax, L2Spawn spawn)
	{
		return nGetSpawnHeight((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, zmin, zmax, spawn);
	}

	@Override
	public String geoPosition(int x, int y)
	{
		int gx = (x - L2World.MAP_MIN_X) >> 4;
		int gy = (y - L2World.MAP_MIN_Y) >> 4;
		return "bx: " + getBlock(gx) + " by: " + getBlock(gy) + " cx: " + getCell(gx) + " cy: " + getCell(gy) + "  region offset: " + getRegionOffset(gx, gy);
	}

	@Override
	public boolean canSeeTarget(L2Object cha, Point3D target)
	{
		if((cha == null) || (target == null))
		{
		 	return false;
		}

		if(Door.getInstance().checkIfDoorsBetween(cha.getX(), cha.getY(), cha.getZ(), target.getX(), target.getY(), target.getZ()))
		{
			return false;
		}
		if(cha.getZ() >= target.getZ())
		{
			return canSeeTarget(cha.getX(), cha.getY(), cha.getZ(), target.getX(), target.getY(), target.getZ());
		}
		else
		{
			return canSeeTarget(target.getX(), target.getY(), target.getZ(), cha.getX(), cha.getY(), cha.getZ());
		}
	}

	@Override
	public boolean canSeeTarget(L2Object cha, L2Object target)
	{
		int z = cha.getZ() + 45;
		if(cha instanceof L2SiegeGuardInstance)
		{
			z += 30;
		}
		int z2 = target.getZ() + 45;
		if(target instanceof L2DoorInstance)
		{
			return true;
		}
		if(Door.getInstance().checkIfDoorsBetween(cha.getX(), cha.getY(), z, target.getX(), target.getY(), z2))
		{
			return false;
		}
		if(target instanceof L2SiegeGuardInstance)
		{
			z2 += 30;
		}
		if(z >= z2)
		{
			return canSeeTarget(cha.getX(), cha.getY(), z, target.getX(), target.getY(), z2);
		}
		else
		{
			return canSeeTarget(target.getX(), target.getY(), z2, cha.getX(), cha.getY(), z);
		}
	}

	@Override
	public boolean canSeeTargetDebug(L2PcInstance gm, L2Object target)
	{
		int z = gm.getZ() + 45;
		int z2 = target.getZ() + 45;
		if(target instanceof L2DoorInstance)
		{
			gm.sendMessage("door always true");
			return true;
		}

		if(z >= z2)
		{
			return canSeeDebug(gm, (gm.getX() - L2World.MAP_MIN_X) >> 4, (gm.getY() - L2World.MAP_MIN_Y) >> 4, z, (target.getX() - L2World.MAP_MIN_X) >> 4, (target.getY() - L2World.MAP_MIN_Y) >> 4, z2);
		}
		else
		{
			return canSeeDebug(gm, (target.getX() - L2World.MAP_MIN_X) >> 4, (target.getY() - L2World.MAP_MIN_Y) >> 4, z2, (gm.getX() - L2World.MAP_MIN_X) >> 4, (gm.getY() - L2World.MAP_MIN_Y) >> 4, z);
		}
	}

	@Override
	public short getNSWE(int x, int y, int z)
	{
		return nGetNSWE((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z);
	}

	@Override
	public boolean canMoveFromToTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		Location destiny = moveCheck(x, y, z, tx, ty, tz);
		return (destiny.getX() == tx && destiny.getY() == ty && destiny.getZ() == tz);
	}

	@Override
	public Location moveCheck(int x, int y, int z, int tx, int ty, int tz)
	{
		Location startpoint = new Location(x, y, z);
		if(Door.getInstance().checkIfDoorsBetween(x, y, z, tx, ty, tz))
		{
			return startpoint;
		}

		Location destiny = new Location(tx, ty, tz);
		return moveCheck(startpoint, destiny, (x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z, (tx - L2World.MAP_MIN_X) >> 4, (ty - L2World.MAP_MIN_Y) >> 4, tz);
	}

	@Override
	public void addGeoDataBug(L2PcInstance gm, String comment)
	{
		int gx = (gm.getX() - L2World.MAP_MIN_X) >> 4;
		int gy = (gm.getY() - L2World.MAP_MIN_Y) >> 4;
		int bx = getBlock(gx);
		int by = getBlock(gy);
		int cx = getCell(gx);
		int cy = getCell(gy);
		int rx = (gx >> 11) + 10;
		int ry = (gy >> 11) + 10;
		String out = rx + ";" + ry + ";" + bx + ";" + by + ";" + cx + ";" + cy + ";" + gm.getZ() + ";" + comment + "\n";
		try
		{
			_geoBugsOut.write(out.getBytes());
			_geoBugsOut.flush();
			gm.sendMessage("GeoData bug saved!");
		}
		catch(Exception e)
		{
			_log.error("", e);
			gm.sendMessage("GeoData bug save Failed!");
		}
	}

	@Override
	public boolean canSeeTarget(int x, int y, int z, int tx, int ty, int tz)
	{
		return canSee((x - L2World.MAP_MIN_X) >> 4, (y - L2World.MAP_MIN_Y) >> 4, z, (tx - L2World.MAP_MIN_X) >> 4, (ty - L2World.MAP_MIN_Y) >> 4, tz);
	}

	@Override
	public boolean hasGeo(int x, int y)
	{
		int gx = (x - L2World.MAP_MIN_X) >> 4;
		int gy = (y - L2World.MAP_MIN_Y) >> 4;
		short region = getRegionOffset(gx, gy);

		return _geodata.get(region) != null;
	}

	private boolean canSee(int x, int y, double z, int tx, int ty, int tz)
	{
		int dx = (tx - x);
		int dy = (ty - y);
		final double dz = (tz - z);
		final int distance2 = dx * dx + dy * dy;

		if(distance2 > 90000)
		{
			return false;
		}
		else if(distance2 < 82)
		{
			if(dz * dz > 22500)
			{
				short region = getRegionOffset(x, y);
				if(_geodata.get(region) != null)
				{
					return false;
				}
			}
			return true;
		}

		final int inc_x = sign(dx);
		final int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		final double inc_z_directionx = dz * dx / (distance2);
		final double inc_z_directiony = dz * dy / (distance2);

		int next_x = x;
		int next_y = y;

		if(dx >= dy)
		{
			int delta_A = 2 * dy;
			int d = delta_A - dx;
			int delta_B = delta_A - 2 * dx;

			for(int i = 0; i < dx; i++)
			{
				x = next_x;
				y = next_y;
				if(d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					z += inc_z_directionx;
					if(!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, tz, false))
					{
						return false;
					}
					next_y += inc_y;
					z += inc_z_directiony;
					if(!nLOS(next_x, y, (int) z, 0, inc_y, inc_z_directiony, tz, false))
					{
						return false;
					}
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
					z += inc_z_directionx;
					if(!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, tz, false))
					{
						return false;
					}
				}
			}
		}
		else
		{
			int delta_A = 2*dx;
			int d = delta_A - dy;
			int delta_B = delta_A - 2 * dy;
			for(int i = 0; i < dy; i++)
			{
				x = next_x;
				y = next_y;
				if(d > 0)
				{
					d += delta_B;
					next_y += inc_y;
					z += inc_z_directiony;
					if(!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, tz, false))
					{
						return false;
					}
					next_x += inc_x;
					z += inc_z_directionx;
					if(!nLOS(x, next_y, (int) z, inc_x, 0, inc_z_directionx, tz, false))
					{
						return false;
					}
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
					z += inc_z_directiony;
					if(!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, tz, false))
					{
						return false;
					}
				}
			}
		}
		return true;
	}

	private boolean canSeeDebug(L2PcInstance gm, int x, int y, double z, int tx, int ty, int tz)
	{
		int dx = (tx - x);
		int dy = (ty - y);
		final double dz = (tz - z);
		final int distance2 = dx * dx + dy * dy;

		if(distance2 > 90000)
		{
			gm.sendMessage("dist > 300");
			return false;
		}
		else if(distance2 < 82)
		{
			if(dz * dz > 22500)
			{
				short region = getRegionOffset(x, y);
				if(_geodata.get(region) != null)
				{
					return false;
				}
			}
			return true;
		}

		final int inc_x = sign(dx);
		final int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);
		final double inc_z_directionx = dz * dx / (distance2);
		final double inc_z_directiony = dz * dy / (distance2);

		gm.sendMessage("Los: from X: " + x + "Y: " + y + "--->> X: " + tx + " Y: " + ty);

		int next_x = x;
		int next_y = y;

		if(dx >= dy)
		{
			int delta_A = 2 * dy;
			int d = delta_A - dx;
			int delta_B = delta_A - 2 * dx;

			for(int i = 0; i < dx; i++)
			{
				x = next_x;
				y = next_y;
				if(d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					z += inc_z_directionx;
					if(!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, tz, true))
					{
						return false;
					}
					next_y += inc_y;
					z += inc_z_directiony;
					if(!nLOS(next_x, y, (int) z, 0, inc_y, inc_z_directiony, tz, true))
					{
						return false;
					}
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
					z += inc_z_directionx;
					if(!nLOS(x, y, (int) z, inc_x, 0, inc_z_directionx, tz, true))
					{
						return false;
					}
				}
			}
		}
		else
		{
			int delta_A = 2 * dx;
			int d = delta_A - dy;
			int delta_B = delta_A - 2 * dy;
			for(int i = 0; i < dy; i++)
			{
				x = next_x;
				y = next_y;
				if(d > 0)
				{
					d += delta_B;
					next_y += inc_y;
					z += inc_z_directiony;
					if(!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, tz, true))
					{
						return false;
					}
					next_x += inc_x;
					z += inc_z_directionx;
					if(!nLOS(x, next_y, (int) z, inc_x, 0, inc_z_directionx, tz, true))
					{
						return false;
					}
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
					z += inc_z_directiony;
					if(!nLOS(x, y, (int) z, 0, inc_y, inc_z_directiony, tz, true))
					{
						return false;
					}
				}
			}
		}
		return true;
	}

	private Location moveCheck(Location startpoint, Location destiny, int x, int y, double z, int tx, int ty, int tz)
	{
		int dx = (tx - x);
		int dy = (ty - y);
		final int distance2 = dx * dx + dy * dy;

		if(distance2 == 0)
		{
			return destiny;
		}
		if(distance2 > 36100)
		{
			double divider = Math.sqrt((double) 30000 / distance2);
			tx = x + (int) (divider * dx);
			ty = y + (int) (divider * dy);
			int dz = (tz - startpoint.getZ());
			tz = startpoint.getZ() + (int) (divider * dz);
			dx = (tx - x);
			dy = (ty - y);
		}

		final int inc_x = sign(dx);
		final int inc_y = sign(dy);
		dx = Math.abs(dx);
		dy = Math.abs(dy);

		int next_x = x;
		int next_y = y;
		double tempz = z;

		if(dx >= dy)
		{
			int delta_A = 2 * dy;
			int d = delta_A - dx;
			int delta_B = delta_A - 2 * dx;

			for(int i = 0; i < dx; i++)
			{
				x = next_x;
				y = next_y;
				if(d > 0)
				{
					d += delta_B;
					next_x += inc_x;
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y, tz);
					if(tempz == Double.MIN_VALUE)
					{
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					}
					else z = tempz;
					next_y += inc_y;
					tempz = nCanMoveNext(next_x, y, (int) z, next_x, next_y, tz);
					if(tempz == Double.MIN_VALUE)
					{
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					}
					else z = tempz;
				}
				else
				{
					d += delta_A;
					next_x += inc_x;
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y, tz);
					if(tempz == Double.MIN_VALUE)
					{
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					}
					else z = tempz;
				}
			}
		}
		else
		{
			int delta_A = 2 * dx;
			int d = delta_A - dy;
			int delta_B = delta_A - 2 * dy;
			for(int i = 0; i < dy; i++)
			{
				x = next_x;
				y = next_y;
				if(d > 0)
				{
					d += delta_B;
					next_y += inc_y;
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y, tz);
					if(tempz == Double.MIN_VALUE)
					{
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					}
					else z = tempz;
					next_x += inc_x;
					tempz = nCanMoveNext(x, next_y, (int) z, next_x, next_y, tz);
					if(tempz == Double.MIN_VALUE)
					{
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					}
					else z = tempz;
				}
				else
				{
					d += delta_A;
					next_y += inc_y;
					tempz = nCanMoveNext(x, y, (int) z, next_x, next_y, tz);
					if(tempz == Double.MIN_VALUE)
					{
						return new Location((x << 4) + L2World.MAP_MIN_X, (y << 4) + L2World.MAP_MIN_Y, (int) z);
					}
					else z = tempz;
				}
			}
		}
		if(z == startpoint.getZ())
		{
			return destiny;
		}
		else
		{
			return new Location(destiny.getX(), destiny.getY(), (int) z);
		}
	}

	private byte sign(int x)
	{
		if(x >= 0)
		{
			return +1;
		}
		else
		{
			return -1;
		}
	}

	private void nInitGeodata()
	{
		BufferedReader lnr = null;
		try
		{
			_log.info("Geo Engine: - Loading Geodata...");
			File Data = new File(Config.DATAPACK_ROOT, "data/geodata/geo_index.txt");
			if(!Data.exists())
			{
				return;
			}

			lnr = new BufferedReader(new FileReader(Data));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load geo_index File.");
		}
		String line;
		try
		{
			while((line = lnr.readLine()) != null)
			{
				if(line.trim().length() == 0)
				{
					continue;
				}
				StringTokenizer st = new StringTokenizer(line, "_");
				byte rx = Byte.parseByte(st.nextToken());
				byte ry = Byte.parseByte(st.nextToken());
				loadGeodataFile(rx,ry);
			}
			_log.info("Geo Engine: Loaded " + count + " geofiles.");
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Read geo_index File.");
		}
		finally
		{
			try
			{
				lnr.close();
			}
			catch(Exception e)
			{
			}
		}
		try
		{
			File geo_bugs = new File(Config.DATAPACK_ROOT, "data/geodata/geo_bugs.txt");
			_geoBugsOut = new BufferedOutputStream(new FileOutputStream(geo_bugs, true));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			throw new Error("Failed to Load geo_bugs.txt File.");
		}
	}

	@Override
	public void unloadGeodata(byte rx, byte ry)
	{
		short regionoffset = (short) ((rx << 5) + ry);
		_geodataIndex.set(regionoffset, null);
		_geodata.set(regionoffset, null);
	}

	@SuppressWarnings("resource")
	@Override
	public boolean loadGeodataFile(byte rx, byte ry)
	{
		if(rx < L2World.MAP_MIN_X || rx > L2World.MAP_MAX_X || ry < L2World.MAP_MIN_Y || ry > L2World.MAP_MAX_Y)
		{
			_log.warn("Failed to load geofile: invalid region " + rx +","+ ry + "\n");
			return false;
		}

		String fname = "data/geodata/" + rx + "_" + ry + ".l2j";
		short regionoffset = (short) ((rx << 5) + ry);

		count++;

		File Geo = new File(Config.DATAPACK_ROOT, fname);
		int size, index = 0, block = 0, flor = 0;
		FileChannel roChannel = null;
		try
		{
			roChannel = new RandomAccessFile(Geo, "r").getChannel();
			size = (int) roChannel.size();
			MappedByteBuffer geo;
			if(Config.FORCE_GEODATA)
			{
				geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size).load();
			}
			else
			{
				geo = roChannel.map(FileChannel.MapMode.READ_ONLY, 0, size);
			}
			geo.order(ByteOrder.LITTLE_ENDIAN);

			if(size > 196608)
			{
				IntBuffer indexs = IntBuffer.allocate(65536);
				while(block < 65536)
				{
					byte type = geo.get(index);
					indexs.put(block,index);
					block++;
					index++;
					if(type == 0)
					{
						index += 2;
					}
					else if(type == 1)
					{
						index += 128;
					}
					else
					{
						for(int b = 0; b < 64; b++)
						{
							byte layers = geo.get(index);
							index += (layers << 1) + 1;
							if(layers > flor)
							{
								flor = layers;
							}
						}
					}
				}
				_geodataIndex.set(regionoffset, indexs);
			}
			_geodata.set(regionoffset,geo);

		}
		catch(Exception e)
		{
			e.printStackTrace();
			_log.warn("Failed to Load GeoFile at block: " + block + "\n");
			return false;
		}
		finally
		{
			try
			{
				if(roChannel != null)
				{
					roChannel.close();
				}
			}
			catch(Exception e)
			{
			}
		}
		return true;
	}

	private short getRegionOffset(int x, int y)
	{
		int rx = x >> 11;
		int ry = y >> 11;
		return (short)(((rx+16) << 5) + (ry+10));
	}

	private int getBlock(int geo_pos)
	{
		return (geo_pos >> 3) % 256;
	}

	private int getCell(int geo_pos)
	{
		return geo_pos % 8;
	}

	private short nGetType(int x, int y)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int index = 0;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			}
			return 0;
		}
		return geo.get(index);
	}

	private short nGetHeight(int geox, int geoy, int z)
	{
		short region = getRegionOffset(geox, geoy);
		int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			}
			return (short) z;
		}
		byte type = geo.get(index);
		index++;
		if(type == 0)
		{
			return geo.getShort(index);
		}
		else if(type == 1)
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1);
			return height;
		}
		else
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			int offset = (cellX << 3) + cellY;
			while(offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height = -1;
			if(layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case1), region: " + region + " - invalid layer count: " + layers + " at: " + geox + " " + geoy);
				return (short) z;
			}
			short temph = Short.MIN_VALUE;
			while(layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1);
				if((z-temph) * (z-temph) > (z-height) * (z-height))
				{
					temph = height;
				}
				layers--;
				index += 2;
			}
			return temph;
		}
	}

	private short nGetUpperHeight(int geox, int geoy, int z)
	{
		short region = getRegionOffset(geox, geoy);
		int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			}
			return (short) z;
		}
		byte type = geo.get(index);
		index++;
		if(type == 0)
		{
			return geo.getShort(index);
		}
		else if(type == 1)
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1);
			return height;
		}
		else
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			int offset = (cellX << 3) + cellY;
			while(offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height =- 1;
			if(layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case1), region: " + region + " - invalid layer count: " + layers + " at: " + geox + " " + geoy);
				return (short) z;
			}
			short temph = Short.MAX_VALUE;
			while(layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1);
				if(height < z)
				{
					return temph;
				}
				temph = height;
				layers--;
				index += 2;
			}
			return temph;
		}
	}

	private short nGetSpawnHeight(int geox, int geoy, int zmin, int zmax, L2Spawn spawn)
	{
		short region = getRegionOffset(geox, geoy);
		int blockX = getBlock(geox);
		int blockY = getBlock(geoy);
		int cellX, cellY, index;
		short temph = Short.MIN_VALUE;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			}
			return (short) zmin;
		}
		byte type = geo.get(index);
		index++;
		if(type == 0)
		{
			temph = geo.getShort(index);
		}
		else if(type == 1)
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1);
			temph = height;
		}
		else
		{
			cellX = getCell(geox);
			cellY = getCell(geoy);
			short height;
			int offset = (cellX << 3) + cellY;
			while(offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			if(layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case2), region: " + region + " - invalid layer count: " + layers + " at: " + geox + " " + geoy);
				return (short) zmin;
			}
			while(layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1);
				if((zmin-temph) * (zmin-temph) > (zmin-height) * (zmin-height))
				{
					temph = height;
				}
				layers--;
				index += 2;
			}
			if(temph > zmax + 200 || temph < zmin - 200)
			{
				if(_log.isDebugEnabled())
				{
					_log.warn("SpawnHeight Error - Couldnt find correct layer to spawn NPC - GeoData or Spawnlist Bug!: zmin: " + zmin + " zmax: " + zmax + " value: " + temph + " SpawnId: " + spawn + " at: " + geox + " : " + geoy);
				}
				return (short) zmin;
			}
		}
		if(temph > zmax + 1000 || temph < zmin - 1000)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("SpawnHeight Error - Spawnlist z value is wrong or GeoData error: zmin: " + zmin + " zmax: " + zmax + " value: " + temph + " SpawnId: " + spawn + " at: " + geox + " : " + geoy);
			}
			return (short) zmin;
		}
		return temph;
	}

	private double nCanMoveNext(int x, int y, int z, int tx, int ty, int tz)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
		short NSWE = 0;

		int index = 0;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " doesn't exist!!");
			}
			return z;
		}
		byte type = geo.get(index);
		index++;
		if(type == 0)
		{
			return geo.getShort(index);
		}
		else if(type == 1)
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			NSWE = (short) (height & 0x0F);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1);
			if(checkNSWE(NSWE, x, y, tx, ty))
			{
				return height;
			}
			else
			{
				return Double.MIN_VALUE;
			}
		}
		else
		{
			cellX = getCell(x);
			cellY = getCell(y);
			int offset = (cellX << 3) + cellY;
			while(offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height = -1;
			if(layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case3), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return z;
			}
			short tempz = Short.MIN_VALUE;
			while(layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1);

				if((z-tempz) * (z-tempz) > (z-height) * (z-height))
				{
					tempz = height;
					NSWE = geo.getShort(index);
					NSWE = (short) (NSWE & 0x0F);
				}
				layers--;
				index += 2;
			}
			if(checkNSWE(NSWE, x, y, tx, ty))
			{
				return tempz;
			}
			else
			{
				return Double.MIN_VALUE;
			}
		}
	}

	private boolean nLOS(int x, int y, int z, int inc_x, int inc_y, double inc_z, int tz, boolean debug)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
		short NSWE = 0;

		int index;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			}
			return true;
		}
		byte type = geo.get(index);
		index++;
		if(type == 0)
		{
			short height = geo.getShort(index);
			if(debug)
			{
				_log.warn("flatheight:" + height);
			}
			if(z > height)
			{
				return inc_z > height;
			}
			else
			{
				return inc_z < height;
			}
		}
		else if(type == 1)
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			NSWE = (short) (height & 0x0F);
			height = (short) (height & 0x0fff0);
			height = (short) (height >> 1);
			if(!checkNSWE(NSWE, x, y, x + inc_x, y + inc_y))
			{
				if(debug)
				{
					_log.warn("height:" + height + " z" + z);
				}

				return z >= nGetUpperHeight(x + inc_x, y + inc_y, height);
			}
			else
			{
				return true;
			}
		}
		else
		{
			cellX = getCell(x);
			cellY = getCell(y);

			int offset = (cellX << 3) + cellY;
			while(offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);

			index++;
			short tempZ = -1;
			if(layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case4), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return false;
			}
			short upperHeight = Short.MAX_VALUE;
			short lowerHeight = Short.MIN_VALUE;
			byte temp_layers = layers;
			boolean highestlayer = true;
			while(temp_layers > 0)
			{
				tempZ = geo.getShort(index);
				tempZ = (short) (tempZ & 0x0fff0);
				tempZ = (short) (tempZ >> 1);

				if(z > tempZ)
				{
					lowerHeight = tempZ;
					NSWE = geo.getShort(index);
					NSWE = (short) (NSWE & 0x0F);
					break;
				}
				else
				{
					highestlayer = false;
					upperHeight = tempZ;
				}

				temp_layers--;
				index += 2;
			}
			if(debug)
			{
				_log.warn("z:" + z + " x: " + cellX + " y:" + cellY + " la " + layers + " lo:" + lowerHeight + " up:" + upperHeight);
			}
			if((z - upperHeight) < -10 && (z - upperHeight) > inc_z - 10 && (z - lowerHeight) > 40)
			{
				if(debug)
				{
					_log.warn("false, incz" + inc_z);
				}
				return false;
			}

			if(!highestlayer)
			{
				if(!checkNSWE(NSWE, x, y, x + inc_x, y + inc_y))
				{
					if(debug)
					{
						_log.warn("block and next in x" + inc_x + " y" + inc_y + " is:" + nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight));
					}
					return z >= nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight);
				}
				else
				{
					return true;
				}
			}
			if(!checkNSWE(NSWE, x, y, x + inc_x, y + inc_y))
			{
				return z >= nGetUpperHeight(x + inc_x, y + inc_y, lowerHeight);
			}
			else
			{
				return true;
			}
		}
	}

	private short nGetNSWE(int x, int y, int z)
	{
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
		short NSWE = 0;

		int index = 0;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			}
			return 15;
		}
		byte type = geo.get(index);
		index++;
		if(type == 0)
		{
			return 15;
		}
		else if(type == 1)
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			NSWE = (short) (height & 0x0F);
		}
		else
		{
			cellX = getCell(x);
			cellY = getCell(y);
			int offset = (cellX << 3) + cellY;
			while(offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height = -1;
			if(layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case5), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return 15;
			}
			short tempz = Short.MIN_VALUE;
			while(layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1);

				if((z-tempz) * (z-tempz) > (z-height) * (z-height))
				{
					tempz = height;
					NSWE = geo.get(index);
					NSWE = (short) (NSWE & 0x0F);
				}
				layers--;
				index += 2;
			}
		}
		return NSWE;
	}

	@Override
	public Node[] getNeighbors(Node n)
	{
		Node newNode;
		int x = n.getNodeX();
		int y = n.getNodeY();
		int parentdirection = 0;
		if(n.getParent() != null)
		{
			if(n.getParent().getNodeX() > x)
			{
				parentdirection = 1;
			}
			if(n.getParent().getNodeX() < x)
			{
				parentdirection = -1;
			}
			if(n.getParent().getNodeY() > y)
			{
				parentdirection = 2;
			}
			if(n.getParent().getNodeY() < y)
			{
				parentdirection = -2;
			}
		}
		short z = n.getZ();
		short region = getRegionOffset(x, y);
		int blockX = getBlock(x);
		int blockY = getBlock(y);
		int cellX, cellY;
		short NSWE = 0;
		int index = 0;
		if(_geodataIndex.get(region) == null)
		{
			index = ((blockX << 8) + blockY) * 3;
		}
		else
		{
			index = _geodataIndex.get(region).get((blockX << 8) + blockY);
		}
		ByteBuffer geo = _geodata.get(region);
		if(geo == null)
		{
			if(_log.isDebugEnabled())
			{
				_log.warn("Geo Region - Region Offset: " + region + " dosnt exist!!");
			}
			return null;
		}

		final Node[] Neighbors = new Node[4];
		int arrayIndex = 0;

		byte type = geo.get(index);
		index++;
		if(type == 0)
		{
			short height = geo.getShort(index);
			n.setZ(height);
			if(parentdirection != 1)
			{
				newNode = CellPathFinding.getInstance().readNode(x + 1, y, height);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != 2)
			{
				newNode = CellPathFinding.getInstance().readNode(x, y + 1, height);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != -2)
			{
				newNode = CellPathFinding.getInstance().readNode(x, y - 1, height);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != -1)
			{
				newNode = CellPathFinding.getInstance().readNode(x - 1, y, height);
				Neighbors[arrayIndex++] = newNode;
			}
		}
		else if(type == 1)
		{
			cellX = getCell(x);
			cellY = getCell(y);
			index += ((cellX << 3) + cellY) << 1;
			short height = geo.getShort(index);
			NSWE = (short) (height & 0x0F);
			height = (short) (height & 0x0fff0);
			height = (short)(height >> 1);
			n.setZ(height);
			if(NSWE != 15 && parentdirection != 0)
			{
				return null;
			}
			if(parentdirection != 1 && checkNSWE(NSWE, x, y, x + 1, y))
			{
				newNode = CellPathFinding.getInstance().readNode(x + 1, y, height);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != 2 && checkNSWE(NSWE, x, y, x, y + 1))
			{
				newNode = CellPathFinding.getInstance().readNode(x, y + 1, height);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != -2 && checkNSWE(NSWE, x, y, x, y - 1))
			{
				newNode = CellPathFinding.getInstance().readNode(x, y - 1, height);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != -1 && checkNSWE(NSWE, x, y, x - 1, y))
			{
				newNode = CellPathFinding.getInstance().readNode(x-1, y, height);
				Neighbors[arrayIndex++] = newNode;
			}
		}
		else
		{
			cellX = getCell(x);
			cellY = getCell(y);
			int offset = (cellX << 3) + cellY;
			while(offset > 0)
			{
				byte lc = geo.get(index);
				index += (lc << 1) + 1;
				offset--;
			}
			byte layers = geo.get(index);
			index++;
			short height = -1;
			if(layers <= 0 || layers > 125)
			{
				_log.warn("Broken geofile (case5), region: " + region + " - invalid layer count: " + layers + " at: " + x + " " + y);
				return null;
			}
			short tempz = Short.MIN_VALUE;
			while(layers > 0)
			{
				height = geo.getShort(index);
				height = (short) (height & 0x0fff0);
				height = (short) (height >> 1);

				if((z-tempz) * (z-tempz) > (z-height) * (z-height))
				{
					tempz = height;
					NSWE = geo.get(index);
					NSWE = (short) (NSWE & 0x0F);
				}
				layers--;
				index += 2;
			}
			n.setZ(tempz);
			if(NSWE != 15 && parentdirection != 0)
			{
				return null;
			}
			if(parentdirection != 1 && checkNSWE(NSWE, x, y, x + 1, y))
			{
				newNode = CellPathFinding.getInstance().readNode(x+1,y,tempz);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != 2 && checkNSWE(NSWE, x, y, x, y + 1))
			{
				newNode = CellPathFinding.getInstance().readNode(x, y + 1, tempz);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != -2 && checkNSWE(NSWE, x, y, x, y - 1))
			{
				newNode = CellPathFinding.getInstance().readNode(x, y - 1, tempz);
				Neighbors[arrayIndex++] = newNode;
			}
			if(parentdirection != -1 && checkNSWE(NSWE, x, y, x - 1, y))
			{
				newNode = CellPathFinding.getInstance().readNode(x - 1, y, tempz);
				Neighbors[arrayIndex++] = newNode;
			}
		}

		return L2Arrays.compact(Neighbors);
	}

	private boolean checkNSWE(short NSWE, int x, int y, int tx, int ty)
	{
		if(NSWE == 15)
		{
			return true;
		}
		if(tx > x)
		{
			if((NSWE & _e) == 0)
			{
				return false;
			}
		}
		else if(tx < x)
		{
			if((NSWE & _w) == 0)
			{
				return false;
			}
		}
		if(ty > y)
		{
			if((NSWE & _s) == 0)
			{
				return false;
			}
		}
		else if(ty < y)
		{
			if((NSWE & _n) == 0)
			{
				return false;
			}
		}
		return true;
	}

}