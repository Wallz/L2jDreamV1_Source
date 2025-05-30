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
package com.src.gameserver.network.clientpackets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.L2GameClient.GameClientState;
import com.src.gameserver.network.serverpackets.ActionFailed;
import com.src.gameserver.network.serverpackets.CharSelected;
import com.src.util.protection.nProtect;

public class CharacterSelected extends L2GameClientPacket
{
	private final static Log _log = LogFactory.getLog(CharacterSelected.class);

	private static final String _C__0D_CHARACTERSELECTED = "[C] 0D CharacterSelected";

	private int _charSlot;

	// private int _unk1; // new in C4
	// private int _unk2; // new in C4
	// private int _unk3; // new in C4
	// private int _unk4; // new in C4

	@Override
	protected void readImpl()
	{
		_charSlot = readD();
		/*_unk1 = */readH();
		/*_unk2 = */readD();
		/*_unk3 = */readD();
		/*_unk4 = */readD();
	}

	@Override
	protected void runImpl()
	{
		if(getClient().getActiveCharLock().tryLock())
		{
			try
			{
				if(getClient().getActiveChar() == null)
				{
					L2PcInstance cha = getClient().loadCharFromDisk(_charSlot);

					if(cha == null)
					{
						_log.warn("Character could not be loaded (slot:" + _charSlot + ")");
						sendPacket(ActionFailed.STATIC_PACKET);
						return;
					}

					if(cha.getAccessLevel().getLevel() < 0)
					{
						cha.deleteMe();
						return;
					}

					cha.setClient(getClient());
					getClient().setActiveChar(cha);
					nProtect.getInstance().sendRequest(getClient());
					getClient().setState(GameClientState.IN_GAME);
					CharSelected cs = new CharSelected(cha, getClient().getSessionId().playOkID1);
					sendPacket(cs);

				}
			}
			catch(Exception e)
			{
				_log.error("", e);
			}
			finally
			{
				getClient().getActiveCharLock().unlock();
			}
		}
	}

	@Override
	public String getType()
	{
		return _C__0D_CHARACTERSELECTED;
	}

}