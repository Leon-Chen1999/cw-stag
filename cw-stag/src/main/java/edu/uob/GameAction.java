package edu.uob;

import java.util.ArrayList;

public class GameAction
{
//    private String name;
    private ArrayList<String> triggerList;
    private ArrayList<String> subjectList;
    private ArrayList<String> consumedList;
    private ArrayList<String> producedList;
    private String narration;

    public  GameAction(){
        this.triggerList = new ArrayList<>();
        subjectList = new ArrayList<>();
        consumedList = new ArrayList<>();
        producedList = new ArrayList<>();
    }

    public ArrayList<String> getSubjectList(){
        return subjectList;
    }

    public void addSubjectList(String subject){
        this.subjectList.add(subject);
    }

    public ArrayList<String> getConsumedList(){
        return consumedList;
    }

    public void addConsumedList(String consumed){
        this.consumedList.add(consumed);
    }

    public ArrayList<String> getProducedList(){
        return producedList;
    }

    public void addProducedList(String produced){
        this.producedList.add(produced);
    }

    public String getNarration(){
        return this.narration;
    }

    public void setNarration(String narration){
        this.narration = narration;
    }


}
