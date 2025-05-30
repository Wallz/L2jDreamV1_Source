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

import com.src.gameserver.model.L2Macro;

public class SendMacroList extends L2GameServerPacket
{
	private static final String _S__E7_SENDMACROLIST = "[S] E7 SendMacroList";

	private final int _rev;
	private final int _count;
	private final L2Macro _macro;

	public SendMacroList(int rev, int count, L2Macro macro)
	{
		_rev = rev;
		_count = count;
		_macro = macro;
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xE7);

		writeD(_rev);
		writeC(0);
		writeC(_count);
		writeC(_macro != null ? 1 : 0);

		if(_macro != null)
		{
			writeD(_macro.id);
			writeS(_macro.name);
			writeS(_macro.descr);
			writeS(_macro.acronym);
			writeC(_macro.icon);

			writeC(_macro.commands.length);

			for(int i = 0; i < _macro.commands.length; i++)
			{
				L2Macro.L2MacroCmd cmd = _macro.commands[i];
				writeC(i + 1);
				writeC(cmd.type);
				writeD(cmd.d1);
				writeC(cmd.d2);
				writeS(cmd.cmd);
			}
		}
	}

	@Override
	public String getType()
	{
		return _S__E7_SENDMACROLIST;
	}

}