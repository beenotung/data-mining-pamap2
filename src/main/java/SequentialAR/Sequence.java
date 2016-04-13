package SequentialAR;
/*
 * Class Sequence.java is use to find the frequent sequences by using Sequential Association Rule
 * 
 * input: ItemSets for all training data
 *    e.g ItemSets[0] = first training data
 *        ItemSets[1] = second training data...
 * Output: ArrayList of the sequences (within min-support count)
 *    e.g ArrayList.get(0) = sequences in 1_sequence
 *        ArrayList.get(1) = sequences in 2_sequence...
 *        
 * 
 */
import java.util.ArrayList;

public class Sequence {
	
	private int min_support_count;
	private ArrayList<ItemSets> activity_set;
	private ArrayList<ArrayList<ItemSets>> seq_result;
// main functions **
	public Sequence(){
	}
	public Sequence(ArrayList<ItemSets> activity){
		this.activity_set = activity;
		this. min_support_count=1;
		this.seq_result = null;
	}
	public void setMin_sup(int min_sup_count){
		this.min_support_count=min_sup_count;
	}
	public ArrayList<ArrayList<ItemSets>> getSeq_result(){
		return seq_result;
	}
	public String getSeqToString(){
		String str="start:\n";
		for(int i=0; i<seq_result.size(); i++){
			for(int j=0; j<seq_result.get(i).size(); j++){
				str+=(i+1)+"-seq["+j+"]:"+seq_result.get(i).get(j).toString()+"\n";
			}
		}
		return str;
	}
	public void run(){
		seq_result = findSeq(activity_set, min_support_count);
	}
//** end of main function
	public static void printSeqArray(ArrayList<ItemSets> seq){
		String str = "";
		for(int i=0; i<seq.size(); i++){
			str+=seq.get(i).toString()+"="+seq.get(i).getCount()+"\n";
		}
		System.out.println(str);
	}
	public static void printSeqArrayList(ArrayList<ItemSets> seq){
		String str = "";
		for(int i=0; i<seq.size(); i++){
			str+=seq.get(i).toString()+"="+seq.get(i).getCount()+"\n";
		}
		System.out.println(str);
	}
	public static ArrayList<ArrayList<ItemSets>> findSeq(ArrayList<ItemSets> activity, int min_sup_count){
		// FirstSeq
		ArrayList<ArrayList<ItemSets>> all_seq_sets = new ArrayList<ArrayList<ItemSets>>();
		
		if(all_seq_sets.size()==0){
			ArrayList<ItemSets> seq_sets = createFirstSeq(activity);
			// count the item_set
			seq_sets = countActSeq(activity, seq_sets);
			ArrayList<ItemSets> seq = checkMinSup(seq_sets, min_sup_count);
			all_seq_sets.add(seq);
		}
			
		for(int i=0; i<all_seq_sets.size(); i++){
			ArrayList<ItemSets> seq_sets = createNextSeq(all_seq_sets.get(i));
			// count the item_set
			seq_sets = countActSeq(activity, seq_sets);
			
			//System.out.println("Content of Array list"+i+":"+seq_sets.toString());		
			ArrayList<ItemSets> seq = checkMinSup(seq_sets, min_sup_count);

			if(seq.size()!=0){
				all_seq_sets.add(seq);
			}
		}
		all_seq_sets = reduceDuplicate(all_seq_sets);

		return all_seq_sets;
	}
	public static ArrayList<ArrayList<ItemSets>> reduceDuplicate(ArrayList<ArrayList<ItemSets>> all_seq_sets){
		for(int i=all_seq_sets.size()-2; i>=0; i--){
			ArrayList<ItemSets> now_itemsets = new ArrayList<ItemSets>();
			for(int j=0; j<all_seq_sets.get(i).size(); j++){
				for(int k=0; k<all_seq_sets.get(i+1).size(); k++){
					if(all_seq_sets.get(i+1).get(k).isContain(all_seq_sets.get(i).get(j).getItemSets())){
						break;
					}
					if(k==all_seq_sets.get(i+1).size()-1){
						now_itemsets.add(all_seq_sets.get(i).get(j));
					}
				}
			}
			/*ItemSets[] new_itemsets = new ItemSets[now_itemsets.size()];
			for(int k=0; k<now_itemsets.size(); k++){
				new_itemsets[k]=now_itemsets.get(k);
			}*/
			all_seq_sets.set(i, now_itemsets);
		}
		return all_seq_sets;
	}
	
	public static ArrayList<ItemSets> createFirstSeq(ArrayList<ItemSets> activity){
		ArrayList<ItemSets> one_seq_sets = new ArrayList<ItemSets>();
		// build the first item_set
		for(int act=0; act<activity.size(); act++){
			ArrayList<ArrayList<String>> activity_iset = activity.get(act).getItemSets();
			
			for(int time_no=0; time_no<activity_iset.size(); time_no++){
				ArrayList<String> time_items = activity_iset.get(time_no);
				// row = first seq
				for(int i=0; i<time_items.size(); i++){
					ArrayList<ArrayList<String>> input = new ArrayList<ArrayList<String>>();
					input.set(0, time_items);
					ItemSets input_iset = new ItemSets(input);
					if(!one_seq_sets.toString().contains(input_iset.toString())){
						one_seq_sets.add(input_iset);
					}
				}
			}
		}
		for(int i=0; i<one_seq_sets.size(); i++){ // checking the arraylist
			ArrayList<ArrayList<String>> act_row = one_seq_sets.get(i).getItemSets();
			for(int act2=0; act2<activity.size(); act2++){
				if(act_row.get(0).size()-1==0){
					break;
				}else{
					ArrayList<ArrayList<String>> a = cutingInRow(act_row.get(0));
					for(int i2=0; i2<a.size(); i2++){
						ArrayList<ArrayList<String>> input = new ArrayList<ArrayList<String>>();
						input.set(0,a.get(i2));
						ItemSets input_iset = new ItemSets(input);
						//**System.out.println(input_iset.toString());
						if(!one_seq_sets.toString().contains(input_iset.toString())){
							one_seq_sets.add(input_iset);
						}
					}
				}
			}
		}
		
		return one_seq_sets;
	}
	
