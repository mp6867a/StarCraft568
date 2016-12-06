package bot;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.sun.jmx.snmp.SnmpUnknownSecModelException;
import javafx.geometry.Pos;
import jnibwapi.BWAPIEventListener;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.RaceType;
import jnibwapi.types.TechType;
import jnibwapi.types.TechType.TechTypes;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.types.UpgradeType;
import jnibwapi.types.UpgradeType.UpgradeTypes;
import jnibwapi.util.BWColor;
import bot.BuildOrder;

import javax.lang.model.type.UnionType;


/**
 * Example Java AI Client using JNI-BWAPI.
 * 
 * Executes a 5-pool rush and cheats using perfect information.
 * 
 * Note: the agent often gets stuck when attempting to build the spawning pool. It works best on
 * maps where the overlord spawns with plenty of free space around it.
 */
public class protossClient implements BWAPIEventListener {
	
	/** reference to JNI-BWAPI */
	private final JNIBWAPI bwapi;
	
	/** used for mineral splits */
	private final HashSet<Unit> claimedMinerals = new HashSet<>();
	
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
	private int armySize;
	
	//is this a buildable area of creep
	private boolean buildHere;
	
	//the area where the spawning pool is built
	private Position buildArea;
	//position of the first pylon
	private Position pyPos;
	//buildable position near first pylon
	private Position gatePos;
	private Position gasFieldPos;
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
	private RaceType myRaceType;
	private RaceType enemyRaceType;
	private UnitType builderType;
	private UnitType supplyType;
	private int gasTrigger;
	private int gasCollecters;
	private boolean gasFieldBuilt;
	private boolean gatheringGas;
	private boolean gasFieldShouldBeBuilt;

	private List<Unit> gateways;
	private List<Unit> probes;
	private List<Unit> zealots;
	private List<Unit> nexus;

	private BuildOrder buildOrder;

	private UnitType unitTypeUnderConstruction;

	public static final int SUCCESSFUL = 0;
	public static final int NOT_ENOUGH_MINERALS = 1;
	public static final int NOT_ENOUGH_GAS = 2;
	public static final int NOT_ENOUGH_MINERALS_AND_GAS = 3;
	public static final int REQUISITE_BUILDING_DOES_NOT_EXIST = 4;

	private int lastState;

	private boolean diagnosticMode;

	/**
	 * Create a Java AI.
	 */
	public static void main(String[] args) {
		new protossClient();
	}
	
	/**
	 * Instantiates the JNI-BWAPI interface and connects to BWAPI.
	 */
	public protossClient() {
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
		diagnosticMode = true;

		System.out.println("Game Started");
		
		bwapi.enableUserInput();
		bwapi.enablePerfectInformation();
		bwapi.setGameSpeed(0);
		
		// reset agent state
		claimedMinerals.clear();
		warpedProbe = false;
		poolDrone = null;
		myRaceType = bwapi.getSelf().getRace();
		supplyCap = 0;
		gasCollecters = 2;
		gasTrigger = 7;
		gasFieldBuilt = false;
		gasFieldShouldBeBuilt = false;
		gatheringGas = false;
		lastState = SUCCESSFUL;

		probes = new ArrayList<Unit>();
		gateways = new ArrayList<Unit>();
		zealots = new ArrayList<Unit>();
		nexus = new ArrayList<Unit>();

		buildOrder = new BuildOrder(bwapi.getSelf(), bwapi.getEnemies().iterator().next());

		setBuilderType();
	}
	
