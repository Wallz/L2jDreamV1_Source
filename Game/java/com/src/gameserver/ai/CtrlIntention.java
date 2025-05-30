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
package com.src.gameserver.ai;

public enum CtrlIntention
{
	AI_INTENTION_IDLE,

	AI_INTENTION_ACTIVE,

	AI_INTENTION_REST,

	AI_INTENTION_ATTACK,

	AI_INTENTION_CAST,

	AI_INTENTION_MOVE_TO,

	AI_INTENTION_FOLLOW,

	AI_INTENTION_PICK_UP,

	AI_INTENTION_INTERACT,

	AI_INTENTION_MOVE_TO_IN_A_BOAT;
}