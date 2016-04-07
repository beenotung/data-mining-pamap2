import SomAlgorithm.AccelerationData;
import SomAlgorithm.SOM;

import java.util.ArrayList;

/**
 * Created by yungyunglui on 26/2/16.
 */
public class SomTest {

  private static double[][] dataset = {
    {1.99964, 6.94837, 5.08845},
    {1.8099, 6.45729, 5.16424},
    {1.82756, 5.93151, 5.78208},
    {1.7576, 5.78202, 5.97397},
    {1.5067, 6.20407, 6.27669},
    {1.51659, 6.47166, 6.50686},
    {1.33406, 6.81557, 6.61842},
    {1.40161, 7.00234, 6.34939},
    {1.46514, 7.56682, 5.88659},
    {1.49543, 8.01912, 5.57762},
    {1.18616, 7.98184, 5.38068}
  };

  public static void main(String[] args) throws Exception{

    ArrayList<AccelerationData> data = new ArrayList<AccelerationData>();
    // compare the Previous time x,y,z; add into ArrayList
    for (int i=1; i < dataset.length; i++){
      AccelerationData previous = new AccelerationData(dataset[i-1]);
      AccelerationData now = new AccelerationData(dataset[i]);
      double[] compare_result = new double[3];
      for (int j=0; j<3; j++){
        compare_result[j]=now.get(j)-previous.get(j);
      }
      data.add(new AccelerationData(compare_result));
    }

    for (int i=0; i<data.size(); i++){
      data.get(i).vertortopolar();
      System.out.println(i+"\n"+data.get(i).toString());
    }

    double[] startbase={0.02, 0.04, 0.06, 0.08, 0.1, 0.12, 0.14, 0.16, 0.2};
    SOM sample_SOM = new SOM(startbase, "hand_x");
    double result=1;
    for (int i=0; i<100; i++){
      int random = (int)Math.random()*data.size();
      result = sample_SOM.addsample(data.get(random).get(0));  // only the x point
      //System.out.println("sample:"+data.get(i).get(0)+"\n"+sample_SOM.toString());
    }
    while (result>0.01){
      int random = (int)Math.random()*data.size();
      result = sample_SOM.addsample(data.get(random).get(0));
      System.out.println("r:"+result);
    }
    System.out.println(sample_SOM.toString());

    // find the closet index
    System.out.println(sample_SOM.getName());
    System.out.println(sample_SOM.similest_mapobject(data.get(0).get(0)));
  }
}
