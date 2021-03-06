package bot;

import jnibwapi.BWAPIEventListener;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.TechType;
import jnibwapi.types.UnitType;
import jnibwapi.types.UpgradeType;
import jnibwapi.util.BWColor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by rehaf on 11/22/16.
 */
public class TestProtoss implements BWAPIEventListener  {

    /** reference to JNI-BWAPI */
    private final JNIBWAPI bwapi;

    /** used for mineral splits */
    private final HashSet<Unit> claimedMinerals = new HashSet<>();
    private final HashSet<Unit> claimedGas = new HashSet<>();

    /** have probe 5 been warped */
    private boolean warpedProbe;

    /** the drone that has been assigned to building a pool */
    private Unit poolDrone;

    /** when should the next overlord be spawned? */
    private int supplyCap;

    //the number of zerglings massed at the base
    private int garrisonZLots;

    //number of probes
    private int numProbes;
    //the number of zealots which have been trained
    private int numZealots;
    private int numDragons;
    private int armySize;

    //is this a buildable area of creep
    private boolean buildHere;

    //the area where the spawning pool is built
    private Position buildArea;
    //position of the first pylon
    private Position pyPos;
    //buildable position near first pylon
    private Position gatePos;
    //is the first pylon built
    private boolean pylonUp=false;
    //is the first gateway built
    private boolean gatewayUp=false;
    private Unit[] allZealots = new Unit[0];
    private Unit[] zealotsAttacking = new Unit[0];
    private double maxProbes = 5;
    private double probesIncrement = 1.0;
    private double getProbesIncrementDivision = 0.95;
    private boolean zealotAttackUnderway = false;
    /**
     * Create a Java AI.
     */
    public static void main(String[] args) {
        new protossClient();
    }

    /**
     * Instantiates the JNI-BWAPI interface and connects to BWAPI.
     */
    public TestProtoss() {
        bwapi = new JNIBWAPI(this, true);
        bwapi.start();
    }

    /**
     * Connection to BWAPI established.
     */
    @Override
    public void connected() {
        System.out.println("Connected");
    }

    /**
     * Called at the beginning of a game.
     */
    @Override
    public void matchStart() {
        System.out.println("Game Started");

        bwapi.enableUserInput();
        bwapi.enablePerfectInformation();
        bwapi.setGameSpeed(0);

        // reset agent state
        claimedMinerals.clear();
        claimedGas.clear();

        warpedProbe = false;
        poolDrone = null;
        supplyCap = 0;
    }

