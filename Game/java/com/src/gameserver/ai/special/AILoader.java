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
package com.src.gameserver.ai.special;

import com.src.gameserver.ai.special.group.AncientEgg;
import com.src.gameserver.ai.special.group.BeastFarm;
import com.src.gameserver.ai.special.group.BrekaOrcOverlord;
import com.src.gameserver.ai.special.group.CatsEyeBandit;
import com.src.gameserver.ai.special.group.Chests;
import com.src.gameserver.ai.special.group.DeluLizardmanSpecialAgent;
import com.src.gameserver.ai.special.group.DeluLizardmanSpecialCommander;
import com.src.gameserver.ai.special.group.EvaBox;
import com.src.gameserver.ai.special.group.FairyTrees;
import com.src.gameserver.ai.special.group.GiantScouts;
import com.src.gameserver.ai.special.group.HotSpringDisease;
import com.src.gameserver.ai.special.group.KarulBugBear;
import com.src.gameserver.ai.special.group.Monastery;
import com.src.gameserver.ai.special.group.OlMahumGeneral;
import com.src.gameserver.ai.special.group.PolymorphingAngel;
import com.src.gameserver.ai.special.group.PolymorphingOnAttack;
import com.src.gameserver.ai.special.group.RetreatOnAttack;
import com.src.gameserver.ai.special.group.ScarletStokateNoble;
import com.src.gameserver.ai.special.group.SeeThroughSilentMove;
import com.src.gameserver.ai.special.group.SpeakingNPCs;
import com.src.gameserver.ai.special.group.Splendor;
import com.src.gameserver.ai.special.group.SummonMinions;
import com.src.gameserver.ai.special.group.SummonOnAttack;
import com.src.gameserver.ai.special.group.TimakOrcOverlord;
import com.src.gameserver.ai.special.group.TimakOrcTroopLeader;
import com.src.gameserver.ai.special.group.Transform;
import com.src.gameserver.ai.special.group.TurekOrcFootman;
import com.src.gameserver.ai.special.group.TurekOrcSupplier;
import com.src.gameserver.ai.special.group.TurekOrcWarlord;
import com.src.gameserver.ai.special.group.VarkaKetraAlly;
import com.src.gameserver.ai.special.group.ZombieGatekeepers;
import com.src.gameserver.ai.special.individual.Anays;
import com.src.gameserver.ai.special.individual.Antharas;
import com.src.gameserver.ai.special.individual.Baium;
import com.src.gameserver.ai.special.individual.Barakiel;
import com.src.gameserver.ai.special.individual.Benom;
import com.src.gameserver.ai.special.individual.Core;
import com.src.gameserver.ai.special.individual.DrChaos;
import com.src.gameserver.ai.special.individual.FleeNpc;
import com.src.gameserver.ai.special.individual.Frintezza;
import com.src.gameserver.ai.special.individual.Golkonda;
import com.src.gameserver.ai.special.individual.Gordon;
import com.src.gameserver.ai.special.individual.Gustav;
import com.src.gameserver.ai.special.individual.Hallate;
import com.src.gameserver.ai.special.individual.IceFairySirra;
import com.src.gameserver.ai.special.individual.Kernon;
import com.src.gameserver.ai.special.individual.Nurka;
import com.src.gameserver.ai.special.individual.Orfen;
import com.src.gameserver.ai.special.individual.QueenAnt;
import com.src.gameserver.ai.special.individual.Sailren;
import com.src.gameserver.ai.special.individual.Valakas;
import com.src.gameserver.ai.special.individual.VanHalter;
import com.src.gameserver.ai.special.individual.Zaken;
import com.src.gameserver.managers.QuestManager;

public class AILoader
{
	public static void init()
	{	
		new AncientEgg(-1, "AncientEgg", "ai_grp");
		new BeastFarm(-1,"beast_farm","ai_grp");
		new BrekaOrcOverlord(-1, "BrekaOrcOverlord", "ai_grp");
		new CatsEyeBandit(-1, "CatsEyeBandit", "ai_grp");
		new Chests(-1, "chests", "ai_grp");
		new DeluLizardmanSpecialAgent(-1, "DeluLizardmanSpecialAgent", "ai_grp");
		new DeluLizardmanSpecialCommander(-1, "DeluLizardmanSpecialCommander", "ai_grp");
		new EvaBox(-1, "EvaBox", "ai_grp");
		new FairyTrees(-1, "FairyTrees", "ai_grp");
		new GiantScouts(-1, "GiantScouts", "ai_grp");
		new HotSpringDisease(-1, "HotSpringDisease", "ai_grp");
		new KarulBugBear(-1, "KarulBugBear", "ai_grp");
		new Monastery(-1, "monastery", "ai_grp");
		new OlMahumGeneral(-1, "OlMahumGeneral", "ai_grp");
		new PolymorphingAngel(-1, "PolymorphingAngel", "ai_grp");
		new PolymorphingOnAttack(-1, "PolymorphingOnAttack", "ai_grp");
		new RetreatOnAttack(-1,"retreatOnAttack","ai_grp");
		new SeeThroughSilentMove(-1,"SeeThroughSilentMove","ai_grp");
		new ScarletStokateNoble(-1, "ScarletStokateNoble", "ai_grp");
		new SpeakingNPCs(-1, "SpeakingNPCs", "ai_grp");
		new Splendor(-1, "Splendor", "ai_grp");
		new SummonMinions(-1, "SummonMinions", "ai_grp");
		new SummonOnAttack(-1, "SummonOnAttack", "ai_grp");
		new TimakOrcOverlord(-1, "TimakOrcOverlord", "ai_grp");
		new TimakOrcTroopLeader(-1, "TimakOrcTroopLeader", "ai_grp");
		new Transform(-1, "transform", "ai_grp");
		new TurekOrcFootman(-1, "TurekOrcFootman", "ai_grp");
		new TurekOrcSupplier(-1, "TurekOrcSupplier", "ai_grp");
		new TurekOrcWarlord(-1, "TurekOrcWarlord", "ai_grp");
		new VarkaKetraAlly(-1, "Varka Ketra Ally", "ai_grp");
		new ZombieGatekeepers(-1, "ZombieGatekeepers", "ai_grp");

		QuestManager.getInstance().getcountInd("ai_grp");

		new Anays(-1, "Anays", "ai_inv");	
		new Antharas(-1, "antharas", "ai_inv");
		new Baium(-1, "baium", "ai_inv");
		new Barakiel(-1, "Barakiel", "ai_inv");
		new Benom(-1,"benom","ai_inv");
		new Core(-1, "core", "ai_inv");
		new DrChaos(-1, "DrChaos", "ai_inv");
		new FleeNpc(+1, "FleeNpc", "ai_inv");
		new Frintezza(-1, "frintezza", "ai_inv");
		new Golkonda(-1, "Golkonda", "ai_inv");
		new Gordon(-1, "Gordon", "ai_inv");
		new Gustav(-1,"gustav","ai_inv");
		new Hallate(-1, "Hallate", "ai_inv");
		new IceFairySirra(-1, "IceFairySirra", "ai_inv");
		new Kernon(-1, "Kernon", "ai_inv");
		new Nurka(-1,"nurka","ai_inv");
		new Orfen(-1, "Orfen", "ai_inv");
		new QueenAnt(-1, "queen_ant", "ai_inv");
		new Sailren(-1, "sailren", "ai_inv");
		new Valakas(-1, "valakas", "ai_inv");
		VanHalter.getInstance();
		new Zaken(-1, "zaken", "ai_inv");

		QuestManager.getInstance().getcountInd("ai_inv");
	}
}