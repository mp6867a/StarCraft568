package bot;

import java.io.*;
import java.text.ParseException;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javafx.geometry.Pos;
import jnibwapi.BWAPIEventListener;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.types.RaceType;
import jnibwapi.types.TechType;
import jnibwapi.types.TechType.TechTypes;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType;
import jnibwapi.types.UpgradeType.UpgradeTypes;
import jnibwapi.util.BWColor;
import jnibwapi.Player;
import jnibwapi.Unit;
import bot.BuildOrder;


public class UnitOrder {

    private Player myPlayer;
    private Player enemyPlayer;

    private List<Unit> myUnits;
    private List<Unit> enemyUnits;

    private List<UnitType> myUnitOrder;
    private List<Integer> myUnitOrderQuantities;

    private int myUnitOrderInterStep;
    private int myUnitOrderIntraStep;

    private int myStrength;
    private int enemyStrength;

    private int myHitponts;
    private int enemyHitpoints;

    private int myInitialHitPoints;
    private int enemyHitPoints;

    private RaceType myRace;
    private RaceType enemyRace;

    public static RaceType protoss = RaceType.RaceTypes.Protoss;
    public static RaceType zerg = RaceType.RaceTypes.Zerg;
    public static RaceType terran = RaceType.RaceTypes.Terran;

    public UnitOrder(Player selfPlayer, Player oppoPlayer){
        myPlayer = selfPlayer;
        enemyPlayer = oppoPlayer;

        myRace = myPlayer.getRace();
        enemyRace = enemyPlayer.getRace();

        myUnitOrderIntraStep = 0;
        myUnitOrderInterStep = 0;

        myUnitOrder = new ArrayList<UnitType>();
        myUnitOrderQuantities = new ArrayList<Integer>();

        createUnitOrder();
    }
    public void createUnitOrder(){
        //read from text file
        String filepath = "UnitOrders/" + myRace.getName() + "/" +enemyRace.getName() + "/unitorder.txt";
        String[] tempLine = new String[2];
        try {
            FileReader fileReader = new FileReader(filepath);
            BufferedReader reader = new BufferedReader(fileReader);
            String line;
            UnitType tempUnitType;
            Integer tempInt;
            while ((line = reader.readLine()) != null) {
                tempLine = line.split(",");
                tempUnitType = getUnitType(tempLine[0]);
                tempInt = Integer.parseInt(tempLine[1]);
                if (tempUnitType != null) {
                    myUnitOrder.add(tempUnitType);
                    myUnitOrderQuantities.add(tempInt);
                }
                else{
                    System.out.println("Unit Type: " + tempLine[0] + " is not a recognized unit.");
                }
            }
        }
        catch (NumberFormatException e){
            System.out.println("Critical error (" + tempLine[1] + " is not a number): unit order could not be loaded.");
        }
        catch (FileNotFoundException e) {
            System.out.println("Critical error (" + filepath + " does not exist): unit order could not be loaded.");
        }
        catch (IOException e){
            System.out.println("Critical error (IO error): unit order could not be loaded.");
        }
    }
    public UnitType getNextUnit(){
        if (myUnitOrderIntraStep == myUnitOrderQuantities.get(myUnitOrderInterStep)){
            myUnitOrderIntraStep = 0;
            myUnitOrderInterStep += 1;
        }
        else{
            myUnitOrderIntraStep += 1;
        }
        if (myUnitOrderInterStep >= myUnitOrder.size()){
            System.out.println("Your unit order has been exhausted.");
            return null;
        }
        return myUnitOrder.get(myUnitOrderInterStep);
    }
    public UnitType getUnitType(String unitName){
        return BuildOrder.getUnitType(unitName, myRace);
    }
}
