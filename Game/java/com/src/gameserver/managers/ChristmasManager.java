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
package com.src.gameserver.managers;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.gameserver.datatables.sql.ItemTable;
import com.src.gameserver.datatables.sql.SpawnTable;
import com.src.gameserver.datatables.xml.NpcTable;
import com.src.gameserver.idfactory.IdFactory;
import com.src.gameserver.model.L2Object;
import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.L2Attackable;
import com.src.gameserver.model.actor.L2Npc;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.model.entity.Announcements;
import com.src.gameserver.model.spawn.L2Spawn;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.CreatureSay;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.templates.chars.L2NpcTemplate;
import com.src.gameserver.thread.ThreadPoolManager;

public class ChristmasManager
{
	private static ChristmasManager _instance = new ChristmasManager();

	protected List<L2Npc> objectQueue = new FastList<L2Npc>();

	protected Random rand = new Random();

	static final Logger _log = Logger.getLogger(ItemsOnGroundManager.class.getName());

	protected String[]

	message =
	{
			"Ho Ho Ho... Merry Christmas!",
			"God is Love...",
			"Christmas is all about love...",
			"Christmas is thus about God and Love...",
			"Love is the key to peace among all Lineage creature kind..",
			"Love is the key to peace and happiness within all creation...",
			"Love needs to be practiced - Love needs to flow - Love needs to make happy...",
			"Love starts with your partner, children and family and expands to all world.",
			"God bless all kind.",
			"God bless Lineage.",
			"Forgive all.",
			"Ask for forgiveness even from all the \"past away\" ones.",
			"Give love in many different ways to your family members, relatives, neighbors and \"foreigners\".",
			"Enhance the feeling within yourself of being a member of a far larger family than your physical family",
			"MOST important - Christmas is a feast of BLISS given to YOU from God and all beloved ones back home in God !!",
			"Open yourself for all divine bliss, forgiveness and divine help that is offered TO YOU by many others AND GOD.",
			"Take it easy. Relax these coming days.",
			"Every day is Christmas day - it is UP TO YOU to create the proper inner attitude and opening for love toward others AND from others within YOUR SELF !",
			"Peace and Silence. Reduced activities. More time for your most direct families. If possible NO other dates or travel may help you most to actually RECEIVE all offered bliss.",
			"What ever is offered to you from God either enters YOUR heart and soul or is LOST for GOOD !!! or at least until another such day - next year Christmas or so !!",
			"Divine bliss and love NEVER can be stored and received later.",
			"There is year round a huge quantity of love and bliss available from God and your Guru and other loving souls, but Christmas days are an extended period FOR ALL PLANET",
			"Please open your heart and accept all love and bliss - For your benefit as well as for the benefit of all your beloved ones.",
			"Beloved children of God",
			"Beyond Christmas days and beyond Christmas season - The Christmas love lives on, the Christmas bliss goes on, the Christmas feeling expands.",
			"The holy spirit of Christmas is the holy spirit of God and God's love for all days.",
			"When the Christmas spirit lives on and on...",
			"When the power of love created during the pre-Christmas days is kept alive and growing.",
			"Peace among all mankind is growing as well =)",
			"The holy gift of love is an eternal gift of love put into your heart like a seed.",
			"Dozens of millions of humans worldwide are changing in their hearts during weeks of pre-Christmas time and find their peak power of love on Christmas nights and Christmas days.",
			"What is special during these days, to give all of you this very special power of love, the power to forgive, the power to make others happy, power to join the loved one on his or her path of loving life.",
			"It only is your now decision that makes the difference !",
			"It only is your now focus in life that makes all the changes. It is your shift from purely worldly matters toward the power of love from God that dwells within all of us that gave you the power to change your own behavior from your normal year long behavior.",
			"The decision of love, peace and happiness is the right one.",
			"Whatever you focus on is filling your mind and subsequently filling your heart.",
			"No one else but you have change your focus these past Christmas days and the days of love you may have experienced in earlier Christmas seasons.",
			"God's love is always present.",
			"God's Love has always been in same power and purity and quantity available to all of you.",
			"Expand the spirit of Christmas love and Christmas joy to span all year of your life...",
			"Do all year long what is creating this special Christmas feeling of love joy and happiness.",
			"Expand the true Christmas feeling, expand the love you have ever given at your maximum power of love days ... ",
			"Expand the power of love over more and more days.",
			"Re-focus on what has brought your love to its peak power and refocus on those objects and actions in your focus of mind and actions.",
			"Remember the people and surrounding you had when feeling most happy, most loved, most magic",
			"People of true loving spirit - who all was present, recall their names, recall the one who may have had the greatest impact in love those hours of magic moments of love...",
			"The decoration of your surrounding - Decoration may help to focus on love - Or lack of decoration may make you drift away into darkness or business - away from love...",
			"Love songs, songs full of living joy - any of the thousands of true touching love songs and happy songs do contribute to the establishment of an inner attitude perceptible of love.",
			"Songs can fine tune and open our heart for love from God and our loved ones.",
			"Your power of will and focus of mind can keep Christmas Love and Christmas joy alive beyond Christmas season for eternity",
			"Enjoy your love for ever!",
			"Christmas can be every day - As soon as you truly love every day =)",
			"Christmas is when you love all and are loved by all.",
			"Christmas is when you are truly happy by creating true happiness in others with love from the bottom of your heart.",
			"Secret in God's creation is that no single person can truly love without ignition of his love.",
			"You need another person to love and to receive love, a person to truly fall in love to ignite your own divine fire of love. ",
			"God created many and all are made of love and all are made to love...",
			"The miracle of love only works if you want to become a fully loving member of the family of divine love.",
			"Once you have started to fall in love with the one God created for you - your entire eternal life will be a permanent fire of miracles of love ... Eternally !",
			"May all have a happy time on Christmas each year. Merry Christmas!",
			"Christmas day is a time for love. It is a time for showing our affection to our loved ones. It is all about love.",
			"Have a wonderful Christmas. May god bless our family. I love you all.",
			"Wish all living creatures a Happy X-mas and a Happy New Year! By the way I would like us to share a warm fellowship in all places.",
			"Just as animals need peace of mind, poeple and also trees need peace of mind. This is why I say, all creatures are waiting upon the Lord for their salvation. May God bless you all creatures in the whole world.",
			"Merry Xmas!",
			"May the grace of Our Mighty Father be with you all during this eve of Christmas. Have a blessed Christmas and a happy New Year.",
			"Merry Christmas my children. May this new year give all of the things you rightly deserve. And may peace finally be yours.",
			"I wish everybody a Merry Christmas! May the Holy Spirit be with you all the time.",
			"May you have the best of Christmas this year and all your dreams come true.",
			"May the miracle of Christmas fill your heart with warmth and love. Merry Christmas!"
	},

