package edu.uob;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.nio.file.Paths;
import java.io.IOException;
import java.time.Duration;

class ExampleSTAGTests {

  private GameServer server;

  // Create a new server _before_ every @Test
  @BeforeEach
  void setup() {
      File entitiesFile = Paths.get("config" + File.separator + "extended-entities.dot").toAbsolutePath().toFile();
      File actionsFile = Paths.get("config" + File.separator + "extended-actions.xml").toAbsolutePath().toFile();
      server = new GameServer(entitiesFile, actionsFile);
  }

  String sendCommandToServer(String command) {
      // Try to send a command to the server - this call will timeout if it takes too long (in case the server enters an infinite loop)
      return assertTimeoutPreemptively(Duration.ofMillis(1000), () -> { return server.handleCommand(command);},
      "Server took too long to respond (probably stuck in an infinite loop)");
  }

  // A lot of tests will probably check the game state using 'look' - so we better make sure 'look' works well !
  @Test
  void testLook() {
    String response = sendCommandToServer("simon: look");
    response = response.toLowerCase();
    assertTrue(response.contains("cabin"), "Did not see the name of the current room in response to look");
    assertTrue(response.contains("log cabin"), "Did not see a description of the room in response to look");
    assertTrue(response.contains("magic potion"), "Did not see a description of artifacts in response to look");
    assertTrue(response.contains("wooden trapdoor"), "Did not see description of furniture in response to look");
    assertTrue(response.contains("forest"), "Did not see available paths in response to look");
  }

  // Test that we can pick something up and that it appears in our inventory
  @Test
  void testGet()
  {
      String response;
      sendCommandToServer("simon: get potion");
      response = sendCommandToServer("simon: inv");
      response = response.toLowerCase();
      assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
      response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
  }

  // Test that we can goto a different location (we won't get very far if we can't move around the game !)
  @Test
  void testGoto()
  {
      sendCommandToServer("simon: goto forest");
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
  }

    // Add more unit tests or integration tests here.

    @Test
    void testThrowGet()
    {
        String response;
        response = sendCommandToServer("simon: get");
        response = response.toLowerCase();
        assertTrue(response.contains("[error]"),"the syntax is wrong");
        response = sendCommandToServer("simon: get log");
        response = response.toLowerCase();
        assertTrue(response.contains("[error]"),"log is not here");
        response = sendCommandToServer("simon: get coin and potion");
        response = response.toLowerCase();
        assertTrue(response.contains("[error]"),"the syntax is wrong");
        sendCommandToServer("simon: potion get ");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertFalse(response.contains("potion"),"wrong order");
        sendCommandToServer("simon: get potion");
        response = sendCommandToServer("simon: get potion");
        response = response.toLowerCase();
        assertTrue(response.contains("[error]"),"already have it");
    }

    @Test
    void testBasicDrop()
    {
        String response;
        sendCommandToServer("simon: get potion");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertFalse(response.contains("potion"), "Potion is still present in the room after an attempt was made to get it");
        sendCommandToServer("simon: drop potion");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertFalse(response.contains("potion"), "Potion is dropped from inv");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Potion is dropped here");
    }

