package org.dcm4chee.xds2.registry;

import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.dcm4che3.audit.ActiveParticipant;
import org.dcm4che3.audit.AuditMessage;
import org.dcm4che3.audit.AuditSourceIdentification;
import org.dcm4che3.audit.AuditMessages.RoleIDCode;
import org.dcm4che3.net.IncompatibleConnectionException;
import org.dcm4che3.net.audit.AuditLogger;
import org.dcm4chee.xds2.common.audit.XDSAudit;
import org.junit.Assert;

public class AuditTestManager {

	public static class AuditLoggerMock extends AuditLogger {
		

		private static final long serialVersionUID = 9185595844488887918L;

		@Override
		public Calendar timeStamp() {
			// TODO Auto-generated method stub
			return super.timeStamp();
		}
		
		@Override
		public ActiveParticipant createActiveParticipant(boolean requestor, RoleIDCode... roleIDs) {
			return createActiveParticipant(requestor, 
	                processID(), 
	               "tstAlternativeUserId",
	                "tstApp",
	                localHost().getHostName(), 
	                roleIDs);
		}
		
		@Override
		public AuditSourceIdentification createAuditSourceIdentification() {
			// TODO Auto-generated method stub
			return new AuditSourceIdentification();
		}
		
		@Override
		public boolean isInstalled() {
			return true;
		}
		
		@Override
		public void write(Calendar timeStamp, AuditMessage message) throws IncompatibleConnectionException, GeneralSecurityException {
			AuditTestManager.lastMessages.add(message);
		}
		
		// set audit source id
		
	}
	
	private static List<AuditMessage> lastMessages = new ArrayList<AuditMessage>();
	private static int howManyMsg;
	
	private static AuditLoggerMock logger = new AuditLoggerMock();
	
	public static AuditLoggerMock getLogger() {
		return logger;
	}
	

	public static void cleanup(){
		
		howManyMsg = -1;
		lastMessages.clear();

	}


	
	/**
	 * Should be run before each test
	 */
	public static void prepareAuditLogger() {
    	AuditTestManager.cleanup();

    	// calling this one time would be enough, but it is more clearly separated this way
    	XDSAudit.setAuditLogger(AuditTestManager.getLogger()); 
    }
	
	/**
	 * Should be run after each test
	 */
	public static void checkAudits() 
    {
		
		if (howManyMsg == -1) 	Assert.assertTrue("No audit log messages sent",lastMessages.size()>0); else
								Assert.assertEquals("Audit messages sent", howManyMsg, lastMessages.size()); 
    }

	public static void expectNumberOfMessages(int i) {
		howManyMsg = i;
	}

	
	
}
