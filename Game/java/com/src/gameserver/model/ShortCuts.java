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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.ExAutoSoulShot;
import com.src.gameserver.network.serverpackets.ShortCutInit;
import com.src.gameserver.templates.item.L2EtcItemType;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class ShortCuts
{
	private static final Log _log = LogFactory.getLog(ShortCuts.class);

	private L2PcInstance _owner;
	private Map<Integer, L2ShortCut> _shortCuts = new TreeMap<Integer, L2ShortCut>();

	public ShortCuts(L2PcInstance owner)
	{
		_owner = owner;
	}

	public L2ShortCut[] getAllShortCuts()
	{
		return _shortCuts.values().toArray(new L2ShortCut[_shortCuts.values().size()]);
	}

	public L2ShortCut getShortCut(int slot, int page)
	{
		L2ShortCut sc = _shortCuts.get(slot + page * 12);

		if(sc != null && sc.getType() == L2ShortCut.TYPE_ITEM)
		{
			if(_owner.getInventory().getItemByObjectId(sc.getId()) == null)
			{
				deleteShortCut(sc.getSlot(), sc.getPage());
				sc = null;
			}
		}

		return sc;
	}

	public synchronized void registerShortCut(L2ShortCut shortcut)
	{
		// verify shortcut
		if (shortcut.getType() == L2ShortCut.TYPE_ITEM)
		{
			L2ItemInstance item = _owner.getInventory().getItemByObjectId(shortcut.getId());
			if (item == null)
				return;
			
			if (item.isEtcItem())
				shortcut.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup());
		}
		L2ShortCut oldShortCut = _shortCuts.put(shortcut.getSlot() + 12 * shortcut.getPage(), shortcut);
		registerShortCutInDb(shortcut, oldShortCut);
	}

	private void registerShortCutInDb(L2ShortCut shortcut, L2ShortCut oldShortCut)
	{
		if(oldShortCut != null)
		{
			deleteShortCutFromDb(oldShortCut);
		}

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("REPLACE INTO character_shortcuts (char_obj_id, slot, page, type, shortcut_id, level, class_index) VALUES (?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, shortcut.getType());
			statement.setInt(5, shortcut.getId());
			statement.setInt(6, shortcut.getLevel());
			statement.setInt(7, _owner.getClassIndex());
			statement.execute();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("Could not restore character shortcuts", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public synchronized void deleteShortCut(int slot, int page)
	{
		L2ShortCut old = _shortCuts.remove(slot + page * 12);

		if(old == null || _owner == null)
		{
			return;
		}

		deleteShortCutFromDb(old);

		if(old.getType() == L2ShortCut.TYPE_ITEM)
		{
			L2ItemInstance item = _owner.getInventory().getItemByObjectId(old.getId());

			if(item != null && item.getItemType() == L2EtcItemType.SHOT)
			{
				_owner.removeAutoSoulShot(item.getItemId());
				_owner.sendPacket(new ExAutoSoulShot(item.getItemId(), 0));
			}

			item = null;
		}

		_owner.sendPacket(new ShortCutInit(_owner));

		for(int shotId : _owner.getAutoSoulShot().values())
		{
			_owner.sendPacket(new ExAutoSoulShot(shotId, 1));
		}

		old = null;
	}

	public synchronized void deleteShortCutByObjectId(int objectId)
	{
		L2ShortCut toRemove = null;

		for(L2ShortCut shortcut : _shortCuts.values())
		{
			if(shortcut.getType() == L2ShortCut.TYPE_ITEM && shortcut.getId() == objectId)
			{
				toRemove = shortcut;
				break;
			}
		}

		if(toRemove != null)
		{
			deleteShortCut(toRemove.getSlot(), toRemove.getPage());
		}

		toRemove = null;
	}

	private void deleteShortCutFromDb(L2ShortCut shortcut)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id = ? AND slot = ? AND page = ? AND class_index = ?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, shortcut.getSlot());
			statement.setInt(3, shortcut.getPage());
			statement.setInt(4, _owner.getClassIndex());
			statement.execute();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("Could not restore character shortcuts", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public void restore()
	{
		_shortCuts.clear();
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_obj_id, slot, page, type, shortcut_id, level FROM character_shortcuts WHERE char_obj_id = ? AND class_index = ?");
			statement.setInt(1, _owner.getObjectId());
			statement.setInt(2, _owner.getClassIndex());

			ResultSet rset = statement.executeQuery();

			while(rset.next())
			{
				int slot = rset.getInt("slot");
				int page = rset.getInt("page");
				int type = rset.getInt("type");
				int id = rset.getInt("shortcut_id");
				int level = rset.getInt("level");

				L2ShortCut sc = new L2ShortCut(slot, page, type, id, level, 1);
				_shortCuts.put(slot + page * 12, sc);
			}

			rset.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("Could not restore character shortcuts", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		// verify shortcuts
		for (L2ShortCut sc : getAllShortCuts())
		{
			if (sc.getType() == L2ShortCut.TYPE_ITEM)
			{
				L2ItemInstance item = _owner.getInventory().getItemByObjectId(sc.getId());
				if (item == null)
					deleteShortCut(sc.getSlot(), sc.getPage());
				else if (item.isEtcItem())
					sc.setSharedReuseGroup(item.getEtcItem().getSharedReuseGroup());
					}
				}
	}
}