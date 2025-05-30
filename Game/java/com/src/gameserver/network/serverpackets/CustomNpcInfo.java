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

import com.src.gameserver.datatables.xml.CharTemplateTable;
import com.src.gameserver.model.actor.L2Npc;

public class CustomNpcInfo extends L2GameServerPacket
{
	private static final String _S__31_CUSTOMNPCINFO = "[S] 31 CustomNpcInfo [dddddsddd dddddddddddd dddddddd hhhh d hhhhhhhhhhhh d hhhh hhhhhhhhhhhhhhhh dddddd dddddddd ffff ddd s ddddd ccccccc h c d c h ddd cc d ccc ddddddddddd]";

	private L2Npc _activeChar;

	public CustomNpcInfo(L2Npc cha)
	{
		_activeChar = cha;
		_activeChar.setClientX(_activeChar.getPosition().getX());
		_activeChar.setClientY(_activeChar.getPosition().getY());
		_activeChar.setClientZ(_activeChar.getPosition().getZ());

	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x31);
		writeD(_activeChar.getX());
		writeD(_activeChar.getY());
		writeD(_activeChar.getZ());
		writeD(_activeChar.getHeading());
		writeD(_activeChar.getObjectId());
		writeS(_activeChar.getCustomNpcInstance().getName());
		writeD(_activeChar.getCustomNpcInstance().getRace());
		writeD(_activeChar.getCustomNpcInstance().isFemaleSex() ? 1 : 0);
		writeD(_activeChar.getCustomNpcInstance().getClassId());
		write('D', 0, 2);
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_RHAND());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_LHAND());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_GLOVES());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_CHEST());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_LEGS());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_FEET());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_HAIR());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_RHAND());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_HAIR());
		writeD(_activeChar.getCustomNpcInstance().PAPERDOLL_HAIR2());

		write('D', 0, 8);
		write('H', 0, 4);
		writeD(0);
		write('H', 0, 12);
		writeD(0);
		write('H', 0, 20);

		writeD(_activeChar.getCustomNpcInstance().getPvpFlag() ? 1 : 0);
		writeD(_activeChar.getCustomNpcInstance().getKarma());

		writeD(_activeChar.getMAtkSpd());
		writeD(_activeChar.getPAtkSpd());

		writeD(_activeChar.getCustomNpcInstance().getPvpFlag() ? 1 : 0);
		writeD(_activeChar.getCustomNpcInstance().getKarma());

		writeD(_activeChar.getRunSpeed());
		writeD(_activeChar.getRunSpeed() / 2);
		writeD(_activeChar.getRunSpeed() / 3);
		writeD(_activeChar.getRunSpeed() / 3);
		writeD(_activeChar.getRunSpeed());
		writeD(_activeChar.getRunSpeed());
		writeD(_activeChar.getRunSpeed());
		writeD(_activeChar.getRunSpeed());
		writeF(_activeChar.getStat().getMovementSpeedMultiplier());
		writeF(_activeChar.getStat().getAttackSpeedMultiplier());

		writeF(CharTemplateTable.getInstance().getTemplate(_activeChar.getCustomNpcInstance().getClassId()).getCollisionRadius());
		writeF(CharTemplateTable.getInstance().getTemplate(_activeChar.getCustomNpcInstance().getClassId()).getCollisionHeight());

		writeD(_activeChar.getCustomNpcInstance().getHairStyle());
		writeD(_activeChar.getCustomNpcInstance().getHairColor());
		writeD(_activeChar.getCustomNpcInstance().getFace());

		writeS(_activeChar.getCustomNpcInstance().getTitle());

		writeD(_activeChar.getCustomNpcInstance().getClanId());
		writeD(_activeChar.getCustomNpcInstance().getClanCrestId());
		writeD(_activeChar.getCustomNpcInstance().getAllyId());
		writeD(_activeChar.getCustomNpcInstance().getAllyCrestId());
		writeD(0);
		writeC(1);
		writeC(_activeChar.isRunning() ? 1 : 0);
		writeC(_activeChar.isInCombat() ? 1 : 0);
		writeC(_activeChar.isAlikeDead() ? 1 : 0);
		write('C', 0, 3);
		writeH(0);
		writeC(0);
		writeD(_activeChar.getAbnormalEffect());
		writeC(0);
		writeH(0);
		writeD(_activeChar.getCustomNpcInstance().getClassId());
		writeD(_activeChar.getMaxCp());
		writeD((int) _activeChar.getStatus().getCurrentCp());
		writeC(_activeChar.getCustomNpcInstance().getEnchantWeapon());
		writeC(0);
		writeD(0);
		writeC(_activeChar.getCustomNpcInstance().isNoble() ? 1 : 0);
		writeC(_activeChar.getCustomNpcInstance().isHero() ? 1 : 0);
		writeC(0);

		write('D', 0, 3);
		writeD(_activeChar.getCustomNpcInstance().nameColor());
		writeD(0);
		writeD(_activeChar.getCustomNpcInstance().getPledgeClass());
		writeD(0);
		writeD(_activeChar.getCustomNpcInstance().titleColor());
		writeD(_activeChar.getCustomNpcInstance().getCursedWeaponLevel());
		write('D', 0, 2);
	}

	private final void write(char type, int value, int times)
	{
		for(int i = 0; i < times; i++)
		{
			switch(type)
			{
				case 'C':
					writeC(value);
					break;
				case 'D':
					writeD(value);
					break;
				case 'F':
					writeF(value);
					break;
				case 'H':
					writeH(value);
					break;
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__31_CUSTOMNPCINFO;
	}

}