package Agents;
import jade.core.*;

public class Runway extends Agent {

    int id;
    boolean isClear;

    public Runway(){}

    /*
     *   The message will be the following :
     *   " id isClear"
     *
     */
    public Runway(String message){
        String[] splitMessage = message.split(" ");
        this.id = Integer.parseInt(splitMessage[0]);
        this.isClear = splitMessage[1].equals("true");
    }

    public int getId(){
        return this.id;
    }
    public boolean isClear(){
        return this.isClear;
    }
}