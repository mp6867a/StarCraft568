package bot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.BaseLocation;

public class CentralCommand {

    private JNIBWAPI bwapi;

    public List<Squad> squads;
    public List<Unit> units;
    public List<Unit> scarbList;
    public int ScarbCount = 0;

    public HashSet<Position> enemyBuildingPositionsList;
    public HashSet<Unit> enemyList;
    public HashSet<Unit> enemyBuildingList;
    public HashSet<Unit> weakEnemyList;
    public HashSet<Unit> allEnemiesList;

    private Position enemyBasePosition;
    private Unit enemyBaseUnit;


    public CentralCommand(JNIBWAPI _bwapi){
        bwapi = _bwapi;
        squads = new ArrayList<Squad>();
        units = new ArrayList<Unit>();
        enemyList = new HashSet<Unit>();
        scarbList = new ArrayList<Unit>() ;
        enemyBuildingPositionsList = new HashSet<Position>();
    }

    /**
     * Issue refresh command to all squads
     */
    public void refresh(){

        // need to refresh enemy base list here evry frame
        for (Squad squad : squads){
            squad.refresh();
        }
        rallyIfNeeded();
    }

    public boolean attack(Unit enemy, int n_squads){
        return attack(enemy, n_squads, false);
    }

    /**
     * Send a given number of squads to attack a particular enemy
     * @param enemy The enemy unit to be attacked.
     * @param n_squads The number of squads to be used in the attack.
     * @param override Currently unused
     * @return If all squads that have been requested to attack have been called to attack.
     */
    public boolean attack(Unit enemy, int n_squads, boolean override){
        int engaged = 0;
        //could be improved by selecting the nearest n free squads
        for (Squad squad : squads){
            if (squad.isAvailable()){
                squad.attack(enemy);
                engaged += 1;
                if (engaged == n_squads){
                    return true;
                }
            }
        }
        return engaged > 0;
    }
    /**
     * Add a Squad to the Command.
     * @param squadToAdd
     */
    public void addSquad(Squad squadToAdd){
        squads.add(squadToAdd);
    }

    /**
     * Adds a unit to the most undersupplied squad and moves that unit to the leader.
     * If all squads are at full strength, create a new unit
     * @param unit The unit to be added to the Command.
     */
    public void addUnitToSquad(Unit unit){
        Squad least = findMostUnderSupplied();
        if (least != null){
            least.addMember(unit);
            unit.move(least.squadLeader.getPosition(), false);
        }
        else{
            //Create a new squad
            Squad newSquad = new Squad("");
            newSquad.addMember(unit);
            addSquad(newSquad);
        }
        units.add(unit);
    }

    /**
     * Finds the Squad with the least number of members.
     * @return the most manpower undersupplied squad.
     */
    public Squad findMostUnderSupplied(){
        int min = Integer.MAX_VALUE;
        int temp;
        Squad leastSquad = null;
        for (Squad squad : squads){
            temp = squad.membersQuantity();
            if (temp < min && temp != squad.maxUnits){
                min = temp;
                leastSquad = squad;
            }
        }
        //We need a squad as none exist or all are full
        if (leastSquad == null){
            Squad newSquad = new Squad("");
            addSquad(newSquad);
            return newSquad;
        }
        return leastSquad;
    }

    public void loadUnits(List<Unit> newUnits){
        for (Unit newUnit : newUnits){
            if (!newUnit.getType().isBuilding() && newUnit.getType() != UnitType.UnitTypes.Protoss_Probe && !units.contains(newUnit)){
                addUnitToSquad(newUnit);
            }
        }
    }

    public void rallyIfNeeded(){
        for (Squad squad : squads){
            if (!squad.isClose() && squad.isAvailable()){
                squad.rallyToLeader();
            }
        }
    }

    /**
     * Gets the most likely position of an enemy base by finding the median position
     * of all enemy units. While this is not guarenteed to find the enemy base, if it
     * does not, it will return a position with a large number of enemies.
     * @return The median location of the enemy
     */
    public Position getEnemyLocation(){
        List<Integer> xPositions = new ArrayList<Integer>();
        List<Integer> yPositions = new ArrayList<Integer>();
        Position tempPosition;
        for (Unit enemyUnit : enemyList){
            tempPosition = enemyUnit.getPosition();
            xPositions.add(tempPosition.getPX());
            yPositions.add(tempPosition.getPY());
        }
        Collections.sort(xPositions);
        Collections.sort(yPositions);
        //return the median x and y positions
        return new Position(xPositions.get((int) (xPositions.size() / 2)),
                            yPositions.get((int) (yPositions.size() / 2)));
    }