	/**
	 * Called each game cycle.
	 */
	@Override
	public void matchFrame() {
		countPopulation();
		// print out some info about any upgrades or research happening
		// draw the terrain information
		bwapi.getMap().drawTerrainData(bwapi);
		dispatchProbes();
		//Building gas fields is to be handled outside of Build Order
		if (gasFieldShouldBeBuilt || (bwapi.getSelf().getMinerals() >= UnitTypes.Protoss_Assimilator.getMineralPrice() && !gasFieldBuilt)) {
			if (getUnitsOfType(UnitTypes.Protoss_Assimilator).size() == 0){
				gasFieldShouldBeBuilt = true;
			}
			else{
				gasFieldShouldBeBuilt = false;
			}
			buildGasField();
			gasFieldBuilt = true;
		}
		else if (gasFieldBuilt && !gasFieldShouldBeBuilt){
			//check if a supply unit is needed.
			if(!buildSupplyIfNeeded()){
				if (lastState == SUCCESSFUL || lastState == REQUISITE_BUILDING_DOES_NOT_EXIST) {
					unitTypeUnderConstruction = buildOrder.getNextBuild();
					lastState = buildAgnostic(unitTypeUnderConstruction);
				}
				else{
					//There is a deficit in gas or minerals... try to build again
					//This block could include logic on rearranging workers to fetch the resource in need.
					lastState = buildAgnostic(unitTypeUnderConstruction);
				}
			}
		}

		//Attack and defence logic here!
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
			if (unit.getType() == UnitTypes.Protoss_Zealot){
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

			if (unit.getType() == UnitTypes.Protoss_Zealot) {
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
			if (unit.getType() == UnitTypes.Protoss_Zealot) {
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

	private void buildGasField(){
		gasFieldPos = getGasLocation();
		try {
			getBestNUnits(builderType, 1).get(0).build(gasFieldPos, UnitTypes.Protoss_Assimilator);
		}
		catch (IndexOutOfBoundsException e){
			System.out.println("No builders exist!");
		}

	}

	private void dispatchToGasField(){
		Unit geyser = getUnitsOfType(UnitTypes.Protoss_Assimilator).get(0);
		List<Unit> builders = getBestNUnits(builderType, 1);
		for (Unit builder : builders) {
			if (!builder.isGatheringGas()){
				builder.gather(geyser, false);
				return;
			}
		}
	}

	/**
	 * get the best units availible by selecting idle ones first.
	 * @param n_units
	 * @return
	 */
	private List<Unit> getBestNUnits(UnitType type, int n_units) {
		List<Unit> builders = getUnitsOfType(type);
		List<Unit> best = new ArrayList<Unit>();
		for (Unit builder : builders){
			if (n_units == 0){
				return best;
			}
			if (builder.isIdle()){
				best.add(builder);
				n_units -= 1;
			}
		}
		//Now we don't care if the unit is idle.
		for (Unit builder : builders){
			if (n_units == 0){
				return best;
			}
			if(!best.contains(builder)){
				best.add(builder);
				n_units -= 1;
			}
		}
		return best;
	}

	private Position getGasLocation(){
		List<Unit> geysers = new ArrayList<Unit>();
		List<Integer> distances = new ArrayList<Integer>();
		int minDistance = Integer.MAX_VALUE;
		int minIndex = 0;
		for (Unit unit : bwapi.getNeutralUnits()){
			if (unit.getType() == UnitTypes.Resource_Vespene_Geyser){
				geysers.add(unit);
				bwapi.drawCircle(unit.getTilePosition(), 50, BWColor.Blue, true, false);
				distances.add(bwapi.getSelf().getStartLocation().getApproxBDistance(unit.getTilePosition()));
			}
		}
		for (int i = 0; i < distances.size(); i++){
			if (distances.get(i) < minDistance){
				minIndex = i;
				minDistance = distances.get(i);
			}
		}
		return geysers.get(minIndex).getTilePosition();
	}
	private void setBuilderType(){
		if (myRaceType == RaceType.RaceTypes.Protoss) {
			builderType = UnitTypes.Protoss_Probe;
			supplyType = UnitTypes.Protoss_Pylon;
		}
		if (myRaceType == RaceType.RaceTypes.Zerg) {
			builderType = UnitTypes.Zerg_Drone;
			supplyType = UnitTypes.Zerg_Overlord;
		}
		if (myRaceType == RaceType.RaceTypes.Terran) {
			builderType = UnitTypes.Terran_SCV;
			supplyType = UnitTypes.Terran_Supply_Depot;
		}
	}
	private void dispatchProbes(){
		List<Unit> probes = getUnitsOfType(builderType);
		List<Unit> minerals = bwapi.getNeutralUnits();

		int gasGatherers = 0;
		int mineralGatherers = 0;
		int builders = 0;
		boolean doingSomething = false;
		for (Unit builder : probes){
			if (builder.isGatheringGas()){
				gasGatherers += 1;
				doingSomething = true;
			}
			if (builder.isConstructing()){
				builders += 1;
				doingSomething = true;
			}
			if (builder.isGatheringMinerals()){
				mineralGatherers += 1;
				doingSomething = true;
			}
			if (!doingSomething){
				for (Unit mineral : minerals){
					if(mineral.getType() == UnitTypes.Resource_Mineral_Field && builder.getDistance(mineral) < 300) {
						builder.gather(mineral, false);
					}
				}
			}
			doingSomething = false;
		}
		if (getUnitsOfType(UnitTypes.Protoss_Assimilator).size() > 0) {
			while (gasGatherers < gasCollecters) {
				dispatchToGasField();
				gasGatherers += 1;
			}
		}
	}

	private void wipePopulations(){
		zealots = new ArrayList<Unit>();
		gateways = new ArrayList<Unit>();
		probes = new ArrayList<Unit>();
		nexus = new ArrayList<Unit>();
	}

	private void countPopulation(){
		wipePopulations();
		for (Unit unit : bwapi.getMyUnits()){
			if (unit.getType() == UnitTypes.Protoss_Probe){
				probes.add(unit);
			}
			if (unit.getType() == UnitTypes.Protoss_Zealot){
				zealots.add(unit);
			}
			if (unit.getType() == UnitTypes.Protoss_Gateway){
				gateways.add(unit);
			}
			if (unit.getType() == UnitTypes.Protoss_Nexus){
				nexus.add(unit);
			}
		}
	}

	/**
	 *
	 * @param buildingOrUnit A UnitType to build
	 * @return code describing success
	 */
	private int buildAgnostic(UnitType buildingOrUnit){
		if (diagnosticMode){
			System.out.print("Attempting to build a " + buildingOrUnit.getName() + " which is a ");
		}
		if (buildingOrUnit.isBuilding()){
			if (diagnosticMode){
				System.out.println("building.");
			}
			return build(buildingOrUnit);
		}
		else{
			if (diagnosticMode){
				System.out.println("unit.");
			}
			return train(buildingOrUnit);
		}
	}

	private int build(UnitType building){
		return build(building, bwapi.getSelf().getStartLocation());
	}
	private int build(UnitType building, Position basePos){
		Position buildPos = getBuildPosition(basePos, 100, building);
		try {
			Unit bestProbe = getBestNUnits(builderType, 1).get(0);
			if (bwapi.getSelf().getMinerals() >= building.getMineralPrice() && bwapi.getSelf().getGas() >= building.getGasPrice()){
				bestProbe.build(basePos, building);
				return SUCCESSFUL;
			}
		}
		catch (IndexOutOfBoundsException e){
			//No probes exist
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
		//Not enough minerals
		return mineral_gas_deficit(building);
	}
	private int train(UnitType unit){
		try {
			//this should be the building that constructs this unit.
			UnitType buildingThatMakes = UnitTypes.getUnitType(unit.getWhatBuildID());
			Unit bestBuilding = getBestNUnits(buildingThatMakes, 1).get(0);
			if (bwapi.getSelf().getMinerals() >= unit.getMineralPrice() && bwapi.getSelf().getGas() >= unit.getGasPrice()){
				bestBuilding.train(unit);
				return SUCCESSFUL;
			}
		}
		catch (IndexOutOfBoundsException e) {
			//No building exists
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
		//Not enough minerals and/or gas
		return mineral_gas_deficit(unit);
	}
	private int mineral_gas_deficit(UnitType unitType){
		int myGas = bwapi.getSelf().getGas();
		int myMinerals = bwapi.getSelf().getMinerals();
		int unitMinerals = unitType.getMineralPrice();
		int unitGas = unitType.getGasPrice();
		if (myGas < unitGas && myMinerals < unitMinerals){
			if (diagnosticMode){
				System.out.println("Not enough gas and minerals to build " + unitType.getName() + ".");
			}
			return NOT_ENOUGH_MINERALS_AND_GAS;
		}
		if (myGas < unitGas){
			if (diagnosticMode){
				System.out.println("Not enough gas to build " + unitType.getName() + ".");
			}
			return NOT_ENOUGH_GAS;
		}
		else{
			if (diagnosticMode){
				System.out.println("Not enough minerals to build " + unitType.getName() + ".");
			}
			return NOT_ENOUGH_MINERALS;
		}
	}

	/**
	 * If this needs to be built, then this must be the priority.
	 * @return
	 */
	private boolean buildSupplyIfNeeded(){
		int supply = bwapi.getSelf().getSupplyUsed();
		int supplyTotal = bwapi.getSelf().getSupplyTotal();
		if (supply + 2 > supplyTotal){
			build(supplyType);
			return true;
		}
		return false;
	}
}
