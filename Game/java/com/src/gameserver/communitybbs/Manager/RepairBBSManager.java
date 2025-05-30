package com.src.gameserver.communitybbs.Manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import com.src.gameserver.cache.HtmCache;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

/**
 * @author Matim, Szponiasty
 * @author 2.0
 * <br><br>
 * Part of the code used from Szponiasty.
 * (released in public)
 */
public class RepairBBSManager extends BaseBBSManager
{
	private final String HTM_PATH = "data/html/communityboard/custom/";
	
	@Override
	public void parsecmd(String command, L2PcInstance activeChar)
	{
		if (command.startsWith("_bbsrepair"))
		{			
			String filename = "data/html/communityboard/custom/repair.htm";
			
			String repairChar = null;
			
			try		
			{
				String val = command.substring(17);
				StringTokenizer st = new StringTokenizer(val);

				if (st.countTokens() == 1)
				{
					repairChar = st.nextToken();
				}
			}
			catch (Exception e)
			{
				repairChar = null;
			}
			
			if (repairChar == null)
			{
				activeChar.sendMessage("You have to choice character name to repair!");
			}
			else
			{
				if (checkAcc(activeChar,repairChar))
				{
					if (checkChar(activeChar,repairChar))
					{
						filename = HTM_PATH + "repair1.htm";
					}
					else if (checkJail(activeChar,repairChar))
					{
						filename = HTM_PATH + "repair2.htm";
					}
					else if (activeChar.isCursedWeaponEquiped())
					{
						filename = HTM_PATH + "repair5.htm";
					}
					else
					{
						repairBadCharacter(repairChar);
						filename = HTM_PATH + "repair3.htm";
					}
				}
				else
				{
					filename = HTM_PATH + "repair4.htm";
				}
			}
			
			String content = HtmCache.getInstance().getHtm(filename);
			content = content.replaceAll("%acc_chars%", getCharList(activeChar));
			separateAndSend(content, activeChar);
		}
	}
	
	
	private boolean checkAcc(L2PcInstance activeChar,String repairChar)
	{
		boolean result=false;
		String repCharAcc="";
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT account_name FROM characters WHERE char_name=?");
			statement.setString(1, repairChar);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				repCharAcc = rset.getString(1);
			}
			rset.close();
			statement.close();

		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return result;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		if (activeChar.getAccountName().compareTo(repCharAcc)==0)
			result=true;
		return result;
	}

	private boolean checkJail(L2PcInstance activeChar,String repairChar)
	{
		boolean result=false;
		int repCharJail = 0;
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT in_jail FROM characters WHERE char_name=?");
			statement.setString(1, repairChar);
			ResultSet rset = statement.executeQuery();
			if (rset.next())
			{
				repCharJail = rset.getInt(1);
			}
			rset.close();
			statement.close();

		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return result;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		if (repCharJail >= 1) // 0 norm, 1 chat ban, 2 jail, 3....
			result=true;
		return result;
	}

	private boolean checkChar(L2PcInstance activeChar,String repairChar)
	{
		boolean result=false;
		if (activeChar.getName().compareTo(repairChar)==0)
			result=true;
		return result;
	}

	private void repairBadCharacter(String charName)
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();

			PreparedStatement statement;
			statement = con.prepareStatement("SELECT obj_Id FROM characters WHERE char_name=?");
			statement.setString(1, charName);
			ResultSet rset = statement.executeQuery();

			int objId = 0;
			if (rset.next())
			{
				objId = rset.getInt(1);
			}
			rset.close();
			statement.close();
			if (objId == 0)
			{
				con.close();
				return;
			}
			statement = con.prepareStatement("UPDATE characters SET x=17867, y=170259, z=-3503 WHERE obj_Id=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("DELETE FROM character_shortcuts WHERE char_obj_id=?");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
			statement = con.prepareStatement("UPDATE items SET loc=\"WAREHOUSE\" WHERE owner_id=? AND loc=\"PAPERDOLL\"");
			statement.setInt(1, objId);
			statement.execute();
			statement.close();
		}
		catch (Exception e)
		{
			System.out.println("GameServer: could not repair character:" + e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}
	
	/**
	 * @param activeChar
	 * @return
	 */
	private String getCharList(L2PcInstance activeChar)
	{
		String result="";
		String repCharAcc=activeChar.getAccountName();
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_name FROM characters WHERE account_name=?");
			statement.setString(1, repCharAcc);
			ResultSet rset = statement.executeQuery();
			while (rset.next())
			{
				if (activeChar.getName().compareTo(rset.getString(1)) != 0)
					result += rset.getString(1)+";";
			}

			rset.close();
			statement.close();
		}
		catch (SQLException e)
		{
			e.printStackTrace();
			return result;
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
		return result;	
	}

	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, L2PcInstance activeChar)
	{
		
	}
	
	public static RepairBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder
	{
		protected static final RepairBBSManager INSTANCE = new RepairBBSManager();
	}
}