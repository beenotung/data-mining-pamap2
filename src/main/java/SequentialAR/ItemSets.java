package SequentialAR;

import java.util.ArrayList;

/*
 * Class Sequence.java is use to collect the one data set in String[a][b]
 *	 a : the order in each item_set
 *   b : the item in item_set
 */
public class ItemSets {
	private ArrayList<ArrayList<String>>  dataset;
	private int count;
	
	public ItemSets(ArrayList<ArrayList<String>> data){

        /*for(int i=0; i<data.size(); i++){
            String[] one_row = data.get(i).toArray(new String[0]);
        }*/
		//this.dataset = data.toArray(new String[0][0]);
        this.dataset = data;
		this.count=0;
	}
	public int getCount(){
		return count;
	}
	public void setCount(int count){
		this.count = count;
	}
	public ArrayList<ArrayList<String>>  getItemSets(){
		return dataset;
	}
	public String toString(){
		String str = "";
		str +="{";
		for(int i=0; i<dataset.size(); i++){
			str +="{";
			for(int j=0; j<dataset.get(i).size(); j++){
				str +=dataset.get(i).get(j);
				if(j<dataset.get(i).size()-1){
					str +=", ";
				}
			}
			str +="}";
			if(i<dataset.size()-1){
				str +=",";
			}
		}
		str +="}";
		return str;
	}
	
	public boolean isContain(ArrayList<ArrayList<String>> sequence_set){
		int i=0;
		int time_no=0;
		int item_no=0;

		for(time_no=0; time_no<sequence_set.size(); time_no++){
			for(item_no=0; item_no<sequence_set.get(time_no).size(); item_no++){
				//System.out.println(time_no+"]["+item_no+"]"+sequence_set[time_no][item_no]+" check: "+i+"//"+sequence_set.length);
				if(i==dataset.size()){
					return false;
				}
				if(!rowHaveItem(sequence_set.get(time_no).get(item_no), i)){
					if(i>=dataset.size()-1){
						return false;
					}else{
						i++;
						item_no=-1;
					}
				}
			}
			i++;
		}
		return true;
	}

	public boolean rowHaveItem(String sequence_col, int time_no){
		for(int j=0; j<dataset.get(time_no).size(); j++){
			if(sequence_col.equals(dataset.get(time_no).get(j))){
				return true;
			}
		}
		return false;
	}
}
