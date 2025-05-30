package com.src.gameserver.model.entity.market;

import com.src.gameserver.model.actor.instance.L2ItemInstance;
import com.src.gameserver.model.actor.instance.L2PcInstance;

public class Bid
{
	private final L2PcInstance bidder;
	
	private final L2ItemInstance bidItem;
	
	private final int costItemId;
	
	private final int costItemCount;
	
	private final int bidId;
	
	private final MarketTaxType taxType;
	
	public Bid(L2PcInstance bidder, int bidId, L2ItemInstance bidItem, int costItemId, int costItemCount, MarketTaxType taxType)
	{
		this.bidder = bidder;
		this.bidId = bidId;
		this.bidItem = bidItem;
		this.costItemId = costItemId;
		this.costItemCount = costItemCount;
		this.taxType = taxType;
	}
	
	public L2PcInstance getBidder()
	{
		return bidder;
	}
	
	public int getBidId()
	{
		return bidId;
	}
	
	public L2ItemInstance getBidItem()
	{
		return bidItem;
	}
	
	public int getCostItemId()
	{
		return costItemId;
	}
	
	public int getCostItemCount()
	{
		return costItemCount;
	}
	
	public MarketTaxType getTaxType()
	{
		return taxType;
	}
}