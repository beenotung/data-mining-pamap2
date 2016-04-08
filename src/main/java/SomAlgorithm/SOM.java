package SomAlgorithm;

/**
 * Created by yungyunglui on 7/4/16.
 */
public class SOM {
  AccelerationData[] base_map = new AccelerationData[9];
  int t;
  String name;
  private static final double alpha0 =0.5, phi0=0.999;

  public SOM(AccelerationData[] startvalue, String name)
  {
    this.base_map = startvalue;
    this.name = name;
    this.t = 0;
  }
  public String toString(){
    String str="{";
    for(int i=0; i<9; i++){
      str=str+" {x:"+base_map[i].get(0)+" y:"+base_map[i].get(1)+" z:"+base_map[i].get(2)+"}";
    }
    str+="}\n";
    return str;
  }
  public String getName(){
    return name;
  }
  public double addsample(AccelerationData sample){
    t++;
    int similest_index=similest_mapobject(sample);

    AccelerationData[] old_base = new AccelerationData[9];
    for (int i=0; i<9; i++){
      old_base[i]=base_map[i];
    }
    double alpha = alpha(t);

    for (int i=0; i<9; i++){
      base_map[i].set(0, base_map[i].get(0)-(base_map[i].get(0)-sample.get(0))*alpha*h(i,similest_index)); //(9-Math.abs(i-similest_index))/9;
      base_map[i].set(1, base_map[i].get(1)-(base_map[i].get(1)-sample.get(1))*alpha*h(i,similest_index));
      base_map[i].set(2, base_map[i].get(2)-(base_map[i].get(2)-sample.get(2))*alpha*h(i,similest_index));
    }
    double result = 0;

    double[] zero_array = {0, 0, 0};
    AccelerationData zero = new AccelerationData(zero_array);
    for (int i=0; i<9; i++){
      result += Math.abs(getDistance(base_map[i], old_base[i]))/getDistance(base_map[i],zero);
    }
    return result/9;
  }
  public double getDistance(AccelerationData map, AccelerationData sample)
  {
    double dx = Math.pow((map.get(0)-sample.get(0)), 2);
    double dy = Math.pow((map.get(1)-sample.get(1)), 2);
    double dz = Math.pow((map.get(2)-sample.get(2)), 2);

    return Math.sqrt(dx+dy+dz);
  }

  public double h(int i, int j){
    return Math.exp(-Math.pow(i-j,2)/(2*Math.pow(sigma(t),2)));
  }
  public double sigma(double t){
    return 1/(1+Math.exp(-t));
  }
  public double alpha(int t){
    double result = alpha0*Math.pow(phi0,t);
    System.out.println("t:"+t);
    return result;
  }
  public int similest_mapobject(AccelerationData value){
    double diff_value = getDistance(base_map[0], value);
    int similest_index = 0;
    for (int i=0; i<base_map.length; i++){
      if(getDistance(base_map[i], value)<diff_value){
        diff_value = getDistance(base_map[0], value);
        similest_index = i;
      }
    }
    System.out.println("distance:"+diff_value);
    return similest_index;
  }
}