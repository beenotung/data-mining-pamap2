package SomAlgorithm;
/**
 * Created by yungyunglui on 7/4/16.
 */
/*
 * Class Sequence.java is use to collect the one data set in String[a][b]
 *	 acceleration
 *	 0 : x   1 : y   2 : z
 *   vectorToPolar
 *   0:radius 1:theta 2:phi
 */

public class AccelerationData {
	//
  private double[] acceleration = new double[3];
  private double[] vectorToPolar = new double[3];

  public AccelerationData(double[] acceleration)
  {
	this.acceleration = acceleration;
  }
  public double[] getAcceleration(){
	return  acceleration;
  }
  public double get(int i){
	return acceleration[i];
  }
  public void set(int i, double value){
	this.acceleration[i] = value;
  }
  public String toString(){
	String str;
	str = "x: "+acceleration[0]+",y: "+acceleration[1]+",z: "+acceleration[2]+"\n";
	str += "radius: "+vectorToPolar[0]+",theta: "+vectorToPolar[1]+",phi: "+vectorToPolar[2]+"\n";
	return str;
  }

  public void vertortopolar(){
	double x = acceleration[0];
	double y = acceleration[1];
	double z = acceleration[2];
	double radius = Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2));
	double theta;
	if (x>0){
	  theta = Math.toDegrees(Math.atan(y/x));
	}else if(x<0 && y>0){
	  theta = Math.toDegrees(Math.atan(y/x)) + Math.PI;
	}else if(x<0 && y<0){
	  theta = Math.toDegrees(Math.atan(y/x)) - Math.PI;
	}else if(x==0 && y>0){
	  theta = Math.PI/2;
	}else{
	  theta = Math.PI/(-2);
	}
	double phi;
	if (z>0){
	  phi = Math.toDegrees(Math.atan(Math.sqrt(Math.pow(x,2) + Math.pow(y,2)) / z));
	}else if(z<0){
	  phi = Math.toDegrees(Math.atan(Math.sqrt(Math.pow(x,2) + Math.pow(y,2)) / z)) + Math.PI;
	}else{
	  phi = Math.PI/2;
	}

	vectorToPolar[0] = radius;
	vectorToPolar[1] = theta;
	vectorToPolar[2] = phi;
  }
}