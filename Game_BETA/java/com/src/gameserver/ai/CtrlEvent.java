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

public enum CtrlEvent
{
	EVT_THINK,

	EVT_ATTACKED,

	EVT_AGGRESSION,

	EVT_STUNNED,

	EVT_SLEEPING,

	EVT_ROOTED,

	EVT_READY_TO_ACT,

	EVT_USER_CMD,

	EVT_ARRIVED,

	EVT_ARRIVED_REVALIDATE,

	EVT_ARRIVED_BLOCKED,

	EVT_FORGET_OBJECT,

	EVT_CANCEL,

	EVT_DEAD,

	EVT_FAKE_DEATH,

	EVT_CONFUSED,

	EVT_MUTED,

	EVT_AFFRAID,

	EVT_FINISH_CASTING,

	EVT_BETRAYED
}