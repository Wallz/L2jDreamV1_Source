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
package com.src.gameserver.model;

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javolution.util.FastList;

import com.src.gameserver.GameTimeController;
import com.src.gameserver.model.actor.L2Character;
import com.src.gameserver.model.actor.instance.L2PcInstance;
import com.src.gameserver.network.SystemMessageId;
import com.src.gameserver.network.serverpackets.ExOlympiadSpelledInfo;
import com.src.gameserver.network.serverpackets.MagicEffectIcons;
import com.src.gameserver.network.serverpackets.PartySpelled;
import com.src.gameserver.network.serverpackets.SystemMessage;
import com.src.gameserver.skills.Env;
import com.src.gameserver.skills.effects.EffectTemplate;
import com.src.gameserver.skills.funcs.Func;
import com.src.gameserver.skills.funcs.FuncTemplate;
import com.src.gameserver.skills.funcs.Lambda;
import com.src.gameserver.templates.skills.L2EffectType;
import com.src.gameserver.thread.ThreadPoolManager;

public abstract class L2Effect
{
	static final Logger _log = Logger.getLogger(L2Effect.class.getName());

	public static enum EffectState
	{
		CREATED,
		ACTING,
		FINISHING
	}
	
	private static final Func[] _emptyFunctionSet = new Func[0];

	private final L2Character _effector;
	protected final L2Character _effected;
	public L2Skill _skill;
	private final Lambda _lambda;
	private EffectState _state;
	private final int _period;
	private int _periodStartTicks;
	private int _periodfirsttime;
	private final FuncTemplate[] _funcTemplates;
	protected int _totalCount;
	private int _count;
	private int _abnormalEffect;
	public boolean preventExitUpdate;

	public final class EffectTask implements Runnable
	{
		protected final int _delay;
		protected final int _rate;

		EffectTask(int pDelay, int pRate)
		{
			_delay = pDelay;
			_rate = pRate;
		}

		@Override
		public void run()
		{
			try
			{
				if(getPeriodfirsttime() == 0)
				{
					setPeriodStartTicks(GameTimeController.getGameTicks());
				}
				else
				{
					setPeriodfirsttime(0);
				}
				scheduleEffect();
			}
			catch(Throwable e)
			{
				_log.log(Level.SEVERE, "", e);
			}
		}
	}

	private ScheduledFuture<?> _currentFuture;
	private EffectTask _currentTask;
	private final String _stackType;
	private final float _stackOrder;
	private final EffectTemplate _template;
	private boolean _inUse = false;

	protected L2Effect(Env env, EffectTemplate template)
	{
		_template = template;
		_state = EffectState.CREATED;
		_skill = env.skill;
		_effected = env.target;
		_effector = env.player;
		_lambda = template.lambda;
		_funcTemplates = template.funcTemplates;
		_count = template.counter;
		_totalCount = _count;
		int temp = template.period;
		if(env.skillMastery)
		{
			temp *= 2;
		}
		_period = temp;
		_abnormalEffect = template.abnormalEffect;
		_stackType = template.stackType;
		_stackOrder = template.stackOrder;
		_periodStartTicks = GameTimeController.getGameTicks();
		_periodfirsttime = 0;
		scheduleEffect();
	}

	public int getCount()
	{
		return _count;
	}

	public int getTotalCount()
	{
		return _totalCount;
	}

	public void setCount(int newcount)
	{
		_count = newcount;
	}

