package bot;

import java.util.*;

import jnibwapi.BWAPIEventListener;
import jnibwapi.JNIBWAPI;
import jnibwapi.Position;
import jnibwapi.Unit;
import jnibwapi.types.RaceType;
import jnibwapi.types.UnitType;
import jnibwapi.types.UnitType.UnitTypes;
import jnibwapi.util.BWColor;

import jnibwapi.ChokePoint;


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
	private int gasCollecters;
	private boolean waitTilStarted;
	private List<Unit> gateways;
	private List<Unit> probes;
	private List<Unit> zealots;
	private List<Unit> nexus;

	private Unit geyser;
	private Unit base;
	
	private List<Integer> buildQuadrants;

	private BuildOrder buildOrder;

	private UnitType unitTypeUnderConstruction;
	private UnitType buildIfIdle;
	private static int errorCode = 0;
	public static final int SUCCESSFUL = errorCode++;
	public static final int NOT_ENOUGH_MINERALS = errorCode++;
	public static final int NOT_ENOUGH_GAS = errorCode++;
	public static final int NOT_ENOUGH_MINERALS_AND_GAS = errorCode++;
	public static final int REQUISITE_BUILDING_DOES_NOT_EXIST = errorCode++;
	public static final int NO_SUITABLE_BUILD_LOCATION = errorCode++;
	public static final int NO_BUILDERS_EXIST = errorCode++;

	private int lastState;
	private int currentCount;
	//Diagnostic mode provides extra logging information
	private boolean diagnosticMode;
	private CentralCommand command;
	private List<Unit> miningProbes;
	private List<Unit> minerals;
	
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
		command = new CentralCommand(bwapi);
		warpedProbe = false;
		poolDrone = null;
		myRaceType = bwapi.getSelf().getRace();
		supplyCap = 0;
		gasCollecters = 2;
		currentCount = -1;
		//Something that cannot be the first unit to be built.
		unitTypeUnderConstruction = UnitTypes.Terran_Nuclear_Silo;

		lastState = REQUISITE_BUILDING_DOES_NOT_EXIST;

		probes = new ArrayList<Unit>();
		gateways = new ArrayList<Unit>();
		zealots = new ArrayList<Unit>();
		nexus = new ArrayList<Unit>();

		buildOrder = new BuildOrder(bwapi.getSelf(), bwapi.getEnemies().iterator().next());
		determineBuildQuadrants();
		setRaceSpecificUnits();
		
		gasFieldPos = getGasLocation();

		List<Unit> neutrals= bwapi.getNeutralUnits();
		minerals = new ArrayList<Unit>();
		for (Unit neutral : neutrals){
			if (neutral.getType() == UnitTypes.Resource_Mineral_Field &&  neutral.getDistance(bwapi.getSelf().getStartLocation()) < 300)
			{
				minerals.add(neutral);
			}
		}
		miningProbes = new ArrayList<Unit>();
		buildIfIdle = buildOrder.getNextBuild(false, true);
		
		base = getUnitsOfType(UnitTypes.Protoss_Nexus).get(0);
	}
	
	/**
	 * Called each game cycle.
	 */
	@Override
	public void matchFrame() {
		countPopulation();
		//Make sure all buildings that require power are in fact powered
		// draw the terrain information
		bwapi.getMap().drawTerrainData(bwapi);
		//We always need to make sure there are no idle probes.
		dispatchProbes();
		command.loadUnits(bwapi.getMyUnits());
		command.refresh();
		if(!buildSupplyIfNeeded() && ensureWarped()){
			if (lastState == SUCCESSFUL || lastState == REQUISITE_BUILDING_DOES_NOT_EXIST ||
					lastState == NO_SUITABLE_BUILD_LOCATION) {
				//If the build was successful, but it was a building, then we must check if building has actually started!
				if (lastState == REQUISITE_BUILDING_DOES_NOT_EXIST || lastState == NO_SUITABLE_BUILD_LOCATION ||
						!unitTypeUnderConstruction.isBuilding() ||
						(getUnitsOfType(unitTypeUnderConstruction).size() != currentCount && isAllCompleted(unitTypeUnderConstruction))){
					unitTypeUnderConstruction = buildOrder.getNextBuild(false, false);
					currentCount = getUnitsOfType(unitTypeUnderConstruction).size();
					lastState = buildAgnostic(unitTypeUnderConstruction);
				}
				else{
					//lastState = buildAgnostic(unitTypeUnderConstruction);
				}
			}
			else{
				//we lacked the minerals or gas on the last try... try again...
				lastState = buildAgnostic(unitTypeUnderConstruction);
			}
		}
		if (bwapi.getSelf().getMinerals()> buildIfIdle.getMineralPrice() * 3 && bwapi.getSelf().getGas() > buildIfIdle.getGasPrice() * 2){
			train(buildIfIdle);

			if (diagnosticMode){
				System.out.print("Building a " + buildIfIdle.getName() + " as resources are being squandered.");
			}
			buildIfIdle = buildOrder.getNextBuild(false, true);
		}

		//Attack and defence logic here!
		if (command.units.size() >= Squad.maxUnits * 2){
			command.attackMostVulerableEnemy(1);
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

	/**
	 * How dangerous is a given Unit.
	 * @param unit A Unit's danger level to be evaluated.
	 * @return A double describing how dangerous a unit is.
	 */
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

	/**
	 * A scoring mechanism to determine the balance of power on the map.
	 * Postive scores favor self. Negative scores favor enemy. Scores are magnitudinal (the farther from zero,
	 * the more unbalanced the game).
	 * @return A floating point number describing the relative power in the map.
	 */
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


	/**
	 * Tests if the given position is on the grid.
	 * @param test The position being tested if it is on the grid.
	 * @return A boolean describing if 'test' is on the map.
	 */
    private boolean onGrid(Position test){
		int testY = test.getPY();
		int testX = test.getPX();
		Position topLeftGeyser = geyser.getTopLeft();
		Position bottomRightBase = base.getBottomRight();
		int bottomY = Math.max(topLeftGeyser.getPY(), bottomRightBase.getPY());
		int rightX = Math.max(topLeftGeyser.getPX(), bottomRightBase.getPX());
		int topY = Math.min(topLeftGeyser.getPY(), bottomRightBase.getPY());
		int leftX = Math.min(topLeftGeyser.getPX(), bottomRightBase.getPX());
		if (testX >= leftX && testX <= rightX && testY >=topY && testY <= bottomY){
			return false;
		}
		return testY >= 0 && testX >= 0 && testY < bwapi.getMap().getSize().getPY() && testX < bwapi.getMap().getSize().getPX();
	}

	private Position getBuildPosition(Position base, int offsetFromCenter, UnitType type){
		return getBuildPosition(base, offsetFromCenter, type, true);
	}

	/**
	 * Finds a good place to put a building given a staring position and reading off of the best grids.
	 * @param base The starting position of this search.
	 * @param offsetFromCenter How far to search the grid.
	 * @param type The type of unit being built.
	 * @param buildTowardEnemy If the building should be built toward the enemy base
	 * @return The position on which to build
	 */
    private Position getBuildPosition(Position base, int offsetFromCenter, UnitType type, boolean buildTowardEnemy){
		int max = 1000;
		bwapi.drawCircle(base, 50,  BWColor.Black, false, false);
		int flip = buildTowardEnemy ? 1 : -1;
		List<Position> positions;
		for (int x_off = offsetFromCenter; x_off <= max; x_off += (int)((max - offsetFromCenter) / 50)){
			for (int y_off = offsetFromCenter; y_off <= x_off; y_off += (int)((max - offsetFromCenter) / 50)){
				positions = inBuildQuadrants(base, flip * x_off, flip * y_off);
				Collections.shuffle(positions);
				for (Position buildArea : positions){
					if (onGrid(buildArea)) {
						if (bwapi.canBuildHere(buildArea, type, true)) {
							return buildArea;
						}
					}
				}
			}
		}
		return null;
	}

	private List<ChokePoint> getBaseChokePoints(){
        List<ChokePoint> chokePoints = bwapi.getMap().getChokePoints();
        List<ChokePoint> baseChokes = new LinkedList<>();
        for (ChokePoint cp : chokePoints) {
            if (cp.getFirstRegion() == bwapi.getMap().getRegion(buildArea)) {
                baseChokes.add(cp);
                System.out.println("in");
            } else if (cp.getSecondRegion() == bwapi.getMap().getRegion(buildArea)) {
                baseChokes.add(cp);
                System.out.println("in");
            }
        }
        return baseChokes;
    }

	/**
	 * Get all of your units of a given type.
	 * @param unitTypeSought the UnitType of Units sought.
	 * @return
	 */
	private List<Unit> getUnitsOfType(UnitType unitTypeSought){
		List<Unit> unitsOfTypeSought = new ArrayList<Unit>();
		for(Unit unit: bwapi.getMyUnits()){
			if(unit.getType() == unitTypeSought){
				unitsOfTypeSought.add(unit);
			}
		}
		return unitsOfTypeSought;
	}

	/**
	 * Build a gas field
	 * @param refinery the race specific refinery
	 * @return the success state
	 */
	private int buildGasField(UnitType refinery){
		try {
			Unit bestProbe = getBestNUnits(builderType, 1).get(0);
			bestProbe.build(gasFieldPos, refinery);
			return SUCCESSFUL;
		}
		catch (IndexOutOfBoundsException e){
			System.out.println("No builders exist!");
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
	}

	/**
	 * Move a worker to gather gas. First look for idle workers, then only workers not already gathering gas.
	 */
	private void dispatchToGasField(){
		Unit geyser = getUnitsOfType(UnitTypes.Protoss_Assimilator).get(0);
		for (Unit builder : getUnitsOfType(builderType)) {
			if ((builder.isIdle() && builder != poolDrone) || builder.isGatheringMinerals()){
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
			if(!best.contains(builder) && !builder.isConstructing()){
				best.add(builder);
				n_units -= 1;
			}
		}
		return best;
	}

	/**
	 * Find the nearest gas field to your base location.
	 * @return The nearest gas field's Tile Position (can be used to build a refinery).
	 */
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
		geyser = geysers.get(minIndex);
		return geysers.get(minIndex).getTilePosition();
	}

	/**
	 * Method to determine race specific units
	 * Supply and Builders are included.
	 * Could expand into other units in future.
	 */
	private void setRaceSpecificUnits(){
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

	/**
	 * Delegates workers to gather minerals if they are not doing anything. Also ensures a certain number of workers are
	 * gathering gas if it is possible
	 */
	private void dispatchProbes(){
		int gasGatherers = 0;
		int mineralGatherers = 0;
		int builders = 0;
		boolean doingSomething = false;
		if (probes.size() % minerals.size() == 0){
			claimedMinerals.clear();
		}
		for (Unit builder : probes){
			if (builder.isGatheringGas()){
				gasGatherers += 1;
				doingSomething = true;
			}
			if (builder.isConstructing()){
				builders += 1;
				doingSomething = true;
			}
			if (!doingSomething && (builder.isGatheringMinerals() || (builder.getDistance(bwapi.getSelf().getStartLocation()) < 300) && builder.isMoving())){
				mineralGatherers += 1;
				doingSomething = true;
			}
			if (builder.isCompleted() && !(doingSomething || builder == poolDrone)){
				for (Unit mineral : minerals){
						if (!claimedMinerals.contains(mineral)) {
							builder.gather(mineral, false);
							claimedMinerals.add(mineral);
							break;
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
		if (poolDrone != null && poolDrone.isIdle()){
			//send the builder drone to the mineral that will be guarenteed to have the minimum number of other drones gathering from it.
			poolDrone.gather(minerals.get(minerals.size() - 1), false);
		}
	}

	/**
	 * Internal method to eliminate population counts.
	 */
	private void wipePopulations(){
		zealots = new ArrayList<Unit>();
		gateways = new ArrayList<Unit>();
		probes = new ArrayList<Unit>();
		nexus = new ArrayList<Unit>();
	}

	/**
	 * Some UnitTypes will be cached to save on computation.
	 */
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
	 * Builds a unit or a building.
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

	/**
	 * Build a building without providing a position.
	 * @param building
	 * @return
	 */
	private int build(UnitType building){

		if (poolDrone == null || !poolDrone.isExists())
		{
			if (probes.size() == 0){
				return NO_BUILDERS_EXIST;
			}
			poolDrone = probes.get(0);
		}
		return build(building, bwapi.getSelf().getStartLocation());
	}

	/**
	 * Build a building with a start position
	 * TODO pull pyPos logic out of this method
	 * Currently only Pylons are considered to be availible for basePos.
	 * Otherwise position is overwritten with the position of the last pylon placed.
	 * @param building
	 * @param basePos
	 * @return
	 */
	private int build(UnitType building, Position basePos){
		Position buildPos;
		try {
			Unit bestProbe = getBestNUnits(builderType, 1).get(0);
			if (bwapi.getSelf().getMinerals() >= building.getMineralPrice() && bwapi.getSelf().getGas() >= building.getGasPrice()){
				if(building.isRefinery()){
					return buildGasField(building);
				}
				if (myRaceType == RaceType.RaceTypes.Protoss && building != UnitTypes.Protoss_Pylon){
					buildPos = getBuildPosition(pyPos, 50, building, false);
					if (buildPos == null){
						buildPos = getBuildPosition(pyPos, 0, building, true);
					}
				}
				if (building == UnitTypes.Protoss_Photon_Cannon || building == UnitTypes.Terran_Missile_Turret){
					buildPos = getBuildPosition(pyPos, 100, building);
				}
				else {
					buildPos = getBuildPosition(basePos, 0, building);
				}
				if (buildPos == null){
					return NO_SUITABLE_BUILD_LOCATION;
				}
				if(diagnosticMode){
					bwapi.drawLine(bestProbe.getPosition(), buildPos, BWColor.White, false);
				}
				if(buildPos != null) {
					poolDrone.build(buildPos, building);
					if (building == UnitTypes.Protoss_Pylon){
						pyPos = buildPos;
					}
					return SUCCESSFUL;
				}
			}
		}
		catch (IndexOutOfBoundsException e){
			//No probes exist
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
		//Not enough minerals
		return mineralGasDeficit(building);
	}

	/**
	 * Given a UnitType, train it.
	 * @param unit The UnitType to build.
	 * @return the success state of the training.
	 */
	private int train(UnitType unit){

		UnitType buildingThatMakes = UnitTypes.getUnitType(unit.getWhatBuildID());
		try {
			//this should be the building that constructs this unit.
			Unit bestBuilding = getBestNUnits(buildingThatMakes, 1).get(0);
			if (bwapi.getSelf().getMinerals() >= unit.getMineralPrice() && bwapi.getSelf().getGas() >= unit.getGasPrice()){
				bestBuilding.train(unit);
				return SUCCESSFUL;
			}
		}
		catch (IndexOutOfBoundsException e) {
			//No building exists
			System.out.println("Unable to build " +  unit.getName() + " as there is no " + buildingThatMakes + ".");
			return REQUISITE_BUILDING_DOES_NOT_EXIST;
		}
		//Not enough minerals and/or gas
		return mineralGasDeficit(unit);
	}

	/**
	 * Determine what resources is in deficit.
	 * @param unitType The type of Unit attempted to be built or trained.
	 * @return Code describing the deficit.
	 */
	private int mineralGasDeficit(UnitType unitType){
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
	 * If this supply is running low, the build order is overriden and a supply unit is built.
	 * @return
	 */
	private boolean buildSupplyIfNeeded(){
		int supply = bwapi.getSelf().getSupplyUsed();
		int supplyTotal = bwapi.getSelf().getSupplyTotal();
		if (supply + 2 > supplyTotal){
			for (Unit unit : getUnitsOfType(supplyType)){
				if (!unit.isCompleted()){
					//don't allow multiple supply units to be built at once from this method.
					return false;
				}
			}
			build(supplyType);
			return true;
		}
		return false;
	}

	/**
	 * Find the half of the map closest to the center from your base location.
	 * Used to place buildings in a more logical area (hopefully away from minerals too)..
	 * TODO Ensure that buildings are not placed in the path between mineral fields and geysers.
	 */
	private void determineBuildQuadrants(){
		Position base = bwapi.getSelf().getStartLocation();
		buildQuadrants = new ArrayList<Integer>();
		if (getDistanceToEdge(new Position(base.getPX() - 1, base.getPY() + 1)) > getDistanceToEdge(new Position(base.getPX() + 1, base.getPY() - 1))){
			buildQuadrants.add(1);
		}
		else{
			buildQuadrants.add(8);
		}
		if (getDistanceToEdge(new Position(base.getPX() - 0, base.getPY() + 1)) > getDistanceToEdge(new Position(base.getPX() + 0, base.getPY() - 1))){
			buildQuadrants.add(2);
		}
		else{
			buildQuadrants.add(7);
		}
		if (getDistanceToEdge(new Position(base.getPX() + 1, base.getPY() + 1)) > getDistanceToEdge(new Position(base.getPX() - 1, base.getPY() - 1))){
			buildQuadrants.add(3);
		}
		else{
			buildQuadrants.add(6);
		}
		if (getDistanceToEdge(new Position(base.getPX() - 1, base.getPY() + 0)) > getDistanceToEdge(new Position(base.getPX() + 1, base.getPY() - 0))){
			buildQuadrants.add(4);
		}
		else{
			buildQuadrants.add(5);
		}
	}

	/**
	 * Get a position from the quadrants that have been selected as good relative to the center of the map.
	 * @param base The starting position.
	 * @param x_off Displacement in the X direction.
	 * @param y_off Displacement in the Y direction.
	 * @return
	 */
	private List<Position> inBuildQuadrants(Position base, int x_off, int y_off){
		List<Position> positions = new ArrayList<Position>();
		int index = 1;
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() - x_off, base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX(), base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() + x_off, base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() - x_off, base.getPY() - y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX(), base.getPY() + y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() - x_off, base.getPY() - y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX(), base.getPY() - y_off));
		}
		if (buildQuadrants.contains(index++)){
			positions.add(new Position(base.getPX() + x_off, base.getPY() - y_off));
		}
		return positions;
	}

	/**
	 * Determines how far a point is from the edge of the map.
	 * @param coordinate The position of which we are seeking its distance to the edge of the map.
	 * @return The number of pixels from 'coordinate' to the nearest edge.
	 */
	private int getDistanceToEdge(Position coordinate){
		int distanceToEdgeMinX = coordinate.getPX();
		int distanceToEdgeMaxX = bwapi.getMap().getSize().getPX() - coordinate.getPX();
		int distanceToEdgeMinY = coordinate.getPY();
		int distanceToEdgeMaxY = bwapi.getMap().getSize().getPY() - coordinate.getPY();
		return Math.min(Math.min(distanceToEdgeMinX, distanceToEdgeMaxX), Math.min(distanceToEdgeMinY, distanceToEdgeMaxY));
	}
	private boolean isAllCompleted(UnitType type){
		for (Unit unit : getUnitsOfType(type)){
			if (!unit.isCompleted()){
				return false;
			}
		}
		return true;
	}

	private boolean ensureWarped(){
		if (myRaceType == RaceType.RaceTypes.Protoss){
			for (Unit building : bwapi.getMyUnits()){
				if (building.getType().isBuilding() && building.getType() != UnitTypes.Protoss_Nexus && building.getType() != UnitTypes.Protoss_Assimilator){
					if (building.isUnpowered()){
						if(!poolDrone.isConstructing()) {
							build(UnitTypes.Protoss_Pylon, getBuildPosition(building.getPosition(), 0, UnitTypes.Protoss_Pylon, false));
							return false;
						}
					}
				}
			}
		}
		return true;
	}
	private void log(String message){
		//TODO write to a file
		if (diagnosticMode){
			System.out.println(message);
		}
	}
}
