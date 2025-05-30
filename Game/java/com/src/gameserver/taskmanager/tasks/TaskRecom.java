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
package com.src.gameserver.taskmanager.tasks;

import java.util.logging.Logger;

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.UserInfo;
import com.src.gameserver.taskmanager.Task;
import com.src.gameserver.taskmanager.TaskManager;
import com.src.gameserver.taskmanager.TaskManager.ExecutedTask;
import com.src.gameserver.taskmanager.TaskTypes;

public class TaskRecom extends Task
{
	private static final Logger _log = Logger.getLogger(TaskRecom.class.getName());
	private static final String NAME = "sp_recommendations";

	@Override
	public String getName()
	{
		return NAME;
	}

	@Override
	public void onTimeElapsed(ExecutedTask task)
	{
		for(L2PcInstance player: L2World.getInstance().getAllPlayers())
		{
			player.restartRecom();
			player.sendPacket(new UserInfo(player));
		}
		_log.config("Recommendation Global Task: launched.");
	}

	@Override
	public void  initializate()
	{
		super.initializate();
		TaskManager.addUniqueTask(NAME,TaskTypes.TYPE_GLOBAL_TASK,"1","13:00:00","");
	}

}