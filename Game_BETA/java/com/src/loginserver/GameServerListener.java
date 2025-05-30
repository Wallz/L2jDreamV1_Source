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
package com.src.loginserver;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

import javolution.util.FastList;

import com.src.Config;

public class GameServerListener extends FloodProtectedListener
{
	private static List<GameServerThread> _gameServers = new FastList<GameServerThread>();

	public GameServerListener() throws IOException
	{
		super(Config.GAME_SERVER_LOGIN_HOST, Config.GAME_SERVER_LOGIN_PORT);
	}

	@Override
	public void addClient(Socket s)
	{
		GameServerThread gst = new GameServerThread(s);
		_gameServers.add(gst);
		gst = null;
	}

	public void removeGameServer(GameServerThread gst)
	{
		_gameServers.remove(gst);
	}

}