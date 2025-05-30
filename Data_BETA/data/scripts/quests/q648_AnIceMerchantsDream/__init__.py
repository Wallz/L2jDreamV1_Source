# @author: Aquanox [RusTeam] , thx to May for idea
import sys
from com.src import Config
from com.src.util.random import Rnd
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest


qn  = "q648_AnIceMerchantsDream"
qn2 = "q115_TheOtherSideofTruth" 
# NPC
RAFFORTY = 32020
ICE_SHELF = 32023

# REWARDS
ADENA = 57
COARSE_BONE_POWDER = 1881
CRAFTED_LEATHER = 1894
STEEL = 1880
EWA = 731
EWB = 947
EAA = 732
EAB = 948
# ITEMS
SILVER_ICE_CRYSTAL = 8077
SILVER_DROP_CHANCE = 75 # 75%
PRICE_PER_SILVER = 300

BLACK_ICE_CRYSTAL = 8078
BLACK_DROP_CHANCE = 10 # 10%
PRICE_PER_BLACK = 1200

HEMOSYCLE = 8057
HEMOSYCLE_CHANCE = 1 # 1%

SHELF_CHANCE = 30

#MOBS
MONSTERS = [22079,22080,22081,22082,22083,22084,22085,22086,22087,22087,22088,22089,22090,22091,22092,22093,22094,22095,22096,22097,22098,32020,32023]

