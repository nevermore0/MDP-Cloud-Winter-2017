package com.mdp.consumer;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.Date;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDB.ConsistencyLevel;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;

import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.influxdb.dto.QueryResult.Result;
import org.influxdb.dto.QueryResult.Series;

import com.mdp.consumer.KafkaMessageConsumer;
import com.mdp.consumer.ConsumerListener;

public class InfluxConsumer implements ConsumerListener {

	private InfluxDB influxDB;
	private BatchPoints batchPoints;
	private String measurementName;
	private Logger logger;
	private SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

	public InfluxConsumer(InfluxDB influxdb, String dbName, String measurementName) {
		this.influxDB = influxdb;
		this.logger = LoggerFactory.getLogger(InfluxConsumer.class.getName());
		this.measurementName = measurementName;
		this.batchPoints = BatchPoints
					.database(dbName)
					.retentionPolicy("autogen")
					.consistency(ConsistencyLevel.ALL)
					.build();
	}

	public void onShutdown(){
		System.out.println("Shutting down");
	}

	//TODO add logging file to show each message getting received
	public void onReceiveMessage(String message){
		String parts[] = message.split("\t");
		if (parts[0].equals("TestBed")) {
			addTestBedData(parts);
		}
		else if(parts[0].equals("Simulation")){
			addSimulationData(parts);
		}
	}

	public void addTestBedData(String[] parts){
		int i = 1;
		while(i < parts.length){
			if(parts[i].equals("TimeStamp")){
				ArrayList<String> stringSend = new ArrayList<String>();
				stringSend.add(parts[i]);
				i++;
				while((i < parts.length) && !(parts[i].equals("TimeStamp"))){
					stringSend.add(parts[i]);
					i++;
				}
				sendTestBedData(stringSend);
			}
			i++;
		}
	}
	public void sendTestBedData(ArrayList<String> parts){
		System.out.println(parts.get(0));
		System.out.println(parts.get(1));
		int i = 2;
		Long timeStamp = Long.parseLong(parts.get(1));
		Map<String, Object> fields_list = new HashMap<String, Object>();
		while(i < parts.size() - 1){
			Double measurementValue = Double.parseDouble(parts.get(i + 1));
			Point tempPoint = Point.measurement(parts.get(i))
			.time(timeStamp, TimeUnit.MILLISECONDS)
			.addField(parts.get(i), measurementValue)
			.build();
			fields_list.put(parts.get(i), measurementValue);
			this.batchPoints.point(tempPoint);
			i += 2;
		}
		Point fullPoint = Point.measurement("Timestamp")
		.time(timeStamp, TimeUnit.MILLISECONDS)
		.fields(fields_list)
		.build();
		this.batchPoints.point(fullPoint);
		influxDB.write(this.batchPoints);
		System.out.println("Data Sent\t\n");
	}
	public void addSimulationData(String[] parts){

	}
}