	public void setFirstTime(int newfirsttime)
	{
		if(_currentFuture != null)
		{
			_periodStartTicks = GameTimeController.getGameTicks() - newfirsttime * GameTimeController.TICKS_PER_SECOND;
			_currentFuture.cancel(false);
			_currentFuture = null;
			_currentTask = null;
			_periodfirsttime = newfirsttime;
			int duration = _period - _periodfirsttime;
			_currentTask = new EffectTask(duration * 1000, -1);
			_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration * 1000);
		}
	}

	public int getPeriod()
	{
		return _period;
	}

	public int getTime()
	{
		return (GameTimeController.getGameTicks() - _periodStartTicks) / GameTimeController.TICKS_PER_SECOND;
	}

	public int getTaskTime()
	{
		if(_count == _totalCount)
		{
			return 0;
		}

		return Math.abs(_count - _totalCount + 1) * _period + getTime() + 1;
	}

	public boolean getInUse()
	{
		return _inUse;
	}

	public void setInUse(boolean inUse)
	{
		_inUse = inUse;
	}

	public String getStackType()
	{
		return _stackType;
	}

	public float getStackOrder()
	{
		return _stackOrder;
	}

	public final L2Skill getSkill()
	{
		return _skill;
	}

	public final L2Character getEffector()
	{
		return _effector;
	}

	public final L2Character getEffected()
	{
		return _effected;
	}

	public boolean isSelfEffect()
	{
		return _skill._effectTemplatesSelf != null;
	}

	public boolean isHerbEffect()
	{
		if(getSkill().getName().contains("Herb"))
		{
			return true;
		}

		return false;
	}

	public final double calc()
	{
		Env env = new Env();
		env.player = _effector;
		env.target = _effected;
		env.skill = _skill;
		return _lambda.calc(env);
	}

	private synchronized void startEffectTask(int duration)
	{
		stopEffectTask();
		_currentTask = new EffectTask(duration, -1);
		_currentFuture = ThreadPoolManager.getInstance().scheduleEffect(_currentTask, duration);

		if(_state == EffectState.ACTING)
		{
			_effected.addEffect(this);
		}
	}

	private synchronized void startEffectTaskAtFixedRate(int delay, int rate)
	{
		stopEffectTask();
		_currentTask = new EffectTask(delay, rate);
		_currentFuture = ThreadPoolManager.getInstance().scheduleEffectAtFixedRate(_currentTask, delay, rate);

		if(_state == EffectState.ACTING)
		{
			_effected.addEffect(this);
		}
	}

	public final void exit()
	{
		this.exit(false);
	}

	public final void exit(boolean preventUpdate)
	{
		preventExitUpdate = preventUpdate;
		_state = EffectState.FINISHING;
		scheduleEffect();
	}

	public synchronized void stopEffectTask()
	{
		if(_currentFuture != null)
		{
			if(!_currentFuture.isCancelled())
			{
				_currentFuture.cancel(false);
			}

			_currentFuture = null;
			_currentTask = null;

			_effected.removeEffect(this);
		}
	}

	public abstract L2EffectType getEffectType();

	public void onStart()
	{
		if(_abnormalEffect != 0)
		{
			getEffected().startAbnormalEffect(_abnormalEffect);
		}
	}

	public void onExit()
	{
		if(_abnormalEffect != 0)
		{
			getEffected().stopAbnormalEffect(_abnormalEffect);
		}
	}

	public abstract boolean onActionTime();

	public final void rescheduleEffect()
	{
		if(_state != EffectState.ACTING)
		{
			scheduleEffect();
		}
		else
		{
			if(_count > 1)
			{
				startEffectTaskAtFixedRate(5, _period * 1000);
				return;
			}
			if(_period > 0 || _period == -1)
			{
				startEffectTask(_period * 1000);
				return;
			}
		}
	}

	public final void scheduleEffect()
	{
		if(_state == EffectState.CREATED)
		{
			_state = EffectState.ACTING;
			onStart();

			if(_skill.isPvpSkill() && getEffected() != null && getEffected() instanceof L2PcInstance && getShowIcon())
			{
				getEffected().sendPacket(new SystemMessage(SystemMessageId.YOU_FEEL_S1_EFFECT).addString(_skill.getName()));
			}

			if(_count > 1)
			{
				startEffectTaskAtFixedRate(5, _period * 1000);
				return;
			}

			if(_period > 0)
			{
				startEffectTask(_period * 1000);
				return;
			}
		}

		if(_state == EffectState.ACTING)
		{
			if(_count-- > 0)
			{
				if(getInUse())
				{
					if(onActionTime())
					{
						return;
					}
				}
				else if(_count > 0)
				{
					return;
				}
			}

			_state = EffectState.FINISHING;
		}

		if(_state == EffectState.FINISHING)
		{
			onExit();

			if(_count == 0 && getEffected() != null && getEffected() instanceof L2PcInstance && getShowIcon())
			{
				getEffected().sendPacket(new SystemMessage(SystemMessageId.S1_HAS_WORN_OFF).addString(_skill.getName()));
			}

			stopEffectTask();
		}
	}

	public Func[] getStatFuncs()
	{
		if(_funcTemplates == null)
		{
			return _emptyFunctionSet;
		}
		List<Func> funcs = new FastList<Func>();
		for(FuncTemplate t : _funcTemplates)
		{
			Env env = new Env();
			env.player = getEffector();
			env.target = getEffected();
			env.skill = getSkill();
			Func f = t.getFunc(env, this);
			if(f != null)
			{
				funcs.add(f);
			}
		}

		if(funcs.size() == 0)
		{
			return _emptyFunctionSet;
		}

		return funcs.toArray(new Func[funcs.size()]);
	}

	public final void addIcon(MagicEffectIcons mi)
	{
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;

		if(task == null || future == null)
		{
			return;
		}

		if(_state == EffectState.FINISHING || _state == EffectState.CREATED)
		{
			return;
		}

		if(!getShowIcon())
		{
			return;
		}

		L2Skill sk = getSkill();

		if(task._rate > 0)
		{
			if(sk.isPotion())
			{
				mi.addEffect(sk.getId(), getLevel(), sk.getBuffDuration() - getTaskTime() * 1000);
			}
			else
			{
				mi.addEffect(sk.getId(), getLevel(), -1);
			}
		}
		else
		{
			mi.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
		}

		task = null;
		future = null;
	}

	public final void addPartySpelledIcon(PartySpelled ps)
	{
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;

		if(task == null || future == null)
		{
			return;
		}

		if(_state == EffectState.FINISHING || _state == EffectState.CREATED)
		{
			return;
		}

		L2Skill sk = getSkill();
		ps.addPartySpelledEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));

		task = null;
		future = null;
		sk = null;
	}

	public final void addOlympiadSpelledIcon(ExOlympiadSpelledInfo os)
	{
		EffectTask task = _currentTask;
		ScheduledFuture<?> future = _currentFuture;

		if(task == null || future == null)
		{
			return;
		}

		if(_state == EffectState.FINISHING || _state == EffectState.CREATED)
		{
			return;
		}

		L2Skill sk = getSkill();
		os.addEffect(sk.getId(), getLevel(), (int) future.getDelay(TimeUnit.MILLISECONDS));
	}

	public int getLevel()
	{
		return getSkill().getLevel();
	}

	public int getPeriodfirsttime()
	{
		return _periodfirsttime;
	}

	public void setPeriodfirsttime(int periodfirsttime)
	{
		_periodfirsttime = periodfirsttime;
	}

	public int getPeriodStartTicks()
	{
		return _periodStartTicks;
	}

	public void setPeriodStartTicks(int periodStartTicks)
	{
		_periodStartTicks = periodStartTicks;
	}

	public final boolean getShowIcon()
	{
		return _template.showIcon;
	}
}