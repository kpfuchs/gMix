package outputStrategy;

import java.security.SecureRandom;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import message.Message;
import message.Reply;
import message.Request;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistributionImpl;

import architectureInterface.OutputStrategyInterface;
import framework.Implementation;


//Diaz 2003 ("Generalising Mixes")
//"a timed pool mix that tosses coins and uses a probability function that 
//depends on the number of messages inside the mix at the time of flushing"
//-> normal cumulative distribution
public class BinomialPool extends Implementation implements OutputStrategyInterface {

	private static SecureRandom secureRandom = new SecureRandom();
	private SimplexBinomialPool requestPool;
	private SimplexBinomialPool replyPool;

	
	@Override
	public void constructor() {
		
		int sendingRate = 1000; // TODO: property file!
		double maxOutputFraction = 0.7d; // TODO: property file!
		double mean = 100; // TODO: property file!
		double stdDev = 30; // TODO: property file!
		this.requestPool = new SimplexBinomialPool(true, sendingRate, maxOutputFraction, mean, stdDev);
		this.replyPool = new SimplexBinomialPool(false, sendingRate, maxOutputFraction, mean, stdDev);

	}
	

	@Override
	public void initialize() {
		// no need to do anything
	}
	

	@Override
	public void begin() {
		// no need to do anything
	}

	
	@Override
	public void addRequest(Request request) {
		requestPool.addMessage((Message) request);
	}

	
	@Override
	public void addReply(Reply reply) {
		replyPool.addMessage((Message) reply);
	}

	
	public class SimplexBinomialPool {

		private boolean isRequestPool;
		private Vector<Message> collectedMessages;
		private boolean isFirstMessage = true;
		private int sendingRate;
		private double maxOutputFraction;
		private NormalDistributionImpl normalDist;
		private Timer timer = new Timer();
		
		
		public SimplexBinomialPool(	boolean isRequestPool, 
				int sendingRate,
				double maxOutputFraction, 
				double mean, 
				double stdDev) {
			
			this.collectedMessages = new Vector<Message>(100);	// TODO: poperty file
			this.isRequestPool = isRequestPool;
			this.sendingRate = sendingRate;
			this.maxOutputFraction = maxOutputFraction;
			this.normalDist = new NormalDistributionImpl(mean, stdDev);
			this.normalDist.reseedRandomGenerator(secureRandom.nextLong());
			
		}
		
		
		public void addMessage(Message mixMessage) {
			
			synchronized (collectedMessages) {
				
				if (isFirstMessage) {
					isFirstMessage = false;
					timer.scheduleAtFixedRate(new TimeoutTask(this), sendingRate, sendingRate);
				}
				
				collectedMessages.add(mixMessage);
				
			}

		}

		
		public void putOutMessages() {
			
			synchronized (collectedMessages) {
				
				try {
					
					double coinBias = maxOutputFraction * normalDist.cumulativeProbability(collectedMessages.size());
					
					for (int i=0; i<collectedMessages.size(); i++) {
						
						Message m = collectedMessages.get(i);
						
						if (secureRandom.nextDouble() <= coinBias) {
							
							if (isRequestPool)
								controller.getInputOutputHandler().addRequest((Request)m);
							else
								controller.getInputOutputHandler().addReply((Reply)m);
							
							collectedMessages.remove(i--);
							
						}
						
					}
					
				} catch (MathException e) {
					throw new RuntimeException("ERROR: problem with generating the biased coin! " +e.getStackTrace()); 
				}
				
			}
	
		}
	
		
		private final class TimeoutTask extends TimerTask {

			private SimplexBinomialPool linkedPool;
			
			protected TimeoutTask(SimplexBinomialPool linkedPool) {
				this.linkedPool = linkedPool;
			}
			
			@Override 
			public void run() {
				linkedPool.putOutMessages();
			}
			
		}
			
	}

	
	@Override
	public String[] getCompatibleImplementations() {
		return (new String[] {	"outputStrategy.BasicBatch",
								"outputStrategy.BasicPool",
								"inputOutputHandler.BasicInputOutputHandler",
								"keyGenerator.BasicKeyGenerator",
								"messageProcessor.BasicMessageProcessor",
								"externalInformationPort.BasicExternalInformationPort",
								"networkClock.BasicSystemTimeClock",
								"userDatabase.BasicUserDatabase",
								"message.BasicMessage"	
			});
	}


	@Override
	public boolean usesPropertyFile() {
		return false;
	}
	
}