    @Test
    void testThrowDrop(){
        String response;
        sendCommandToServer("simon: get potion");
        sendCommandToServer("simon: inv");
        sendCommandToServer("simon: potion drop");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Potion is not dropped from inv");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Potion is not dropped from inv");
        response = sendCommandToServer("simon: drop log");
        response = response.toLowerCase();
        assertTrue(response.contains("[error]"), "log is not dropped from inv");
    }

  @Test
    void testOpen()
  {
      sendCommandToServer("simon: goto forest");
      String response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
      sendCommandToServer("simon: get key");
      response = sendCommandToServer("simon: inv");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Did not see the key in the inventory after an attempt was made to get it");
      sendCommandToServer("simon: goto cabin");
      sendCommandToServer("simon: open trapdoor");
      response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("cellar"), "there need be a cellar in the path");
      response = sendCommandToServer("simon: goto cellar");
      response = response.toLowerCase();
      assertTrue(response.contains("elf"), "Failed attempt to use 'goto' command to move to the cellar - there is no key in the current location");
  }

  @Test
  void testThrowOpenOne(){
      String response;
      sendCommandToServer("simon: goto forest");
      sendCommandToServer("simon: get key");
      response = sendCommandToServer("simon: open trapdoor");
      response = response.toLowerCase();
      assertTrue(response.contains("[error]"), "door is not here");
      response = sendCommandToServer("simon: inv");
      response = response.toLowerCase();
      assertTrue(response.contains("key"), "Did not see the key in the inventory after an attempt was made to get it");
      sendCommandToServer("simon: goto cabin");
      sendCommandToServer("simon: open trapdoor");
      response = sendCommandToServer("simon: look");
      response = response.toLowerCase();
      assertTrue(response.contains("cellar"), "there need be a cellar in the path");
  }

    @Test
    void testThrowOpenTwo(){
        String response;
        sendCommandToServer("simon: open trapdoor");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertFalse(response.contains("cellar"), "there need a key");
    }

    @Test
    void testUnlock()
    {
        sendCommandToServer("simon: goto forest");
        String response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
        sendCommandToServer("simon: get key");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Did not see the key in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: unlock trapdoor");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("cellar"), "there need be a cellar in the path");
        response = sendCommandToServer("simon: goto cellar");
        response = response.toLowerCase();
        assertTrue(response.contains("elf"), "Failed attempt to use 'goto' command to move to the cellar - there is no key in the current location");
    }

    @Test
    void testChop(){
        String response;
        sendCommandToServer("simon: get axe");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: chop tree with axe");//todo change the syntax
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("log"), "there need be a log in the forest");
        response = sendCommandToServer("simon: inv");//todo check axe whether consumed
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
    }

    @Test
    void testChopTwo(){
        String response;
        sendCommandToServer("simon: get axe");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: chop tree ");//todo change the syntax
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("log"), "there need be a log in the forest");
        response = sendCommandToServer("simon: inv");//todo check axe whether consumed
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
    }

    @Test
    void testChopThree(){
        String response;
        sendCommandToServer("simon: get axe");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: chop axe ");//todo change the syntax
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("log"), "there need be a log in the forest");
        response = sendCommandToServer("simon: inv");//todo check axe whether consumed
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
    }

    @Test
    void testThrowChopOne(){
        String response;
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: chop tree with axe");
        response = sendCommandToServer("simon look ");
        response = response.toLowerCase();
        assertTrue(response.contains("tree"), "Did not chop the tree");
    }
    @Test
    void testThrowChopTwo(){
        String response;
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: chop tree with axe");
        response = sendCommandToServer("simon look ");
        response = response.toLowerCase();
        assertFalse(response.contains("tree"), "Did not chop the tree");
        response = sendCommandToServer("simon: chop tree with axe");
        response = response.toLowerCase();
        assertTrue(response.contains("[error]"), "Did not see the tree");
    }

    @Test
    void testCut(){
        String response;
        sendCommandToServer("simon: get axe");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: cut tree with axe");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("log"), "there need be a log in the forest");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "Did not see the axe in the inventory after an attempt was made to get it");
    }

    @Test
    void testBasicDrink(){
        String response;
        sendCommandToServer("simon: get potion");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("potion"), "Did not see the potion in the inventory after an attempt was made to get it");
        response = sendCommandToServer("simon: drink potion");
        response = response.toLowerCase();
        assertTrue(response.contains("3"), "drink false");
    }

    @Test
    void testFight(){
        sendCommandToServer("simon: goto forest");
        String response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
        sendCommandToServer("simon: get key");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Did not see the key in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: unlock trapdoor");
        sendCommandToServer("simon: look");
        sendCommandToServer("simon: goto cellar");
        response = sendCommandToServer("simon: fight elf");
        response = response.toLowerCase();
        assertTrue(response.contains("2"), "You should lose 1 health");
    }

    @Test
    void testHit(){
        sendCommandToServer("simon: goto forest");
        String response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Failed attempt to use 'goto' command to move to the forest - there is no key in the current location");
        sendCommandToServer("simon: get key");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("key"), "Did not see the key in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: unlock trapdoor");
        sendCommandToServer("simon: look");
        sendCommandToServer("simon: goto cellar");
        response = sendCommandToServer("simon: hit elf");
        response = response.toLowerCase();
        assertTrue(response.contains("2"), "You should lose 1 health");
        response = sendCommandToServer("simon: attack elf");
        response = response.toLowerCase();
        assertTrue(response.contains("1"), "You should lose 1 health");
        response = sendCommandToServer("simon: hit elf");
        response = response.toLowerCase();
        assertTrue(response.contains("died"), "You should lose 1 health");
    }

    @Test
    void testPay(){
        String response;
        sendCommandToServer("simon: get coin");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("coin"), "Did not see the coin in the inventory after an attempt was made to get it");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: unlock trapdoor");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("simon: pay elf");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("shovel"), "it will give you a shovel");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertFalse(response.contains("coin"), "coin will be consumed");
        sendCommandToServer("simon: get shovel");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("shovel"), "Did not see the shovel in the inventory after an attempt was made to get it");
    }

    @Test
    void testBridge(){
        String response;
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: cut tree with axe");
        sendCommandToServer("simon: get log");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("simon: bridge river");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("clearing"), "get path to clearing");
        response = sendCommandToServer("simon: goto clearing");
        response = response.toLowerCase();
        assertTrue(response.contains("clearing"), "goto clearing");
    }

    @Test
    void testDig(){
        String response;
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: get coin");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: cut tree with axe");
        sendCommandToServer("simon: get log");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: open trapdoor");
        sendCommandToServer("simon: goto cellar");
        sendCommandToServer("simon: pay elf");
        sendCommandToServer("simon: get shovel");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("simon: bridge river");
        sendCommandToServer("simon: goto clearing");
        response = sendCommandToServer("simon: dig ground");
        response = response.toLowerCase();
        assertTrue(response.contains("ground"), "dig false");
        assertTrue(response.contains("gold"), "dig false");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("gold"), "produce a lot of gold");
        assertTrue(response.contains("hole"), "create a hole");
        sendCommandToServer("simon: get gold");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertTrue(response.contains("gold"), "get the gold into inv");
    }

    @Test
    void testBlow(){
        String response;
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: goto riverbank");
        sendCommandToServer("simon: blow horn");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("wood cutter"), "create a lumberjack");
    }

    @Test
    void testDie(){
        String response;
        sendCommandToServer("simon: goto forest");
        sendCommandToServer("simon: get key");
        sendCommandToServer("simon: goto cabin");
        sendCommandToServer("simon: get coin");
        sendCommandToServer("simon: get axe");
        sendCommandToServer("simon: open trapdoor");
        sendCommandToServer("simon: goto cellar");
        response = sendCommandToServer("simon: hit elf");
        response = response.toLowerCase();
        assertTrue(response.contains("2"), "You should lose 1 health");
        response = sendCommandToServer("simon: attack elf");
        response = response.toLowerCase();
        assertTrue(response.contains("1"), "You should lose 1 health");
        response = sendCommandToServer("simon: hit elf");
        response = response.toLowerCase();
        assertTrue(response.contains("died"), "You lose the game");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("cabin"), "return init location");
        sendCommandToServer("simon: goto cellar");
        response = sendCommandToServer("simon: look");
        response = response.toLowerCase();
        assertTrue(response.contains("axe"), "lose the entity in inv");
        assertTrue(response.contains("coin"), "lose the entity in inv");
        response = sendCommandToServer("simon: inv");
        response = response.toLowerCase();
        assertFalse(response.contains("axe"), "lose the entity in inv");
        assertFalse(response.contains("coin"), "lose the entity in inv");
    }
}
