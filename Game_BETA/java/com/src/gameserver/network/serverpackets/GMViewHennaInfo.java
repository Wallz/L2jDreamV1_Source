package com.src.gameserver.network.serverpackets;

import com.src.gameserver.model.actor.instance.L2HennaInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class GMViewHennaInfo extends L2GameServerPacket
{
	private final L2PcInstance _cha;
	private final L2HennaInstance[] _hennas = new L2HennaInstance[3];
	private int _count;

	public GMViewHennaInfo(L2PcInstance cha)
	{
		_cha = cha;
		
		int j = 0;
		for (int i = 0; i < 3; i++)
		{
			L2HennaInstance h = _cha.getHennas(i + 1);
			if (h != null)
				_hennas[(j++)] = h;
		}
		_count = j;
	}

	@Override
	protected void writeImpl()
	{
		writeC(234);

		writeC(_cha.getHennaStatINT());
		writeC(_cha.getHennaStatSTR());
		writeC(_cha.getHennaStatCON());
		writeC(_cha.getHennaStatMEN());
		writeC(_cha.getHennaStatDEX());
		writeC(_cha.getHennaStatWIT());

		writeD(3);

		writeD(_count);
		for (int i = 0; i < _count; i++)
		{
			writeD(_hennas[i].getSymbolId());
			writeD(1);
		}
	}

	@Override
	public String getType()
	{
		return "[S] 0xea GMHennaInfo";
	}
}