# Arrogant Search version 0.1 
# by Fulminus
# in this version, the quest only works as total solo (no option to work with friends) and
# only for the purpose of gaining access to Baium's floor (not for making money via rewards).
#Updated by RusTeam
import sys
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest
#Quest info
qn = "q348_ArrogantSearch"

#Messages
default   = "<html><body>I have nothing to say to you.</body></html>"
#MOBS TO KILL
ARK_GUARDIAN_ELBEROTH = 27182
ARK_GUARDIAN_SHADOWFANG = 27183
ANGEL_KILLER = 27184
PLATINUM = [20828,20829]
ANGELS = [20830,20831,20860,20859,21070,21071]
LESSER_GIANT_MAGE = 20657
LESSER_GIANT_ELDER = 20658

#NPCS TO TALK TO
HANELLIN = 30864
HOLY_ARK_OF_SECRECY_1 = 30977
HOLY_ARK_OF_SECRECY_2 = 30978
HOLY_ARK_OF_SECRECY_3 = 30979
ARK_GUARDIANS_CORPSE = 30980
HARNE = 30144
CLAUDIA_ATHEBALT = 31001
MARTIEN = 30645

#items
TITANS_POWERSTONE = 4287
HANELLINS_FIRST_LETTER = 4288
HANELLINS_SECOND_LETTER = 4289
HANELLINS_THIRD_LETTER = 4290
FIRST_KEY_OF_ARK = 4291
SECOND_KEY_OF_ARK = 4292
THIRD_KEY_OF_ARK = 4293
WHITE_FABRIC_1 = 4294  #to use on Platinum Tribe Shamans/Overlords
BLOODED_FABRIC = 4295
HANELLINS_WHITE_FLOWER = 4394
HANELLINS_RED_FLOWER = 4395
HANELLINS_YELLOW_FLOWER = 4396
BOOK_OF_SAINT = 4397  # Ark2 (after fight with Elberoth)
BLOOD_OF_SAINT = 4398 # Ark1 (after fight with Angel Killer)
BRANCH_OF_SAINT = 4399 # Ark3 (after fight with Shadowfang)
WHITE_FABRIC_0 = 4400  #talk to Hanellin to see what to do (for companions)
WHITE_FABRIC_2 = 5232  #to use on Guardian Angels and Seal Angels

ANTIDOTE = 1831
HEALING_POTION = 1061

#ARK: [key, summon, no-key text, openning-with-key text, already-openned text, content item]
ARKS={
HOLY_ARK_OF_SECRECY_1: [FIRST_KEY_OF_ARK,0,"30977-01.htm","30977-02.htm","30977-03.htm",BLOOD_OF_SAINT],
HOLY_ARK_OF_SECRECY_2: [SECOND_KEY_OF_ARK,ARK_GUARDIAN_ELBEROTH,"That doesn't belong to you.  Don't touch it!","30978-02.htm","30978-03.htm",BOOK_OF_SAINT],
HOLY_ARK_OF_SECRECY_3: [THIRD_KEY_OF_ARK,ARK_GUARDIAN_SHADOWFANG, "Get off my sight, you infidels!","30979-02.htm","30979-03.htm",BRANCH_OF_SAINT],
}

# npc: letter to take, item to check for, 1st time htm, return htm, completed part htm, [x,y,z of chest]
ARK_OWNERS={
HARNE: [HANELLINS_FIRST_LETTER, BLOOD_OF_SAINT, '30144-01.htm', '30144-02.htm', '30144-03.htm', [-418,44174,-3568]],
CLAUDIA_ATHEBALT: [HANELLINS_SECOND_LETTER, BOOK_OF_SAINT, '31001-01.htm', '31001-02.htm', '31001-03.htm', [181472,7158,-2725]],
MARTIEN: [HANELLINS_THIRD_LETTER, BRANCH_OF_SAINT, '30645-01.htm', '30645-02.htm', '30645-03.htm', [50693,158674,376]]
}

#mob: cond, giveItem, amount, chance%, takeItem (assumed to take only 1 of it)
DROPS ={
LESSER_GIANT_MAGE: [2,TITANS_POWERSTONE,1,10,0],
LESSER_GIANT_ELDER: [2,TITANS_POWERSTONE,1,10,0],
ANGEL_KILLER: [5, FIRST_KEY_OF_ARK,1,100,0],
ARK_GUARDIAN_ELBEROTH: [5, SECOND_KEY_OF_ARK,1,100,0],
ARK_GUARDIAN_SHADOWFANG: [5, THIRD_KEY_OF_ARK,1,100,0],
}