    private void fixEnemyBasePositon(){
        Position basePosition;
        for (BaseLocation startPosition: bwapi.getMap().getStartLocations()){
            basePosition = startPosition.getPosition();
            for (Unit building : enemyBuildingList){
                if (building.getDistance(basePosition) < 500){
                    enemyBasePosition = basePosition;
                    enemyBaseUnit = building;
                    return;
                }
            }
        }
    }

    public  void attackAtStartBaseLocation(int n_squads){
        if(enemyBasePosition == null || !enemyBaseUnit.isExists()){
            fixEnemyBasePositon();
        }
        if(enemyBasePosition != null) {
            attack(enemyBaseUnit, n_squads);
        }
    }

    /** this still needs work - it should check the enemy add it to list and if it doesn't exist remove it
     if it is an enemy that can attack ( i.e. not a buliding) // other checks might be better -
     */
    public void gatherEnemies() {
        enemyList.clear();
        //loop through enemy units
        for (Unit enemyUnit : bwapi.getEnemyUnits()) {
            // if the enemy unit can attack - not sure if this is applicable here there are other checks that may be more helpful
            if (enemyUnit.getType().isAttackCapable() && !enemyUnit.getType().isBuilding()){
                enemyList.add(enemyUnit.getTarget());
            }
            else{
                if (enemyUnit.getType().isBuilding()){
                    enemyBuildingPositionsList.add(enemyUnit.getPosition());
                    enemyBuildingList.add(enemyUnit);

                }
                else{
                    weakEnemyList.add(enemyUnit);
                }
            }
            allEnemiesList.add(enemyUnit);
        }
    }
    //TODO I do not think this is valid code. Please make it more generic.
    //call this every frame so it will keep on building
    public void DeployOneScarbs() {
        int count = 0;
        for (Unit unit : units) { // if units is the list that stores our units? // or acsess the squad that has a reaver
            //is that a thing ? so far we only have one reaver in build order

            if (unit.getType() == UnitType.UnitTypes.Protoss_Reaver) {
                //or supplay cap - not sure where you would liek this or if we have a cap somehwe
                //if the unit is not doing anything ( not currenlty buldings/atatcking ) issue a build order
                if (bwapi.getSelf().getMinerals() >= 15 && unit.isIdle()) {

                    // not sure if it is build addon , its the once I  foudnd that can take a type as a paramted
                    //morph is for changing it
                    //attack is for attacking - need one to issue build of scarbs
                    unit.buildAddon(UnitType.UnitTypes.Protoss_Scarab);

                    // need to add built scarb to the list - or does it do this automaticlly
                   // scarbList.add()

                }
                // can add other unit checks here and have them build or issue commands

            }


        }
    }

        public void SendScarbs(Unit enmey){
            ScarbCount = scarbList.size(); // set the couunter to the size of the scarb list

            for (Unit unit : scarbList){

                if (ScarbCount!= 0){ // if we have scarbs

                    unit.attack(enmey.getPosition(), false);
                    scarbList.remove(unit); // remove the used scarb from the list after it was deployed
                    break;
                }
            }
        }


    /**
     * Attack the a certain type of enemy as defined by 'enemyType'.
     * @param enemyType The type of enemy to be attacked.
     * @param n_squads How many squads should be used to attack the enemy.
     */
    public void attackSpecificEnemyType(UnitType enemyType, int n_squads){
            for (Unit enemy : enemyList){
                if (enemy.getType() == enemyType){
                    attack(enemy, n_squads);
                    return;
                }
            }

    }
    public void attackSpecificEnemyType(UnitType enemyType){
        attackSpecificEnemyType(enemyType, 1);
    }


    public boolean attackMostVulerableEnemy(int n_squads){
        return attack(getMostVulnerableEnemy(), n_squads);
    }

    private Unit getMostVulnerableEnemy(){
        int index = 0;
        Unit weakestEnemy = null;
        double weakest = 1; //1 is the maximum value of the division of hitpoints / init hitpoints
        double tempHealth;
        for (Unit enemyUnit: enemyList){
                tempHealth = (enemyUnit.getHitPoints() + 1) / (enemyUnit.getInitialHitPoints() + 1);
                if (tempHealth < weakest) {
                    weakest = tempHealth;
                    weakestEnemy = enemyUnit;
                }
            index += 1;
        }
        return enemyList.size() == 0 ? null : weakestEnemy;
    }
}








