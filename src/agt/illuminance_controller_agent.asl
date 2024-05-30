//illuminance controller agent

/*
* The URL of the W3C Web of Things Thing Description (WoT TD) of a lab environment
* Simulated lab WoT TD: "https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab.ttl"
* Real lab WoT TD: Get in touch with us by email to acquire access to it!
*/

/* Initial beliefs and rules */

// the agent has a belief about the location of the W3C Web of Thing (WoT) Thing Description (TD)
// that describes a lab environment to be learnt
learning_lab_environment("https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab.ttl").
real_lab_environment("https://raw.githubusercontent.com/Interactions-HSG/example-tds/was/tds/interactions-lab-real.ttl").

// the agent believes that the task that takes place in the 1st workstation requires an indoor illuminance
// level of Rank 2, and the task that takes place in the 2nd workstation requires an indoor illumincance 
// level of Rank 3. Modify the belief so that the agent can learn to handle different goals.
// Here are all goalDescriptions
// training_goal_descriptions([0,0],[0,1],[0,2],[0,3],[1,0],[1,1],[1,2],[1,3],[2,0],[2,1],[2,2],[2,3],[3,0],[3,1],[3,2],[3,3]).
// Here only a selection for faster learning
// training_goal_descriptions([[0,2],[2,1],[3,3]]).
// Here only one goal for even faster learning
training_goal_descriptions([[2,3]]).
// Here is the real goal description, which should be manifested in the real lab
manifest_goal_description([2,3]).
// Here is a belief to control whether manifestation of the goal description should take place in the real lab or only in the simulated lab
manifest_goal_description_in_real_lab_boolean(false).

/* Initial goals */
!start. // the agent has the goal to start

