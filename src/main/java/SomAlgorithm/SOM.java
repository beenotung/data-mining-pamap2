package SomAlgorithm;

/**
 * Created by yungyunglui on 7/4/16.
 */
public class SOM {
  double[] base_map = new double[9];
  int t;
  String name;
  private static final double alpha0 =0.5, phi0=0.999;

  public SOM(double[] startvalue, String name)
  {
    this.base_map = startvalue;
    this.name = name;
    this.t = 0;
  }
  public String toString()
  {
    String str="{";
    for(int i=0; i<9; i++){
      str=str+" "+base_map[i];
    }
    str+="}\n";
    return str;
  }
  public String getName()
  {
    return name;
  }
  public double addsample(double sample)
  {
    t++;
    int similest_index=similest_mapobject(sample);

    double[] old_base = new double[9];
    for (int i=0; i<9; i++){
      old_base[i]=base_map[i];
    }
    double alpha = alpha(t);

    for (int i=0; i<9; i++){
      base_map[i] = base_map[i]-(base_map[i]-sample)*alpha*h(i,similest_index);; //(9-Math.abs(i-similest_index))/9;
    }
    double result = 0;
    for (int i=0; i<9; i++){
      result += Math.abs(base_map[i]-old_base[i])/base_map[i];
    }
    return result/9;
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
  public int similest_mapobject(double value){
    double diff_value=Math.pow(base_map[0]-value,2);;
    int similest_index=0;
    for (int i=0; i<base_map.length; i++){
      if(Math.pow(base_map[i]-value,2)<diff_value){
        diff_value = Math.pow(base_map[i]-value,2);
        similest_index = i;
      }
    }
    return similest_index;
  }
}