    /**
     * Called each game cycle.
     */
    @Override
    public void matchFrame() {
        // print out some info about any upgrades or research happening
        String msg = "=";
        //System.out.println("New Frame");
        numProbes = countUnits(UnitType.UnitTypes.Protoss_Probe);
        numZealots = countUnits(UnitType.UnitTypes.Protoss_Zealot);
        int alt = (int)(Math.sqrt(numZealots) * 2);
        maxProbes = 5 > alt ? 5 : alt;
        for (TechType t : TechType.TechTypes.getAllTechTypes()) {
            if (bwapi.getSelf().isResearching(t)) {
                msg += "Researching " + t.getName() + "=";
            }
            // Exclude tech that is given at the start of the game
            UnitType whatResearches = t.getWhatResearches();
            if (whatResearches == UnitType.UnitTypes.None) {
                continue;
            }
            if (bwapi.getSelf().isResearched(t)) {
                msg += "Researched " + t.getName() + "=";
            }
        }
        for (UpgradeType t : UpgradeType.UpgradeTypes.getAllUpgradeTypes()) {
            if (bwapi.getSelf().isUpgrading(t)) {
                msg += "Upgrading " + t.getName() + "=";
            }
            if (bwapi.getSelf().getUpgradeLevel(t) > 0) {
                int level = bwapi.getSelf().getUpgradeLevel(t);
                msg += "Upgraded " + t.getName() + " to level " + level + "=";
            }
        }
        bwapi.drawText(new Position(0, 20), msg, true);
        // draw the terrain information
        bwapi.getMap().drawTerrainData(bwapi);
        // spawn a drone
        for (Unit unit : bwapi.getMyUnits()) {
            // Note you can use referential equality
            if (unit.getType() == UnitType.UnitTypes.Protoss_Nexus) {
                if (bwapi.getSelf().getMinerals() >= 50 && !warpedProbe && numProbes <= maxProbes) {
                    unit.morph(UnitType.UnitTypes.Protoss_Probe);
                    warpedProbe = true;
                }
            }
        }
        //This seems like a pretty weak search for a suitable build location. Could we do an iteration of random values?
        buildArea = getBuildPosition(bwapi.getSelf().getStartLocation(), 200, UnitType.UnitTypes.Protoss_Pylon);
        // build a pylon
        if (bwapi.getSelf().getMinerals() >= 100 && pylonUp == false) {
            for (Unit unit : bwapi.getMyUnits()) {
                if (unit.getType() == UnitType.UnitTypes.Protoss_Probe) {
                    poolDrone = unit;
                    unit.build(buildArea, UnitType.UnitTypes.Protoss_Pylon);
                    pyPos = buildArea;
                    gatewayUp = false; //build a new gateway
                    pylonUp=true;
                    break;
                }
            }
        }
        // build the gateway next to the pylon
        if(bwapi.getSelf().getMinerals() >= UnitType.UnitTypes.Protoss_Gateway.getBuildScore() && pyPos != null && !gatewayUp){
            gatePos = getBuildPosition(pyPos, 100, UnitType.UnitTypes.Protoss_Gateway);
            // build a gateway
            if (gatePos != null) {
                bwapi.drawCircle(gatePos, 50, BWColor.Orange, false, false);
                for (Unit probe : bwapi.getMyUnits()) {
                    if (probe.getType() == UnitType.UnitTypes.Protoss_Probe) {
                        poolDrone = probe;

                        probe.build(gatePos, UnitType.UnitTypes.Protoss_Gateway);
                        gatewayUp = true;
                        break;
                    }
                }
            }
        }
        // Build Pylons
        if (bwapi.getSelf().getSupplyUsed() + 2 >= bwapi.getSelf().getSupplyTotal()
                && bwapi.getSelf().getSupplyTotal() >= supplyCap) {
            bwapi.sendText("pylon required");
            if (bwapi.getSelf().getMinerals() >= 100) {
                for (Unit builder : bwapi.getMyUnits()) {
                    if (builder.getType() == UnitType.UnitTypes.Protoss_Probe) {
                        builder.build(buildArea, UnitType.UnitTypes.Protoss_Pylon);
                        gatewayUp = false;
                        supplyCap = bwapi.getSelf().getSupplyTotal();
                        break;
                    }

                    //for (Unit minerals : bwapi.getNeutralUnits()) {
                    //	if (minerals.getType().isMineralField()
                    //			&& !claimedMinerals.contains(minerals)) {
                    //		double distance = poolDrone.getDistance(minerals);

                    //	if (distance < 300) {
                    //			builder.rightClick(minerals, false);
                    //			claimedMinerals.add(minerals);
                    //			break;
                    //		}
                    //	}
                    //}
                }
            }
        }
        // spawn probes

        else if (bwapi.getSelf().getMinerals() >= 50 && (numProbes <= maxProbes)){
            for (Unit unit : bwapi.getMyUnits()) {
                if (unit.getType() == UnitType.UnitTypes.Protoss_Nexus && unit.isCompleted()) {
                    unit.train(UnitType.UnitTypes.Protoss_Probe);

                    break;
                }
            }
        }

        // spawn zealots if we didn't spawn anything else
        else if (bwapi.getSelf().getMinerals() >= UnitType.UnitTypes.Protoss_Zealot.getBuildScore() && gatewayUp) {
            int n_minerals = bwapi.getSelf().getMinerals();
            for (Unit unit : bwapi.getMyUnits()) {
                if (unit.getType() == UnitType.UnitTypes.Protoss_Gateway && unit.isCompleted()) {
                    unit.train(UnitType.UnitTypes.Protoss_Zealot);
                    n_minerals -= UnitType.UnitTypes.Protoss_Zealot.getBuildScore();
                }
                if(n_minerals < UnitType.UnitTypes.Protoss_Zealot.getBuildScore()) {
                    break;
                }
            }
        }
        // Force probes to gather minerals if they are idle
        for (Unit probe : bwapi.getMyUnits()){
            if (probe.getType() == UnitType.UnitTypes.Protoss_Probe){
                if(probe.isIdle()){
                    for (Unit minerals : bwapi.getNeutralUnits()) {
                        if (minerals.getType().isMineralField()) {
                            double distance = probe.getDistance(minerals);
                            if (distance < 300) {
                                probe.gather(minerals, false);
                                break;
                            }
                        }
                    }
                }
            }
        }
        //mass at least 8 zealots before attacking
        // '8 zealots' should be a configurable value based on environment
        //nitpicking on names here, we shouldn't use 'garrison' to describe units being massed for attack
        //also, this is an all-out attack; I'm changing this to a configurable amount to attack with
        int minZealotsForAttack = 8;
        double reserveRatio = 0.2;
        double lossTolerance = 0.5;
        double minAdvantage = 0.1;
        double strengthDisadvantage = -0.15;
        double strengthBalance = strengthBalance();
        if (!zealotAttackUnderway && strengthBalance >= minAdvantage) {
            //this logic leaves some Zealots in reserve
            allZealots = getZealots();
            zealotsAttacking = new Unit[(int)(allZealots.length * (1 - reserveRatio))];
            System.arraycopy(allZealots, 0, zealotsAttacking, 0, (int) (allZealots.length * (1 - reserveRatio)));
            if (zealotsAttacking.length >= minZealotsForAttack) {
                //Mass the units
                massZealots(zealotsAttacking);
                //I'm guessing that we need to wait until they are actually massed together...
                // attack move toward an enemy
                zealotAttackUnderway = zealotAttack(zealotsAttacking);
            }
        }
        else {
            if(countZealots() < lossTolerance * zealotsAttacking.length || strengthBalance <= strengthDisadvantage) {
                //some form of retreat if we are taking on large losses or at a disadvantage
            }
            else if (zealotAttackUnderway){
                //keep attacking if the attack isn't going horribly
                zealotAttackUnderway = zealotAttack(zealotsAttacking);
            }
        }
    }
    @Override
    public void keyPressed(int keyCode) {}
    @Override
    public void matchEnd(boolean winner) {}
    @Override
    public void sendText(String text) {}
    @Override
    public void receiveText(String text) {}
    @Override
    public void nukeDetect(Position p) {}
    @Override
    public void nukeDetect() {}
    @Override
    public void playerLeft(int playerID) {}
    @Override
    public void unitCreate(int unitID) {}
    @Override
    public void unitDestroy(int unitID) {}
    @Override
    public void unitDiscover(int unitID) {}
    @Override
    public void unitEvade(int unitID) {}
    @Override
    public void unitHide(int unitID) {}
    @Override
    public void unitMorph(int unitID) {}
    @Override
    public void unitShow(int unitID) {}
    @Override
    public void unitRenegade(int unitID) {}
    @Override
    public void saveGame(String gameName) {}
    @Override
    public void unitComplete(int unitID) {}
    @Override
    public void playerDropped(int playerID) {}

