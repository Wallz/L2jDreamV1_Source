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

import java.util.logging.Logger;

import com.src.Config;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.managers.CursedWeaponsManager;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.itemcontainer.Inventory;
import com.src.gameserver.templates.chars.L2NpcTemplate;

public class CharInfo extends L2GameServerPacket
{
	private static final Logger _log = Logger.getLogger(CharInfo.class.getName());

	private static final String _S__03_CHARINFO = "[S] 03 CharInfo";

	private L2PcInstance _activeChar;
	private Inventory _inv;
	private int _x, _y, _z, _heading;
	private int _mAtkSpd, _pAtkSpd;
	private int _runSpd, _walkSpd, _swimRunSpd, _swimWalkSpd, _flRunSpd, _flWalkSpd, _flyRunSpd, _flyWalkSpd;
	private float _moveMultiplier, _attackSpeedMultiplier;

	public CharInfo(L2PcInstance cha)
	{
		_activeChar = cha;
		_inv = cha.getInventory();
		_x = _activeChar.getX();
		_y = _activeChar.getY();
		_z = _activeChar.getZ();
		_heading = _activeChar.getHeading();
		_mAtkSpd = _activeChar.getMAtkSpd();
		_pAtkSpd = _activeChar.getPAtkSpd();
		_moveMultiplier = _activeChar.getMovementSpeedMultiplier();
		_attackSpeedMultiplier = _activeChar.getAttackSpeedMultiplier();
		_runSpd = (int) (_activeChar.getRunSpeed() / _moveMultiplier);
		_walkSpd = (int) (_activeChar.getWalkSpeed() / _moveMultiplier);
		_swimRunSpd = _flRunSpd = _flyRunSpd = _runSpd;
		_swimWalkSpd = _flWalkSpd = _flyWalkSpd = _walkSpd;
	}

