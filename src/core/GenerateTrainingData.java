package core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.FSDirectory;

public class GenerateTrainingData {
	
	private static final int MIN_PARATEXT_LENGTH = 100;
	
	public FSDirectory initLucene(String dirPath) {
		try {
			return FSDirectory.open((new File(dirPath)).toPath());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public IndexReader getReader(FSDirectory dir) {
		try {
			return DirectoryReader.open(dir);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public IndexSearcher getSearcher(IndexReader ir) {
		return new IndexSearcher(ir);
	}
	
	public String getParaText(IndexSearcher is, String paraID) {
		QueryParser qpID = new QueryParser("Id", new StandardAnalyzer());
		try {
			String paraText = is.doc(is.search(qpID.parse(paraID), 1).scoreDocs[0].doc).get("Text");
			return paraText;
		} catch (IOException | ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public HashMap<String, String> getParaTexts(String[] paraIDs, IndexSearcher is) {
		HashMap<String, String> texts = new HashMap<String, String>();
		for(int i=0; i<paraIDs.length; i++)
			texts.put(paraIDs[i], getParaText(is, paraIDs[i]));
		return texts;
	}
	
	public void writeBERTData(String[] paras, HashMap<String, String> paraTextMap, ArrayList<String> posPairs, ArrayList<String> negPairs, BufferedWriter bw) {
		try {
			int minNumPairs = posPairs.size();
			if(negPairs.size() < minNumPairs)
				minNumPairs = negPairs.size();
			for(int i=0; i<minNumPairs; i++) {
				String pair = posPairs.get(i);
				String p1 = paras[Integer.parseInt(pair.split("_")[0])]; 
				String p2 = paras[Integer.parseInt(pair.split("_")[1])];
				String p1Text = paraTextMap.get(p1); 
				String p2Text = paraTextMap.get(p2);
				if(p1Text.length() >= MIN_PARATEXT_LENGTH && p2Text.length() >= MIN_PARATEXT_LENGTH) {
					bw.write("1\t"+p1+"\t"+p2+"\t"+p1Text+"\t"+p2Text+"\n");
				}
			}
			for(int i=0; i<minNumPairs; i++) {
				String pair = negPairs.get(i);
				String p1 = paras[Integer.parseInt(pair.split("_")[0])];
				String p2 = paras[Integer.parseInt(pair.split("_")[1])];
				String p1Text = paraTextMap.get(p1);
				String p2Text = paraTextMap.get(p2);
				if(p1Text.length() >= MIN_PARATEXT_LENGTH && p2Text.length() >= MIN_PARATEXT_LENGTH) {
					bw.write("0\t"+p1+"\t"+p2+"\t"+p1Text+"\t"+p2Text+"\n");
				}
			}
		} catch (NumberFormatException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void produceTrainingData(String parapairFilePath, String indexPath, String outputFilePath) {
		try {
			FSDirectory fsd = initLucene(indexPath);
			IndexReader ir = getReader(fsd);
			IndexSearcher is = getSearcher(ir);
			
			BufferedReader br = new BufferedReader(new FileReader(new File(parapairFilePath)));
			BufferedWriter bw = new BufferedWriter(new FileWriter(new File(outputFilePath)));
			//bw.write("Quality\t#1 ID\t#2 ID\t#1 String\t#2 String\n");
			String l = br.readLine();
			int c = 0;
			while(l != null) {
				String pageId = l;
				String[] paras = br.readLine().split(" ");
				HashMap<String, String> paraTextMap = getParaTexts(paras, is);
				String pos = br.readLine();
				String neg = br.readLine();
				ArrayList<String> posPairs = new ArrayList<String>();
				ArrayList<String> negPairs = new ArrayList<String>();
				if(pos.length() > 5) {
					Collections.addAll(posPairs, pos.split(": ")[1].split(" "));
					Collections.shuffle(posPairs);
				}
				if(neg.length() > 5) {
					Collections.addAll(negPairs, neg.split(": ")[1].split(" "));
					Collections.shuffle(negPairs);
				}
				br.readLine();
				writeBERTData(paras, paraTextMap, posPairs, negPairs, bw);
				c++;
				if(c%100 == 0)
					System.out.println(c+" pages completed");
				l = br.readLine();
			}
			System.out.println("Done!");
			bw.close();
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String parapairFilePath = args[0];
		String indexPath = args[1];
		String outPath = args[2];
		GenerateTrainingData gen = new GenerateTrainingData();
		gen.produceTrainingData(parapairFilePath, indexPath, outPath);
	}

}
