package org.universAAL.samples.ctxtbus;

import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.universAAL.context.che.ontology.ContextHistoryService;
import org.universAAL.middleware.owl.MergedRestriction;
import org.universAAL.middleware.rdf.PropertyPath;
import org.universAAL.middleware.rdf.Resource;
import org.universAAL.middleware.service.CallStatus;
import org.universAAL.middleware.service.DefaultServiceCaller;
import org.universAAL.middleware.service.ServiceCaller;
import org.universAAL.middleware.service.ServiceRequest;
import org.universAAL.middleware.service.ServiceResponse;
import org.universAAL.middleware.service.owls.process.ProcessOutput;
import org.universAAL.middleware.container.ModuleContext;
import org.universAAL.middleware.context.ContextEvent;

public class HistoryCaller {
    private static final String HISTORY_CLIENT_NAMESPACE = "http://ontology.itaca.es/HistoryClient.owl#";
    private static final String OUTPUT_LIST_OF_EVENTS = HISTORY_CLIENT_NAMESPACE
	    + "listOfEvents";
    private static final String OUTPUT_RESULT_STRING = HISTORY_CLIENT_NAMESPACE
	    + "resultString";
    private final static Logger log = LoggerFactory
	    .getLogger(HistoryCaller.class);

    private ServiceCaller caller;

    public HistoryCaller(ModuleContext context) {
	caller = new DefaultServiceCaller(context);
    }

    private Object getReturnValue(List outputs, String expectedOutput) {
	Object returnValue = null;
	if (outputs == null)
	    log.info("History Client: No events found!");
	else
	    for (Iterator i = outputs.iterator(); i.hasNext();) {
		ProcessOutput output = (ProcessOutput) i.next();
		if (output.getURI().equals(expectedOutput))
		    if (returnValue == null)
			returnValue = output.getParameterValue();
		    else
			log.info("History Client: redundant return value!");
		else
		    log.info("History Client - output ignored: "
			    + output.getURI());
	    }

	return returnValue;
    }

    public int callGetEvents(
	    org.universAAL.context.che.ontology.ContextEvent matchEvent,
	    long tstFrom, long tstTo) {
	int count = 0;
	ServiceResponse response = caller.call(getGetEventsRequest(matchEvent,
		tstFrom, tstTo));
	if (response.getCallStatus() == CallStatus.succeeded) {
	    Object value = getReturnValue(response.getOutputs(),
		    OUTPUT_LIST_OF_EVENTS);
	    if (value instanceof Resource) {
		if (((Resource) value).getURI().contains("#nil")) {
		    log.info("History Client - result is empty");
		    return 0;
		}
	    } else {
		try {
		    ContextEvent[] events = (ContextEvent[]) ((List) value)
			    .toArray(new ContextEvent[((List) value).size()]);

		    for (int j = 0; j < events.length; j++) {
			count++;
			log.info("Retrieved context event from CHe:\n"
				+ "    Subject     ="
				+ events[j].getSubjectURI() + "\n"
				+ "    Subject type="
				+ events[j].getSubjectTypeURI() + "\n"
				+ "    Predicate   ="
				+ events[j].getRDFPredicate() + "\n"
				+ "    Object      ="
				+ events[j].getRDFObject());
			log.info(Activator.ser.serialize(events[j]));
		    }
		} catch (Exception e) {
		    log.info("History Client: List of events corrupt!", e);
		}
	    }
	} else
	    log.info("History Client - status of getEvents(): "
		    + response.getCallStatus());
	return count;
    }

    private ServiceRequest getGetEventsRequest(
	    org.universAAL.context.che.ontology.ContextEvent matchEvent,
	    long tstFrom, long tstTo) {
	ServiceRequest getEvents = new ServiceRequest(
		new ContextHistoryService(null), null);

	MergedRestriction r = MergedRestriction.getFixedValueRestriction(
		ContextHistoryService.PROP_MANAGES, matchEvent);

	getEvents.getRequestedService().addInstanceLevelRestriction(r,
		new String[] { ContextHistoryService.PROP_MANAGES });
	if (tstFrom > 0) {
	    MergedRestriction tstr1 = MergedRestriction.getFixedValueRestriction(
		    ContextHistoryService.PROP_TIMESTAMP_FROM,
		    new Long(tstFrom));
	    getEvents.getRequestedService().addInstanceLevelRestriction(tstr1,
		    new String[] { ContextHistoryService.PROP_TIMESTAMP_FROM });

	}
	if (tstTo > 0) {
	    MergedRestriction tstr2 = MergedRestriction.getFixedValueRestriction(
		    ContextHistoryService.PROP_TIMESTAMP_TO, new Long(tstTo));
	    getEvents.getRequestedService().addInstanceLevelRestriction(tstr2,
		    new String[] { ContextHistoryService.PROP_TIMESTAMP_TO });
	}
	getEvents.addSimpleOutputBinding(new ProcessOutput(
		OUTPUT_LIST_OF_EVENTS), new PropertyPath(null, false,
		new String[] { ContextHistoryService.PROP_MANAGES })
		.getThePath());
	return getEvents;
    }

