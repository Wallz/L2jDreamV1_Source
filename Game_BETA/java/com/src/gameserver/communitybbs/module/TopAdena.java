package com.src.gameserver.communitybbs.module;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javolution.text.TextBuilder;

import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

/**
 * @author Matim
 * @version 2.0
 */
public class TopAdena
{
	private int _posId;
	private TextBuilder _playerList = new TextBuilder();
	
	public TopAdena()
	{
		loadFromDB();
	}
	
	private void loadFromDB()
	{
		Connection con = null;
		
		try
	    {
	         _posId = 0;
	         con = L2DatabaseFactory.getInstance().getConnection();
	         PreparedStatement statement = con.prepareStatement("SELECT count, owner_id FROM items WHERE item_id=57 order by count desc limit 20");
	         ResultSet result = statement.executeQuery();
	         
	         while (result.next())
	         {  
				int adenaCount = result.getInt("count");
				int ownerId = result.getInt("owner_id");
				//_posId = _posId + 1;

				PreparedStatement statement2 = con.prepareStatement("SELECT char_name, base_class, level, online FROM characters WHERE accesslevel=0 AND obj_Id=" + ownerId);
				ResultSet result2 = statement2.executeQuery();

				while (result2.next())
				{
					boolean status = false; 
					_posId = _posId + 1;

					if(result2.getInt("online") == 1)
						status = true;

					addPlayerToList(_posId, result2.getString("char_name"), result2.getInt("base_class"), result2.getInt("level"), adenaCount, status);
				}

				result2.close();
				statement2.close();
	         }
	            
	         result.close();
	         ResourceUtil.closeStatement(statement);
	      }
	      catch (Exception e)
	      {
	         e.printStackTrace();
	      }
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}
	
	private void addPlayerToList(int objId, String name, int ChrClass, int level, int adena, boolean isOnline)
	{
		_playerList.append("<table border=0 cellspacing=0 cellpadding=2 width=750>");
		_playerList.append("<tr>");
	    _playerList.append("<td FIXWIDTH=10></td>");
	 	_playerList.append("<td FIXWIDTH=20>" + objId + ".</td>");
	 	_playerList.append("<td FIXWIDTH=60>" + name + "</td>");
	 	_playerList.append("<td FIXWIDTH=30>" + level + "</td>");
	 	_playerList.append("<td FIXWIDTH=80>" + PlayerList.className(ChrClass) + "</td>");
	 	_playerList.append("<td FIXWIDTH=120>" + adena + "</td>");
		_playerList.append("<td FIXWIDTH=90>" + ((isOnline) ? "<font color=99FF00>Online</font>" : "<font color=CC0000>Offline</font>")+"</td>");
		_playerList.append("<td FIXWIDTH=5></td>");
		_playerList.append("</tr>");
		_playerList.append("</table>");
		_playerList.append("<img src=\"L2UI.Squaregray\" width=\"740\" height=\"1\">");
	}
	
	public String loadPlayerList()
	{	
		return _playerList.toString();
	}
}