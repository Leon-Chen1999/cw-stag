package edu.uob.Entities;

import edu.uob.GameEntity;

import java.util.HashSet;

public class PlayerEntity extends GameEntity {
    private int health;
    private HashSet<ArtefactEntity> playerInv;

    private LocationEntity currentLocation;
    public PlayerEntity(String name, String description, int health) {
        super(name, description);
        this.health = health;
        this.playerInv = new HashSet<ArtefactEntity>();
        currentLocation = new LocationEntity("", "");
    }

    public void setHealth(int health) {
        this.health = health;
    }

    public int getHealth() {
        return health;
    }

    public void addPlayerInv(ArtefactEntity artefactEntity){
        playerInv.add(artefactEntity);
    }

    public void dropPlayerInv(ArtefactEntity artefactEntity){
        playerInv.remove(artefactEntity);
    }

    public HashSet<ArtefactEntity> getPlayerInv(){
        return playerInv;
    }

    public void setCurrentLocation(LocationEntity currentLocation){
        this.currentLocation = currentLocation;
    }

    public LocationEntity getCurrentLocation(){
        return currentLocation;
    }

}
