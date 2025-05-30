/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.admincommandhandlers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.StringTokenizer;

import com.src.Config;
import com.src.gameserver.communitybbs.Manager.RegionBBSManager;
import com.src.gameserver.handler.IAdminCommandHandler;
import com.src.gameserver.model.GMAudit;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.thread.LoginServerThread;
import com.src.util.database.L2DatabaseFactory;



/**
 * This class handles following admin commands:
 * - ban_acc <account_name> = changes account access level to -100 and logs him off. If no account is specified target's account is used.
 * - ban_char <char_name> = changes a characters access level to -100 and logs him off. If no character is specified target is used.
 * - ban_chat <char_name> <duration> = chat bans a character for the specified duration. If no name is specified the target is chat banned indefinitely.
 * - unban_acc <account_name> = changes account access level to 0.
 * - unban_char <char_name> = changes specified characters access level to 0.
 * - unban_chat <char_name> = lifts chat ban from specified player. If no player name is specified current target is used.
 * - jail charname [penalty_time] = jails character. Time specified in minutes. For ever if no time is specified.
 * - unjail charname = Unjails player, teleport him to Floran.
 *
 * @version $Revision: 1.1.6.3 $ $Date: 2005/04/11 10:06:06 $
 */
public class AdminBan implements IAdminCommandHandler {
    private static final String[] ADMIN_COMMANDS =
    {
        "admin_ban", // returns ban commands
        "admin_ban_acc",
        "admin_ban_char",
        "admin_banchat",
        "admin_unban", // returns unban commands
        "admin_unban_acc",
        "admin_unban_char",
        "admin_unbanchat",
        "admin_jail",
        "admin_unjail"
    };

