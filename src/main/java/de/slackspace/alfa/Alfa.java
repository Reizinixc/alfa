package de.slackspace.alfa;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import de.slackspace.alfa.azure.LogFetcher;
import de.slackspace.alfa.elasticsearch.ElasticSearchServer;
import de.slackspace.alfa.elasticsearch.LogCleaner;
import de.slackspace.alfa.exception.ConnectionException;

public class Alfa {

	private static final String ALFA_CONFIG = "conf/alfa.properties";
	private static final String ELASTICSEARCH_CONFIG = "conf/elasticsearch-server.properties";
	private ElasticSearchServer elasticSearchServer;
	private List<LogFetcher> logFetchers;
	private LogCleaner logCleaner;
	
	public void start(boolean runAsService) throws IOException, ConnectionException {
		startElasticSearchServer(runAsService);
		initializeLogFetcher(runAsService);
		initializeLogCleaner();
		fetchAndStoreLogs();
	}
	
	public void stop() {
		if(elasticSearchServer != null) {
			elasticSearchServer.stop();
		}
	}

	private void initializeLogFetcher(boolean runAsService) throws ConnectionException {
		String configFile = ALFA_CONFIG;
		if(runAsService) {
			configFile = "../../" + configFile;
		}
		
		this.logFetchers = ObjectFactory.constructLogFetcher(configFile, elasticSearchServer.getClient());
	}
	
	private void initializeLogCleaner() {
		this.logCleaner = new LogCleaner(elasticSearchServer.getClient());
	}

	private void startElasticSearchServer(boolean runAsService) throws IOException {
		String configFile = ELASTICSEARCH_CONFIG;
		if(runAsService) {
			configFile = "../../" + configFile;
		}
				
		Properties config = new Properties();
		config.load(new FileReader(configFile));
		
		elasticSearchServer = new ElasticSearchServer(config);
		elasticSearchServer.start();
	}
	
	private void fetchAndStoreLogs() {
		ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(logFetchers.size());
		
		//this will execute the logFetcher every n minutes, if a logFetcher run exceeds n minutes, 
		//the next start will be blocked until the first one has finished 
		for (LogFetcher logFetcher : logFetchers) {
			scheduledExecutorService.scheduleAtFixedRate(logFetcher, 0, logFetcher.getPollingIntervalMinutes(), TimeUnit.MINUTES);
		}
		
		//this will execute the logCleaner every 24 hours
		scheduledExecutorService.scheduleAtFixedRate(logCleaner, 0, 24, TimeUnit.HOURS);
	}

}
