import sys
from com.src import Config
from com.src.gameserver.model.quest import State
from com.src.gameserver.model.quest import QuestState
from com.src.gameserver.model.quest.jython import QuestJython as JQuest
qn = "q662_AGameofCards"

DROP_CHANCE=15
NADO=50 

#NPCs
KLUMP = 30845

#Quest items
RED_GEM = 8765

#Rewards
ZIG_GEM = 8868
EWS = 959
EWA = 729
EWB = 947
EWD = 955
EWC = 951
EAD = 956

#Mobs
MOBS = [20677,22112,21109,21114,21116,21004,21006,21010,20674,21008,21002,20955,20966,20959,20965,20672,20958,20673,21510,21508,21525,21513,21526,21530,21516]

class Quest (JQuest) :

 def __init__(self,id,name,descr): JQuest.__init__(self,id,name,descr)
 
 def onEvent (self,event,st) :
    htmltext = event
    if event == "ok" :
       st.setState(STARTED)
       st.set("cond","1")
       st.playSound("ItemSound.quest_accept")
       htmltext = "spisokmobov.htm"
    elif event == "igra.htm" :
       if st.getQuestItemsCount(RED_GEM) >= NADO :
          st.takeItems(RED_GEM,NADO)
          st.set("viborov","0")
          st.set("karta1","201")
          st.set("karta2","202")
          st.set("karta3","203")
          st.set("karta4","204")
          st.set("karta5","205")
          st.set("countkarta1","1")
          st.set("countkarta2","1")
          st.set("countkarta3","1")
          st.set("countkarta4","1")
          st.set("countkarta5","1")
          st.set("co1","0")
          st.set("co2","0")
          st.set("co3","0")
          st.set("co4","0")
          st.set("co5","0")
       else :
         htmltext = "nehvataet.htm"       
    else :
       for i in range(1,6) :
          if event == str(i) :
            vibor = st.getInt("viborov")
            karta01 = st.getInt("karta1")
            karta02 = st.getInt("karta2")
            karta03 = st.getInt("karta3")
            karta04 = st.getInt("karta4")
            karta05 = st.getInt("karta5")
            karta = str(i)
            bukva = str(st.getRandom(140))
            st.set("karta"+karta,"".join(bukva))
            if vibor < 6 :                      
                ewevibor = str(vibor+1)
                st.set("viborov","".join(ewevibor))
                karta01 = st.getInt("karta1")
                karta02 = st.getInt("karta2")
                karta03 = st.getInt("karta3")
                karta04 = st.getInt("karta4")
                karta05 = st.getInt("karta5")      
                karta1 = str(st.getInt("karta1"))
                karta2 = str(st.getInt("karta2"))
                karta3 = str(st.getInt("karta3"))
                karta4 = str(st.getInt("karta4"))
                karta5 = str(st.getInt("karta5"))                   
                htmltext = "<html><body>Warehouse Freightman Klump:<br>Check all 5 of your cards and then show them to me.<br><table width=200 height=25 border=1><tr>"
                if karta01 == 201 :
                    htmltext += "<td align=center width=40><font color=FFFF66>?</font></td>"
                else :
                    if karta01 >= 0 and karta01 <=10:
                        karta1 = "A"
                        num1 = "1"
                    elif karta01 >= 10 and karta01 <=20:     
                        karta1 = "C"
                        num1 = "2"
                    elif karta01 >= 20 and karta01 <=30:     
                        karta1 = "D"
                        num1 = "3"
                    elif karta01 >= 30 and karta01 <=40:     
                        karta1 = "F"
                        num1 = "4"
                    elif karta01 >= 40 and karta01 <=50:     
                        karta1 = "H"
                        num1 = "5"
                    elif karta01 >= 50 and karta01 <=60:
                        karta1 = "K"
                        num1 = "6"
                    elif karta01 >= 60 and karta01 <=70:
                        karta1 = "M"
                        num1 = "7"
                    elif karta01 >= 70 and karta01 <=80:
                        karta1 = "O"
                        num1 = "8" 
                    elif karta01 >= 80 and karta01 <=90:
                        karta1 = "P"
                        num1 = "9"
                    elif karta01 >= 90 and karta01 <=100:
                        karta1 = "R"
                        num1 = "10"
                    elif karta01 >= 100 and karta01 <=110:
                        karta1 = "T"
                        num1 = "11"
                    elif karta01 >= 110 and karta01 <=120:
                        karta1 = "V"
                        num1 = "12"
                    elif karta01 >= 120 and karta01 <=130:
                        karta1 = "X"
                        num1 = "13"
                    elif karta01 >= 130 and karta01 <=140:
                        karta1 = "Z"
                        num1 = "14"
                    st.set("bukva1",num1)   
                    htmltext += "<td align=center width=40><font color=FF0000>"+karta1+"</font></td>"
                if karta02 == 202 :
                    htmltext += "<td align=center width=40><font color=FFFF66>?</font></td>"
                else:
                    if karta02 >= 0 and karta02 <=10:
                        karta2 = "A"
                        num2 = "1"
                    elif karta02 >= 10 and karta02 <=20:     
                        karta2 = "C"
                        num2 = "2"
                    elif karta02 >= 20 and karta02 <=30:     
                        karta2 = "D"
                        num2 = "3"
                    elif karta02 >= 30 and karta02 <=40:     
                        karta2 = "F"
                        num2 = "4"
                    elif karta02 >= 40 and karta02 <=50:     
                        karta2 = "H"
                        num2 = "5"
                    elif karta02 >= 50 and karta02 <=60:
                        karta2 = "K"
                        num2 = "6"
                    elif karta02 >= 60 and karta02 <=70:
                        karta2 = "M"
                        num2 = "7"
                    elif karta02 >= 70 and karta02 <=80:
                        karta2 = "O"
                        num2 = "8"
                    elif karta02 >= 80 and karta02 <=90:
                        karta2 = "P"
                        num2 = "9"
                    elif karta02 >= 90 and karta02 <=100:
                        karta2 = "R"
                        num2 = "10"
                    elif karta02 >= 100 and karta02 <=110:
                        karta2 = "T"
                        num2 = "11"
                    elif karta02 >= 110 and karta02 <=120:
                        karta2 = "V"
                        num2 = "12"
                    elif karta02 >= 120 and karta02 <=130:
                        karta2 = "X"
                        num2 = "13"
                    elif karta02 >= 130 and karta02 <=140:
                        karta2 = "Z"
                        num2 = "14"
                    st.set("bukva2",num2)
                    htmltext += "<td align=center width=40><font color=FF0000>"+karta2+"</font></td>"
                if karta03 == 203 :
                    htmltext += "<td align=center width=40><font color=FFFF66>?</font></td>"
                else:
                    if karta03 >= 0 and karta03 <=10:
                        karta3 = "A"
                        num3 = "1"
                    elif karta03 >= 10 and karta03 <=20:     
                        karta3 = "C"
                        num3 = "2"
                    elif karta03 >= 20 and karta03 <=30:     
                        karta3 = "D"
                        num3 = "3"
                    elif karta03 >= 30 and karta03 <=40:     
                        karta3 = "F"
                        num3 = "4"
                    elif karta03 >= 40 and karta03 <=50:     
                        karta3 = "H"
                        num3 = "5"
                    elif karta03 >= 50 and karta03 <=60:
                        karta3 = "K"
                        num3 = "6"
                    elif karta03 >= 60 and karta03 <=70:
                        karta3 = "M"
                        num3 = "7"
                    elif karta03 >= 70 and karta03 <=80:
                        karta3 = "O"
                        num3 = "8"
                    elif karta03 >= 80 and karta03 <=90:
                        karta3 = "P"
                        num3 = "9"
                    elif karta03 >= 90 and karta03 <=100:
                        karta3 = "R"
                        num3 = "10"
                    elif karta03 >= 100 and karta03 <=110:
                        karta3 = "T"
                        num3 = "11"
                    elif karta03 >= 110 and karta03 <=120:
                        karta3 = "V"
                        num3 = "12"
                    elif karta03 >= 120 and karta03 <=130:
                        karta3 = "X"
                        num3 = "13"
                    elif karta03 >= 130 and karta03 <=140:
                        karta3 = "Z"
                        num3 = "14"
                    st.set("bukva3",num3)   
                    htmltext += "<td align=center width=40><font color=FF0000>"+karta3+"</font></td>"
                if karta04 == 204 :
                    htmltext += "<td align=center width=40><font color=FFFF66>?</font></td>"
                else:
                    if karta04 >= 0 and karta04 <=10:
                        karta4 = "A"
                        num4 = "1"
                    elif karta04 >= 10 and karta04 <=20:     
                        karta4 = "C"
                        num4 = "2"
                    elif karta04 >= 20 and karta04 <=30:     
                        karta4 = "D"
                        num4 = "3"
                    elif karta04 >= 30 and karta04 <=40:     
                        karta4 = "F"
                        num4 = "4"
                    elif karta04 >= 40 and karta04 <=50:     
                        karta4 = "H"
                        num4 = "5"
                    elif karta04 >= 50 and karta04 <=60:
                        karta4 = "K"
                        num4 = "6"
                    elif karta04 >= 60 and karta04 <=70:
                        karta4 = "M"
                        num4 = "7"
                    elif karta04 >= 70 and karta04 <=80:
                        karta4 = "O"
                        num4 = "8"
                    elif karta04 >= 80 and karta04 <=90:
                        karta4 = "P"
                        num4 = "9"
                    elif karta04 >= 90 and karta04 <=100:
                        karta4 = "R"
                        num4 = "10"
                    elif karta04 >= 100 and karta04 <=110:
                        karta4 = "T"
                        num4 = "11"
                    elif karta04 >= 110 and karta04 <=120:
                        karta4 = "V"
                        num4 = "12"
                    elif karta04 >= 120 and karta04 <=130:
                        karta4 = "X"
                        num4 = "13"
                    elif karta04 >= 130 and karta04 <=140:
                        karta4 = "Z"
                        num4 = "14"
                    st.set("bukva4",num4)    
                    htmltext += "<td align=center width=40><font color=FF0000>"+karta4+"</font></td>"
                if karta05 == 205 :
                    htmltext += "<td align=center width=40><font color=FFFF66>?</font></td>"
                else:
                    if karta05 >= 0 and karta05 <=10:
                        karta5 = "A"
                        num5 = "1"
                    elif karta05 >= 10 and karta05 <=20:     
                        karta5 = "C"
                        num5 = "2"
                    elif karta05 >= 20 and karta05 <=30:     
                        karta5 = "D"
                        num5 = "3"
                    elif karta05 >= 30 and karta05 <=40:     
                        karta5 = "F"
                        num5 = "4"
                    elif karta05 >= 40 and karta05 <=50:     
                        karta5 = "H"
                        num5 = "5"
                    elif karta05 >= 50 and karta05 <=60:
                        karta5 = "K"
                        num5 = "6"
                    elif karta05 >= 60 and karta05 <=70:
                        karta5 = "M"
                        num5 = "7"
                    elif karta05 >= 70 and karta05 <=80:
                        karta5 = "O"
                        num5 = "8"
                    elif karta05 >= 80 and karta05 <=90:
                        karta5 = "P"
                        num5 = "9"
                    elif karta05 >= 90 and karta05 <=100:
                        karta5 = "R"
                        num5 = "10"
                    elif karta05 >= 100 and karta05 <=110:
                        karta5 = "T"
                        num5 = "11"
                    elif karta05 >= 110 and karta05 <=120:
                        karta5 = "V"
                        num5 = "12"
                    elif karta05 >= 120 and karta05 <=130:
                        karta5 = "X"
                        num5 = "13"
                    elif karta05 >= 130 and karta05 <=140:
                        karta5 = "Z"
                        num5 = "14"
                    st.set("bukva5",num5)    
                    htmltext += "<td align=center width=40><font color=FF0000>"+karta5+"</font></td>"                  
                htmltext += "</tr></table>"
                htmltext += "<br>"
                htmltext += "<a action=\"bypass -h Quest q662_AGameofCards 1\">Put the first card face up.</a><br>"
                htmltext += "<a action=\"bypass -h Quest q662_AGameofCards 2\">Put the second card face up.</a><br>"
                htmltext += "<a action=\"bypass -h Quest q662_AGameofCards 3\">Put the third card face up.</a><br>"
                htmltext += "<a action=\"bypass -h Quest q662_AGameofCards 4\">Put the fourth card face up.</a><br>"
                htmltext += "<a action=\"bypass -h Quest q662_AGameofCards 5\">Put the fifth card face up.</a><br>"                
                htmltext += "</body></html>"
                vibor = st.getInt("viborov")
            if vibor == 5 :
                if vibor > 4 :
                    karta01 = st.getInt("karta1")
                    karta02 = st.getInt("karta2")
                    karta03 = st.getInt("karta3")
                    karta04 = st.getInt("karta4")
                    karta05 = st.getInt("karta5")
                    karta1 = str(st.getInt("karta1"))
                    karta2 = str(st.getInt("karta2"))
                    karta3 = str(st.getInt("karta3"))
                    karta4 = str(st.getInt("karta4"))
                    karta5 = str(st.getInt("karta5"))
                    htmltext1 = "<html><body>Warehouse Freightman Klump:<br>Did you have a look at your cards? Show them to me.<br><table width=200 height=25 border=1><tr>"
                    if karta01 >= 0 and karta01 <=10:
                        kartaa1 = "A"
                    elif karta01 >= 10 and karta01 <=20:     
                        kartaa1 = "C"
                    elif karta01 >= 20 and karta01 <=30:     
                        kartaa1 = "D"
                    elif karta01 >= 30 and karta01 <=40:     
                        kartaa1 = "F"
                    elif karta01 >= 40 and karta01 <=50:     
                        kartaa1 = "H"
                    elif karta01 >= 50 and karta01 <=60:
                        kartaa1 = "K"
                    elif karta01 >= 60 and karta01 <=70:
                        kartaa1 = "M"
                    elif karta01 >= 70 and karta01 <=80:
                        kartaa1 = "O"
                    elif karta01 >= 80 and karta01 <=90:
                        kartaa1 = "P"
                    elif karta01 >= 90 and karta01 <=100:
                        kartaa1 = "R"
                    elif karta01 >= 100 and karta01 <=110:
                        kartaa1 = "T"
                    elif karta01 >= 110 and karta01 <=120:
                        kartaa1 = "V"
                    elif karta01 >= 120 and karta01 <=130:
                        kartaa1 = "X"
                    elif karta01 >= 130 and karta01 <=140:
                        kartaa1 = "Z"
                    htmltext2 = "<td align=center width=40><font color=FF0000>"+kartaa1+"</font></td>"
                    if karta02 >= 0 and karta02 <=10:
                        kartaa2 = "A"
                    elif karta02 >= 10 and karta02 <=20:     
                        kartaa2 = "C"
                    elif karta02 >= 20 and karta02 <=30:     
                        kartaa2 = "D"
                    elif karta02 >= 30 and karta02 <=40:     
                        kartaa2 = "F"
                    elif karta02 >= 40 and karta02 <=50:     
                        kartaa2 = "H"
                    elif karta02 >= 50 and karta02 <=60:
                        kartaa2 = "K"
                    elif karta02 >= 60 and karta02 <=70:
                        kartaa2 = "M"
                    elif karta02 >= 70 and karta02 <=80:
                        kartaa2 = "O"
                    elif karta02 >= 80 and karta02 <=90:
                        kartaa2 = "P"
                    elif karta02 >= 90 and karta02 <=100:
                        kartaa2 = "R"
                    elif karta02 >= 100 and karta02 <=110:
                        kartaa2 = "T"
                    elif karta02 >= 110 and karta02 <=120:
                        kartaa2 = "V"
                    elif karta02 >= 120 and karta02 <=130:
                        kartaa2 = "X"
                    elif karta02 >= 130 and karta01 <=140:
                        kartaa2 = "Z"
                    htmltext3 = "<td align=center width=40><font color=FF0000>"+kartaa2+"</font></td>"
                    if karta03 >= 0 and karta03 <=10:
                        kartaa3 = "A"
                    elif karta03 >= 10 and karta03 <=20:     
                        kartaa3 = "C"
                    elif karta03 >= 20 and karta03 <=30:     
                        kartaa3 = "D"
                    elif karta03 >= 30 and karta03 <=40:     
                        kartaa3 = "F"
                    elif karta03 >= 40 and karta03 <=50:     
                        kartaa3 = "H"
                    elif karta03 >= 50 and karta03 <=60:
                        kartaa3 = "K"
                    elif karta03 >= 60 and karta03 <=70:
                        kartaa3 = "M"
                    elif karta03 >= 70 and karta03 <=80:
                        kartaa3 = "O"
                    elif karta03 >= 80 and karta03 <=90:
                        kartaa3 = "P"
                    elif karta03 >= 90 and karta03 <=100:
                        kartaa3 = "R"
                    elif karta03 >= 100 and karta03 <=110:
                        kartaa3 = "T"
                    elif karta03 >= 110 and karta03 <=120:
                        kartaa3 = "V"
                    elif karta03 >= 120 and karta03 <=130:
                        kartaa3 = "X"
                    elif karta03 >= 130 and karta03 <=140:
                        kartaa3 = "Z"
                    htmltext4 = "<td align=center width=40><font color=FF0000>"+kartaa3+"</font></td>"
                    if karta04 >= 0 and karta04 <=10:
                        kartaa4 = "A"
                    elif karta04 >= 10 and karta04 <=20:     
                        kartaa4 = "C"
                    elif karta04 >= 20 and karta04 <=30:     
                        kartaa4 = "D"
                    elif karta04 >= 30 and karta04 <=40:     
                        kartaa4 = "F"
                    elif karta04 >= 40 and karta04 <=50:     
                        kartaa4 = "H"
                    elif karta04 >= 50 and karta04 <=60:
                        kartaa4 = "K"
                    elif karta04 >= 60 and karta04 <=70:
                        kartaa4 = "M"
                    elif karta04 >= 70 and karta04 <=80:
                        kartaa4 = "O"
                    elif karta04 >= 80 and karta04 <=90:
                        kartaa4 = "P"
                    elif karta04 >= 90 and karta04 <=100:
                        kartaa4 = "R"
                    elif karta04 >= 100 and karta04 <=110:
                        kartaa4 = "T"
                    elif karta04 >= 110 and karta04 <=120:
                        kartaa4 = "V"
                    elif karta04 >= 120 and karta04 <=130:
                        kartaa4 = "X"
                    elif karta04 >= 130 and karta04 <=140:
                        kartaa4 = "Z"
                    htmltext5 = "<td align=center width=40><font color=FF0000>"+kartaa4+"</font></td>"
                    if karta05 >= 0 and karta05 <=10:
                        kartaa5 = "A"
                    elif karta05 >= 10 and karta05 <=20:     
                        kartaa5 = "C"
                    elif karta05 >= 20 and karta05 <=30:     
                        kartaa5 = "D"
                    elif karta05 >= 30 and karta05 <=40:     
                        kartaa5 = "F"
                    elif karta05 >= 40 and karta05 <=50:     
                        kartaa5 = "H"
                    elif karta05 >= 50 and karta05 <=60:
                        kartaa5 = "K"
                    elif karta05 >= 60 and karta05 <=70:
                        kartaa5 = "M"
                    elif karta05 >= 70 and karta05 <=80:
                        kartaa5 = "O"
                    elif karta05 >= 80 and karta05 <=90:
                        kartaa5 = "P"
                    elif karta05 >= 90 and karta05 <=100:
                        kartaa5 = "R"
                    elif karta05 >= 100 and karta05 <=110:
                        kartaa5 = "T"
                    elif karta05 >= 110 and karta05 <=120:
                        kartaa5 = "V"
                    elif karta05 >= 120 and karta05 <=130:
                        kartaa5 = "X"
                    elif karta05 >= 130 and karta05 <=140:
                        kartaa5 = "Z"
                    htmltext6 = "<td align=center width=40><font color=FF0000>"+kartaa5+"</font></td>"                  
                    htmltext7 = "</tr></table>"
                    htmltext8 = "<br>"
                    bukva1 = str(st.getInt("bukva1"))
                    bukva2 = str(st.getInt("bukva2"))
                    bukva3 = str(st.getInt("bukva3"))
                    bukva4 = str(st.getInt("bukva4"))
                    bukva5 = str(st.getInt("bukva5"))
                    if bukva1 == bukva2 :
                        countkartaone = st.getInt("countkarta1")
                        ewe = str(countkartaone+1)
                        st.set("countkarta1","".join(ewe))
                        coone = st.getInt("co1")
                        col = str(coone+1)
                        st.set("co1","".join(col))
                    if bukva1 == bukva3 :
                        countkartaone = st.getInt("countkarta1")
                        ewe = str(countkartaone+1)
                        st.set("countkarta1","".join(ewe))
                        coone = st.getInt("co1")
                        col = str(coone+1)
                        st.set("co1","".join(col))
                    if bukva1 == bukva4 :
                        countkartaone = st.getInt("countkarta1")
                        ewe = str(countkartaone+1)
                        st.set("countkarta1","".join(ewe))                        
                        coone = st.getInt("co1")
                        col = str(coone+1)
                        st.set("co1","".join(col))
                    if bukva1 == bukva5 :
                        countkartaone = st.getInt("countkarta1")
                        ewe = str(countkartaone+1)
                        st.set("countkarta1","".join(ewe))
                        coone = st.getInt("co1")
                        col = str(coone+1)
                        st.set("co1","".join(col))
                        #
                    if bukva2 != bukva1 and bukva2 == bukva3 :
                        countkartaone = st.getInt("countkarta2")
                        ewe = str(countkartaone+1)
                        st.set("countkarta2","".join(ewe))
                        cotwo = st.getInt("co2")
                        col = str(cotwo+1)
                        st.set("co2","".join(col))
                    if bukva2 != bukva1 and bukva2 == bukva4 :
                        countkartaone = st.getInt("countkarta2")
                        ewe = str(countkartaone+1)
                        st.set("countkarta2","".join(ewe))
                        cotwo = st.getInt("co2")
                        col = str(cotwo+1)
                        st.set("co2","".join(col))
                    if bukva2 != bukva1 and bukva2 == bukva5 :
                        countkartaone = st.getInt("countkarta2")
                        ewe = str(countkartaone+1)
                        st.set("countkarta2","".join(ewe))
                        cotwo = st.getInt("co2")
                        col = str(cotwo+1)
                        st.set("co2","".join(col))
                        #
                    if bukva3 != bukva1 and bukva3 !=bukva2 and bukva3 == bukva4 :
                        countkartaone = st.getInt("countkarta3")
                        ewe = str(countkartaone+1)
                        st.set("countkarta3","".join(ewe))
                        cothree = st.getInt("co3")
                        col = str(cothree+1)
                        st.set("co3","".join(col))
                    if bukva3 != bukva1 and bukva3 !=bukva2 and bukva3 == bukva5 :
                        countkartaone = st.getInt("countkarta3")
                        ewe = str(countkartaone+1)
                        st.set("countkarta3","".join(ewe))
                        cothree = st.getInt("co3")
                        col = str(cothree+1)
                        st.set("co3","".join(col))
                        #
                    if bukva4 != bukva1 and bukva4 != bukva3 and bukva4 != bukva2 and bukva4 == bukva5 :
                        countkartaone = st.getInt("countkarta4")
                        ewe = str(countkartaone+1)
                        st.set("countkarta4","".join(ewe))
                        cofour = st.getInt("co4")
                        col = str(cofour+1)
                        st.set("co4","".join(col))
                    con1 = st.getInt("countkarta1")
                    con2 = st.getInt("countkarta2")
                    con3 = st.getInt("countkarta3")
                    con4 = st.getInt("countkarta4")
                    con5 = st.getInt("countkarta5")
                    co1 = st.getInt("co1")
                    co2 = st.getInt("co2")
                    co3 = st.getInt("co3")
                    co4 = st.getInt("co4")
                    co5 = st.getInt("co5") 
                    if (co1 + co2 + co3 + co4 + co5) == 2 :
                        if con1 == 3 or con2 == 3 or con3 == 3 or con4 == 3 or con5 == 3 :
                            htmltext =  htmltext1
                            htmltext += htmltext2
                            htmltext += htmltext3
                            htmltext += htmltext4
                            htmltext += htmltext5
                            htmltext += htmltext6
                            htmltext += htmltext7
                            htmltext += htmltext8
                            htmltext += "Hmmmm....? This is... Three of kind? You got lucky this time, but i wonder if tt'll last. Here is your prize."
                            htmltext += "<br><a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                            htmltext += "</body></html>"
                            st.giveItems(EWC,2)
                        else :
                            htmltext =  htmltext1
                            htmltext += htmltext2
                            htmltext += htmltext3
                            htmltext += htmltext4
                            htmltext += htmltext5
                            htmltext += htmltext6
                            htmltext += htmltext7
                            htmltext += htmltext8
                            htmltext += "Hmmmm....? This is...  Two pairs? You got lucky this time, but i wonder if tt'll last. Here is your prize."
                            htmltext += "<br><a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                            htmltext += "</body></html>"                        
                            st.giveItems(EWC,1)
                    else :
                        if (co1 + co2 + co3 + co4 + co5) == 3 :
                            htmltext =  htmltext1
                            htmltext += htmltext2
                            htmltext += htmltext3
                            htmltext += htmltext4
                            htmltext += htmltext5
                            htmltext += htmltext6
                            htmltext += htmltext7
                            htmltext += htmltext8
                            st.giveItems(EWA,1)
                            st.giveItems(EWB,2)
                            st.giveItems(EWD,1)
                            htmltext += "Hmmmm....? This is...  Full House! You got lucky this time, but i wonder if tt'll last. Here is your prize."
                            htmltext += "<br><a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                            htmltext += "</body></html>"
                        if (con1 + con2 + con3 + con4 + con5) == 6 :
                            htmltext =  htmltext1
                            htmltext += htmltext2
                            htmltext += htmltext3
                            htmltext += htmltext4
                            htmltext += htmltext5
                            htmltext += htmltext6
                            htmltext += htmltext7
                            htmltext += htmltext8
                            htmltext += "Hmmmm....? This is... One pair? You got lucky this time, but i wonder if tt'll last. Here is your prize."
                            htmltext += "<br><a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                            htmltext += "</body></html>"
                            st.giveItems(EAD,2)
                        else :
                            if karta1 == karta2 == karta3 == karta4 == karta5 :
                                htmltext =  htmltext1
                                htmltext += htmltext2
                                htmltext += htmltext3
                                htmltext += htmltext4
                                htmltext += htmltext5
                                htmltext += htmltext6
                                htmltext += htmltext7
                                htmltext += htmltext8
                                htmltext += "Hmmmm....? This is... Five of a kind! What luck! The goddess of victory must be with you! Here is your prize! Well earned, well played!"
                                htmltext += "<a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                                htmltext += "</body></html>"           
                                st.giveItems(ZIG_GEM,43)
                                st.giveItems(EWS,3)
                                st.giveItems(EWA,1)
                            else :
                                if karta1 != karta2 != karta3 != karta4 != karta5 :
                                    htmltext =  htmltext1
                                    htmltext += htmltext2
                                    htmltext += htmltext3
                                    htmltext += htmltext4
                                    htmltext += htmltext5
                                    htmltext += htmltext6
                                    htmltext += htmltext7
                                    htmltext += htmltext8
                                    htmltext += "Hmmmm...? This is... No pair? Tough luck, my friend! Want to try again? Perhaps your luck will take a turn for the better...<br>"
                                    htmltext += "<a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                                    htmltext += "</body></html>"
                                else :
                                    for i in range(1,6) :
                                        k = str(i)  
                                        if st.getInt("countkarta"+k) == 4 :                                    
                                            htmltext =  htmltext1
                                            htmltext += htmltext2
                                            htmltext += htmltext3
                                            htmltext += htmltext4
                                            htmltext += htmltext5
                                            htmltext += htmltext6
                                            htmltext += htmltext7
                                            htmltext += htmltext8
                                            htmltext += "Hmmmm....? This is...  Four of kind? You got lucky this time, but i wonder if tt'll last. Here is your prize."
                                            htmltext += "<br><a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                                            htmltext += "</body></html>" 
                                            st.giveItems(EWS,2)
                                            st.giveItems(EWC,2)
                                        else: 
                                            if  st.getInt("countkarta"+k)== 3 :                                    
                                                htmltext =  htmltext1
                                                htmltext += htmltext2
                                                htmltext += htmltext3
                                                htmltext += htmltext4
                                                htmltext += htmltext5
                                                htmltext += htmltext6
                                                htmltext += htmltext7
                                                htmltext += htmltext8
                                                htmltext += "Hmmmm....? This is... Three of kind? You got lucky this time, but i wonder if tt'll last. Here is your prize."
                                                htmltext += "<br><a action=\"bypass -h Quest q662_AGameofCards tasuetkarti.htm\">Play again.</a><br>"
                                                htmltext += "</body></html>"
                                                st.giveItems(EWC,2) 
    return htmltext

 def onTalk (self,npc,player):
    st = player.getQuestState(qn)
    htmltext = "<html><body>You are either not on a quest that involves this NPC, or you don't meet this NPC's minimum quest requirements.</body></html>" 
    if not st: return htmltext
    npcId = npc.getNpcId()
    id = st.getState()
    cond = st.getInt("cond")
    if npcId == KLUMP :
       if id == CREATED :
          if player.getLevel() >= 66 :
             htmltext = "privetstvie.htm"
          else :
             htmltext = "lvl.htm"
             st.exitQuest(1)
       elif cond == 1:
          if st.getQuestItemsCount(RED_GEM) >= 50 :
             htmltext = "privetstvieigra.htm"
          else:
             htmltext = "nehvataet.htm"
    return htmltext

 def onKill (self, npc, player,isPet):
    partyMember = self.getRandomPartyMember(player,"1")
    if not partyMember: return
    st = partyMember.getQuestState(qn)
    if st :
        if st.getState() == STARTED :
            npcId = npc.getNpcId()
            cond = st.getInt("cond")
            count = st.getQuestItemsCount(RED_GEM)
            chance = DROP_CHANCE*Config.RATE_DROP_QUEST
            numItems, chance = divmod(chance,100)
            if st.getRandom(100) < chance : 
                numItems += 1
            if numItems :
                if int(count+numItems)/NADO > int(count)/NADO :    
                    st.playSound("ItemSound.quest_middle")
                else :
                    st.playSound("ItemSound.quest_itemget")
                st.giveItems(RED_GEM,int(numItems))
        return

# Quest class and state definition
QUEST       = Quest(662, qn, "A Game of Cards")
CREATED     = State('Start',     QUEST)
STARTED     = State('Started',   QUEST)

QUEST.setInitialState(CREATED)
# Quest NPC starter initialization
QUEST.addStartNpc(KLUMP)
# Quest initialization
QUEST.addTalkId(KLUMP)

for i in MOBS :
  QUEST.addKillId(i)
