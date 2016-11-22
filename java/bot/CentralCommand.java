package bot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.layout.TilePane;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.UnitType;
import jnibwapi.BaseLocation;
import jnibwapi.JNIBWAPI;

public class CentralCommand {
            JNIBWAPI bwapi;

    public List<Squad> squads;
    public List<Unit> units;


    public HashSet<Position> enemyBuildsingsPos = new HashSet();
    public List<Unit> EnemyList ;

    public CentralCommand(){
        squads = new ArrayList<Squad>();
        units = new ArrayList<Unit>();
        EnemyList = new ArrayList<Unit>() ;
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
    ///
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


    public Position retrunEnemyLocation(){


return null;
    }


//this method goes over possible bases locations

    public  void attackAtStartBaseLocation(){

       for(BaseLocation base: bwapi.getMap().getStartLocations() ){

            if(base.isStartLocation()){
                //add check over enemy units
                for ( Unit unit : units){//modify this to squad if needed

                        unit.attack(base.getPosition(), true);
                }
            }

       }

    }

    public  void  GetEnemeyBaseToMemoery() {

        //loop through enemy units
        for (Unit enemyUnit : bwapi.getEnemyUnits()) {
            // if it is a bulding type
            if (enemyUnit.getType().isBuilding()) {

                // if the it does not contain a an enemy unit's postion - then we will add a postion
                if (!enemyBuildsingsPos.contains(enemyUnit.getPosition())) enemyBuildsingsPos.add(enemyUnit.getPosition());

            }
        }


        // loop through postions in list
        for (Position postion : enemyBuildsingsPos ){

            // need to get x and y values of the postion in relation to world map - not sure if it is BX VS Px
            Position top = new Position( postion.getPX()/32, postion.getPY()/32 );


            //check if that postion is visble
            if(bwapi.isVisible(top)){

                boolean BuildingIsStillThere = false;

                for ( Unit enemyUni: bwapi.getEnemyUnits()){

                        if(enemyUni.getType().isBuilding() && enemyUni.getPosition() == postion){

                            BuildingIsStillThere = true;
                            break;
                        }
                }

                if(BuildingIsStillThere == false){

                    enemyBuildsingsPos.remove(postion);
                    break;
                }


            }


        }
    }




///// this still needs work - it should check the enemy add it to list and if it doesnt exsist remove it
    /// if it is an anymy that can attack ( i.e. not a bulding) // other checks might be better -
    public  void  getEnemyiesToList() {

        //loop through enemy units
        for (Unit enemyUnit : bwapi.getEnemyUnits()) {
            /// if the enemy unit can attack - not sure if this is applicable here there are other checks that may be more helpfull
            if (enemyUnit.getType().isAttackCapable()) {

                if (!EnemyList.contains(enemyUnit.getTarget())) EnemyList.add(enemyUnit.getTarget());

            }
        }


        // loop through enemy list  in list
        for (Unit unit : EnemyList ){



                boolean enemyIsDeadk = false;

                for ( Unit enemyUni: bwapi.getEnemyUnits()){

                    //if(enemyUni.getType().is){  --- can update this to nclude contions
                    //unit . isstuck might be usefull for us later on - in another calss
                    if( !unit.isExists()) { //dpes not exsist
                        enemyIsDeadk = true;
                        break;
                    }
                }

                if(enemyIsDeadk == false){

                    EnemyList.remove(unit);
                    break;
                }





        }
    }
    }





