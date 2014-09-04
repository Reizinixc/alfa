package de.slackspace.alfa.elasticsearch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.elasticsearch.action.admin.cluster.state.ClusterStateRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.hppc.ObjectLookupContainer;
import org.elasticsearch.common.hppc.cursors.ObjectCursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.slackspace.alfa.date.DateFormatter;
import de.slackspace.alfa.exception.ConnectionException;

public class LogCleaner implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogCleaner.class);
	private Client client;
	private int KEEP_DAYS = 30;
	
	public LogCleaner(Client client) {
		this.client = client;
	}
	
	public void deleteOldLogs() throws ConnectionException {
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Deleting old logs...");
		}
		
		List<String> indicesToKeep = createIndicesToKeep();
		
		ObjectLookupContainer<String> indizes = client.admin().cluster().state(new ClusterStateRequest()).actionGet().getState().getMetaData().getIndices().keys();
		Iterator<ObjectCursor<String>> iter = indizes.iterator();
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Found " + indizes.size() + " indices.");
		}
		
		while(iter.hasNext()) {
			ObjectCursor<String> cursor = iter.next();
			
			if(!indicesToKeep.contains(cursor.value)) {
				DeleteIndexResponse delete = client.admin().indices().delete(new DeleteIndexRequest(cursor.value)).actionGet();
				if (!delete.isAcknowledged()) {
					LOGGER.warn("Could not delete index " + cursor.value);
				}
			}
		}
		
		if(LOGGER.isDebugEnabled()) {
			LOGGER.debug("Old logs deleted.");
		}
	}

	@Override
	public void run() {
		deleteOldLogs();
	}
	
	private List<String> createIndicesToKeep() {
		List<String> indicesToKeep = new ArrayList<String>();
		indicesToKeep.add("kibana-int"); //do not delete internal kibana index
		
		for (int i = 0; i < KEEP_DAYS; i++) {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, i * -1);
			
			indicesToKeep.add("logs-" + DateFormatter.toYYYYMMDD(cal.getTime()));
		}
		
		return indicesToKeep;
	}
}