	public static ArrayList<ItemSets> createNextSeq(ArrayList<ItemSets> previous_seq_sets){
		ArrayList<ItemSets> seq_sets = new ArrayList<ItemSets>();
		for(int seq_no1=0; seq_no1<previous_seq_sets.size(); seq_no1++){
			ArrayList<ArrayList<String>> array1 = previous_seq_sets.get(seq_no1).getItemSets();
			for(int seq_no2=0; seq_no2<previous_seq_sets.size(); seq_no2++){
				ArrayList<ArrayList<String>> array2 = previous_seq_sets.get(seq_no2).getItemSets();
				if(equals2DArray(array1, array2, array1.size()-1)){
					ArrayList<ArrayList<String>> input = addArrayRow(array1, array2.get(array2.size()-1));
					ItemSets input_set = new ItemSets(input);
					if(!seq_sets.toString().contains(input_set.toString())){
						seq_sets.add(input_set);
					}
				}
			}
		}
		return seq_sets;
	}
	
	public static ArrayList<ItemSets> countActSeq(ArrayList<ItemSets> activity, ArrayList<ItemSets> seq_sets){
		// count the item_set
		for(int i=0; i<seq_sets.size(); i++){
			ArrayList<ArrayList<String>> array1 = seq_sets.get(i).getItemSets();
			for(int act=0; act<activity.size(); act++){
				if(activity.get(act).isContain(array1)){
					int new_sum = seq_sets.get(i).getCount()+1;
					seq_sets.get(i).setCount(new_sum);
				}
			}
		}
		//System.out.println("Content of Array list - "+seq_sets.get(0).getItemSets().length+":"+seq_sets.toString());
		return seq_sets;
	}
	
	public static ArrayList<ItemSets> checkMinSup(ArrayList<ItemSets> seq_sets, int min_sup_count){
		// return in array
		int n=0;
		for(int i=0; i<seq_sets.size(); i++){
			if(seq_sets.get(i).getCount()>=min_sup_count){
				n++;
			}
		}
		ArrayList<ItemSets> seq = new ArrayList<ItemSets>();
		n=0;
		for(int i=0; i<seq_sets.size(); i++){
			if(seq_sets.get(i).getCount()>=min_sup_count){
				seq.set(n,seq_sets.get(i));
				n++;
			}
		}
		return seq;
	}
	
	public static ArrayList<ArrayList<String>> cutingInRow(ArrayList<String> item_input){
		int n;// cuting number
		ArrayList<ArrayList<String>> result = new ArrayList<ArrayList<String>>();
		for(n=0; n<item_input.size(); n++){
			int k=0;
			for(int i=0; i<item_input.size(); i++){
				if(i!=n){
					result.get(n).set(k,item_input.get(i));
					k++;
				}
			}
		}
		return result;
	}
//	combine sequence sets
	public static ArrayList<ItemSets> combine2SeqCount(ArrayList<ItemSets> seq1, ArrayList<ItemSets> seq2){
		ArrayList<ItemSets> seq_sets = new ArrayList<ItemSets>();
		int i, j;
		for(i=0; i<seq1.size(); i++){
			seq_sets.add(seq1.get(i));
		}
		for(i=0; i<seq2.size(); i++){
			for(j=0; j<seq1.size(); j++){
				if(seq2.get(i).toString().equals(seq1.get(j).toString())){
					int new_sum = seq_sets.get(j).getCount()+seq2.get(i).getCount();
					seq_sets.get(j).setCount(new_sum);
					j=seq1.size();
				}else{
					if(j==seq1.size()-1){
						seq_sets.add(seq2.get(i));
					}
				}
			}
		}
		return seq_sets;
	}
//  for array checking
	public static boolean equals2DArray(ArrayList<ArrayList<String>> array1, ArrayList<ArrayList<String>> array2, int length){
		for(int i=0; i<length; i++){
			for(int j=0; j<array1.get(i).size() && array1.get(i).size()==array2.get(i).size(); j++){
				if(array1.get(i).get(j)!=array2.get(i).get(j)){
					return false;
				}
			}
		}
		return true;
	}
	public static ArrayList<ArrayList<String>> addArrayRow(ArrayList<ArrayList<String>> array1, ArrayList<String> array2){
		ArrayList<ArrayList<String>> a = new ArrayList<ArrayList<String>>();
		for(int i=0; i<a.size(); i++){
			if(i==a.size()-1){
				a.set(i,array2);
			}else{
				a.set(i,array1.get(i));
			}
		}
		return a;
	}
}