    private int countZealots()
    {
        int zealots = 0;
        for (Unit unit : idleUnits()) {
            if (unit.getType() == UnitType.UnitTypes.Protoss_Zealot){
                zealots += 1;
            }
        }
        return zealots;
    }

    private int countUnits(UnitType searchType)
    {
        int count = 0;
        for (Unit unit : bwapi.getMyUnits()) {
            if (unit.getType() == searchType){
                count += 1;
            }
        }
        return count;
    }
    private Unit[] getZealots() {
        ArrayList<Unit> zealots = new ArrayList<Unit>();
        for (Unit unit : bwapi.getMyUnits()) {
            if (unit.getType() == UnitType.UnitTypes.Protoss_Zealot){
                zealots.add(unit);
            }
        }
        return zealots.toArray(new Unit[0]);
    }
    private Unit[] idleUnits()
    {
        ArrayList<Unit> idleUnitsList = new ArrayList<Unit>();
        for (Unit unit : bwapi.getMyUnits()) {
            if (unit.isIdle()) {
                idleUnitsList.add(unit);
            }
        }
        return idleUnitsList.toArray(new Unit[0]);
    }

    private void massZealots(Unit[] zealotsArray) {
        //get the first zealots position
        Position basePosition = zealotsArray[0].getPosition();
        for (Unit zealot : zealotsArray) {
            //move to base location
        }
    }

