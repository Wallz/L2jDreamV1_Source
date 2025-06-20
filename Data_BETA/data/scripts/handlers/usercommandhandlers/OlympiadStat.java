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
package handlers.usercommandhandlers;

import com.src.gameserver.handler.IUserCommandHandler;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.olympiad.Olympiad;

public class OlympiadStat implements IUserCommandHandler
{
	private static final int[] COMMAND_IDS =
	{
		109
	};

	public boolean useUserCommand(int id, L2PcInstance activeChar)
	{
		if(id != COMMAND_IDS[0])
		{
			return false;
		}

		if(activeChar.isNoble())
		{
			activeChar.sendMessage("Your current record for this Grand Olympiad is " + Olympiad.getInstance().getCompetitionDone(activeChar.getObjectId()) + " match(s) played. You have earned " + Olympiad.getInstance().getNoblePoints(activeChar.getObjectId()) + " Olympiad Point(s)");
			return true;
		}
		else
		{
			activeChar.sendMessage("This command can only be used by a Noblesse.");
			return true;
		}
	}

	public int[] getUserCommandList()
	{
		return COMMAND_IDS;
	}
}