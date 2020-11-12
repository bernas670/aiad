package com.aiad.agents;

import com.aiad.messages.ArrivingAirplaneRequest;
import com.aiad.messages.RunwayOperationCfp;
import com.aiad.messages.RunwayOperationProposal;
import jade.core.*;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFSubscriber;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.UnreadableException;
import jade.proto.ContractNetInitiator;
import jade.proto.SubscriptionInitiator;

import java.io.IOException;
import java.lang.module.FindException;
import java.util.ArrayList;
import java.util.Vector;

public class ControlTower extends Agent {

    private int arrivalCounter = 0, departureCounter = 0;
    protected ArrayList<AID> runways;

    public ControlTower() {
        runways = new ArrayList<>();
    }

    public void handleMessage(ACLMessage message) throws UnreadableException {

        switch (message.getPerformative()) {
            case ACLMessage.REQUEST:

                System.out.println("Request received");

                int airplaneId, minTime;

                // get airplane information
                ArrivingAirplaneRequest airplaneRequest = (ArrivingAirplaneRequest) message.getContentObject();
                airplaneId = airplaneRequest.getId();
                minTime = airplaneRequest.getEta();

                // start contractnet
                ACLMessage cfp = new ACLMessage(ACLMessage.CFP);

                RunwayOperationCfp cfpContent = new RunwayOperationCfp();
                cfpContent.setAirplaneId(airplaneId);
                cfpContent.setMinTime(minTime);

                try {
                    cfp.setContentObject(cfpContent);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                this.addBehaviour(new RunwayAllocator(this, cfp));
                // check result of contractnet

                // if can land
                    // send agree (can land)
                    // if ok
                        // send inform (runway number)
                    // else
                        // send failure (try again/go to another airport)
                // else
                    // send refuse (try again later/go to another airport)




                // can send refuse or agree
                // Send agree
                ACLMessage reply = new ACLMessage(ACLMessage.AGREE);
                reply.setContent("Esta mensagem é um Agree para o avião");
                reply.addReceiver(message.getSender());
                reply.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                send(reply);
                //Send notification
                ACLMessage notification = new ACLMessage(ACLMessage.INFORM);
                notification.setContent("Esta mensagem é um INFORM para o avião");
                notification.addReceiver(message.getSender());
                notification.setProtocol(FIPANames.InteractionProtocol.FIPA_REQUEST);
                send(notification);
                break;
            default:
                System.err.println("NOT A REQUEST");
                System.out.println(message);
                break;
        }
    }

    @Override
    protected void setup() {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());
        ServiceDescription service = new ServiceDescription();
        service.setName("control_tower");
        service.setType("control_tower");
        description.addServices(service);

        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            e.printStackTrace();
        }

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                MessageTemplate msgTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
                //Receive message
                ACLMessage msg = receive(msgTemplate);
                if (msg != null) {
                    try {
                        handleMessage(msg);
                    } catch (UnreadableException e) {
                        e.printStackTrace();
                    }
                } else block();
            }
        });

        DFAgentDescription runwayTemplate = new DFAgentDescription();
        ServiceDescription runwayService = new ServiceDescription();
        runwayService.setType("runway");
        runwayTemplate.addServices(runwayService);

        addBehaviour(new RunwaySubscriber(this, runwayTemplate));
    }

    public int getTotalArrivals() {
        return this.arrivalCounter;
    }

    public int getTotalDepartures() {
        return this.departureCounter;
    }

    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    class RunwaySubscriber extends SubscriptionInitiator {
        public RunwaySubscriber(ControlTower agent, DFAgentDescription template) {
            super(agent, DFService.createSubscriptionMessage(agent, getDefaultDF(), template, null));
        }

        protected void handleInform(ACLMessage message) {
            ControlTower controlTower = (ControlTower) myAgent;

            try {
                DFAgentDescription[] dfds = DFService.decodeNotification(message.getContent());

                for (int i = 0; i < dfds.length; i++) {
                    AID agent = dfds[i].getName();
                    controlTower.runways.add(agent);
                    System.out.println("new runway added : " + agent.getLocalName());
                }
            } catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }

    }

    class RunwayAllocator extends ContractNetInitiator {
        public RunwayAllocator(Agent a, ACLMessage cfp) {
            super(a, cfp);
        }

        protected Vector prepareCfps(ACLMessage cfp) {
            ArrayList<AID> runways = ((ControlTower) myAgent).runways;
            Vector v = new Vector();

            for (int i = 0; i < runways.size(); i++) {
                cfp.addReceiver(runways.get(i));
            }

            System.out.println("CFPs prepared");

            v.add(cfp);
            return v;
        }

        @Override
        protected void handleAllResponses(Vector responses, Vector acceptances) {
            int bestProposalIndex = 0;
            int minOperationTime = Integer.MAX_VALUE;

            System.out.println("Received all " + responses.size() + " proposals");

            try {
                minOperationTime = ((RunwayOperationProposal) ((ACLMessage) responses.get(0)).getContentObject()).getOperationTime();
            } catch (UnreadableException e) {
                e.printStackTrace();
            }

            for (int i = 1; i < responses.size(); i++) {

                ACLMessage message = (ACLMessage) responses.get(i);
                int runwayId = -1;
                int operationTime = Integer.MAX_VALUE;
                try {
                    RunwayOperationProposal proposal = (RunwayOperationProposal) message.getContentObject();
                    runwayId = proposal.getRunwayId();
                    operationTime = proposal.getOperationTime();
                } catch (UnreadableException e) {
                    e.printStackTrace();
                }


                // compare best proposal with current proposal
                boolean isBetter = minOperationTime > operationTime;
                System.out.println(minOperationTime + " > " + operationTime);

                int rejectedProposalIndex = i;

                if (isBetter) {
                    rejectedProposalIndex = bestProposalIndex;      // previous best proposal index
                    bestProposalIndex = i;                          // new best proposal index
                }

                // create reply for rejected proposal
                ACLMessage reply = ((ACLMessage) responses.get(rejectedProposalIndex)).createReply();
                reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                reply.setContent("proposal rejected");
                acceptances.add(reply);
            }

            System.out.println("All proposals considered");
            System.out.println("Best proposal: " + minOperationTime);

            // create reply for accepted proposal
            ACLMessage reply = ((ACLMessage) responses.get(bestProposalIndex)).createReply();
            reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            reply.setContent("proposal accepted");
            acceptances.add(reply);
        }

        // TODO: method to receive inform messages related to the activity
    }
}
