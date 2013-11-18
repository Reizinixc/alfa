package de.slackspace.alfa.azure;

import java.util.Calendar;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.windowsazure.services.core.ServiceException;
import com.microsoft.windowsazure.services.table.TableContract;
import com.microsoft.windowsazure.services.table.models.Filter;
import com.microsoft.windowsazure.services.table.models.QueryEntitiesOptions;
import com.microsoft.windowsazure.services.table.models.QueryEntitiesResult;

import de.slackspace.alfa.domain.TableResultPartial;

public class AzureService {

	private static final Logger LOGGER = LoggerFactory.getLogger(AzureService.class);
	private static final String DEPLOYMENTTABLE = "deployments";
	private static final String WADLOGSTABLE = "WADLogsTable";
	private static final int MAX_DAYS_BACK = 10;
	private TableContract tableContract;
	
	public AzureService(TableContract contract) {
		this.tableContract = contract;
	}
	
	public TableResultPartial getLogEntries(String nextPartitionKey, String nextRowKey) {
		try {
			QueryEntitiesOptions options = constructFilter(nextPartitionKey, nextRowKey);
			QueryEntitiesResult result = tableContract.queryEntities(WADLOGSTABLE, options);
			return new TableResultPartial(result.getEntities(), result.getNextPartitionKey(), result.getNextRowKey());
		} catch (Exception e) {
			LOGGER.error("Could not query table " + WADLOGSTABLE + ". Error was: ", e);
			return new TableResultPartial();
		}
	}
	
	public TableResultPartial getDeploymentEntries() {
		try {
			QueryEntitiesResult result = tableContract.queryEntities(DEPLOYMENTTABLE);
			return new TableResultPartial(result.getEntities(), null, null);
		} catch (ServiceException e) {
			LOGGER.warn("Could not find optional table " + DEPLOYMENTTABLE + ". Ignoring...", e);
			return new TableResultPartial();
		}
	}
	
	private QueryEntitiesOptions constructFilter(String nextPartitionKey, String nextRowKey) {
		QueryEntitiesOptions options = new QueryEntitiesOptions();
		if(nextPartitionKey != null && nextRowKey != null) {
			options.setNextPartitionKey(nextPartitionKey);
			options.setNextRowKey(nextRowKey);
		}
		else {
			Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
			cal.add(Calendar.DAY_OF_MONTH, MAX_DAYS_BACK * -1);
			String partitionKey = PartitionCalculator.calculatePartitionKeyFor(cal);
			options.setFilter(Filter.queryString("PartitionKey ge '" + partitionKey + "'"));
		}
		
		return options;
	}
	
}