/* 
 * Plan for reacting to the addition of the goal !start
 * Triggering event: addition of goal !start
 * Context: the agent believes that there is a WoT TD of a lab environment located at Url, and that 
 * the tasks taking place in the workstations require indoor illuminance levels of Rank Z1Level and Z2Level
 * respectively
 * Body: (currently) creates a QLearnerArtifact and a ThingArtifact for learning and acting on the lab environment.
*/
@start
+!start : learning_lab_environment(Url)
  & real_lab_environment(UrlReal)
  & training_goal_descriptions(GoalDescriptionList)
  & manifest_goal_description(ManifestGoalDescription)
  & manifest_goal_description_in_real_lab_boolean(ManifestGoalDescriptionInRealLabBoolean) <-
  
  .print("Hello world");

  // creates a QLearner artifact for learning the lab Thing described by the W3C WoT TD located at URL
  makeArtifact("qlearner", "tools.QLearner", [Url], QLArtId);

  // Loop through all possible goalDescriptions in the list
  for ( .member([Z1Level,Z2Level],GoalDescriptionList) ) {
    .print("I want to achieve Z1Level=", Z1Level, " and Z2Level=",Z2Level);
    calculateQ([Z1Level,Z2Level], 200, 0.2, 0.9, 0.3, 100, ManifestGoalDescriptionInRealLabBoolean)[artifact_id(QLArtId)];
  }
  
  // creates a ThingArtifact artifact for reading and acting on the state of the lab Thing
  // Note: the ThingArtifact is created either for the simulated lab or the real lab, 
  // depending on the value of the manifest_goal_description_in_real_lab_boolean belief
  if (ManifestGoalDescriptionInRealLabBoolean) {
    makeArtifact("lab", "org.hyperagents.jacamo.artifacts.wot.ThingArtifact", [UrlReal], LabArtId);
  } else {
    makeArtifact("lab", "org.hyperagents.jacamo.artifacts.wot.ThingArtifact", [Url], LabArtId);
  }

  // Parse the ManifestGoalDescription into variables
  .nth(0, ManifestGoalDescription, ManifestGoalDescriptionZ1Level);
  .nth(1, ManifestGoalDescription, ManifestGoalDescriptionZ2Level);

  // In this loop, the agent tries to manifest the goal description in the real lab
  // The agent reads the state of the lab, discretizes the state, and compares it to the goal description
  // If the state is not the goal description, the agent takes an action to change the state of the lab
  // The agent waits for the lab to change and repeats the process
  // The agent stops after 5 unsuccessful attempts
  -+goal_description_manifested(false);
  for ( .range(I,1,5) & goal_description_manifested(TrueFalse) & TrueFalse == false ) {

    .print("=============================== Manifesting Try: ", I);

    readProperty("https://example.org/was#Status", Key, Value)[artifact_id(LabArtId)];
    .print("Key=", Key, " Value=", Value);

    // Parse the values from the response according to the value in ManifestGoalDescriptionInRealLabBoolean variable.
    if (ManifestGoalDescriptionInRealLabBoolean) {
      .nth(0, Value, Z2Level);
      .nth(1, Value, Z1Blinds);
      .nth(2, Value, Sunshine);
      .nth(3, Value, Z2Light);
      .nth(4, Value, Z1Light);
      .nth(5, Value, Z2Blinds);
      .nth(6, Value, Z1Level);
      .print("Z1Level=", Z1Level, " Z2Level=", Z2Level, " Z1Blinds=", Z1Blinds, " Sunshine=", Sunshine, " Z2Light=", Z2Light, " Z1Light=", Z1Light, " Z2Blinds=", Z2Blinds);
    } else {
      .nth(0, Value, Z2Level);
      .nth(1, Value, Z1Blinds);
      .nth(2, Value, Hour);
      .nth(3, Value, Sunshine);
      .nth(4, Value, Z2Light);
      .nth(5, Value, Z1Light);
      .nth(6, Value, Z2Blinds);
      .nth(7, Value, Z1Level);
      .print("Z1Level=", Z1Level, " Z2Level=", Z2Level, " Z1Blinds=", Z1Blinds, " Hour=", Hour, " Sunshine=", Sunshine, " Z2Light=", Z2Light, " Z1Light=", Z1Light, " Z2Blinds=", Z2Blinds);
    }

    // Pre-process the values
    discretizeLightLevel(Z1Level, DiscZ1Level);
    discretizeLightLevel(Z2Level, DiscZ2Level);
    discretizeSunshine(Sunshine, DiscSunshine);
    .nth(0, DiscZ2Level, DiscretizedZ2Level);
    .nth(0, DiscZ1Level, DiscretizedZ1Level);
    .nth(0, DiscSunshine, DiscretizedSunshine);

    if (DiscretizedZ1Level == ManifestGoalDescriptionZ1Level & DiscretizedZ2Level == ManifestGoalDescriptionZ2Level) {
      .print("SUCCESS => Z1Level=", DiscretizedZ1Level, " Z2Level=", DiscretizedZ2Level, " ManifestGoalDescriptionZ1Level=", ManifestGoalDescriptionZ1Level, " ManifestGoalDescriptionZ2Level=", ManifestGoalDescriptionZ2Level);
      -+goal_description_manifested(true);
    } else {
      getActionFromState(ManifestGoalDescription, [DiscretizedZ1Level,DiscretizedZ2Level,Z1Light,Z2Light,Z1Blinds,Z2Blinds,DiscretizedSunshine], ActionTag, PayloadTags, Payload);
      .print("Perform action => ActionTag=", ActionTag, " PayloadTags=", PayloadTags, " Payload=", Payload);
      invokeAction(ActionTag, PayloadTags, Payload)[artifact_id(LabArtId)];
      // wait for the lab to change (e.g. raise blinds and sensor to measure new illuminance level)
      // Note: the agent should wait for a sufficient amount of time for the lab to change
      // This is 60 seconds in the real lab, can be reduced to 1 second in the simulated lab
      if (ManifestGoalDescriptionInRealLabBoolean) {
        .wait(60000);
      } else {
        .wait(1000);
      }
    }
  }
  -+goal_description_manifested(false).
