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
package com.src.gameserver.skills.effects;

import com.src.Config;
import com.src.gameserver.geo.GeoData;
import com.src.gameserver.model.L2Effect;
import com.src.gameserver.model.Location;
import com.src.gameserver.network.serverpackets.FlyToLocation;
import com.src.gameserver.network.serverpackets.FlyToLocation.FlyType;
import com.src.gameserver.network.serverpackets.ValidateLocation;
import com.src.gameserver.skills.Env;
import com.src.gameserver.templates.skills.L2EffectType;

public class EffectThrowUp extends L2Effect
{
	private int _x, _y, _z;

	public EffectThrowUp(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	@Override
	public L2EffectType getEffectType()
	{
		return L2EffectType.THROW_UP;
	}

	@Override
	public void onStart()
	{
		// Get current position of the L2Character
		final int curX = getEffected().getX();
		final int curY = getEffected().getY();
		final int curZ = getEffected().getZ();

		// Get the difference between effector and effected positions
		double dx = getEffector().getX() - curX;
		double dy = getEffector().getY() - curY;
		double dz = getEffector().getZ() - curZ;

		// Calculate distance between effector and effected current position
		double distance = Math.sqrt(dx * dx + dy * dy);
		if(distance < 1 || distance > 2000)
			return;

		int offset = Math.min((int) distance + getSkill().getFlyRadius(), 1400);
		double cos, sin;

		// approximation for moving futher when z coordinates are different
		// TODO: handle Z axis movement better
		offset += Math.abs(dz);
		if(offset < 5)
			offset = 5;

		// Calculate movement angles needed
		sin = dy / distance;
		cos = dx / distance;

		// Calculate the new destination with offset included
		_x = getEffector().getX() - (int) (offset * cos);
		_y = getEffector().getY() - (int) (offset * sin);
		_z = getEffected().getZ();

		if(Config.GEODATA > 0)
		{
			Location destiny = GeoData.getInstance().moveCheck(getEffected().getX(), getEffected().getY(), getEffected().getZ(), _x, _y, _z);
			_x = destiny.getX();
			_y = destiny.getY();
		}

		getEffected().startStunning();
		getEffected().broadcastPacket(new FlyToLocation(getEffected(), _x, _y, _z, FlyType.THROW_UP));
	}

	@Override
	public boolean onActionTime()
	{
		return false;
	}

	@Override
	public void onExit()
	{
		getEffected().stopStunning(null);
		getEffected().setXYZ(_x, _y, _z);
		getEffected().broadcastPacket(new ValidateLocation(getEffected()));
	}

}