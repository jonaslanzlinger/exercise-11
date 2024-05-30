package tools;

import java.util.*;
import java.util.logging.*;
import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import java.lang.Math;

public class QLearner extends Artifact {

  private Lab lab; // the lab environment that will be learnt
  private int stateCount; // the number of possible states in the lab environment
  private int actionCount; // the number of possible actions in the lab environment
  private HashMap<Integer, double[][]> qTables; // a map for storing the qTables computed for different goals

  private static final Logger LOGGER = Logger.getLogger(QLearner.class.getName());

  public void init(String environmentURL) {

    // the URL of the W3C Thing Description of the lab Thing
    this.lab = new Lab(environmentURL);

    this.stateCount = this.lab.getStateCount();
    LOGGER.info("Initialized with a state space of n=" + stateCount);

    this.actionCount = this.lab.getActionCount();
    LOGGER.info("Initialized with an action space of m=" + actionCount);

    qTables = new HashMap<>();
  }

  /**
   * Computes a Q matrix for the state space and action space of the lab, and
   * against
   * a goal description. For example, the goal description can be of the form
   * [z1level, z2Level],
   * where z1Level is the desired value of the light level in Zone 1 of the lab,
   * and z2Level is the desired value of the light level in Zone 2 of the lab.
   * For exercise 11, the possible goal descriptions are:
   * [0,0], [0,1], [0,2], [0,3],
   * [1,0], [1,1], [1,2], [1,3],
   * [2,0], [2,1], [2,2], [2,3],
   * [3,0], [3,1], [3,2], [3,3].
   *
   * <p>
   * HINT: Use the methods of {@link LearningEnvironment} (implemented in
   * {@link Lab})
   * to interact with the learning environment (here, the lab), e.g., to retrieve
   * the
   * applicable actions, perform an action at the lab during learning etc.
   * </p>
   * 
   * @param goalDescription                         the desired goal against the
   *                                                which the Q matrix is
   *                                                calculated (e.g., [2,3])
   * @param episodesObj                             the number of episodes used
   *                                                for calculating the Q
   *                                                matrix
   * @param alphaObj                                the learning rate with range
   *                                                [0,1].
   * @param gammaObj                                the discount factor [0,1]
   * @param epsilonObj                              the exploration probability
   *                                                [0,1]
   * @param rewardObj                               the reward assigned when
   *                                                reaching the goal state
   * @param manifestGoalDescriptionInRealLabBoolean whether the goal description
   *                                                is to be manifested in the
   *                                                real lab or the simulated lab
   **/
  @OPERATION
  public void calculateQ(Object[] goalDescription, Object episodesObj, Object alphaObj, Object gammaObj,
      Object epsilonObj, Object rewardObj, Object manifestGoalDescriptionInRealLabBoolean) {

    // ensure that the right datatypes are used
    Integer episodes = Integer.valueOf(episodesObj.toString());
    Double alpha = Double.valueOf(alphaObj.toString());
    Double gamma = Double.valueOf(gammaObj.toString());
    Double epsilon = Double.valueOf(epsilonObj.toString());
    Integer reward = Integer.valueOf(rewardObj.toString());
    Boolean manifestGoalDescriptionInRealLab = Boolean.valueOf(manifestGoalDescriptionInRealLabBoolean.toString());

    // Get all compatible states of the goal description
    List<Integer> compatibleStates = lab.getCompatibleStates(Arrays.asList(goalDescription));

    // Initialize a Q-Table
    double[][] qTable = initializeQTable();

    // Iterate through a number of episodes to update the Q-table
    for (int episode = 0; episode < episodes; episode++) {

      System.out.println("=============================== Episode: " + episode);

      // Initialize randomly by performing 8 random actions on the environment
      for (int i = 0; i < 4; i++) {
        lab.performAction((int) (Math.random() * actionCount));
      }

      while (!compatibleStates.contains(lab.readCurrentState())) {

        System.out.println("===============================");

        // Get the current state
        int currentState = lab.readCurrentState();

        // Get all applicable actions
        List<Integer> applicableActions = lab.getApplicableActions(currentState);

        // Choose A from S (using epsilon-greedy policy based on Q)
        int bestAction = 0;
        if (Math.random() < epsilon) {
          /* Exploration */
          bestAction = applicableActions.get((int) (Math.random() * applicableActions.size()));
        } else {
          /* Exploitation */
          double[] bestActionIndexAndMaxQ = getBestActionIndexAndMaxQByState(qTable, currentState,
              applicableActions);
          bestAction = (int) bestActionIndexAndMaxQ[0];
        }

        // Perform action, ...
        lab.performAction(bestAction);

        // ... observe S'
        int newState = lab.readCurrentState();

        // ... observe R
        // The state is rewarded the reward if it is a compatible state of the goal,
        // otherwise it is rewarded -1.
        double immediateReward = compatibleStates.contains(newState) ? reward : -1;

        // Q(S,A) <- Q(S,A) + alpha * [immediateReward + gamma * maxQ(S',a) - Q(S,A)]
        List<Integer> applicableActionsNewState = lab.getApplicableActions(newState);
        double[] bestActionIndexAndMaxQNewState = getBestActionIndexAndMaxQByState(qTable, newState,
            applicableActionsNewState);
        double maxQValueNewState = bestActionIndexAndMaxQNewState[1];
        qTable[currentState][bestAction] = qTable[currentState][bestAction]
            + alpha * (immediateReward + gamma * maxQValueNewState - qTable[currentState][bestAction]);
        System.out.println(
            "Q-Table updated => qTable[" + currentState + "][" + bestAction + "]: " + qTable[currentState][bestAction]);

        // S <- S'
        // ... nothing to do here

        System.out.println("Current state: " + Arrays.toString(getStateDescriptionByIndex(currentState)));
        System.out.println("Best action: " + lab.getAction(bestAction));
        System.out.println("New state: " + Arrays.toString(getStateDescriptionByIndex(newState)));
      }
    }

    // Append the Q-Table to the qTables map with the hashed goal description as key
    qTables.put(Arrays.hashCode(goalDescription), qTable);

    printQTable(qTable);

    // Show how many lines in the qtable are not 0
    int count = 0;
    for (int i = 0; i < qTable.length; i++) {
      for (int j = 0; j < qTable[i].length; j++) {
        if (qTable[i][j] != 0) {
          count++;
        }
      }
    }
    System.out.println("Number of non-zero values in QTable: " + count);

    // If the goal description is to be manifested in the simulated lab, then
    // randomize the environment again,
    // otherwise the environment will already be in the goal state, and manifesting
    // the goal description is not necessary.
    if (!manifestGoalDescriptionInRealLab) {
      for (int i = 0; i < 8; i++) {
        lab.performAction((int) (Math.random() * actionCount));
      }
    }
  }

