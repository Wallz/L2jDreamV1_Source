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
package com.src.gameserver.cache;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javolution.util.FastMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.Config;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Clan;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class CrestCache
{
	private static Log _log = LogFactory.getLog(CrestCache.class);

	public static CrestCache getInstance()
	{
		return SingletonHolder._instance;
	}

	private final FastMap<Integer, byte[]> _cachePledge = new FastMap<Integer, byte[]>().shared();
	private final FastMap<Integer, byte[]> _cachePledgeLarge = new FastMap<Integer, byte[]>().shared();
	private final FastMap<Integer, byte[]> _cacheAlly = new FastMap<Integer, byte[]>().shared();

	private int _loadedFiles;
	private long _bytesBuffLen;

	private CrestCache()
	{
		convertOldPedgeFiles();
		reload();
	}

	public synchronized void reload()
	{
		FileFilter filter = new BmpFilter();

		File dir = new File(Config.DATAPACK_ROOT, "data/crests/");

		File[] files = dir.listFiles(filter);
		if(files == null)
			files = new File[0];
		byte[] content;

		_loadedFiles = 0;
		_bytesBuffLen = 0;

		_cachePledge.clear();
		_cachePledgeLarge.clear();
		_cacheAlly.clear();

		for(File file : files)
		{
			RandomAccessFile f = null;
			try
			{
				f = new RandomAccessFile(file, "r");
				content = new byte[(int)f.length()];
				f.readFully(content);

				if(file.getName().startsWith("Crest_Large_"))
				{
					_cachePledgeLarge.put(Integer.valueOf(file.getName().substring(12, file.getName().length() - 4)), content);
				}
				else if(file.getName().startsWith("Crest_"))
				{
					_cachePledge.put(Integer.valueOf(file.getName().substring(6, file.getName().length() - 4)), content);
				}
				else if(file.getName().startsWith("AllyCrest_"))
				{
					_cacheAlly.put(Integer.valueOf(file.getName().substring(10, file.getName().length() - 4)), content);
				}
				_loadedFiles++;
				_bytesBuffLen += content.length;
			}
			catch(Exception e)
			{
				_log.error("Problem with loading crest bmp file: " + file, e);
			}
			finally
			{
				try
				{
					if(f != null)
						f.close();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		}

		_log.info("Cache[Crest]: " + getLoadedFiles() + " files loaded.");
	}

	public void convertOldPedgeFiles()
	{
		File dir = new File(Config.DATAPACK_ROOT, "data/crests/");

		File[] files = dir.listFiles(new OldPledgeFilter());

		if(files == null)
			files = new File[0];

		for(File file : files)
		{
			int clanId = Integer.parseInt(file.getName().substring(7, file.getName().length() - 4));

			_log.info("Found old crest file \"" + file.getName() + "\" for clanId " + clanId);

			int newId = IdFactory.getInstance().getNextId();

			L2Clan clan = ClanTable.getInstance().getClan(clanId);

			if(clan != null)
			{
				removeOldPledgeCrest(clan.getCrestId());

				file.renameTo(new File(Config.DATAPACK_ROOT, "data/crests/Crest_" + newId + ".bmp"));
				_log.info("Renamed Clan crest to new format: Crest_" + newId + ".bmp");

				Connection con = null;

				try
				{
					con = L2DatabaseFactory.getInstance().getConnection();
					PreparedStatement statement = con.prepareStatement("UPDATE clan_data SET crest_id = ? WHERE clan_id = ?");
					statement.setInt(1, newId);
					statement.setInt(2, clan.getClanId());
					statement.executeUpdate();
					ResourceUtil.closeStatement(statement);
				}
				catch(SQLException e)
				{
					_log.error("Could not update the crest id:", e);
				}
				finally
				{
					ResourceUtil.closeConnection(con); 
				}

				clan.setCrestId(newId);
				clan.setHasCrest(true);
			}
			else
			{
				_log.info("Clan Id: " + clanId + " does not exist in table.. deleting.");
				file.delete();
			}
		}
	}

	@Override
	public String toString()
	{
		return "Cache[Crest] : " + String.format("%.3f", (float)_bytesBuffLen / 1048576) + " megabytes on " + _loadedFiles + " file(s) loaded.";
	}

	public int getLoadedFiles()
	{
		return _loadedFiles;
	}

	public byte[] getPledgeCrest(int id)
	{
		return _cachePledge.get(id);
	}

	public byte[] getPledgeCrestLarge(int id)
	{
		return _cachePledgeLarge.get(id);
	}

	public byte[] getAllyCrest(int id)
	{
		return _cacheAlly.get(id);
	}

	public void removePledgeCrest(int id)
	{
		File crestFile = new File(Config.DATAPACK_ROOT, "data/crests/Crest_" + id + ".bmp");
		_cachePledge.remove(id);
		try
		{
			crestFile.delete();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public boolean removePledgeCrestLarge(int id)
	{
		File crestFile = new File(Config.DATAPACK_ROOT, "data/crests/Crest_Large_" + id + ".bmp");
		try
		{
			crestFile.delete();
		}
		catch(Exception e)
		{
			_log.error("", e);
			return false;
		}
		return _cachePledgeLarge.remove(id) != null;
	}

	public void removeOldPledgeCrest(int id)
	{
		File crestFile = new File(Config.DATAPACK_ROOT, "data/crests/Pledge_" + id + ".bmp");
		try
		{
			crestFile.delete();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public void removeAllyCrest(int id)
	{
		File crestFile = new File(Config.DATAPACK_ROOT, "data/crests/AllyCrest_" + id + ".bmp");
		_cacheAlly.remove(id);
		try
		{
			crestFile.delete();
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
	}

	public boolean savePledgeCrest(int newId, byte[] data)
	{
		File crestFile = new File(Config.DATAPACK_ROOT, "data/crests/Crest_" + newId + ".bmp");
		try
		{
			FileOutputStream out = new FileOutputStream(crestFile);
			out.write(data);
			out.close();
			_cachePledge.put(newId, data);
			return true;
		}
		catch(IOException e)
		{
			_log.error("Error saving pledge crest" + crestFile + ":", e);
			return false;
		}
	}

	public boolean savePledgeCrestLarge(int newId, byte[] data)
	{
		File crestFile = new File(Config.DATAPACK_ROOT, "data/crests/Crest_Large_" + newId + ".bmp");
		try
		{
			FileOutputStream out = new FileOutputStream(crestFile);
			out.write(data);
			out.close();
			_cachePledgeLarge.put(newId, data);
			return true;
		}
		catch(IOException e)
		{
			_log.error("Error saving Large pledge crest" + crestFile + ":", e);
			return false;
		}
	}

	public boolean saveAllyCrest(int newId, byte[] data)
	{
		File crestFile = new File(Config.DATAPACK_ROOT, "data/crests/AllyCrest_" + newId + ".bmp");
		try
		{
			FileOutputStream out = new FileOutputStream(crestFile);
			out.write(data);
			out.close();
			_cacheAlly.put(newId, data);
			return true;
		}
		catch(IOException e)
		{
			_log.error("Error saving ally crest" + crestFile + ":", e);
			return false;
		}
	}

	class BmpFilter implements FileFilter
	{
		@Override
		public boolean accept(File file)
		{
			return (file.getName().endsWith(".bmp"));
		}
	}

	class OldPledgeFilter implements FileFilter
	{
		@Override
		public boolean accept(File file)
		{
			return (file.getName().startsWith("Pledge_"));
		}
	}

	public double getMemoryUsage()
	{
		return (float) _bytesBuffLen / 1048576;
	}

	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final CrestCache _instance = new CrestCache();
	}
}