    public boolean useAdminCommand(String command, L2PcInstance activeChar)
    {
        StringTokenizer st = new StringTokenizer(command);
        st.nextToken();
        String player = "";
        int duration = -1;
        L2PcInstance targetPlayer = null;

        if (st.hasMoreTokens())
        {
            player = st.nextToken();
            targetPlayer = L2World.getInstance().getPlayer(player);

            if (st.hasMoreTokens())
            {
                try
                {
                    duration = Integer.parseInt(st.nextToken());
                }
                catch (NumberFormatException nfe)
                {
                	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Invalid number format used: ").append(nfe).append(".").toString());
                    return false;
                }
            }
        }
        else
        {
            if (activeChar.getTarget() != null && activeChar.getTarget() instanceof L2PcInstance)
            {
                targetPlayer = (L2PcInstance)activeChar.getTarget();
            }
        }

        if (targetPlayer != null && targetPlayer.equals(activeChar))
        {
            activeChar.sendPacket(new SystemMessage(SystemMessageId.CANNOT_USE_ON_YOURSELF));
            return false;
        }

        if (command.startsWith("admin_ban ") || command.equalsIgnoreCase("admin_ban"))
        {
        	activeChar.sendChatMessage(0, 0, "SYS", "Available ban commands: //ban_acc, //ban_char, //ban_chat .");
            return false;
        }
        else if (command.startsWith("admin_ban_acc"))
        {
            // May need to check usage in admin_ban_menu as well.

            if (targetPlayer == null && player.equals(""))
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //ban_acc <account_name> (if none, target char's account gets banned).");
                return false;
            }
            else if (targetPlayer == null)
            {
                LoginServerThread.getInstance().sendAccessLevel(player, -100);
                activeChar.sendMessage(new StringBuilder().append("Ban request sent for account ").append(player).toString());
                auditAction(command, activeChar, player);
            }
            else
            {
                targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.ACC, 0);
                activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Account ").append(targetPlayer.getAccountName()).append(" banned.").toString());
                auditAction(command, activeChar, targetPlayer.getAccountName());
            }
        }
        else if (command.startsWith("admin_ban_char"))
        {
            if (targetPlayer == null && player.equals(""))
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //ban_char <char_name> (if none, target char is banned).");
                return false;
            }
            else
            {
                auditAction(command, activeChar, (targetPlayer == null ? player : targetPlayer.getName()));
                return changeCharAccessLevel(targetPlayer, player, activeChar, -100);
            }
        }
        else if (command.startsWith("admin_banchat"))
        {
            if (targetPlayer == null && player.equals(""))
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //ban_chat <char_name> [penalty_minutes].");
                return false;
            }
            if (targetPlayer != null)
            {
                if (targetPlayer.getPunishLevel().value() > 0)
                {
                	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append(targetPlayer.getName()).append(" is already jailed or banned.").toString());
                    return false;
                }
                String banLengthStr = "";

                targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.CHAT, duration);
                if (duration > 0)
                    banLengthStr = " for " + duration + " minutes";
                activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append(targetPlayer.getName()).append(" is now chat banned").append(banLengthStr).append(".").toString());
                auditAction(command, activeChar, targetPlayer.getName());
            }
            else
            {
                banChatOfflinePlayer(activeChar, player, duration, true);
                auditAction(command, activeChar, player);
            }
        }
        else if (command.startsWith("admin_unbanchat"))
        {
            if (targetPlayer == null && player.equals(""))
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //unban_chat <onlinecharname>.");
                return false;
            }
            if (targetPlayer != null)
            {
                if (targetPlayer.isChatBanned())
                {
                    targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
                    activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append(targetPlayer.getName()).append("'s chat ban has now been lifted.").toString());
                    auditAction(command, activeChar, targetPlayer.getName());
                }
                else
                {
                	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append(targetPlayer.getName()).append(" is not currently chat banned.").toString());
                }
            }
            else
            {
                banChatOfflinePlayer(activeChar, player, 0, false);
                auditAction(command, activeChar, player);
            }
        }
        else if (command.startsWith("admin_unban ") || command.equalsIgnoreCase("admin_unban"))
        {
        	activeChar.sendChatMessage(0, 0, "SYS", "Available unban commands: //unban_acc, //unban_char, //unban_chat .");
            return false;
        }
        else if (command.startsWith("admin_unban_acc"))
        {

            if (targetPlayer != null)
            {
            	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append(targetPlayer.getName()).append(" is currently online so must not be banned.").toString());
                return false;
            }
            else if (!player.equals(""))
            {
                LoginServerThread.getInstance().sendAccessLevel(player, 0);
                activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Unban request sent for account ").append(player).append(".").toString());
                auditAction(command, activeChar, player);
            }
            else
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //unban_acc <account_name>.");
                return false;
            }
        }
        else if (command.startsWith("admin_unban_char"))
        {
            if (targetPlayer == null && player.equals(""))
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //unban_char <char_name>.");
                return false;
            }
            else if (targetPlayer != null)
            {
            	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append(targetPlayer.getName()).append(" is currently online so must not be banned.").toString());
                return false;
            }
            else
            {
                auditAction(command, activeChar, player);
                return changeCharAccessLevel(null, player, activeChar, 0);
            }
        }
        else if (command.startsWith("admin_jail"))
        {
            if (targetPlayer == null && player.equals(""))
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //jail <charname> [penalty_minutes] (if no name is given, selected target is jailed indefinitely).");
                return false;
            }
            if (targetPlayer != null)
            {
                targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.JAIL, duration);
                activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Character ").append(targetPlayer.getName()).append(" jailed for ").append(duration > 0 ? new StringBuilder().append(duration).append(" minutes.").toString() : "ever!").toString());
                auditAction(command, activeChar, targetPlayer.getName());
            }
            else
            {
                jailOfflinePlayer(activeChar, player, duration);
                auditAction(command, activeChar, player);
            }
        }
        else if (command.startsWith("admin_unjail"))
        {
            if (targetPlayer == null && player.equals(""))
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "Usage: //unjail <charname> (If no name is given target is used)");
                return false;
            }
            else if (targetPlayer != null)
            {
                targetPlayer.setPunishLevel(L2PcInstance.PunishLevel.NONE, 0);
                activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Character ").append(targetPlayer.getName()).append(" removed from jail.").toString());
                auditAction(command, activeChar, targetPlayer.getName());
            }
            else
            {
                unjailOfflinePlayer(activeChar, player);
                auditAction(command, activeChar, player);
            }
        }
        return true;
    }

    private void auditAction(String fullCommand, L2PcInstance activeChar, String target)
    {
        if (!Config.GMAUDIT)
            return;

        String[] command = fullCommand.split(" ");

        GMAudit.auditGMAction(activeChar.getName()+" ["+activeChar.getObjectId()+"]", command[0], (target.equals("") ? "no-target" : target), (command.length > 2 ? command[2] : ""));
    }

    private void banChatOfflinePlayer(L2PcInstance activeChar, String name, int delay, boolean ban)
    {
        Connection con = null;
        int level = 0;
        long value = 0;
        if(ban)
        {
            level = L2PcInstance.PunishLevel.CHAT.value();
            value = (delay > 0 ? delay * 60000L : 60000);
        }
        else
        {
            level = L2PcInstance.PunishLevel.NONE.value();
            value = 0;
        }

        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();

            PreparedStatement statement = con.prepareStatement("UPDATE characters SET punish_level=?, punish_timer=? WHERE char_name=?");
            statement.setInt(1, level);
            statement.setLong(2, value);
            statement.setString(3, name);

            statement.execute();
            int count = statement.getUpdateCount();
            statement.close();

            if (count == 0)
            	activeChar.sendChatMessage(0, 0, "SYS", "Character not found!");
            else
                if(ban)
                	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Character ").append(name).append(" chat-banned for ").append(delay > 0 ? new StringBuilder().append(delay).append(" minutes.").toString() : "ever!").toString());
                else
                	activeChar.sendChatMessage(0, 0, "SYS", "Character " + name + "'s chat-banned lifted");
        }
        catch (SQLException se)
        {
        	activeChar.sendChatMessage(0, 0, "SYS", "SQLException while chan-ban player.");
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    private void jailOfflinePlayer(L2PcInstance activeChar, String name, int delay)
    {
        Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();

            PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
            statement.setInt(1, -114356);
            statement.setInt(2, -249645);
            statement.setInt(3, -2984);
            statement.setInt(4, L2PcInstance.PunishLevel.JAIL.value());
            statement.setLong(5, (delay > 0 ? delay * 60000L : 0));
            statement.setString(6, name);

            statement.execute();
            int count = statement.getUpdateCount();
            statement.close();

            if (count == 0)
            	activeChar.sendChatMessage(0, 0, "SYS", "Character not found!");
            else
            	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Character ").append(name).append(" jailed for ").append(delay > 0 ? new StringBuilder().append(delay).append(" minutes.").toString() : "ever!").toString());
        }
        catch (SQLException se)
        {
        	activeChar.sendChatMessage(0, 0, "SYS", "SQLException while jailing player.");
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    private void unjailOfflinePlayer(L2PcInstance activeChar, String name)
    {
        Connection con = null;
        try
        {
            con = L2DatabaseFactory.getInstance().getConnection();
            PreparedStatement statement = con.prepareStatement("UPDATE characters SET x=?, y=?, z=?, punish_level=?, punish_timer=? WHERE char_name=?");
            statement.setInt(1, 17836);
            statement.setInt(2, 170178);
            statement.setInt(3, -3507);
            statement.setInt(4, 0);
            statement.setLong(5, 0);
            statement.setString(6, name);
            statement.execute();
            int count = statement.getUpdateCount();
            statement.close();
            if (count == 0)
            	activeChar.sendChatMessage(0, 0, "SYS", "Character not found!");
            else
            	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("Character ").append(name).append(" removed from jail.").toString());
        }
        catch (SQLException se)
        {
        	activeChar.sendChatMessage(0, 0, "SYS", "SQLException while jailing player.");
        }
        finally
        {
            try
            {
                con.close();
            }
            catch (Exception e)
            {
            }
        }
    }

    private boolean changeCharAccessLevel(L2PcInstance targetPlayer, String player, L2PcInstance activeChar, int lvl)
    {
        if (targetPlayer != null)
        {
            targetPlayer.setAccessLevel(lvl);
            targetPlayer.sendChatMessage(0, 0, "SYS", "Your character has been banned. Good Bye.");
            targetPlayer.logout();
            RegionBBSManager.getInstance().changeCommunityBoard();
            activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append("The character ").append(targetPlayer.getName()).append(" has now been banned.").toString());
        }
        else
        {
            Connection con = null;
            try
            {
                con = L2DatabaseFactory.getInstance().getConnection();
                PreparedStatement statement = con.prepareStatement("UPDATE characters SET accesslevel=? WHERE char_name=?");
                statement.setInt(1, lvl);
                statement.setString(2, player);
                statement.execute();
                int count = statement.getUpdateCount();
                statement.close();
                if (count == 0)
                {
                	activeChar.sendChatMessage(0, 0, "SYS", "Character not found or access level unaltered.");
                    return false;
                }
                else
                	activeChar.sendChatMessage(0, 0, "SYS", new StringBuilder().append(player).append(" now has an access level of ").append(lvl).toString());
            }
            catch (SQLException se)
            {
            	activeChar.sendChatMessage(0, 0, "SYS", "SQLException while changing character's access level.");
                return false;
            }
            finally
            {
                L2DatabaseFactory.close(con);
            }
        }
        return true;
    }

    public String[] getAdminCommandList() {
        return ADMIN_COMMANDS;
    }
}