	sender =
	{
			"Santa Claus",
			"Papai Noel",
			"Shengdan Laoren",
			"Santa",
			"Viejo Pascuero",
			"Sinter Klaas",
			"Father Christmas",
			"Saint Nicholas",
			"Joulupukki",
			"Pere Noel",
			"Saint Nikolaus",
			"Kanakaloka",
			"De Kerstman",
			"Winter grandfather",
			"Babbo Natale",
			"Hoteiosho",
			"Kaledu Senelis",
			"Black Peter",
			"Kerstman",
			"Julenissen",
			"Swiety Mikolaj",
			"Ded Moroz",
			"Julenisse",
			"El Nino Jesus",
			"Jultomten",
			"Reindeer Dasher",
			"Reindeer Dancer",
			"Christmas Spirit",
			"Reindeer Prancer",
			"Reindeer Vixen",
			"Reindeer Comet",
			"Reindeer Cupid",
			"Reindeer Donner",
			"Reindeer Donder",
			"Reindeer Dunder",
			"Reindeer Blitzen",
			"Reindeer Bliksem",
			"Reindeer Blixem",
			"Reindeer Rudolf",
			"Christmas Elf"
	};

	protected int[] presents =
	{
			5560, 5560, 5560, 5560, 5560,
			5560, 5560, 5560, 5560, 5560, 5561, 5561, 5561, 5561, 5561,
			5562, 5562, 5562, 5562,
			5563, 5563, 5563, 5563,
			5564, 5564, 5564, 5564,
			5565, 5565, 5565, 5565,
			5566, 5566, 5566, 5566,
			5583, 5583,
			5584, 5584,
			5585, 5585,
			5586, 5586,
			5587, 5587,
			6403, 6403, 6403, 6403,
			6403, 6403, 6403, 6403, 6406, 6406, 6406, 6406,
			6407, 6407,
			5555,
			7838,
			9139,
			5808
	};

	protected Future<?> _XMasMessageTask = null, _XMasPresentsTask = null;

	protected int isManagerInit = 0;

	protected long _IntervalOfChristmas = 600000;

	private final int first = 25000, last = 73099;

	public ChristmasManager()
	{
	}

	public static ChristmasManager getInstance()
	{
		if(_instance == null)
		{
			_instance = new ChristmasManager();
		}

		return _instance;
	}

	public void init(L2PcInstance activeChar)
	{

		if(isManagerInit > 0)
		{
			activeChar.sendMessage("Christmas Manager has already begun or is processing. Please be patient....");
			return;
		}

		activeChar.sendMessage("Started!!!! This will take a 2-3 hours (in order to reduce system lags to a minimum), please be patient... The process is working in the background and will spawn npcs, give presents and messages at a fixed rate.");

		spawnTrees();

		startFestiveMessagesAtFixedRate();
		isManagerInit++;

		givePresentsAtFixedRate();
		isManagerInit++;

		checkIfOkToAnnounce();

	}

