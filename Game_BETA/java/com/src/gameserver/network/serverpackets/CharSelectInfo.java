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
package com.src.gameserver.network.serverpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import javolution.util.FastList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.CharSelectInfoPackage;
import com.src.gameserver.model.L2Clan;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.network.L2GameClient;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class CharSelectInfo extends L2GameServerPacket
{
	private final static Log _log = LogFactory.getLog(CharSelectInfo.class);

	private static final String _S__1F_CHARSELECTINFO = "[S] 1F CharSelectInfo";

	private String _loginName;
	private int _sessionId, _activeId;
	private CharSelectInfoPackage[] _characterPackages;

	public CharSelectInfo(String loginName, int sessionId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo();
		_activeId = -1;
	}

	public CharSelectInfo(String loginName, int sessionId, int activeId)
	{
		_sessionId = sessionId;
		_loginName = loginName;
		_characterPackages = loadCharacterSelectInfo();
		_activeId = activeId;
	}

	public CharSelectInfoPackage[] getCharInfo()
	{
		return _characterPackages;
	}

	@Override
	protected final void writeImpl()
	{
		int size = _characterPackages.length;

		writeC(0x13);
		writeD(size);

		long lastAccess = 0L;

		if(_activeId == -1)
		{
			for(int i = 0; i < size; i++)
			{
				if(lastAccess < _characterPackages[i].getLastAccess())
				{
					lastAccess = _characterPackages[i].getLastAccess();
					_activeId = i;
				}
			}
		}

		for(int i = 0; i < size; i++)
		{
			CharSelectInfoPackage charInfoPackage = _characterPackages[i];

			L2PcInstance tmp = L2World.getInstance().getPlayer(charInfoPackage.getName());
			if(tmp != null)
			{
				tmp.closeNetConnection();
				tmp.store();
				tmp.deleteMe();
			}

			writeS(charInfoPackage.getName());
			writeD(charInfoPackage.getCharId());
			writeS(_loginName);
			writeD(_sessionId);
			writeD(charInfoPackage.getClanId());
			writeD(0x00);

			writeD(charInfoPackage.getSex());
			writeD(charInfoPackage.getRace());

			if(charInfoPackage.getClassId() == charInfoPackage.getBaseClassId())
			{
				writeD(charInfoPackage.getClassId());
			}
			else
			{
				writeD(charInfoPackage.getBaseClassId());
			}

			writeD(0x01);

			writeD(0x00);
			writeD(0x00);
			writeD(0x00);

			writeF(charInfoPackage.getCurrentHp());
			writeF(charInfoPackage.getCurrentMp());

			writeD(charInfoPackage.getSp());
			writeQ(charInfoPackage.getExp());
			writeD(charInfoPackage.getLevel());

			writeD(charInfoPackage.getKarma());
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);
			writeD(0x00);

			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
			writeD(charInfoPackage.getPaperdollObjectId(Inventory.PAPERDOLL_FACE));

			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_REAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_NECK));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(charInfoPackage.getPaperdollItemId(Inventory.PAPERDOLL_FACE));

			writeD(charInfoPackage.getHairStyle());
			writeD(charInfoPackage.getHairColor());
			writeD(charInfoPackage.getFace());

			writeF(charInfoPackage.getMaxHp());
			writeF(charInfoPackage.getMaxMp());

			long deleteTime = charInfoPackage.getDeleteTimer();
			int accesslevels = charInfoPackage.getAccessLevel();
			int deletedays = 0;

			if(deleteTime > 0)

				deletedays = (int) ((deleteTime - System.currentTimeMillis()) / 1000);
			else if(accesslevels < 0)
				deletedays = -1;

			writeD(deletedays);
			writeD(charInfoPackage.getClassId());

			if(i == _activeId)
			{
				writeD(0x01);
			}
			else
			{
				writeD(0x00);
			}

			writeC(charInfoPackage.getEnchantEffect() > 127 ? 127 : charInfoPackage.getEnchantEffect());

			writeD(charInfoPackage.getAugmentationId());
		}
	}

	private CharSelectInfoPackage[] loadCharacterSelectInfo()
	{
		CharSelectInfoPackage charInfopackage;
		List<CharSelectInfoPackage> characterList = new FastList<CharSelectInfoPackage>();

		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name, obj_Id, char_name, level, maxHp, curHp, maxMp, curMp, acc, crit, evasion, mAtk, mDef, mSpd, pAtk, pDef, pSpd, runSpd, walkSpd, str, con, dex, _int, men, wit, face, hairStyle, hairColor, sex, heading, x, y, z, movement_multiplier, attack_speed_multiplier, colRad, colHeight, exp, sp, karma, pvpkills, pkkills, clanid, maxload, race, classid, deletetime, cancraft, title, rec_have, rec_left, accesslevel, online, char_slot, lastAccess, base_class FROM characters WHERE account_name = ?");
			statement.setString(1, _loginName);
			ResultSet charList = statement.executeQuery();

			while(charList.next())
			{
				charInfopackage = restoreChar(charList);
				if(charInfopackage != null)
				{
					characterList.add(charInfopackage);
				}
			}

			statement.close();

			return characterList.toArray(new CharSelectInfoPackage[characterList.size()]);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}

		return new CharSelectInfoPackage[0];
	}

	private void loadCharacterSubclassInfo(CharSelectInfoPackage charInfopackage, int ObjectId, int activeClassId)
	{
		Connection con = null;

		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT exp, sp, level FROM character_subclasses WHERE char_obj_id = ? && class_id = ? ORDER BY char_obj_id");
			statement.setInt(1, ObjectId);
			statement.setInt(2, activeClassId);
			ResultSet charList = statement.executeQuery();

			if(charList.next())
			{
				charInfopackage.setExp(charList.getLong("exp"));
				charInfopackage.setSp(charList.getInt("sp"));
				charInfopackage.setLevel(charList.getInt("level"));
			}

			charList.close();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	private CharSelectInfoPackage restoreChar(ResultSet chardata) throws Exception
	{
		int objectId = chardata.getInt("obj_id");

		long deletetime = chardata.getLong("deletetime");
		if(deletetime > 0)
		{
			if(System.currentTimeMillis() > deletetime)
			{
				L2PcInstance cha = L2PcInstance.load(objectId);
				L2Clan clan = cha.getClan();
				if(clan != null)
				{
					clan.removeClanMember(cha.getName(), 0);
				}

				L2GameClient.deleteCharByObjId(objectId);
				return null;
			}
		}

		String name = chardata.getString("char_name");

		CharSelectInfoPackage charInfopackage = new CharSelectInfoPackage(objectId, name);
		charInfopackage.setLevel(chardata.getInt("level"));
		charInfopackage.setMaxHp(chardata.getInt("maxhp"));
		charInfopackage.setCurrentHp(chardata.getDouble("curhp"));
		charInfopackage.setMaxMp(chardata.getInt("maxmp"));
		charInfopackage.setCurrentMp(chardata.getDouble("curmp"));
		charInfopackage.setKarma(chardata.getInt("karma"));

		charInfopackage.setFace(chardata.getInt("face"));
		charInfopackage.setHairStyle(chardata.getInt("hairstyle"));
		charInfopackage.setHairColor(chardata.getInt("haircolor"));
		charInfopackage.setSex(chardata.getInt("sex"));

		charInfopackage.setExp(chardata.getLong("exp"));
		charInfopackage.setSp(chardata.getInt("sp"));
		charInfopackage.setClanId(chardata.getInt("clanid"));

		charInfopackage.setRace(chardata.getInt("race"));

		charInfopackage.setAccessLevel(chardata.getInt("accesslevel"));
		final int baseClassId = chardata.getInt("base_class");
		final int activeClassId = chardata.getInt("classid");

		if(baseClassId != activeClassId)
		{
			loadCharacterSubclassInfo(charInfopackage, objectId, activeClassId);
		}

		charInfopackage.setClassId(activeClassId);

		int weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND);
		if(weaponObjId < 1)
		{
			weaponObjId = charInfopackage.getPaperdollObjectId(Inventory.PAPERDOLL_RHAND);
		}

		if(weaponObjId > 0)
		{
			Connection con = null;
			try
			{
				con = L2DatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT attributes FROM augmentations WHERE item_id = ?");
				statement.setInt(1, weaponObjId);
				ResultSet result = statement.executeQuery();

				if(result.next())
				{
					charInfopackage.setAugmentationId(result.getInt("attributes"));
				}

				result.close();
				ResourceUtil.closeStatement(statement);
			}
			catch(Exception e)
			{
				_log.error("Could not restore augmentation info", e);
			}
			finally
			{
				ResourceUtil.closeConnection(con); 
			}
		}

		if(baseClassId == 0 && activeClassId > 0)
		{
			charInfopackage.setBaseClassId(activeClassId);
		}
		else
		{
			charInfopackage.setBaseClassId(baseClassId);
		}

		charInfopackage.setDeleteTimer(deletetime);
		charInfopackage.setLastAccess(chardata.getLong("lastAccess"));

		return charInfopackage;
	}

	@Override
	public String getType()
	{
		return _S__1F_CHARSELECTINFO;
	}
}