  public double[] getBestActionIndexAndMaxQByState(double[][] qTable, int state,
      List<Integer> applicableActions) {
    int bestAction = 0;
    double maxQValue = Double.NEGATIVE_INFINITY;
    for (int i = 0; i < applicableActions.size(); i++) {
      int action = applicableActions.get(i);
      if (qTable[state][action] > maxQValue) {
        bestAction = action;
        maxQValue = qTable[state][action];
      }
    }
    return new double[] { bestAction, maxQValue };
  }

  private int[] getStateDescriptionByIndex(int stateIndex) {
    List<List<Integer>> stateSpaceList = new ArrayList<>(lab.stateSpace);
    return stateSpaceList.get(stateIndex).stream().mapToInt(i -> i).toArray();
  }

  private int getStateIndexByDescription(Object[] stateDescription) {
    List<Integer> stateList = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      stateList.add(((Byte) stateDescription[i]).intValue());
    }
    for (int i = 2; i < 6; i++) {
      stateList.add((boolean) stateDescription[i] ? 1 : 0);
    }
    stateList.add(((Byte) stateDescription[6]).intValue());
    List<List<Integer>> stateSpaceList = new ArrayList<>(lab.stateSpace);
    return stateSpaceList.indexOf(stateList);
  }

  @OPERATION
  public void discretizeLightLevel(Object lightLevel, OpFeedbackParam<Object[]> discretizedLightLevel) {
    double value = ((Number) lightLevel).doubleValue();
    if (value < 50) {
      discretizedLightLevel.set(new Object[] { 0 });
    } else if (value < 100) {
      discretizedLightLevel.set(new Object[] { 1 });
    } else if (value < 300) {
      discretizedLightLevel.set(new Object[] { 2 });
    } else {
      discretizedLightLevel.set(new Object[] { 3 });
    }
  }

  @OPERATION
  public void discretizeSunshine(Object sunshine, OpFeedbackParam<Object[]> discretizedSunshine) {
    double value = ((Number) sunshine).doubleValue();
    if (value < 50) {
      discretizedSunshine.set(new Object[] { 0 });
    } else if (value < 200) {
      discretizedSunshine.set(new Object[] { 1 });
    } else if (value < 700) {
      discretizedSunshine.set(new Object[] { 2 });
    } else {
      discretizedSunshine.set(new Object[] { 3 });
    }
  }

  /**
   * Returns information about the next best action based on a provided state and
   * the QTable for
   * a goal description. The returned information can be used by agents to invoke
   * an action
   * using a ThingArtifact.
   *
   * @param goalDescription           the desired goal against the which the Q
   *                                  matrix is calculated (e.g., [2,3])
   * @param currentStateDescription   the current state e.g.
   *                                  [2,2,true,false,true,true,2]
   * @param nextBestActionTag         the (returned) semantic annotation of the
   *                                  next best action, e.g.
   *                                  "http://example.org/was#SetZ1Light"
   * @param nextBestActionPayloadTags the (returned) semantic annotations of the
   *                                  payload of the next best action, e.g.
   *                                  [Z1Light]
   * @param nextBestActionPayload     the (returned) payload of the next best
   *                                  action, e.g. true
   **/
  @OPERATION
  public void getActionFromState(Object[] goalDescription, Object[] currentStateDescription,
      OpFeedbackParam<String> nextBestActionTag, OpFeedbackParam<Object[]> nextBestActionPayloadTags,
      OpFeedbackParam<Object[]> nextBestActionPayload) {

    // Get qTable from qTables map with the goal description as key
    double[][] qTable = qTables.get(Arrays.hashCode(goalDescription));

    System.out.println("Goal Description: " + Arrays.toString(goalDescription));
    System.out.println("Current state: " + Arrays.toString(currentStateDescription));

    // Get current state index
    int currentStateIndex = getStateIndexByDescription(currentStateDescription);

    // Get all applicable actions
    List<Integer> applicableActions = lab.getApplicableActions(currentStateIndex);

    // Get best action from qTable based on current state
    double[] bestActionIndexAndMaxQ = getBestActionIndexAndMaxQByState(qTable, currentStateIndex, applicableActions);

    // Set the next best action tag, payload tags, and payload
    nextBestActionTag.set(lab.getAction((int) bestActionIndexAndMaxQ[0]).getActionTag());
    nextBestActionPayloadTags.set(lab.getAction((int) bestActionIndexAndMaxQ[0]).getPayloadTags());
    nextBestActionPayload.set(lab.getAction((int) bestActionIndexAndMaxQ[0]).getPayload());
  }

  /**
   * Print the Q matrix
   *
   * @param qTable the Q matrix
   */
  void printQTable(double[][] qTable) {
    System.out.println("Q matrix");
    for (int i = 0; i < qTable.length; i++) {
      System.out.print("From state " + i + ":  ");
      for (int j = 0; j < qTable[i].length; j++) {
        System.out.printf("%6.2f ", (qTable[i][j]));
      }
      System.out.println();
    }
  }

  /**
   * Initialize a Q matrix
   *
   * @return the Q matrix
   */
  private double[][] initializeQTable() {
    double[][] qTable = new double[this.stateCount][this.actionCount];
    for (int i = 0; i < stateCount; i++) {
      for (int j = 0; j < actionCount; j++) {
        qTable[i][j] = 0.0;
      }
    }
    return qTable;
  }
}
