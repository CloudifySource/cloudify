package org.cloudifysource.usm;

public class ProcessDeathNotifier {

	private boolean processDead = false;
	private UniversalServiceManagerBean usm;
	
	
	public ProcessDeathNotifier(UniversalServiceManagerBean usm) {
		super();
		this.usm = usm;
	}


	public synchronized void processDeathDetected() {
		if(processDead) {
			return;
		}
		
		processDead = true;
		usm.onProcessDeath();
	}
	
}
