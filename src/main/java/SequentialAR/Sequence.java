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
	private ItemSets activity_set[];
	private ArrayList<ItemSets[]> seq_result;
// main functions **
	public Sequence(){
	}
	public Sequence(ItemSets[] activity){
		this.activity_set = activity;
		this. min_support_count=1;
		this.seq_result = null;
	}
	public void setMin_sup(int min_sup_count){
		this.min_support_count=min_sup_count;
	}
	public ArrayList<ItemSets[]> getSeq_result(){
		return seq_result;
	}
	public String getSeqToString(){
		String str="start:\n";
		for(int i=0; i<seq_result.size(); i++){
			for(int j=0; j<seq_result.get(i).length; j++){
				str+=(i+1)+"-seq["+j+"]:"+seq_result.get(i)[j].toString()+"\n";
			}
		}
		return str;
	}
	public void run(){
		seq_result = findSeq(activity_set, min_support_count);
	}
//** end of main function
	public static void printSeqArray(ItemSets[] seq){
		String str = "";
		for(int i=0; i<seq.length; i++){
			str+=seq[i].toString()+"="+seq[i].getCount()+"\n";
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
	public static ArrayList<ItemSets[]> findSeq(ItemSets[] activity, int min_sup_count){
		// FirstSeq
		ArrayList<ItemSets[]> all_seq_sets = new ArrayList<ItemSets[]>();
		
		if(all_seq_sets.size()==0){
			ArrayList<ItemSets> seq_sets = createFirstSeq(activity);
			// count the item_set
			seq_sets = countActSeq(activity, seq_sets);
			ItemSets[] seq = checkMinSup(seq_sets, min_sup_count);
			all_seq_sets.add(seq);
		}
			
		for(int i=0; i<all_seq_sets.size(); i++){
			ArrayList<ItemSets> seq_sets = createNextSeq(all_seq_sets.get(i));
			// count the item_set
			seq_sets = countActSeq(activity, seq_sets);
			
			//System.out.println("Content of Array list"+i+":"+seq_sets.toString());		
			ItemSets[] seq = checkMinSup(seq_sets, min_sup_count);

			if(seq.length!=0){
				all_seq_sets.add(seq);
			}
		}
		return all_seq_sets;
	}
	
	public static ArrayList<ItemSets> createFirstSeq(ItemSets[] activity){
		ArrayList<ItemSets> one_seq_sets = new ArrayList<ItemSets>();
		// build the first item_set
		for(int act=0; act<activity.length; act++){
			String[][] activity_iset = activity[act].getItemSets();
			
			for(int time_no=0; time_no<activity_iset.length; time_no++){
				String[] time_items = activity_iset[time_no];
				// row = first seq
				for(int i=0; i<time_items.length; i++){
					String[][] input = new String[1][time_items.length];
					input[0]=time_items;
					ItemSets input_iset = new ItemSets(input);
					if(!one_seq_sets.toString().contains(input_iset.toString())){
						one_seq_sets.add(input_iset);
					}
				}
			}
		}
		for(int i=0; i<one_seq_sets.size(); i++){ // checking the arraylist
			String[][] act_row = one_seq_sets.get(i).getItemSets();
			for(int act2=0; act2<activity.length; act2++){
				if(act_row[0].length-1==0){
					break;
				}else{
					String[][] a = cutingInRow(act_row[0]);
					for(int i2=0; i2<a.length; i2++){
						String[][] input = new String[1][act_row[0].length-1];
						input[0]=a[i2];
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
	
	public static ArrayList<ItemSets> createNextSeq(ItemSets[] previous_seq_sets){
		ArrayList<ItemSets> seq_sets = new ArrayList<ItemSets>();
		for(int seq_no1=0; seq_no1<previous_seq_sets.length; seq_no1++){
			String[][] array1 = previous_seq_sets[seq_no1].getItemSets();
			for(int seq_no2=0; seq_no2<previous_seq_sets.length; seq_no2++){
				String[][] array2 = previous_seq_sets[seq_no2].getItemSets();
				if(equals2DArray(array1, array2, array1.length-1)){
					String[][] input = addArrayRow(array1, array2[array2.length-1]);
					ItemSets input_set = new ItemSets(input);
					if(!seq_sets.toString().contains(input_set.toString())){
						seq_sets.add(input_set);
					}
				}
			}
		}
		return seq_sets;
	}
	
	public static ArrayList<ItemSets> countActSeq(ItemSets[] activity, ArrayList<ItemSets> seq_sets){
		// count the item_set
		for(int i=0; i<seq_sets.size(); i++){
			String[][] array1 = seq_sets.get(i).getItemSets();
			for(int act=0; act<activity.length; act++){
				if(activity[act].isContain(array1)){
					int new_sum = seq_sets.get(i).getCount()+1;
					seq_sets.get(i).setCount(new_sum);
				}
			}
		}
		//System.out.println("Content of Array list - "+seq_sets.get(0).getItemSets().length+":"+seq_sets.toString());
		return seq_sets;
	}
	
	public static ItemSets[] checkMinSup(ArrayList<ItemSets> seq_sets, int min_sup_count){
		// return in array
		int n=0;
		for(int i=0; i<seq_sets.size(); i++){
			if(seq_sets.get(i).getCount()>=min_sup_count){
				n++;
			}
		}
		ItemSets[] seq = new ItemSets[n];
		n=0;
		for(int i=0; i<seq_sets.size(); i++){
			if(seq_sets.get(i).getCount()>=min_sup_count){
				seq[n] = seq_sets.get(i);
				n++;
			}
		}
		return seq;
	}
	
	public static String[][] cutingInRow(String[] item_input){
		int n;// cuting number
		String[][] result = new String[item_input.length][item_input.length-1];
		for(n=0; n<item_input.length; n++){
			int k=0;
			for(int i=0; i<item_input.length; i++){
				if(i!=n){
					result[n][k]=item_input[i];
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
	public static boolean equals2DArray(String[][] array1, String[][] array2, int length){
		for(int i=0; i<length; i++){
			for(int j=0; j<array1[i].length && array1[i].length==array2[i].length; j++){
				if(array1[i][j]!=array2[i][j]){
					return false;
				}
			}
		}
		return true;
	}
	public static String[][] addArrayRow(String[][] array1, String[] array2){
		String[][]a = new String[array1.length+1][];
		for(int i=0; i<a.length; i++){
			if(i==a.length-1){
				a[i]=array2;
			}else{
				a[i]=array1[i];
			}
		}
		return a;
	}
}
