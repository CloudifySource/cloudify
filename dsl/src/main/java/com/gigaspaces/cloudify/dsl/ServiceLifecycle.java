package com.gigaspaces.cloudify.dsl;

import java.io.Serializable;

public class ServiceLifecycle implements Serializable {

	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    private Object init;
	
	private Object preInstall;
	private Object install;
	private Object postInstall;
	
	private Object preStart;
	private Object start;
	private Object postStart;
	
	private Object preStop;
	private Object stop;
	private Object postStop;
	
	private Object shutdown;
	
	private Object preServiceStart;
	private Object preServiceStop;
	
	private Object startDetection;
	private Object monitors;
	private Object details;
	
	private int startDetectionTimeoutSecs = 90;
    public int getStartDetectionTimeoutSecs() {
		return startDetectionTimeoutSecs;
	}
	public void setStartDetectionTimeoutSecs(int startDetectionTimeoutSecs) {
		this.startDetectionTimeoutSecs = startDetectionTimeoutSecs;
	}
	public int getStartDetectionIntervalSecs() {
		return startDetectionIntervalSecs;
	}
	public void setStartDetectionIntervalSecs(int startDetectionIntervalSecs) {
		this.startDetectionIntervalSecs = startDetectionIntervalSecs;
	}

	private int startDetectionIntervalSecs = 1;

	private Object stopDetection;

	public void setStopDetection(Object stopDetection) {
		this.stopDetection = stopDetection;
	}
	public ServiceLifecycle() {
		
	}
	public Object getInit() {
		return init;
	}

	public void setInit(Object init) {
		this.init = init;
	}

	public Object getPreInstall() {
		return preInstall;
	}

	public void setPreInstall(Object preInstall) {
		this.preInstall = preInstall;
	}

	public Object getInstall() {
		return install;
	}

	public void setInstall(Object install) {
		this.install = install;
	}

	public Object getPostInstall() {
		return postInstall;
	}

	public void setPostInstall(Object postInstall) {
		this.postInstall = postInstall;
	}

	public Object getPreStart() {
		return preStart;
	}

	public void setPreStart(Object preStart) {
		this.preStart = preStart;
	}

	public Object getStart() {
		return start;
	}

	public void setStart(Object start) {
		this.start = start;
	}

	public Object getPostStart() {
		return postStart;
	}

	public void setPostStart(Object postStart) {
		this.postStart = postStart;
	}

	public Object getPreStop() {
		return preStop;
	}

	public void setPreStop(Object preStop) {
		this.preStop = preStop;
	}

	public Object getStop() {
		return stop;
	}

	public void setStop(Object stop) {
		this.stop = stop;
	}

	public Object getPostStop() {
		return postStop;
	}

	public void setPostStop(Object postStop) {
		this.postStop = postStop;
	}

	public Object getShutdown() {
		return shutdown;
	}

	public void setShutdown(Object shutdown) {
		this.shutdown = shutdown;
	}
	
	public void setPreServiceStart(Object preServiceStart){
		this.preServiceStart = preServiceStart;
	}
	
	public Object getPreServiceStart(){
		return this.preServiceStart;
	}
	
	public void setPreServiceStop(Object preServiceStop){
		this.preServiceStop = preServiceStop;
	}
	
	public Object getPreServiceStop(){
		return this.preServiceStop;
	}
	
	public void setStartDetection(Object startDetection){
		this.startDetection = startDetection;
	}
	
	public Object getStartDetection(){
		return this.startDetection;
	}
	
	public Object getStopDetection(){
		return this.stopDetection;
	}
	
	
	public void setMonitors(Object monitors) {
		this.monitors = monitors;
	}
	
	public Object getMonitors() {
		return this.monitors;
	}
	
	public void setDetails(Object details) {
		this.details = details;
	}
								  
	public Object getDetails() {
		return details;
	}
}
