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
package com.src.gameserver.managers;

import com.src.gameserver.datatables.GmListTable;
import com.src.gameserver.datatables.sql.ClanTable;
import com.src.gameserver.datatables.xml.AccessLevels;
import com.src.gameserver.datatables.xml.AdminCommandAccessRights;
import com.src.gameserver.datatables.xml.AugmentationData;
import com.src.gameserver.datatables.xml.ExperienceTable;
import com.src.gameserver.datatables.xml.HelperBuffTable;

public class DatatablesManager
{
	public static void reloadAll()
	{
		AccessLevels.getInstance();
		AdminCommandAccessRights.getInstance();
		GmListTable.getInstance();
		AugmentationData.getInstance();
		ClanTable.getInstance();
		HelperBuffTable.getInstance();
	}

	public static void LoadSTS()
	{
		ExperienceTable.getInstance();
	}

}