class Quest (JQuest):
    
    def __init__(self,id,name,descr): 
        JQuest.__init__(self,id,name,descr)
    
    def onAdvEvent (self,event,npc,player):
        st = player.getQuestState(qn)
        cond = st.getInt("cond")
        if npc.getNpcId() == RAFFORTY :
            if event == "start" :
                st2 = player.getQuestState("q115_TheOtherSideOfTruth")
                if st2 :
                    st.set("cond","2")
                    st.setState(STARTED)
                    st.playSound("ItemSound.quest_accept")
                else :
                    st.set("cond","1")
                    st.setState(STARTED)
                    st.playSound("ItemSound.quest_accept")
                return "03.htm"
            elif event == "quit":
                st.takeItems(SILVER_ICE_CRYSTAL, st.getQuestItemsCount(SILVER_ICE_CRYSTAL))
                st.takeItems(BLACK_ICE_CRYSTAL,  st.getQuestItemsCount(BLACK_ICE_CRYSTAL))
                st.playSound("ItemSound.quest_finish")
                st.exitQuest(1)
                return "exit.htm"
            elif event == "stay":
                return "04.htm"
            elif event == "hemo":
                return "06.htm"
            elif event == "reward":
                if st.getQuestItemsCount(SILVER_ICE_CRYSTAL) + st.getQuestItemsCount(BLACK_ICE_CRYSTAL) >=1 :
                    st2 = player.getQuestState("q115_TheOtherSideOfTruth")
                    if st2 :
                        st.set("cond","2")
                        return "02_2.htm"
                    else :
                        return "02_1.htm"                    
                else :
                    return "05.htm"
            elif event == "adena" :
                num = st.getQuestItemsCount(SILVER_ICE_CRYSTAL)
                if cond >= 1 and num > 0 :
                    st.takeItems(SILVER_ICE_CRYSTAL, num)
                    st.giveItems(ADENA, int(PRICE_PER_SILVER*num))
                    num2 = st.getQuestItemsCount(SILVER_ICE_CRYSTAL)
                    if cond == 2 and num2 > 0 :
                        st.takeItems(BLACK_ICE_CRYSTAL, num)
                        st.giveItems(ADENA, int(PRICE_PER_BLACK*num))
                    st.playSound("ItemSound.quest_finish")
                    qst = player.getQuestState(qn2)
                    if qst :
                        if qst.getState() == COMPLETED :
                            st.set("cond","2")
                return "03.htm"
            if cond == 2 :
                    htmltext = "noitems.htm"
                    if event == "resources" :
                        htmltext = "res.htm"
                    elif event == "cbp":
                        if st.getQuestItemsCount(SILVER_ICE_CRYSTAL) >= 6 :
                            st.takeItems(SILVER_ICE_CRYSTAL, 6)
                            st.giveItems(COARSE_BONE_POWDER, 1)
                            htmltext = "res.htm"
                        else :
                            htmltext = "noitems.htm"
                    elif event == "cl":
                        if st.getQuestItemsCount(SILVER_ICE_CRYSTAL) >= 23 :
                            st.takeItems(SILVER_ICE_CRYSTAL, 23)
                            st.giveItems(CRAFTED_LEATHER, 1)
                            htmltext = "res.htm"
                        else :
                            htmltext = "noitems.htm"
                    elif event == "steel":
                        if st.getQuestItemsCount(SILVER_ICE_CRYSTAL) >= 8 :
                            st.takeItems(SILVER_ICE_CRYSTAL, 8)
                            st.giveItems(STEEL, 1)
                            htmltext = "res.htm"
                        else :
                            htmltext = "noitems.htm"
                    elif event == "ewa":
                        if st.getQuestItemsCount(BLACK_ICE_CRYSTAL) >= 1800 :
                            st.takeItems(BLACK_ICE_CRYSTAL, 1800)
                            st.giveItems(EWA, 1)
                            htmltext = "res.htm"
                        else :
                            htmltext = "noitems.htm"
                    elif event == "eaa":
                        if st.getQuestItemsCount(BLACK_ICE_CRYSTAL) >= 240 :
                            st.takeItems(BLACK_ICE_CRYSTAL, 240)
                            st.giveItems(EAA, 1)
                            htmltext = "res.htm"
                        else :
                            htmltext = "noitems.htm"
                    elif event == "ewb":
                        if st.getQuestItemsCount(BLACK_ICE_CRYSTAL) >= 500 :
                            st.takeItems(BLACK_ICE_CRYSTAL, 500)
                            st.giveItems(EWB, 1)
                            htmltext = "res.htm"
                        else :
                            htmltext = "noitems.htm"
                    elif event == "eab":
                        if st.getQuestItemsCount(BLACK_ICE_CRYSTAL) >= 80 :
                            st.takeItems(BLACK_ICE_CRYSTAL, 80)
                            st.giveItems(EAB, 1)
                            htmltext = "res.htm"
                        else :
                            htmltext = "noitems.htm"
                    return htmltext
        elif npc.getNpcId() == ICE_SHELF :
            if cond == 2 :
                if st.getQuestItemsCount(SILVER_ICE_CRYSTAL) < 1 :
                    return "noitems.htm"
                if event == "startwrk" :
                    return "shelf_2.htm"
                elif event == "chisel" :
                    return "shelf_3.htm"
                elif event == "scramer" :
                    return "shelf_3.htm"
                elif event == "knife" :
                    return "shelf_4.htm"
                elif event == "file" :
                    return "shelf_4.htm"
                elif event == "try" :
                    if Rnd.chance(SHELF_CHANCE):
                        st.takeItems(SILVER_ICE_CRYSTAL, 1)
                        st.giveItems(BLACK_ICE_CRYSTAL, 1)
                        st.playSound("ItemSound.quest_itemget")                        
                        return "shelf_0.htm"
                    else :
                        st.takeItems(SILVER_ICE_CRYSTAL, 1)
                        st.playSound("ItemSound.trash_basket")
                        return "Fail"

    def onTalk (self,npc,player):
        htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
        st = player.getQuestState(qn)
        if not st: return
        npcId = npc.getNpcId()
        cond = st.getInt("cond")
        if npcId == RAFFORTY:
            if cond == 0:
                if player.getLevel() >= 53 and player.getLevel() <= 63:
                    htmltext = "00.htm"
                else:
                    htmltext = "wrong_level.htm"
            elif cond == 1:
                htmltext = "01_1.htm"
                st2 = player.getQuestState("q115_TheOtherSideOfTruth")
                if st2 :
                    st.set("cond","2")
            elif cond == 2:
                htmltext = "01_2.htm"           
        elif npcId == ICE_SHELF:
            htmltext = "shelf_0.htm"
            if cond == 2 :
                htmltext = "shelf_1.htm"
        return htmltext

    def onKill (self, npc, player,isPet):
        st = player.getQuestState(qn)
        if not st : return
        cond = st.getInt("cond")
        npcId = npc.getNpcId()
        if (cond == 1) or (cond == 2):
            if Rnd.get(100) <= SILVER_DROP_CHANCE  :
                st.playSound("ItemSound.quest_itemget")
                st.giveItems(SILVER_ICE_CRYSTAL,int(1*Config.RATE_DROP_QUEST))
        if cond == 2 :
            if Rnd.get(100) <= HEMOSYCLE_CHANCE*Config.RATE_DROP_QUEST  :
                st.playSound("ItemSound.quest_itemget")
                st.giveItems(HEMOSYCLE,Rnd.get(1,2))

QUEST       = Quest(648, qn, "An Ice Merchants Dream") 

CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)
COMPLETED   = State('Completed', QUEST)

for npcId in MONSTERS:
    QUEST.addAttackId(npcId)
    QUEST.addKillId(npcId)

QUEST.setInitialState(CREATED)

QUEST.addStartNpc(RAFFORTY)
QUEST.addStartNpc(ICE_SHELF)

QUEST.addTalkId(RAFFORTY)
QUEST.addTalkId(ICE_SHELF)
