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

public class ExShowScreenMessage extends L2GameServerPacket
{
	public static enum SMPOS
	{
		DUMMY, 
		TOP_LEFT, 
		TOP_CENTER, 
		TOP_RIGHT, 
		MIDDLE_LEFT, 
		MIDDLE_CENTER, 
		MIDDLE_RIGHT, 
		BOTTOM_CENTER, 
		BOTTOM_RIGHT,
	}
	
	private int _type;
	private int _sysMessageId;
	private boolean _hide;
	private int _unk2;
	private int _unk3;
	private boolean _fade;
	private int _size;
	private int _position;
	private boolean _effect;
	private String _text;
	private int _time;
	
	public ExShowScreenMessage(String text, int time)
	{
		_type = 1;
		_sysMessageId = -1;
		_hide = false;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = 0x02;
		_text = text;
		_time = time;
		_size = 0;
		_effect = false;
	}
	
	public ExShowScreenMessage(String text, int time, SMPOS pos, boolean effect)
	{
		this(text, time, pos.ordinal(), effect);
	}
	
	public ExShowScreenMessage(String text, int time, int pos, boolean effect)
	{
		_type = 1;
		_sysMessageId = -1;
		_hide = false;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = pos;
		_text = text;
		_time = time;
		_size = 0;
		_effect = effect;
	}

	public ExShowScreenMessage(String text, int time, int pos, int size, boolean effect)
	{
		_type = 1;
		_sysMessageId = -1;
		_hide = false;
		_unk2 = 0;
		_unk3 = 0;
		_fade = false;
		_position = pos;
		_text = text;
		_time = time;
		_size = size;
		_effect = effect;
	}
	
	public ExShowScreenMessage(int type, int messageId, int position, boolean hide, int size, int unk2, int unk3, boolean showEffect, int time, boolean fade, String text)
	{
		_type = type;
		_sysMessageId = messageId;
		_hide = hide;
		_unk2 = unk2;
		_unk3 = unk3;
		_fade = fade;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
	}
	
	@Override
	protected void writeImpl()
	{
		writeC(0xfe);
		writeH(0x38);
		writeD(_type); // 0 - system messages, 1 - your defined text
		writeD(_sysMessageId); // system message id (_type must be 0 otherwise no effect)
		writeD(_position); // message position
		writeD(_hide == true ? 1 : 0); // hide
		writeD(_size); // font size 0 - normal, 1 - small
		writeD(_unk2); // ?
		writeD(_unk3); // ?
		writeD(_effect == true ? 1 : 0); // upper effect (0 - disabled, 1 enabled) - _position must be 2 (center) otherwise no effect
		writeD(_time); // time
		writeD(_fade == true ? 1 : 0); // fade effect (0 - disabled, 1 enabled)
		writeS(_text); // your text (_type must be 1, otherwise no effect)
	}
	
	@Override
	public String getType()
	{
		return "[S]FE:38 ExShowScreenMessage";
	}
}