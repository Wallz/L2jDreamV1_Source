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

public class RestartResponse extends L2GameServerPacket
{
        private static final String _S__74_RESTARTRESPONSE = "[S] 74 RestartResponse";
        private static final RestartResponse STATIC_PACKET_TRUE = new RestartResponse(true);
        private static final RestartResponse STATIC_PACKET_FALSE = new RestartResponse(false);
        private String _message;
        private boolean _result;
        
        public static final RestartResponse valueOf(boolean result)
        {
                return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
        }
                
        public RestartResponse(boolean result)
        {
                _result = result;
                _message = "ok merong~ khaha";
        }

        @Override
        protected final void writeImpl()
        {
                writeC(0x5f);
                writeD(_result ? 1 : 0);
                writeS(_message);
        }

        @Override
        public String getType()
        {
                return _S__74_RESTARTRESPONSE;
        }
}