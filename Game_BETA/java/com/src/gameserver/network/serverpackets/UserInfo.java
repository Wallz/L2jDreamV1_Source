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

import com.src.Config;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.L2Summon;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class UserInfo extends L2GameServerPacket
{
	private static final String _S__04_USERINFO = "[S] 04 UserInfo";
	private L2PcInstance _activeChar;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd, _relation;
	private float _moveMultiplier;

	public UserInfo(L2PcInstance character)
	{
		_activeChar = character;

		_moveMultiplier = _activeChar.getMovementSpeedMultiplier();
		_runSpd = Math.round((_activeChar.getRunSpeed() / _moveMultiplier));
		_walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
		_relation = _activeChar.isClanLeader() ? 0x40 : 0;
		if(_activeChar.getSiegeState() == 1)
		{
			_relation |= 0x180;
		}

		if(_activeChar.getSiegeState() == 2)
		{
			_relation |= 0x80;
		}
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x04);

		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getHeading());
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.getName());
		writeD(_activeChar.getRace().ordinal());
		writeD(_activeChar.getAppearance().getSex() ? 1 : 0);

		if(_activeChar.getClassIndex() == 0)
		{
			writeD(_activeChar.getClassId().getId());
		}
		else
		{
			writeD(_activeChar.getBaseClass());
		}

		writeD(_activeChar.getLevel());
		writeQ(_activeChar.getExp());
		writeD(_activeChar.getSTR());
		writeD(_activeChar.getDEX());
		writeD(_activeChar.getCON());
		writeD(_activeChar.getINT());
		writeD(_activeChar.getWIT());
		writeD(_activeChar.getMEN());
		writeD(_activeChar.getMaxHp());
		writeD((int) _activeChar.getCurrentHp());
		writeD(_activeChar.getMaxMp());
		writeD((int) _activeChar.getCurrentMp());
		writeD(_activeChar.getSp());
		writeD(_activeChar.getCurrentLoad());
		writeD(_activeChar.getMaxLoad());

		writeD(_activeChar.getActiveWeaponItem() != null ? 40 : 20);

		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_DHAIR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HEAD));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_RHAND));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LHAND));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_GLOVES));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_CHEST));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LEGS));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FEET));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_BACK));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_LRHAND));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_HAIR));
		writeD(_activeChar.getInventory().getPaperdollObjectId(Inventory.PAPERDOLL_FACE));

		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_REAR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEAR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_NECK));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LFINGER));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FEET));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_BACK));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
		writeD(_activeChar.getInventory().getPaperdollItemId(Inventory.PAPERDOLL_FACE));

		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeD(_activeChar.getInventory().getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);
		writeH(0x00);

		writeD(_activeChar.getPAtk(null));
		writeD(_activeChar.getPAtkSpd());
		writeD(_activeChar.getPDef(null));
		writeD(_activeChar.getEvasionRate(null));
		writeD(_activeChar.getAccuracy());
		writeD(_activeChar.getCriticalHit(null, null));
		writeD(_activeChar.getMAtk(null, null));

		writeD(_activeChar.getMAtkSpd());
		writeD(_activeChar.getPAtkSpd());

		writeD(_activeChar.getMDef(null, null));

		writeD(_activeChar.getPvpFlag());
		writeD(_activeChar.getKarma());

		writeD(_runSpd);
		writeD(_walkSpd);
		writeD(_swimRunSpd);
		writeD(_swimWalkSpd);
		writeD(_flRunSpd);
		writeD(_flWalkSpd);
		writeD(_flyRunSpd);
		writeD(_flyWalkSpd);
		writeF(_moveMultiplier);
		writeF(_activeChar.getAttackSpeedMultiplier());

		L2Summon pet = _activeChar.getPet();
		if(_activeChar.getMountType() != 0 && pet != null)
		{
			writeF(pet.getTemplate().collisionRadius);
			writeF(pet.getTemplate().collisionHeight);
		}
		else
		{
			writeF(_activeChar.getBaseTemplate().collisionRadius);
			writeF(_activeChar.getBaseTemplate().collisionHeight);
		}

		writeD(_activeChar.getAppearance().getHairStyle());
		writeD(_activeChar.getAppearance().getHairColor());
		writeD(_activeChar.getAppearance().getFace());
		writeD(_activeChar.isGM() ? 1 : 0);

		String title = _activeChar.getTitle();
		if(_activeChar.getAppearance().getInvisible() && _activeChar.isGM())
		{
			title = "(Invisible)";
		}

		if(_activeChar.getPoly().isMorphed())
		{
			L2NpcTemplate polyObj = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());
			if(polyObj != null)
			{
				title += " - " + polyObj.name;
			}
		}
		writeS(title);

		writeD(_activeChar.getClanId());
		writeD(_activeChar.getClanCrestId());
		writeD(_activeChar.getAllyId());
		writeD(_activeChar.getAllyCrestId());
		writeD(_relation);
		writeC(_activeChar.getMountType());
		writeC(_activeChar.getPrivateStoreType());
		writeC(_activeChar.hasDwarvenCraft() ? 1 : 0);
		writeD(_activeChar.getPkKills());
		writeD(_activeChar.getPvpKills());

		writeH(_activeChar.getCubics().size());
		for(int id : _activeChar.getCubics().keySet())
		{
			writeH(id);
		}

		writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);

		if(_activeChar.getAppearance().getInvisible() && _activeChar.isGM())
		{
			writeD(_activeChar.getAbnormalEffect() | L2Character.ABNORMAL_EFFECT_STEALTH);
		}
		else
		{
			writeD(_activeChar.getAbnormalEffect());
		}
		writeC(0x00);

		writeD(_activeChar.getClanPrivileges());

		writeH(_activeChar.getRecomLeft());
		writeH(_activeChar.getRecomHave());
		writeD(0x00);
		writeH(_activeChar.GetInventoryLimit());

		writeD(_activeChar.getClassId().getId());
		writeD(0x00);
		writeD(_activeChar.getMaxCp());
		writeD((int) _activeChar.getCurrentCp());
		writeC(_activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect());

		if(_activeChar.getTeam() == 1)
		{
			writeC(0x01);
		}
		else if(_activeChar.getTeam() == 2)
		{
			writeC(0x02);
		}
		else
		{
			writeC(0x00);
		}

		writeD(_activeChar.getClanCrestLargeId());
		writeC(_activeChar.isNoble() ? 1 : 0);
		writeC(_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) ? 1 : 0);

		writeC(_activeChar.isFishing() ? 1 : 0);
		writeD(_activeChar.GetFishx());
		writeD(_activeChar.GetFishy());
		writeD(_activeChar.GetFishz());
		writeD(_activeChar.getAppearance().getNameColor());

		writeC(_activeChar.isRunning() ? 0x01 : 0x00);

		writeD(_activeChar.getPledgeClass());
		writeD(_activeChar.getPledgeType());

		writeD(_activeChar.getAppearance().getTitleColor());

		if(_activeChar.isCursedWeaponEquiped())
		{
			writeD(CursedWeaponsManager.getInstance().getLevel(_activeChar.getCursedWeaponEquipedId()));
		}
		else
		{
			writeD(0x00);
		}
	}

	@Override
	public String getType()
	{
		return _S__04_USERINFO;
	}

}