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
package com.src.gameserver.model.base;

import com.src.Config;

public class SoulCrystal
{
	public static final int[][] HighSoulConvert =
	{
			{
					4639, 5577
			},
			{
					5577, 5580
			},
			{
					5580, 5908
			},

			{
					4650, 5578
			},
			{
					5578, 5581
			},
			{
					5581, 5911
			},

			{
					4661, 5579
			},
			{
					5579, 5582
			},
			{
					5582, 5914
			}
	};

	public static final int[] SoulCrystalTable =
	{
			4629,
			4630,
			4631,
			4632,
			4633,
			4634,
			4635,
			4636,
			4637,
			4638,
			4639,
			5577,
			5580,
			5908,
			4640,
			4641,
			4642,
			4643,
			4644,
			4645,
			4646,
			4647,
			4648,
			4649,
			4650,
			5578,
			5581,
			5911,
			4651,
			4652,
			4653,
			4654,
			4655,
			4656,
			4657,
			4658,
			4659,
			4660,
			4661,
			5579,
			5582,
			5914
	};

	public static final int MAX_CRYSTALS_LEVEL = Config.SOUL_CRYSTAL_MAX_LEVEL;
	public static final int BREAK_CHANCE = Config.SOUL_CRYSTAL_BREAK_CHANCE;
	public static final int LEVEL_CHANCE = Config.SOUL_CRYSTAL_LEVEL_CHANCE;

	public static final int RED_BROKEN_CRYSTAL = 4662;
	public static final int GRN_BROKEN_CYRSTAL = 4663;
	public static final int BLU_BROKEN_CRYSTAL = 4664;

	public static final int RED_NEW_CRYSTAL = 4629;
	public static final int GRN_NEW_CYRSTAL = 4640;
	public static final int BLU_NEW_CRYSTAL = 4651;

}