	public void end(L2PcInstance activeChar)
	{

		if(isManagerInit < 4)
		{
			if(activeChar != null)
			{
				activeChar.sendMessage("Christmas Manager is not activated yet. Already Ended or is now processing....");
			}

			return;
		}

		if(activeChar != null)
		{
			activeChar.sendMessage("Terminating! This may take a while, please be patient...");
		}

		ThreadPoolManager.getInstance().executeTask(new DeleteSpawns());

		endFestiveMessagesAtFixedRate();
		isManagerInit--;

		endPresentGivingAtFixedRate();
		isManagerInit--;

		checkIfOkToAnnounce();

	}

	public void spawnTrees()
	{
		GetTreePos gtp = new GetTreePos(first);
		ThreadPoolManager.getInstance().executeTask(gtp);
		gtp = null;
	}

	private int getTreeId()
	{
		int[] ids =
		{
				13006, 13007
		};
		return ids[rand.nextInt(ids.length)];
	}

	public class GetTreePos implements Runnable
	{
		private int _iterator;
		private Future<?> _task;

		public GetTreePos(int iter)
		{
			_iterator = iter;
		}

		public void setTask(Future<?> task)
		{
			_task = task;
		}

		@Override
		public void run()
		{
			if(_task != null)
			{
				_task.cancel(true);
				_task = null;
			}
			try
			{
				L2Object obj = null;

				while(obj == null)
				{
					obj = SpawnTable.getInstance().getTemplate(_iterator).getLastSpawn();
					_iterator++;

					if(obj != null && obj instanceof L2Attackable)
					{
						if(rand.nextInt(100) > 10)
						{
							obj = null;
						}
					}
				}

				if(obj != null && rand.nextInt(100) > 50)
				{
					spawnOneTree(getTreeId(), obj.getX() + rand.nextInt(200) - 100, obj.getY() + rand.nextInt(200) - 100, obj.getZ());
				}
				obj = null;
			}
			catch(Throwable t)
			{
			}

			if(_iterator >= last)
			{
				isManagerInit++;

				SpawnSantaNPCs ssNPCs = new SpawnSantaNPCs(first);

				_task = ThreadPoolManager.getInstance().scheduleGeneral(ssNPCs, 300);
				ssNPCs.setTask(_task);

				ssNPCs = null;

				return;
			}

			_iterator++;
			GetTreePos gtp = new GetTreePos(_iterator);

			_task = ThreadPoolManager.getInstance().scheduleGeneral(gtp, 300);
			gtp.setTask(_task);

			gtp = null;
		}
	}

	public class DeleteSpawns implements Runnable
	{
		@Override
		public void run()
		{
			if(objectQueue == null || objectQueue.isEmpty())
			{
				return;
			}

			for(L2Npc deleted : objectQueue)
			{
				if(deleted == null)
				{
					continue;
				}
				else
				{
					try
					{
						L2World.getInstance().removeObject(deleted);

						deleted.decayMe();
						deleted.deleteMe();
					}
					catch(Throwable t)
					{
						continue;
					}
				}

			}

			objectQueue.clear();
			objectQueue = null;

			isManagerInit = isManagerInit - 2;
			checkIfOkToAnnounce();
		}
	}

	private void spawnOneTree(int id, int x, int y, int z)
	{
		try
		{
			L2NpcTemplate template1 = NpcTable.getInstance().getTemplate(id);
			L2Spawn spawn = new L2Spawn(template1);

			template1 = null;

			spawn.setId(IdFactory.getInstance().getNextId());

			spawn.setLocx(x);
			spawn.setLocy(y);
			spawn.setLocz(z);

			L2Npc tree = spawn.spawnOne();
			L2World.storeObject(tree);
			objectQueue.add(tree);

			spawn = null;
			tree = null;
		}
		catch(Throwable t)
		{
		}
	}

	private void endFestiveMessagesAtFixedRate()
	{
		if(_XMasMessageTask != null)
		{
			_XMasMessageTask.cancel(true);
			_XMasMessageTask = null;
		}
	}

	private void startFestiveMessagesAtFixedRate()
	{
		SendXMasMessage XMasMessage = new SendXMasMessage();
		_XMasMessageTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(XMasMessage, 60000, _IntervalOfChristmas);
		XMasMessage = null;
	}

	class SendXMasMessage implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				for(L2PcInstance pc : L2World.getInstance().getAllPlayers())
				{
					if(pc == null)
					{
						continue;
					}
					else if(pc.isOnline() == 0)
					{
						continue;
					}

