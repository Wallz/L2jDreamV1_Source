package com.src.gameserver.network.serverpackets;

import com.src.gameserver.model.actor.instance.L2PcInstance;

public class ExOlympiadUserInfoSpectator extends L2GameServerPacket
{
  private static int _side;
  private static L2PcInstance _player;

  public ExOlympiadUserInfoSpectator(L2PcInstance player, int side)
  {
    _player = player;
    _side = side;
  }

  @Override
protected final void writeImpl()
  {
    writeC(0xFE);
    writeH(0x29);
    writeC(_side);
    writeD(_player.getObjectId());
    writeS(_player.getName());
    writeD(_player.getClassId().getId());
    writeD((int)_player.getCurrentHp());
    writeD(_player.getMaxHp());
    writeD((int)_player.getCurrentCp());
    writeD(_player.getMaxCp());
  }

  @Override
public String getType()
  {
    return "[S] FE:29 OlympiadUserInfoSpectator";
  }
}