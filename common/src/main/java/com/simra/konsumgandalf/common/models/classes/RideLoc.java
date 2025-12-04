package com.simra.konsumgandalf.common.models.classes;

import com.opencsv.bean.CsvBindByName;

public class RideLoc {

	@CsvBindByName(column = "lat")
	private Double lat;

	@CsvBindByName(column = "lon")
	private Double lng;

	@CsvBindByName(column = "X")
	private double X;

	@CsvBindByName(column = "Y")
	private double Y;

	@CsvBindByName(column = "Z")
	private double Z;

	@CsvBindByName(column = "timeStamp")
	private long timeStamp;

	@CsvBindByName(column = "acc")
	private double acc;

	@CsvBindByName(column = "a")
	private double a;

	@CsvBindByName(column = "b")
	private double b;

	@CsvBindByName(column = "c")
	private double c;

	@CsvBindByName(column = "obsDistanceLeft1")
	private double obsDistanceLeft1;

	@CsvBindByName(column = "obsDistanceLeft2")
	private double obsDistanceLeft2;

	@CsvBindByName(column = "obsDistanceRight1")
	private double obsDistanceRight1;

	@CsvBindByName(column = "obsDistanceRight2")
	private double obsDistanceRight2;

	@CsvBindByName(column = "obsClosePassEvent")
	private double obsClosePassEvent;

	@CsvBindByName(column = "XL")
	private double XL;

	@CsvBindByName(column = "YL")
	private double YL;

	@CsvBindByName(column = "ZL")
	private double ZL;

	@CsvBindByName(column = "RX")
	private double RX;

	@CsvBindByName(column = "RY")
	private double RY;

	@CsvBindByName(column = "RZ")
	private double RZ;

	@CsvBindByName(column = "RC")
	private double RC;

	public Double getLat() {
		return lat;
	}

	public void setLat(Double lat) {
		this.lat = lat;
	}

	public Double getLng() {
		return lng;
	}

	public void setLng(Double lon) {
		this.lng = lon;
	}

	public double getX() {
		return X;
	}

	public void setX(double x) {
		X = x;
	}

	public double getY() {
		return Y;
	}

	public void setY(double y) {
		Y = y;
	}

	public double getZ() {
		return Z;
	}

	public void setZ(double z) {
		Z = z;
	}

	public long getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(long timeStamp) {
		this.timeStamp = timeStamp;
	}

	public double getAcc() {
		return acc;
	}

	public void setAcc(double acc) {
		this.acc = acc;
	}

	public double getA() {
		return a;
	}

	public void setA(double a) {
		this.a = a;
	}

	public double getB() {
		return b;
	}

	public void setB(double b) {
		this.b = b;
	}

	public double getC() {
		return c;
	}

	public void setC(double c) {
		this.c = c;
	}

	public double getObsDistanceLeft1() {
		return obsDistanceLeft1;
	}

	public void setObsDistanceLeft1(double obsDistanceLeft1) {
		this.obsDistanceLeft1 = obsDistanceLeft1;
	}

	public double getObsDistanceLeft2() {
		return obsDistanceLeft2;
	}

	public void setObsDistanceLeft2(double obsDistanceLeft2) {
		this.obsDistanceLeft2 = obsDistanceLeft2;
	}

	public double getObsDistanceRight1() {
		return obsDistanceRight1;
	}

	public void setObsDistanceRight1(double obsDistanceRight1) {
		this.obsDistanceRight1 = obsDistanceRight1;
	}

	public double getObsDistanceRight2() {
		return obsDistanceRight2;
	}

	public void setObsDistanceRight2(double obsDistanceRight2) {
		this.obsDistanceRight2 = obsDistanceRight2;
	}

	public double getObsClosePassEvent() {
		return obsClosePassEvent;
	}

	public void setObsClosePassEvent(double obsClosePassEvent) {
		this.obsClosePassEvent = obsClosePassEvent;
	}

	public double getXL() {
		return XL;
	}

	public void setXL(double XL) {
		this.XL = XL;
	}

	public double getYL() {
		return YL;
	}

	public void setYL(double YL) {
		this.YL = YL;
	}

	public double getZL() {
		return ZL;
	}

	public void setZL(double ZL) {
		this.ZL = ZL;
	}

	public double getRX() {
		return RX;
	}

	public void setRX(double RX) {
		this.RX = RX;
	}

	public double getRY() {
		return RY;
	}

	public void setRY(double RY) {
		this.RY = RY;
	}

	public double getRZ() {
		return RZ;
	}

	public void setRZ(double RZ) {
		this.RZ = RZ;
	}

	public double getRC() {
		return RC;
	}

	public void setRC(double RC) {
		this.RC = RC;
	}

	// Getters and setters

}