	@Override
	protected final void writeImpl()
	{
		boolean gmSeeInvis = false;

		if(_activeChar.getAppearance().getInvisible())
		{
			L2PcInstance tmp = getClient().getActiveChar();
			if(tmp != null && tmp.isGM())
				gmSeeInvis = true;
			else
				return;
		}

		if(_activeChar.getPoly().isMorphed())
		{
			L2NpcTemplate template = NpcTable.getInstance().getTemplate(_activeChar.getPoly().getPolyId());

			if(template != null)
			{
				writeC(0x16);
				writeD(_activeChar.getObjectId());
				writeD(_activeChar.getPoly().getPolyId() + 1000000);
				writeD(_activeChar.getKarma() > 0 ? 1 : 0);
				writeD(_x);
				writeD(_y);
				writeD(_z);
				writeD(_heading);
				writeD(0x00);
				writeD(_mAtkSpd);
				writeD(_pAtkSpd);
				writeD(_runSpd);
				writeD(_walkSpd);
				writeD(_swimRunSpd);
				writeD(_swimWalkSpd);
				writeD(_flRunSpd);
				writeD(_flWalkSpd);
				writeD(_flyRunSpd);
				writeD(_flyWalkSpd);
				writeF(_moveMultiplier);
				writeF(_attackSpeedMultiplier);
				writeF(template.collisionRadius);
				writeF(template.collisionHeight);
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
				writeD(0);
				writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
				writeC(1);
				writeC(_activeChar.isRunning() ? 1 : 0);
				writeC(_activeChar.isInCombat() ? 1 : 0);
				writeC(_activeChar.isAlikeDead() ? 1 : 0);

				if(gmSeeInvis)
				{
					writeC(0);
				}
				else
				{
					writeC(_activeChar.getAppearance().getInvisible() ? 1 : 0);
				}

				writeS(_activeChar.getName());

				if(gmSeeInvis)
				{
					writeS("Invisible");
				}
				else
				{
					writeS(_activeChar.getTitle());
				}

				writeD(0);
				writeD(0);
				writeD(0000);

				if(gmSeeInvis)
				{
					writeD((_activeChar.getAbnormalEffect() | L2Character.ABNORMAL_EFFECT_STEALTH));
				}
				else
				{
					writeD(_activeChar.getAbnormalEffect());
				}

				writeD(0);
				writeD(0);
				writeD(0);
				writeD(0);
				writeC(0);
			}
			else
			{
				_log.warning("Character " + _activeChar.getName() + " (" + _activeChar.getObjectId() + ") morphed in a Npc (" + _activeChar.getPoly().getPolyId() + ") w/o template.");
			}
		}
		else
		{
			writeC(0x03);
			writeD(_x);
			writeD(_y);
			writeD(_z);
			writeD(_heading);
			writeD(_activeChar.getObjectId());
			writeS(_activeChar.getName());
			writeD(_activeChar.getRace().ordinal());
			writeD(_activeChar.getAppearance().getSex() ? 1 : 0);

			if(_activeChar.getClassIndex() == 0)
				writeD(_activeChar.getClassId().getId());
			else
				writeD(_activeChar.getBaseClass());

			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_DHAIR));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HEAD));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_RHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_GLOVES));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_CHEST));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LEGS));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FEET));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_BACK));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_LRHAND));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_HAIR));
			writeD(_inv.getPaperdollItemId(Inventory.PAPERDOLL_FACE));

			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_RHAND));
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
			writeD(_inv.getPaperdollAugmentationId(Inventory.PAPERDOLL_LRHAND));
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);
			writeH(0x00);

			writeD(_activeChar.getPvpFlag());
			writeD(_activeChar.getKarma());

			writeD(_mAtkSpd);
			writeD(_pAtkSpd);

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
			writeF(_activeChar.getMovementSpeedMultiplier());
			writeF(_activeChar.getAttackSpeedMultiplier());
			writeF(_activeChar.getBaseTemplate().collisionRadius);
			writeF(_activeChar.getBaseTemplate().collisionHeight);

			writeD(_activeChar.getAppearance().getHairStyle());
			writeD(_activeChar.getAppearance().getHairColor());
			writeD(_activeChar.getAppearance().getFace());

			if(gmSeeInvis)
				writeS("Invisible");
			else
				writeS(_activeChar.getTitle());

			writeD(_activeChar.getClanId());
			writeD(_activeChar.getClanCrestId());
			writeD(_activeChar.getAllyId());
			writeD(_activeChar.getAllyCrestId());
			
			writeD(0);

			writeC(_activeChar.isSitting() ? 0 : 1);
			writeC(_activeChar.isRunning() ? 1 : 0);
			writeC(_activeChar.isInCombat() ? 1 : 0);
			writeC(_activeChar.isAlikeDead() ? 1 : 0);

			if(gmSeeInvis)
				writeC(0);
			else
				writeC(_activeChar.getAppearance().getInvisible() ? 1 : 0);

			writeC(_activeChar.getMountType());
			writeC(_activeChar.getPrivateStoreType());

			writeH(_activeChar.getCubics().size());
			for(int id : _activeChar.getCubics().keySet())
				writeH(id);

			writeC(_activeChar.isInPartyMatchRoom() ? 1 : 0);

			if(gmSeeInvis)
				writeD((_activeChar.getAbnormalEffect() | L2Character.ABNORMAL_EFFECT_STEALTH));
			else
				writeD(_activeChar.getAbnormalEffect());

			writeC(_activeChar.getRecomLeft());
			writeH(_activeChar.getRecomHave());
			writeD(_activeChar.getClassId().getId());

			writeD(_activeChar.getMaxCp());
			writeD((int) _activeChar.getCurrentCp());
			writeC(_activeChar.isMounted() ? 0 : _activeChar.getEnchantEffect());

			if(_activeChar.getTeam() == 1)
				writeC(0x01);
			else if(_activeChar.getTeam() == 2)
				writeC(0x02);
			else
				writeC(0x00);

			writeD(_activeChar.getClanCrestLargeId());
			writeC(_activeChar.isNoble() ? 1 : 0);
			writeC(_activeChar.isHero() || (_activeChar.isGM() && Config.GM_HERO_AURA) ? 1 : 0);

			writeC(_activeChar.isFishing() ? 1 : 0);
			writeD(_activeChar.GetFishx());
			writeD(_activeChar.GetFishy());
			writeD(_activeChar.GetFishz());

			writeD(_activeChar.getAppearance().getNameColor());

			writeD(0x00);

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
	}

	@Override
	public String getType()
	{
		return _S__03_CHARINFO;
	}

}