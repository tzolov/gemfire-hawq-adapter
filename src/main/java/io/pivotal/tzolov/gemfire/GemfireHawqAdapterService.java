/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.pivotal.tzolov.gemfire;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class GemfireHawqAdapterService implements CommandLineRunner {

	private String hostName;

	private String port;

	private String oqlQuery;

	private String columnDelimiter;

	@Autowired
	public GemfireHawqAdapterService(@Value("${gf.host:localhost}") String gemfireHostName, @Value("${gf.port:8081}") String gemfirePort,
			@Value("${gf.oqlQuery}") String oqlQuery, @Value("${gf.columnDelimiter:|}") String columnDelimiter) {

		this.hostName = gemfireHostName;
		this.port = gemfirePort;
		this.oqlQuery = oqlQuery;
		this.columnDelimiter = columnDelimiter;
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void run(String... args) throws Exception {

		List rows = new RestTemplate().getForObject("http://{hostname}:{port}/gemfire-api/v1/queries/adhoc?q={query}", List.class,
				hostName, port, oqlQuery);

		for (Object row : rows) {

			String outputRow = "";

			if (row instanceof String) {
				outputRow = (String) row;
			} else if (row instanceof Map) {

				Map mapItem = ((Map) row);

				int i = 0;
				for (Object value : mapItem.values()) {
					i++;
					String columnValue = toColumnValue(value);
					outputRow = (i == 1) ? columnValue : outputRow + columnDelimiter + columnValue;
				}
			} else {
				throw new IllegalStateException("Unsupported return class type: " + row.getClass());
			}

			System.out.println(outputRow);
		}
	}

	/**
	 * Naive escape logic
	 * 
	 * @param value
	 *            - input value to clean
	 * @return Returns escaped value
	 */
	private String toColumnValue(Object value) {

		String valueStr = "";

		if (value != null) {
			valueStr = value.toString();
			valueStr = valueStr.replace("\\", "\\\\");
			valueStr = valueStr.replace("|", "\\|");
			valueStr = valueStr.replace("\t", " ");
			valueStr = valueStr.replace("\r", " ");
			valueStr = valueStr.replace("\n", " ");
			valueStr = valueStr.replace("\0", "");
		}

		return valueStr;
	}
}
