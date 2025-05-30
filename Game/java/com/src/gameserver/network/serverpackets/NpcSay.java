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

import com.src.gameserver.model.actor.L2Npc;

public final class NpcSay extends L2GameServerPacket
{
	private static final String _S__30_NPCSAY = "[S] 02 NpcSay";
	private final int _objectId;
	private final int _textType;
	private final int _npcId;
	private final String _text;

	public NpcSay(int objectId, int messageType, int npcId, String text)
	{
		_objectId = objectId;
		_textType = messageType;
		_npcId = 1000000 + npcId;
		_text = text;
	}

	public NpcSay(L2Npc npc, int messageType, String text)
	{
		this(npc.getObjectId(), messageType, npc.getNpcId(), text);
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x02);
		writeD(_objectId);
		writeD(_textType);
		writeD(_npcId);
		writeS(_text);
	}

	@Override
	public String getType()
	{
		return _S__30_NPCSAY;
	}

}