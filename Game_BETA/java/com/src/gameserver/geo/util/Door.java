package com.src.gameserver.geo.util;


import com.src.gameserver.datatables.xml.DoorTable;
import com.src.gameserver.datatables.xml.MapRegionTable;
import com.src.gameserver.geo.pathfinding.Node;
import com.src.gameserver.model.actor.instance.L2DoorInstance;

public class Door
{
	private static Door _instance;

	public static Door getInstance()
	{
		if(_instance == null)
		{
			_instance = new Door();
		}

		return _instance;
	}
	public boolean checkIfDoorsBetween(Node start, Node end)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ());
	}

	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz)
	{
		int region;
		try
		{
			region = MapRegionTable.getInstance().getMapRegion(x, y);
		}
		catch(Exception e)
		{
			return false;
		}

		for(L2DoorInstance doorInst : DoorTable.getInstance().getDoors())
		{
			if(doorInst.getMapRegion() != region)
			{
				continue;
			}
			if(doorInst.getXMax() == 0)
			{
				continue;
			}

			if(x <= doorInst.getXMax() && tx >= doorInst.getXMin() || tx <= doorInst.getXMax() && x >= doorInst.getXMin())
			{
				if(y <= doorInst.getYMax() && ty >= doorInst.getYMin() || ty <= doorInst.getYMax() && y >= doorInst.getYMin())
				{
					if(doorInst.getStatus().getCurrentHp() > 0 && !doorInst.getOpen())
					{
						int px1 = doorInst.getXMin();
						int py1 = doorInst.getYMin();
						int pz1 = doorInst.getZMin();
						int px2 = doorInst.getXMax();
						int py2 = doorInst.getYMax();
						int pz2 = doorInst.getZMax();

						int l = tx - x;
						int m = ty - y;
						int n = tz - z;

						int dk;

						if((dk = (doorInst.getA() * l + doorInst.getB() * m + doorInst.getC() * n)) == 0) continue;

						float p = (float)(doorInst.getA() * x + doorInst.getB() * y + doorInst.getC() * z + doorInst.getD()) / (float)dk;

						int fx = (int)(x - l * p);
						int fy = (int)(y - m * p);
						int fz = (int)(z - n * p);

						if((Math.min(x, tx) <= fx && fx <= Math.max(x, tx)) && (Math.min(y, ty) <= fy && fy <= Math.max(y, ty)) && (Math.min(z, tz) <= fz && fz <= Math.max(z, tz)))
						{
							if(((fx >= px1 && fx <= px2) || (fx >= px2 && fx <= px1)) && ((fy >= py1 && fy <= py2) || (fy >= py2 && fy <= py1)) && ((fz >= pz1 && fz <= pz2) || (fz >= pz2 && fz <= pz1)))
							{
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}
}