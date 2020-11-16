package dogs.sim;

import java.util.*;
import java.io.*;

import dogs.sim.Owner.OwnerName;


public class Dictionary {
	
	public static List<String> words;
	
	static {
		words = new ArrayList<>();
		BufferedReader dictionaryReader;
		try {
			dictionaryReader = new BufferedReader(new FileReader("dogs/sim/dictionary.txt"));
			String line;
			while((line = dictionaryReader.readLine()) != null) {
				String trimmedLine = line.trim();
				if(trimmedLine.length() <= 10)
					words.add(trimmedLine.toLowerCase());
			}
			for(OwnerName ownerName : OwnerName.values()) {
				String ownerNameAsString = ownerName.name().toLowerCase();
				if(!words.contains(ownerNameAsString))
					words.add(ownerNameAsString);
			}
			words.add("_");
		} catch(Exception e) {
			Log.writeToLogFile("Cannot read dictionary file!");
		}
	}
	
	public static boolean isInDictionary(String newWord) {
		return words.contains(newWord.toLowerCase());
	}
	
	public static boolean areAllInDictionary(List<String> newWords) {
		for(String newWord : newWords)
			if(!words.contains(newWord.toLowerCase()))
				return false;
		return true;
	}

	public static List<String> getAllWordsContaining(String substring) {
		List<String> newWords = new ArrayList<>();
		for(String word : words)
			if(word.contains(substring.toLowerCase()))
				newWords.add(word);
		return newWords;
	}
	
	public static List<String> getAllWordsStartingWith(String prefix) {
		List<String> newWords = new ArrayList<>();
		for(String word : words)
			if(word.startsWith(prefix.toLowerCase()))
				newWords.add(word);
		return newWords;
	}
	
	public static List<String> getAllWordsEndingWith(String suffix) {
		List<String> newWords = new ArrayList<>();
		for(String word : words)
			if(word.endsWith(suffix.toLowerCase()))
				newWords.add(word);
		return newWords;
	}	
}