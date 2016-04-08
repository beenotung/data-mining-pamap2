import SomAlgorithm.AccelerationData;
import SomAlgorithm.SOM;

/**
 * Created by yungyunglui on 26/2/16.
 */
public class SomTest {

  public static double[][] dataset = {
            {-8.93218,3.44072,2.08847},
            {-8.86195,3.4388,1.93558},
            {-8.90455,3.32427,1.81987},
            {-8.81855,3.47779,2.09005},
            {-8.89145,3.47904,2.16593},
            {-8.89733,3.40182,2.01215},
            {-8.89412,3.47837,2.08891},
            {-8.92844,3.40379,2.20411},
            {-8.8968,3.4777,2.0119},
            {-8.93726,3.28796,1.97345},
            {-9.0123,3.36442,1.97207},
            {-8.97371,3.47795,1.97226},
            {-8.86649,3.36191,1.82031},
            {-8.93753,3.43938,1.93444},
            {-8.8247,3.55199,1.89727},
            {-8.89172,3.4411,2.16605},
            {-8.81453,3.47879,2.20557},
            {-8.81988,3.47745,2.05154},
            {-8.93084,3.44106,2.12698},
            {-8.81828,3.51573,2.08992},
            {-8.8976,3.36388,2.01227},
            {-8.89974,3.43909,1.93501},
            {-8.8255,3.43817,1.89764},
            {-8.90562,3.36187,1.78123},
            {-9.01364,3.36409,1.93356},
            {-9.04769,3.51681,2.00951},
            {-9.05063,3.4782,1.93262},
            {-8.97398,3.44001,1.97238},
            {-8.90188,3.5143,1.85775},
            {-8.86088,3.4012,1.97421},
            {-8.93271,3.36485,2.08872},
            {-8.82389,3.47645,1.93602},
            {-8.78183,3.5151,2.05198},
            {-8.78423,3.36301,2.01397},
            {-9.01418,3.47757,1.89468},
            {-8.85821,3.40187,2.05122},
            {-8.93164,3.32724,2.12735},
            {-8.93619,3.43972,1.97295},
            {-8.78503,3.43855,1.97522},
            {-8.82603,3.36229,1.89789},
            {-9.00589,3.51752,2.12559},
            {-9.00616,3.47958,2.12572},
            {-9.08869,3.44055,1.93218},
            {-9.04849,3.40299,2.00988},
            {-9.01364,3.36409,1.93356},
            {-8.97986,3.36279,1.81861},
            {-8.85928,3.43947,2.01259},
            {-8.85955,3.59089,1.97359},
            {-8.89225,3.36522,2.1663},
            {-8.86195,3.4388,1.93558},
            {-8.93272,3.55421,2.04959},
            {-8.89653,3.51564,2.01178},
            {-9.01659,3.51484,1.81754},
            {-8.8566,3.44014,2.0896},
            {-8.89386,3.51631,2.08879},
            {-8.89038,3.6308,2.16543},
            {-9.00589,3.51752,2.12559},
            {-8.89065,3.4035,2.20468},
            {-8.74484,3.59035,2.0138},
            {-8.89974,3.43909,1.93501},
            {-8.93806,3.3635,1.93469},
            {-8.9378,3.40144,1.93457},
            {-8.89974,3.43909,1.93501},
            {-8.7845,3.51443,1.97497},
            {-8.93646,3.40178,1.97308},
            {-8.9378,3.40144,1.93457},
            {-8.97425,3.40207,1.97251},
            {-8.90509,3.43775,1.78099},
            {-8.93272,3.55421,2.04959},
            {-8.90402,3.40015,1.81962},
            {-8.97478,3.32619,1.97276},
            {-8.85874,3.32599,2.05147},
            {-8.86248,3.36292,1.93583},
            {-8.8984,3.43943,1.97352},
            {-9.01391,3.51551,1.89456},
            {-8.93806,3.3635,1.93469}
  };

  public static void main(String[] args) throws Exception{

       /* ArrayList<AccelerationData> data = new ArrayList<AccelerationData>();
        // compare the Previous time x,y,z; add into ArrayList
        for (int i=1; i < dataset.length; i++){
            AccelerationData previous = new AccelerationData(dataset[i-1]);
            AccelerationData now = new AccelerationData(dataset[i]);
            double[] compare_result = new double[3];
            for (int j=0; j<3; j++){
                compare_result[j]=now.get(j)-previous.get(j);
            }
            data.add(new AccelerationData(compare_result));
        }*/
    AccelerationData[] data = new AccelerationData[dataset.length];
    for (int i=0; i<dataset.length; i++){
      AccelerationData default_set = new AccelerationData(dataset[i]);
      data[i] = default_set;
    }

    AccelerationData[] startbase = new AccelerationData[9];
    for (int i=0; i<9; i++){
      double[] default_array = {0, 0, 0};
      AccelerationData default_set = new AccelerationData(default_array);
      startbase[i] = default_set;
    }

    SOM sample_SOM = new SOM(startbase, "hand_x");
    double result=1;
    for (int i=0; i<100; i++){
      int random = (int)(Math.random()*data.length);
      result = sample_SOM.addsample(data[random]);
      //System.out.println("sample:"+data.get(i).get(0)+"\n"+sample_SOM.toString());
    }
    while (result>0.01){
      int random = (int)(Math.random()*data.length);
      result = sample_SOM.addsample(data[random]);
      System.out.println("r:"+result);
    }

    System.out.println(sample_SOM.toString());

    // find the closet index
    System.out.println(sample_SOM.getName());
    System.out.println(sample_SOM.similest_mapobject(data[0]));
  }
}