package SequentialAR;
import java.util.ArrayList;

public class Main {
	/*private static String[][][] dataset1 = {	
		{
			{"Buy Stock#1", "Sell Stock#2", "Buy Stock#3"},
			{"Buy Stock#1", "Sell Stock#2"},
			{"Buy Stock#2", "Buy Stock#3"}
		},

		{
			{"Buy Stock#2", "Buy Stock#3"},
			{"Buy Stock#1", "Sell Stock#3"},
			{"Sell Stock#2", "Buy Stock#3"}
		},

		{
			{"Buy Stock#1", "Sell Stock#2"},
			{"Buy Stock#1", "Buy Stock#2"},
			{"Buy Stock#3"}
		},

		{
			{"Buy Stock#1", "Buy Stock#3"}
		}
	};*/
	private static String[][][] dataset1 = {	
		{
			{"Buy Stock#1", "Buy Stock#2", "Buy Stock#3"},
			{"Buy Stock#1"}
		}
	};
	private static String[][][] dataset2 = {	
		{
			{"Buy Stock#1", "Sell Stock#2", "Sell Stock#3"},
			{"Buy Stock#1", "Buy Stock#2"}
		}
	};
	
	public static void main(String[] args) throws Exception{
		ItemSets activity[] = new ItemSets[dataset1.length];
		for(int act=0; act<dataset1.length; act++){
			activity[act] = new ItemSets(dataset1[act]);
		}
		ItemSets activity2[] = new ItemSets[dataset2.length];
		for(int act=0; act<dataset2.length; act++){
			activity2[act] = new ItemSets(dataset2[act]);
		}
		int min_sup_count = 2;
		
// ***	method-1 (for one machine)
		Sequence seq1 = new Sequence(activity);
		seq1.setMin_sup(min_sup_count);
		seq1.run();
		System.out.println(seq1.getSeqToString());

// ***  method-2 (for many machine)
		ArrayList<ItemSets[]> all_seq_sets = new ArrayList<ItemSets[]>();
	// machine 1		
		// FirstSeq
		ArrayList<ItemSets> one_seq_sets = Sequence.createFirstSeq(activity);
		// count from activity
		one_seq_sets = Sequence.countActSeq(activity, one_seq_sets);
		System.out.println("1-sequence (machine 1)");
		Sequence.printSeqArrayList(one_seq_sets);
	// machine 2
		// repeat the function by using other datasets
		ArrayList<ItemSets> one_seq_sets2 = Sequence.createFirstSeq(activity2);
		one_seq_sets2 = Sequence.countActSeq(activity2, one_seq_sets2);
		System.out.println("1-sequence (machine 2)");
		Sequence.printSeqArrayList(one_seq_sets2);
	// main-machine		
		// combine 2 set_seq with count 
		one_seq_sets = Sequence.combine2SeqCount(one_seq_sets, one_seq_sets2);
		System.out.println("1-sequence (After Combine)");
		Sequence.printSeqArrayList(one_seq_sets);
		
		// check with min_sup (after combine)
		ItemSets[] seq = Sequence.checkMinSup(one_seq_sets, min_sup_count);
		System.out.println("1-sequence (after min_sup)");
		Sequence.printSeqArray(seq);
		all_seq_sets.add(seq);
		
		// a loop for whole process
		for(int i=2; i<all_seq_sets.size()+2; i++){
	// machine 1 
			ArrayList<ItemSets> next_seq_sets = Sequence.createNextSeq(seq);
			next_seq_sets = Sequence.countActSeq(activity, next_seq_sets);
			System.out.println(i+"-sequence (machine 1)");
			Sequence.printSeqArrayList(next_seq_sets);
	// machine 2 
			ArrayList<ItemSets> next_seq_sets2 = Sequence.createNextSeq(seq);
			next_seq_sets2 = Sequence.countActSeq(activity2, next_seq_sets2);
			System.out.println(i+"-sequence (machine 2)");
			Sequence.printSeqArrayList(next_seq_sets2);
	// main-machine
		// combine 2 set_seq with count 
			next_seq_sets = Sequence.combine2SeqCount(next_seq_sets, next_seq_sets2);
			System.out.println(i+"-sequence (After Combine)");
			Sequence.printSeqArrayList(next_seq_sets);
			// check with min_sup (after combine)
			seq = Sequence.checkMinSup(next_seq_sets, min_sup_count);
			if(seq.length!=0){
				System.out.println(i+"-sequence (after min_sup)");
				Sequence.printSeqArray(seq);
				all_seq_sets.add(seq);
			}else{
				System.out.println("no sequence can from in "+i+"-sequence");
			}
		}
	}
}