					pc.sendPacket(getXMasMessage());
				}
			}
			catch(Throwable t)
			{
			}
		}
	}

	private CreatureSay getXMasMessage()
	{
		CreatureSay cs = new CreatureSay(0, 17, getRandomSender(), getRandomXMasMessage());
		return cs;
	}

	private String getRandomSender()
	{
		return sender[rand.nextInt(sender.length)];
	}

	private String getRandomXMasMessage()
	{
		return message[rand.nextInt(message.length)];
	}

	private void givePresentsAtFixedRate()
	{
		XMasPresentGivingTask XMasPresents = new XMasPresentGivingTask();
		_XMasPresentsTask = ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(XMasPresents, _IntervalOfChristmas, _IntervalOfChristmas * 3);
	}

	class XMasPresentGivingTask implements Runnable
	{
		@Override
		public void run()
		{
			try
			{
				for(L2PcInstance pc : L2World.getInstance().getAllPlayers())
				{
					if(pc == null)
					{
						continue;
					}
					else if(pc.isOnline() == 0)
					{
						continue;
					}
					else if(pc.GetInventoryLimit() <= pc.getInventory().getSize())
					{
						pc.sendMessage("Santa wanted to give you a Present but your inventory was full :(");
						continue;
					}

					else if(rand.nextInt(100) < 50)
					{

						int itemId = getSantaRandomPresent();

						L2ItemInstance item = ItemTable.getInstance().createItem("Christmas Event", itemId, 1, pc);
						pc.getInventory().addItem("Christmas Event", item.getItemId(), 1, pc, pc);
						String itemName = ItemTable.getInstance().getTemplate(itemId).getName();

						item = null;

						pc.broadcastPacket(new SystemMessage(SystemMessageId.EARNED_ITEM_S1).addString(itemName + " from Santa's Present Bag..."));

						itemName = null;
					}
				}
			}
			catch(Throwable t)
			{
			}
		}
	}

	private int getSantaRandomPresent()
	{
		return presents[rand.nextInt(presents.length)];
	}

	private void endPresentGivingAtFixedRate()
	{
		if(_XMasPresentsTask != null)
		{
			_XMasPresentsTask.cancel(true);
			_XMasPresentsTask = null;
		}
	}

	public class SpawnSantaNPCs implements Runnable
	{

		private int _iterator;

		private Future<?> _task;

		public SpawnSantaNPCs(int iter)
		{
			_iterator = iter;
		}

		public void setTask(Future<?> task)
		{
			_task = task;
		}

		@Override
		public void run()
		{
			if(_task != null)
			{
				_task.cancel(true);
				_task = null;
			}

			try
			{
				L2Object obj = null;
				while(obj == null)
				{
					obj = SpawnTable.getInstance().getTemplate(_iterator).getLastSpawn();
					_iterator++;
					if(obj != null && obj instanceof L2Attackable)
					{
						obj = null;
					}
				}

				if(obj != null && rand.nextInt(100) < 80 && obj instanceof L2Npc)
				{
					spawnOneTree(getSantaId(), obj.getX() + rand.nextInt(500) - 250, obj.getY() + rand.nextInt(500) - 250, obj.getZ());
				}
				obj = null;
			}
			catch(Throwable t)
			{
			}

			if(_iterator >= last)
			{
				isManagerInit++;
				checkIfOkToAnnounce();

				return;
			}

			_iterator++;
			SpawnSantaNPCs ssNPCs = new SpawnSantaNPCs(_iterator);

			_task = ThreadPoolManager.getInstance().scheduleGeneral(ssNPCs, 300);
			ssNPCs.setTask(_task);

			ssNPCs = null;
		}
	}

	private int getSantaId()
	{
		return rand.nextInt(100) < 50 ? 31863 : 31864;
	}

	private void checkIfOkToAnnounce()
	{
		if(isManagerInit == 4)
		{
			Announcements.getInstance().announceToAll("Christmas Event has begun, have a Merry Christmas and a Happy New Year.");
			Announcements.getInstance().announceToAll("Christmas Event will end in 24 hours.");
			_log.info("ChristmasManager:Init ChristmasManager was started successfully, have a festive holiday.");

			EndEvent ee = new EndEvent();
			Future<?> task = ThreadPoolManager.getInstance().scheduleGeneral(ee, 86400000);
			ee.setTask(task);

			task = null;
			isManagerInit = 5;
		}

		if(isManagerInit == 0)
		{
			Announcements.getInstance().announceToAll("Christmas Event has ended... Hope you enjoyed the festivities.");
			_log.info("ChristmasManager:Terminated ChristmasManager.");

			isManagerInit = -1;
		}
	}

	public class EndEvent implements Runnable
	{

		private Future<?> _task;

		public void setTask(Future<?> task)
		{
			_task = task;
		}

		@Override
		public void run()
		{
			if(_task != null)
			{
				_task.cancel(true);
				_task = null;
			}

			end(null);
		}
	}

}