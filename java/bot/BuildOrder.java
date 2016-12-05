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

public class BuildOrder {
    private Player myPlayer;
    private Player enemyPlayer;

    private List<Unit> myUnits;
    private List<Unit> enemyUnits;

    private List<UnitType> myBuildOrder;
    private List<Integer> myBuildOrderQuantities;

    private int myBuildOrderInterStep;
    private int myBuildOrderIntraStep;

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

    public BuildOrder(Player selfPlayer, Player oppoPlayer){
        myPlayer = selfPlayer;
        enemyPlayer = oppoPlayer;

        myRace = myPlayer.getRace();
        enemyRace = enemyPlayer.getRace();

        myBuildOrderIntraStep = 0;
        myBuildOrderInterStep = 0;

        myBuildOrder = new ArrayList<UnitType>();
        myBuildOrderQuantities = new ArrayList<Integer>();

        createBuildOrder();
    }
    public void createBuildOrder(){
        //read from text file
        String filepath = "BuildOrders/" + myRace.getName() + "/" +enemyRace.getName() + "/buildorder.txt";
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
                    myBuildOrder.add(tempUnitType);
                    myBuildOrderQuantities.add(tempInt);
                }
                else{
                    System.out.println("Unit Type: " + tempLine[0] + " is not a recognized unit.");
                }
            }
        }
        catch (NumberFormatException e){
                System.out.println("Critical error (" + tempLine[1] + " is not a number): build order could not be loaded.");
            }
        catch (FileNotFoundException e) {
            System.out.println("Critical error (" + filepath + " does not exist): build order could not be loaded.");
        }
        catch (IOException e){
            System.out.println("Critical error (IO error): build order could not be loaded.");
        }
    }
    public UnitType getNextBuild(){
        if (myBuildOrderIntraStep == myBuildOrderQuantities.get(myBuildOrderInterStep)){
            myBuildOrderIntraStep = 0;
            myBuildOrderInterStep += 1;
        }
        else{
            myBuildOrderIntraStep += 1;
        }
        if (myBuildOrderInterStep >= myBuildOrder.size()){
            System.out.println("Your build order has been exhausted.");
            return null;
        }
        return myBuildOrder.get(myBuildOrderInterStep);
    }

    private UnitType getUnitType(String unitName){
        return getUnitType(unitName, myRace);
    }

    public static UnitType getUnitType(String unitName, RaceType myRace){
        UnitType tempType;
        if (myRace == protoss){
            tempType = UnitTypes.Protoss_Assimilator;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Arbiter;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Arbiter_Tribunal;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Archon;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Carrier;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Citadel_of_Adun;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Corsair;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Cybernetics_Core;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Dark_Archon;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Dark_Templar;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Dragoon;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Fleet_Beacon;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Forge;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Gateway;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_High_Templar;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Interceptor;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Nexus;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Observatory;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Observer;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Photon_Cannon;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Probe;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Pylon;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Reaver;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Robotics_Facility;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Robotics_Support_Bay;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Scarab;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Scout;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Shield_Battery;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Shuttle;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Stargate;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Templar_Archives;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
            tempType = UnitTypes.Protoss_Zealot;
            if(unitName.equals(tempType.getName())){
                return tempType;
            }
        }
        if (myRace == zerg) {
            tempType = UnitTypes.Zerg_Zergling;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Broodling;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Cocoon;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Creep_Colony;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Defiler;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Defiler_Mound;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Devourer;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Drone;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Egg;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Evolution_Chamber;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Extractor;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Greater_Spire;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Guardian;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Hatchery;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Hive;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Hydralisk;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Hydralisk_Den;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Infested_Command_Center;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Infested_Terran;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Lair;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Larva;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Lurker;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Lurker_Egg;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Mutalisk;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Nydus_Canal;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Overlord;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Queen;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Queens_Nest;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Scourge;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Spawning_Pool;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Spire;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Spore_Colony;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Sunken_Colony;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Ultralisk;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Zerg_Ultralisk_Cavern;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
        }
        if (myRace == terran){
            tempType = UnitTypes.Terran_Academy;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Armory;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Barracks;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Battlecruiser;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Bunker;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Civilian;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Command_Center;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Comsat_Station;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Control_Tower;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Covert_Ops;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Dropship;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Engineering_Bay;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Factory;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Firebat;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Ghost;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Goliath;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Machine_Shop;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Marine;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Medic;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Missile_Turret;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Nuclear_Missile;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Nuclear_Silo;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Physics_Lab;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Refinery;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Science_Facility;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Science_Vessel;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_SCV;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Siege_Tank_Siege_Mode;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Siege_Tank_Tank_Mode;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Starport;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Supply_Depot;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Valkyrie;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Vulture;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Vulture_Spider_Mine;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
            tempType = UnitTypes.Terran_Wraith;
            if (unitName.equals(tempType.getName())) {
                return tempType;
            }
        }
        return null;
    }
}
