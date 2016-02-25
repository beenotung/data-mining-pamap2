package SequentialAR;
/*
 * Class Sequence.java is use to collect the one data set in String[a][b]
 *	 a : the order in each item_set
 *   b : the item in item_set
 */
public class ItemSets {
	private String[][] dataset;
	private int count;
	
	public ItemSets(String[][] data){
		this.dataset = data;
		this.count=0;
	}
	public int getCount(){
		return count;
	}
	public void setCount(int count){
		this.count = count;
	}
	public String[][] getItemSets(){
		return dataset;
	}
	public String toString(){
		String str = "";
		str +="{";
		for(int i=0; i<dataset.length; i++){
			str +="{";
			for(int j=0; j<dataset[i].length; j++){
				str +=dataset[i][j];
				if(j<dataset[i].length-1){
					str +=", ";
				}
			}
			str +="}";
			if(i<dataset.length-1){
				str +=",";
			}
		}
		str +="}";
		return str;
	}
	
	public boolean isContain(String[][] sequence_set){
		int i=0;
		int time_no=0;
		int item_no=0;

		for(time_no=0; time_no<sequence_set.length; time_no++){
			for(item_no=0; item_no<sequence_set[time_no].length; item_no++){
				//System.out.println(time_no+"]["+item_no+"]"+sequence_set[time_no][item_no]+" check: "+i+"//"+sequence_set.length);
				if(i==dataset.length){
					return false;
				}
				if(!rowHaveItem(sequence_set[time_no][item_no], i)){
					if(i>=dataset.length-1){
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
		for(int j=0; j<dataset[time_no].length; j++){
			if(sequence_col.equals(dataset[time_no][j])){
				return true;
			}
		}
		return false;
	}
}
