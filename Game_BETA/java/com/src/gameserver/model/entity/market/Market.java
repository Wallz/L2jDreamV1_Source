package com.src.gameserver.model.entity.market;

import java.util.Collection;
import java.util.Map;

import javolution.util.FastList;
import javolution.util.FastMap;

import com.src.gameserver.model.L2World;
import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.serverpackets.NpcHtmlMessage;
import com.src.gameserver.templates.item.L2EtcItem;

public class Market
{
	public static final int MAX_LOTS = 50;
	public static final int MAX_CHAR_LOTS = 12;
	public static final int LOTS_PER_PAGE = 10;

	public static final int[] DISALLOWED_ITEMS_FOR_BID = { 5588, 7694  };
	public static final double MARKET_TAX = 0.1;

	public static final boolean SEND_MESSAGE_AFTER_TRADE = true;
	public static final boolean ALLOW_AUGMENTATED_ITEMS = true;
	public static final boolean ALLOW_ETC_ITEMS_FOR_SELL = false;
	public static final boolean ALLOW_ENCHATED_ITEMS = true;
	public static final String TRADE_MESSAGE_FORSELLER = "Đ’Đ°Ń� Ń‚ĐľĐ˛Đ°Ń€ %item% Đ±Ń‹Đ» Ń�Ń�ĐżĐµŃ�Đ˝Đľ ĐżŃ€ĐľĐ´Đ°Đ˝.";
	public static final String TRADE_MESSAGE_FORBUYER = "Đ’Ń‹ Ń�Ń�ĐżĐµŃ�Đ˝Đľ ĐşŃ�ĐżĐ¸Đ»Đ¸ Ń‚ĐľĐ˛Đ°Ń€ %item%.";
	
	private int lotsCount = 0;
	
	private static Map<Integer, FastList<Bid>> lots;
	private static Map<String, Integer> prices;
	
	private Market()
	{
		lots = new FastMap<Integer, FastList<Bid>>();
		prices = new FastMap<String, Integer>();
		prices.put("Adena", 57);
		prices.put("CoL", 4037);
	}
	