    /**
     have our zealots attack the FIRST unit in the stack.
     Returns if the zealots are actually attacking an enemy.
     Maybe we should calculate the nearest enemy to attack?
     */
    private boolean zealotAttack(Unit[] zealotArray) {
        boolean attackMoveMade = false;
        //all should attack the most vulnerable enemy
        //maybe should look for closest?
        Unit enemyToAttack = getMostVulnerableEnemy();
        for (Unit unit : zealotArray) {

            if (unit.getType() == UnitType.UnitTypes.Protoss_Zealot) {
                if (unit.isAttacking() || unit.isMoving()){
                    //either the unit is attacking or moving (could be rallying) and no action should be taken
                    attackMoveMade = true;
                    continue;
                }
                if (enemyToAttack != null) {
                    unit.attack(enemyToAttack.getPosition(), false);
                    attackMoveMade = true;
                }
            }
        }
        return attackMoveMade;
    }

    private boolean destroyEnemyBuildings() {
        boolean attackMoveMade = false;
        for (Unit unit : idleUnits()) {
            if (unit.getType() == UnitType.UnitTypes.Protoss_Zealot) {
                //some logic to check if 'unit' is a building
                for (Unit enemy : bwapi.getEnemyUnits()) {
                    unit.attack(enemy.getPosition(), false);
                    attackMoveMade = true;
                    break;
                }
            }
        }
        return attackMoveMade;
    }

    private double getEnemyHealth(){
        double strength = 0.0;
        //included for future use
        double initialStrength = 0.0;
        for (Unit enemyUnit: bwapi.getEnemyUnits()) {
            strength += enemyUnit.getHitPoints();
            initialStrength += enemyUnit.getInitialHitPoints();
        }
        return strength;
    }

    private double getSelfHealth(){
        double strength = 0.0;
        double initialStrength = 0.0;
        for (Unit enemyUnit: bwapi.getMyUnits()) {
            strength += enemyUnit.getHitPoints();
            initialStrength += enemyUnit.getInitialHitPoints();
        }
        return strength;
    }

    private double getEnemyAttackStrength(){
        double strength = 0.0;
        double score = 0.0;
        for (Unit enemyUnit: bwapi.getEnemyUnits()){
            strength += scoreUnitAttackThreat(enemyUnit);
        }
        return strength;
    }

