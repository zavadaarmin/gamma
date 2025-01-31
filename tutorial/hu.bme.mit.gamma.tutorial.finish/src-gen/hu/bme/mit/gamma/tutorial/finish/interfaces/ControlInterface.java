package hu.bme.mit.gamma.tutorial.finish.interfaces;

import hu.bme.mit.gamma.tutorial.finish.*;
import java.util.List;

public interface ControlInterface {
	
	interface Provided extends Listener.Required {
		
		public boolean isRaisedToggle();
		
		void registerListener(Listener.Provided listener);
		List<Listener.Provided> getRegisteredListeners();
	}
	
	interface Required extends Listener.Provided {
		
		
		void registerListener(Listener.Required listener);
		List<Listener.Required> getRegisteredListeners();
	}
	
	interface Listener {
		
		interface Provided  {
			void raiseToggle();
		}
		
		interface Required  {
		}
		
	}
}