	public void addLot(int playerid, int itemObjId, int costItemId, int costItemCount, String tax)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/market/MarketReturnResult.htm");
		L2PcInstance player = L2World.getInstance().getPlayer(playerid);
		L2ItemInstance item = player.getInventory().getItemByObjectId(itemObjId);
		MarketTaxType taxType = null;
		if(tax.equalsIgnoreCase("Seller"))
			taxType = MarketTaxType.SELLER;
		else if(tax.equalsIgnoreCase("Buyer"))
			taxType = MarketTaxType.BUYER;
		if(!checkItemForMarket(item))
		{
			html.replace("%text%", "Đ�Đ·Đ˛Đ¸Đ˝Đ¸Ń‚Đµ, ŃŤŃ‚ĐľŃ‚ Đ°ĐąŃ‚ĐµĐĽ Đ˝ĐµĐ»ŃŚĐ·ŃŹ Đ˛Ń‹Ń�Ń‚Đ°Đ˛Đ¸Ń‚ŃŚ Đ˝Đ° Ń€Ń‹Đ˝ĐľĐş.");
			player.sendPacket(html);
			return;
		}
		if(!prices.containsValue(costItemId))
		{
			html.replace("%text%", "Đ�Đ·Đ˛Đ¸Đ˝Đ¸Ń‚Đµ, ŃŤŃ‚Đ° Đ˛Đ°Đ»ŃŽŃ‚Đ° Đ˝Đµ ĐżĐľĐ´Đ´ĐµŃ€Đ¶Đ¸Đ˛Đ°ĐµŃ‚Ń�ŃŹ Ń€Ń‹Đ˝ĐşĐľĐĽ.");
			player.sendPacket(html);
			return;
		}
		if((getBidsCount() +1) > MAX_LOTS)
		{
			html.replace("%text%", "Đ�Đ·Đ˛Đ¸Đ˝Đ¸Ń‚Đµ, Đ°Ń�ĐşŃ†Đ¸ĐľĐ˝ ĐżĐµŃ€ĐµĐżĐľĐ»Đ˝ĐµĐ˝.");
			player.sendPacket(html);
			return;
		}
		if(lots.get(player.getObjectId()) != null && (lots.get(player.getObjectId()).size() +1 > MAX_CHAR_LOTS))
		{
			html.replace("%text%", "Đ�Đ·Đ˛Đ¸Đ˝Đ¸Ń‚Đµ, Đ˛Ń‹ ĐżŃ€ĐµĐ˛Ń‹Ń�Đ¸Đ»Đ¸ ĐĽĐ°ĐşŃ�. ĐşĐľĐ»Đ¸Ń‡ĐµŃ�Ń‚Đ˛Đľ Ń‚ĐľĐ˛Đ°Ń€ĐľĐ˛.");
			player.sendPacket(html);
			return;
		}
		if(taxType == MarketTaxType.SELLER && (player.getInventory().getItemByItemId(costItemId) != null && player.getInventory().getItemByItemId(costItemId).getCount() < (costItemCount * MARKET_TAX)))
		{
			html.replace("%text%", "Đ�Đ·Đ˛Đ¸Đ˝Đ¸Ń‚Đµ, Ń� Đ’Đ°Ń� Đ˝Đµ Đ´ĐľŃ�Ń‚Đ°Ń‚ĐľŃ‡Đ˝Đľ Ń�Ń€ĐµĐ´Ń�Ń‚Đ˛ Đ´Đ»ŃŹ ĐľĐżĐ»Đ°Ń‚Ń‹ Đ˝Đ°Đ»ĐľĐłĐ° Ń€Ń‹Đ˝ĐşĐ°.");
			player.sendPacket(html);
			return;
		}
		Bid biditem = new Bid(player, lotsCount++, item, costItemId, costItemCount, taxType);
		if(biditem.getTaxType() == MarketTaxType.SELLER) 
			player.destroyItemByItemId("Market tax", costItemId, (int)(costItemCount * MARKET_TAX), null, false);
		if(lots.get(player.getObjectId()) != null)
			lots.get(player.getObjectId()).add(biditem);
		else
		{
			FastList<Bid> charBidItems = new FastList<Bid>();
			charBidItems.add(biditem);
			lots.put(player.getObjectId(), charBidItems);
		}
		html.replace("%text%", "Đ˘ĐľĐ˛Đ°Ń€ Ń�Ń�ĐżĐµŃ�Đ˝Đľ Đ´ĐľĐ±Đ°Đ˛Đ»ĐµĐ˝ Đ˝Đ° Ń€Ń‹Đ˝ĐľĐş.");
		player.sendPacket(html);
	}
	
	public void deleteLot(int charObjId, int bidId)
	{
		L2PcInstance player = L2World.getInstance().getPlayer(charObjId);
		Bid bid = getBidById(bidId);
		if(bid.getBidder().getObjectId() != player.getObjectId())
			return;
		if(!(lots.get(player.getObjectId()).contains(bid)))
			return;
		lots.get(player.getObjectId()).remove(bid);
		sendResultHtml(player, "Đ’Đ°Ń� ĐżŃ€ĐµĐ´ĐĽĐµŃ‚ Ń�Ń�ĐżĐµŃ�Đ˝Đľ Ń�Đ´Đ°Đ»ĐµĐ˝ Ń� Ń€Ń‹Đ˝ĐşĐ°.");
	}
	
	public void buyLot(int buyerId, int bidId)
	{
		Bid bid = getBidById(bidId);
		L2PcInstance seller = L2World.getInstance().getPlayer(bid.getBidder().getObjectId());
		L2PcInstance buyer = L2World.getInstance().getPlayer(buyerId);
		if(seller == null || buyer == null || buyer.getObjectId() == bid.getBidder().getObjectId())
		{
			lots.get(bid.getBidder().getObjectId()).clear();
			return;
		}
		if(seller.getInventory().getItemByItemId(bid.getBidItem().getItemId()) == null)
		{
			if(lots.get(seller.getObjectId()) != null)
				lots.get(seller.getObjectId()).remove(bid);
			return;
		}
		if(buyer.getInventory().getItemByItemId(bid.getCostItemId()) == null || (bid.getTaxType() == MarketTaxType.BUYER && (buyer.getInventory().getItemByItemId(bid.getCostItemId()).getCount() < (bid.getCostItemCount() + (bid.getCostItemCount() * MARKET_TAX)))) || 
				(bid.getTaxType() == MarketTaxType.SELLER && (buyer.getInventory().getItemByItemId(bid.getCostItemId()).getCount() < bid.getCostItemCount())))
		{
			sendResultHtml(buyer, "Đ�Đ·Đ˛Đ¸Đ˝Đ¸Ń‚Đµ, Ń� Đ’Đ°Ń� Đ˝Đµ Ń…Đ˛Đ°Ń‚Đ°ĐµŃ‚ Đ´ĐµĐ˝ĐµĐł Đ˝Đ° ĐľĐżĐ»Đ°Ń‚Ń� Ń‚ĐľĐ˛Đ°Ń€Đ°.");
			return;
		}
		L2ItemInstance item = seller.getInventory().getItemByObjectId(bid.getBidItem().getObjectId());
		if(item == null) return;
		double itemcount = (bid.getTaxType() == MarketTaxType.BUYER ? (bid.getCostItemCount() + (bid.getCostItemCount() * MARKET_TAX)) : bid.getCostItemCount());
		buyer.destroyItemByItemId("Market", bid.getCostItemId(), (int)itemcount, buyer, false);
		seller.addItem("Market", bid.getCostItemId(), bid.getCostItemCount(), seller, false);
		seller.transferItem("Market", item.getObjectId(), 1, buyer.getInventory(), seller);
		if(SEND_MESSAGE_AFTER_TRADE)
		{
			seller.sendMessage((TRADE_MESSAGE_FORSELLER.replace("%item%", bid.getBidItem().getItemName() + " +" + bid.getBidItem().getEnchantLevel())));
			buyer.sendMessage((TRADE_MESSAGE_FORBUYER.replace("%item%", bid.getBidItem().getItemName() + " +" + bid.getBidItem().getEnchantLevel())));
		}
		lots.get(bid.getBidder().getObjectId()).remove(bid);
	}
	
	public Bid getBidById(int bidId)
	{
		Collection<FastList<Bid>> collect = lots.values();
		for(FastList<Bid> list: collect)
		{
			for(Bid bid: list)
			{
				if(bid.getBidId() == bidId)
					return bid;
			}
		}
		return null;
	}
	
	public FastList<Bid> getAllBids()
	{
		FastList<Bid> result = new FastList<Bid>();
		Collection<FastList<Bid>> collect = lots.values();
		for(FastList<Bid> list: collect)
		{
			for(Bid bid: list)
			{
				result.add(bid);
			}
		}
		return result;
	}
	
	public int getBidsCount()
	{
		int count = 0;
		Collection<FastList<Bid>> collect = lots.values();
		for(FastList<Bid> list: collect)
		{
			count += list.size();
		}
		return count;
	}
	
	public String getShortItemName(int id)
	{
		for(Map.Entry<String, Integer> entry: prices.entrySet())
		{
			if(entry.getValue() == id)
				return entry.getKey();
		}
		return "";
	}
	
	public int getShortItemId(String name)
	{
		for(Map.Entry<String, Integer> entry: prices.entrySet())
		{
			if(entry.getKey().equalsIgnoreCase(name))
				return entry.getValue();
		}
		return 0;
	}
	
	public String getPriceList()
	{
		String res = "";
		Object[] str = Market.prices.keySet().toArray();
		for(int i = 0;i < str.length;i++)
		{
			res += (String)str[i];
			if(!(i == str.length-1))
			{
				res += ";";
			}
		}
		return res;
	}
	
	public boolean isInArray(int[] arr, int item)
	{
		for(int i: arr)
		{
			if(i == item)
				return true;
		}
		return false;
	}
	
	private void sendResultHtml(L2PcInstance player, String text)
	{
		NpcHtmlMessage html = new NpcHtmlMessage(1);
		html.setFile("data/html/market/MarketReturnResult.htm");
		html.replace("%text%", text);
		player.sendPacket(html);
	}
	
	public boolean checkItemForMarket(L2ItemInstance item)
	{
		if(isInArray(DISALLOWED_ITEMS_FOR_BID, item.getItemId()) || (item.isAugmented() && !ALLOW_AUGMENTATED_ITEMS) || item.isStackable()
				|| ((item.getItem() instanceof L2EtcItem) && !ALLOW_ETC_ITEMS_FOR_SELL) || (item.getEnchantLevel() > 0 && !ALLOW_ENCHATED_ITEMS))
			return false;
		return true;
	}
	
	public Map<Integer, FastList<Bid>> getLots()
	{
		return lots;
	}
	
	private static Market _instance;
	
	public static Market getInstance()
	{
		if(_instance == null)
			_instance = new Market();
		return _instance;
	}
	
}