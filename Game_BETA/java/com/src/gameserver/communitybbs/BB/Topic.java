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
package com.src.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.src.gameserver.communitybbs.Manager.TopicBBSManager;
import com.src.util.ResourceUtil;
import com.src.util.database.L2DatabaseFactory;

public class Topic
{
	private final static Log _log = LogFactory.getLog(Topic.class);

	public static final int MORMAL = 0;
	public static final int MEMO = 1;

	private int _id;
	private int _forumId;
	private String _topicName;
	private long _date;
	private String _ownerName;
	private int _ownerId;
	private int _type;
	private int _cReply;

	/**
	 * @param restaure
	 * @param i
	 * @param j
	 * @param string
	 * @param k
	 * @param string2
	 * @param l
	 * @param m
	 * @param n
	 */
	public Topic(ConstructorType ct, int id, int fid, String name, long date, String oname, int oid, int type, int Creply)
	{
		_id = id;
		_forumId = fid;
		_topicName = name;
		_date = date;
		_ownerName = oname;
		_ownerId = oid;
		_type = type;
		_cReply = Creply;
		TopicBBSManager.getInstance().addTopic(this);

		if(ct == ConstructorType.CREATE)
		{
			insertindb();
		}
	}

	/**
	 *
	 */
	public void insertindb()
	{
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO topic (topic_id, topic_forum_id, topic_name, topic_date, topic_ownername, topic_ownerid, topic_type, topic_reply) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			statement.setInt(1, _id);
			statement.setInt(2, _forumId);
			statement.setString(3, _topicName);
			statement.setLong(4, _date);
			statement.setString(5, _ownerName);
			statement.setInt(6, _ownerId);
			statement.setInt(7, _type);
			statement.setInt(8, _cReply);
			statement.execute();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("error while saving new Topic to db", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	public enum ConstructorType
	{
		RESTORE,
		CREATE
	}

	/**
	 * @return
	 */
	public int getID()
	{
		return _id;
	}

	public int getForumID()
	{
		return _forumId;
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return _topicName;
	}

	public String getOwnerName()
	{
		return _ownerName;
	}

	public void deleteme(Forum f)
	{
		TopicBBSManager.getInstance().delTopic(this);
		f.rmTopicByID(getID());
		Connection con = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			PreparedStatement statement = con.prepareStatement("DELETE FROM topic WHERE topic_id = ? AND topic_forum_id = ?");
			statement.setInt(1, getID());
			statement.setInt(2, f.getID());
			statement.execute();
			ResourceUtil.closeStatement(statement);
		}
		catch(Exception e)
		{
			_log.error("", e);
		}
		finally
		{
			ResourceUtil.closeConnection(con); 
		}
	}

	/**
	 * @return
	 */
	public long getDate()
	{
		return _date;
	}
}