    public String callDoSPARQL(String query) {
	ServiceResponse response = caller.call(getDoSPARQLRequest(query));

	if (response.getCallStatus() == CallStatus.succeeded) {
	    try {
		String results = (String) getReturnValue(response.getOutputs(),
			OUTPUT_RESULT_STRING);
		// Uncomment this line if you want to show the raw results. Do
		// this for CONSTRUCT, ASK or DESCRIBE
		log.info("Result of SPARQL query was:\n" + results);
		return results;
	    } catch (Exception e) {
		log.error("History Client: Result corrupt!", e);
		return "";
	    }
	} else
	    log.info("History Client - status of doSparqlQuery(): "
		    + response.getCallStatus());
	return "";
    }

    private ServiceRequest getDoSPARQLRequest(String query) {
	ServiceRequest getQuery = new ServiceRequest(new ContextHistoryService(
		null), null);

	MergedRestriction r = MergedRestriction.getFixedValueRestriction(
		ContextHistoryService.PROP_PROCESSES, query);

	getQuery.getRequestedService().addInstanceLevelRestriction(r,
		new String[] { ContextHistoryService.PROP_PROCESSES });
	getQuery.addSimpleOutputBinding(
		new ProcessOutput(OUTPUT_RESULT_STRING), new PropertyPath(null,
			true,
			new String[] { ContextHistoryService.PROP_RETURNS })
			.getThePath());
	return getQuery;
    }

    public int callGetEventsSPARQL(String query) {
	int count = 0;
	ServiceResponse response = caller
		.call(getGetEventsSPARQLRequest(query));
	if (response.getCallStatus() == CallStatus.succeeded) {
	    Object value = getReturnValue(response.getOutputs(),
		    OUTPUT_LIST_OF_EVENTS);
	    if (value instanceof Resource) {
		if (((Resource) value).getURI().contains("#nil")) {
		    log.info("History Client - result is empty");
		    return 0;
		}
	    } else {
		try {
		    ContextEvent[] events = (ContextEvent[]) ((List) value)
			    .toArray(new ContextEvent[((List) value).size()]);

		    for (int j = 0; j < events.length; j++) {
			count++;
			log.info("Retrieved context event from CHe:\n"
				+ "    Subject     ="
				+ events[j].getSubjectURI() + "\n"
				+ "    Subject type="
				+ events[j].getSubjectTypeURI() + "\n"
				+ "    Predicate   ="
				+ events[j].getRDFPredicate() + "\n"
				+ "    Object      ="
				+ events[j].getRDFObject());
			log.info(Activator.ser.serialize(events[j]));
		    }
		} catch (Exception e) {
		    log.info("History Client: List of events corrupt!", e);
		}
	    }
	} else
	    log.info("History Client - status of getEvents(): "
		    + response.getCallStatus());
	return count;
    }

    private ServiceRequest getGetEventsSPARQLRequest(String query) {
	ServiceRequest getQuery = new ServiceRequest(new ContextHistoryService(
		null), null);

	MergedRestriction r = MergedRestriction.getFixedValueRestriction(
		ContextHistoryService.PROP_PROCESSES, query);

	getQuery.getRequestedService().addInstanceLevelRestriction(r,
		new String[] { ContextHistoryService.PROP_PROCESSES });
	getQuery.addSimpleOutputBinding(
		new ProcessOutput(OUTPUT_LIST_OF_EVENTS), new PropertyPath(
			null, false,
			new String[] { ContextHistoryService.PROP_MANAGES })
			.getThePath());
	return getQuery;
    }

}