    private double getSelfAttackStrength(){
        double strength = 0.0;
        double score = 0.0;
        for (Unit myUnit: bwapi.getMyUnits()){
            strength += scoreUnitAttackThreat(myUnit);
        }
        return strength;
    }
    private double scoreUnitAttackThreat(Unit unit)
    {
        UnitType thisUnitType = unit.getType();
        //takes the damage the unit can do and adds in some modification based on armor and speed.
        double speedBuffer = 10.0;
        double armorBuffer = 1.5;
        double baseAttack = thisUnitType.getGroundWeapon().getDamageAmount();
        double armorMod = (thisUnitType.getArmor() + armorBuffer) / armorBuffer;
        double speedMod = ((thisUnitType.getTopSpeed() + speedBuffer) / speedBuffer);
        double allMods = speedMod * armorMod;
        return baseAttack * allMods;
    }

    private double strengthBalance()
    {
        double myStrength = getSelfAttackStrength();
        double myHealth = getSelfHealth();
        double enemyStrength = getEnemyAttackStrength();
        double enemyHealth = getEnemyHealth();
        double myScore = (myStrength * myHealth);
        double enemyScore = (enemyStrength * enemyHealth);
        //negative scores favor the enemy, positive favors Self
        if (enemyScore > myScore){
            //subtract by 1 to set a tie at 0.0
            //multiply by -1 to show enemy advantage
            return -1 * (enemyScore / myScore - 1);
        }
        else {
            return (myScore / enemyScore - 1);
        }
    }

    private Unit getMostVulnerableEnemy(){
        int index = 0;
        int minIndex = 0;
        double weakest = 1; //1 is the maximum value of the divison of hitpoints / init hitpoints
        double tempHealth;
        List<Unit> enemyUnits = bwapi.getEnemyUnits();
        for (Unit enemyUnit: enemyUnits){
            tempHealth = (enemyUnit.getHitPoints() + 1) / (enemyUnit.getInitialHitPoints() + 1);
            if (tempHealth <  weakest){
                weakest = tempHealth;
                minIndex = index;
            }
            index += 1;
        }
        return enemyUnits.size() == 0 ? null : enemyUnits.get(minIndex);
    }
    private Position getBuildPosition(Position base, int offsetFromCenter, UnitType type){
        Position buildArea= new Position(base.getPX()+offsetFromCenter,base.getPY());
        if(bwapi.canBuildHere(buildArea, type, true) == false){
            buildArea= new Position(base.getPX()-offsetFromCenter,base.getPY());
            if(bwapi.canBuildHere(buildArea, type, true) == false){
                buildArea= new Position(base.getPX(),base.getPY()+offsetFromCenter);
                if(bwapi.canBuildHere(buildArea, type, true) == false){
                    buildArea= new Position(base.getPX(),base.getPY()-offsetFromCenter);
                    if(bwapi.canBuildHere(buildArea, type, true) == false){
                        buildArea= new Position(base.getPX()-offsetFromCenter,base.getPY()+offsetFromCenter);
                        if(bwapi.canBuildHere(buildArea, type, true) == false){
                            buildArea= new Position(base.getPX()-offsetFromCenter,base.getPY()+offsetFromCenter);
                            if(bwapi.canBuildHere(buildArea, type, true) == false){
                                buildArea= new Position(base.getPX()+offsetFromCenter,base.getPY()-offsetFromCenter);
                                if(bwapi.canBuildHere(buildArea, type, true) == false) {
                                    buildArea= new Position(base.getPX()+offsetFromCenter,base.getPY()+offsetFromCenter);
                                    if(bwapi.canBuildHere(buildArea, type, true) == false){
                                        if (offsetFromCenter > 1000){
                                            //prevent an endless recursion
                                            return null;
                                        }
                                        buildArea = getBuildPosition(base, (int)(offsetFromCenter * 1.1), type);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return buildArea;
    }
    private List<Unit> getUnitsOfType(UnitType unitTypeSought){
        List<Unit> unitsOfTypeSought = new ArrayList<Unit>();
        for(Unit unit: bwapi.getMyUnits()){
            if(unit.getType() == unitTypeSought){
                unitsOfTypeSought.add(unit);
            }
        }
        return unitsOfTypeSought;
    }
}
