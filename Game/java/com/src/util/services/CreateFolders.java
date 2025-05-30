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
package com.src.util.services;

import java.io.File;

import com.src.Config;

/**
 * @author Vhalior
 * @version 1.0
 */
public class CreateFolders
{
	/**
	 * Create basic folders:
	 * <li>data/pathnode</li>
	 * <li>data/geodata</li>
	 * <li>data/faenor</li>
	 * <li>data/clans</li>
	 * <li>data/crests</li>
	 * <li>log</li>
	 */
	public static void createFolders()
	{
		new File(Config.DATAPACK_ROOT, "data/pathnode").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/geodata").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/faenor").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/clans").mkdirs();
		new File(Config.DATAPACK_ROOT, "data/crests").mkdirs();
		new File(Config.DATAPACK_ROOT, "log").mkdir();
	}

}