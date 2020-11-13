package com.aiad.agents;

import com.aiad.messages.ArrivingAirplaneRequest;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;

import java.io.IOException;

public class DepartingAirplane extends Airplane {


    public DepartingAirplane(int id, int timeToArrive) {
        super(id, timeToArrive);
    }

}