"""
notes:
In order to make it easier to participate in a Baium Raid, the number of Platinum Race monsters that must be
killed to get "Blooded Fabric" in the Arrogant Search quest has been reduced a bit.  However, if a player clicks
"Say that you will come back" to Magister Hanellin while carrying out the quest alone and repeats the quest,
he must hunt the same number of Platinum Race monsters as before.
In the process of delivering the Blooded Fabric to Sir Athebaldt/Iason Heine/Hardin when carrying out the
Arrogant Search quest alone, if it is not the number of "Blooded Fabrics" that the related NPCs want, the
game has been modified to have the player go back to Hanellin. Hanellin gives a reward in proportion to the
number of errands run when the number of "Blooded Fabrics" that have to be delivered is not right and then
the quest is ended.
"""

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)

 def onAdvEvent (self,event,npc,player):
    st = player.getQuestState(qn)
    if not st : return
    if player :
        st = player.getQuestState(qn)
        if not st : return
        htmltext = event
        if event == "30864-03.htm" :
            st.set("cond","2")
        elif event == "30864_04a" :  #work alone
            st.set("cond","4")
            st.takeItems(TITANS_POWERSTONE,-1)
            htmltext = "30864-04c.htm"
            st.set("companions","0")
        elif event == "30864_04b" :  #work with friends
            st.set("cond","3")
            st.set("companions","1")
            st.takeItems(TITANS_POWERSTONE,-1)
            htmltext = "not yet implemented"
            #todo: give flowers & handle the multiperson quest...
        elif event == "viborodin" :
            st.takeItems(ANTIDOTE,5)
            st.takeItems(HEALING_POTION,1)
            st.giveItems(WHITE_FABRIC_1,1)      
            st.set("cond","24")
            htmltext = "30864-07.htm"    # go get platinum tribe blood...
        elif event == "vibordwa" :
            st.takeItems(ANTIDOTE,5)
            st.takeItems(HEALING_POTION,1)
            st.giveItems(WHITE_FABRIC_1,1)
            st.set("cond","25")
            htmltext = "30864-07b.htm"    # go get platinum tribe blood...  
        elif event == "nagradadwa" :
            st.giveItems(WHITE_FABRIC_2,9)
            st.set("cond","26")
            htmltext = "nagradadwab.htm"
        return htmltext
    elif event == "pokraska" :
        st.takeItems(WHITE_FABRIC_1,1)
        st.giveItems(BLOODED_FABRIC,1)
        st.playSound("ItemSound.quest_itemget")
        htmltext = "30864-06a.htm"
        self.cancelQuestTimer("pokraska",None,None)
    return     

    
 def onTalk (self,npc,player):
    htmltext = "<html><body>You are either not carrying out your quest or don't meet the criteria.</body></html>"
    st = player.getQuestState(qn)
    if not st : return htmltext

    npcId = npc.getNpcId()
    id = st.getState()
    if npcId != HANELLIN and id != PROGRESS : return htmltext

    cond = st.getInt("cond")
    if npcId == HANELLIN :
        if id == CREATED :
            if player.getLevel() < 60 :
                st.exitQuest(1)
                htmltext = "30864-01.htm"     #not qualified
                st.exitQuest(1)
            else :
                st.setState(PROGRESS)
                st.set("cond","1")
                htmltext = "privetstvie.htm"    # Successful start: begin the dialog which will set cond=2
        # Has returned before getting the powerstone
        elif cond==2 and st.getQuestItemsCount(TITANS_POWERSTONE)==0 :
            htmltext = "30864-03a.htm"    # go get the titan's powerstone
        elif cond==2 :
            htmltext = "30864-04.htm"    # Ask "work alone or in group?"...only alone is implemented in v0.1
        elif cond==4 :
            st.set("cond","5")
            st.giveItems(HANELLINS_FIRST_LETTER,1)
            st.giveItems(HANELLINS_SECOND_LETTER,1)
            st.giveItems(HANELLINS_THIRD_LETTER,1)
            htmltext = "30864-05.htm"    # Go get the 3 sacred relics
        elif cond == 5 and st.getQuestItemsCount(BOOK_OF_SAINT)+st.getQuestItemsCount(BLOOD_OF_SAINT)+st.getQuestItemsCount(BRANCH_OF_SAINT)<3 :
            htmltext = "30864-05.htm"    # Repeat: Go get the 3 sacred relics
        elif cond == 5 :
            htmltext = "30864-06.htm"     # All relics collected!...Get me antidotes & greater healing
            st.takeItems(BOOK_OF_SAINT,-1)
            st.takeItems(BLOOD_OF_SAINT,-1)
            st.takeItems(BRANCH_OF_SAINT,-1)
            st.set("cond","22")
        elif cond == 22 :
            if st.getQuestItemsCount(ANTIDOTE)<5 and st.getQuestItemsCount(HEALING_POTION)<1:
                htmltext = "30864-06a.htm"     # where are my antidotes & greater healing
            else :
                if st.getQuestItemsCount(ANTIDOTE) >= 5 and st.getQuestItemsCount(HEALING_POTION) >= 1 :
                    htmltext = "vibortryapki.htm"
                else :
                    htmltext = "30864-06a.htm"
        elif cond == 24 :
            if st.getQuestItemsCount(BLOODED_FABRIC) >= 1 :
                htmltext = "vibortryapki.htm"
                st.takeItems(BLOODED_FABRIC,1)
        elif cond == 25 :
            if st.getQuestItemsCount(BLOODED_FABRIC) >= 1 :
                htmltext = "privetstviedva.htm"
        elif cond == 26 :
            if st.getQuestItemsCount(WHITE_FABRIC_2) >= 1 :
                htmltext = "tryapkadwa.htm"
            else :
                htmltext = "bolwenet.htm"
                st.playSound("ItemSound.quest_finish")
                st.exitQuest(1)
    # Other NPCs follow:
    elif cond == 5:
        if npcId in ARK_OWNERS.keys() :
            # first meeting...have the letter
            if st.getQuestItemsCount(ARK_OWNERS[npcId][0])==1:
                st.takeItems(ARK_OWNERS[npcId][0],1)
                htmltext = ARK_OWNERS[npcId][2]
                st.addRadar(ARK_OWNERS[npcId][5][0],ARK_OWNERS[npcId][5][1],ARK_OWNERS[npcId][5][2])
            # do not have letter and do not have the item
            elif st.getQuestItemsCount(ARK_OWNERS[npcId][1]) < 1:
                htmltext = ARK_OWNERS[npcId][3]
                st.addRadar(ARK_OWNERS[npcId][5][0],ARK_OWNERS[npcId][5][1],ARK_OWNERS[npcId][5][2])
            else:   #have the item (done)
                htmltext = ARK_OWNERS[npcId][4]
        elif npcId in ARKS.keys():
            # if you do not have the key (first meeting)
            if st.getQuestItemsCount(ARKS[npcId][0])==0:
                if ARKS[npcId][1] != 0 :    # spawn the NPC, if appropriate
                    st.addSpawn(ARKS[npcId][1],120000)
                return ARKS[npcId][2]
            # if the player already has openned the chest and has its content, show "chest empty"
            elif st.getQuestItemsCount(ARKS[npcId][5])==1:  
                htmltext = ARKS[npcId][4]
            else:   # the player has the key and doesn't have the contents, give the contents
                htmltext = ARKS[npcId][3]
                st.takeItems(ARKS[npcId][0],1)
                st.giveItems(ARKS[npcId][5],1)
        elif npcId == ARK_GUARDIANS_CORPSE :
            # if you do not have the key (first meeting)
            if st.getQuestItemsCount(FIRST_KEY_OF_ARK)==0 and st.getInt("angelKillerIsDefeated")==0 :
                st.addSpawn(ANGEL_KILLER,120000)
                htmltext = "30980-01.htm"
            elif st.getQuestItemsCount(FIRST_KEY_OF_ARK)==0 and st.getInt("angelKillerIsDefeated")==1 :
                st.giveItems(FIRST_KEY_OF_ARK,1)
                htmltext = "30980-02.htm"
            else :
                htmltext = "30980-03.htm"
    return htmltext

 def onAttack (self,npc,player,damage,isPet):
     st = player.getQuestState(qn)
     if st :
         npcId = npc.getNpcId()
         cond = st.getInt("cond")
         if npcId in PLATINUM :
             if cond == 24 or cond == 25 :
                 wanse = st.getRandom(300)
                 if wanse < 1 :
                     st.takeItems(WHITE_FABRIC_1,1)
                     st.giveItems(BLOODED_FABRIC,1)
                     st.playSound("ItemSound.quest_itemget")
     return
 def onKill(self,npc,player,isPet):
     st = player.getQuestState(qn)
     if not st : return 
     if st.getState() != PROGRESS : return 
   
     npcId = npc.getNpcId()
     if npcId in DROPS.keys() :
         cond = DROPS[npcId][0]
         chance =  DROPS[npcId][3]
         if (st.getInt("cond") == cond) and (st.getQuestItemsCount(DROPS[npcId][1]) < DROPS[npcId][2]) and (st.getRandom(100) < DROPS[npcId][3]) :
             st.giveItems(DROPS[npcId][1],DROPS[npcId][2])
             st.playSound("ItemSound.quest_itemget")
             if DROPS[npcId][4] != 0:
                 st.takeItems(DROPS[npcId][4],1)
     if npcId == ANGEL_KILLER :
         return "Ha, that was fun! If you wish to find the key, search the corpse"
     if npcId in ANGELS :
         if st.getQuestItemsCount(WHITE_FABRIC_2) >= 1 :
             if st.getRandom(100) < 10 :
                 st.takeItems(WHITE_FABRIC_2,1)
                 st.giveItems(BLOODED_FABRIC,1)
                 st.playSound("ItemSound.quest_itemget")   
     return


# Quest class and state definition
QUEST       = Quest(348, qn, "An Arrogant Search")

CREATED     = State('Start',     QUEST)
PROGRESS    = State('Progress',   QUEST)
COMPLETED   = State('Completed', QUEST)

QUEST.setInitialState(CREATED)
QUEST.addStartNpc(HANELLIN)
QUEST.addTalkId(HANELLIN)
QUEST.addTalkId(ARK_GUARDIANS_CORPSE)

for i in ARK_OWNERS.keys() :
    QUEST.addTalkId(i)
for i in ARKS.keys() :
    QUEST.addTalkId(i)

for i in DROPS.keys():
  QUEST.addKillId(i)
  
for i in PLATINUM:
  QUEST.addAttackId(i)
  
for i in ANGELS:
  QUEST.addKillId(i)