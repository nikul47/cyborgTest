/*
 * 
 * Copyright 2013 Digital Audio Processing Lab, Indian Institute of Technology.  
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "license.terms" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */

/**
 *
 * @author  : Nicool
 * @contact : nicool@iitb.ac.in
 */

package edu.iitb.cyborg.aligner;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

public class FilesLoader {
	
	private static HashMap<String, String> hashMapMdef;
	private static HashMap<String, String> hashMapDict;
	
	//-------------------//
	static float mean[][][];
	static float var[][][];
	static float mixWt[][];
	static float tmat[][][];
	static float feat[][];
	static int gaussian;
	static int senones;
	static int ci_states;
	//-------------------//

	/**
     * Reads a 'mdef' file and loads it into HashMap. Each value is considered to be seperated by tab ('\t') <br> <br>
     * 
	 * <b>Line Format</b> : #base	lft	rt	position	attrib	tmat	stateId's	LnEnd(N)<br>
	 * <b>eg</b>: a	SIL	d'	b	n/a	11	265	296	324	N <br> <br>  
     *
     * <b>Hash Entry Example:</b><br>
     * <blockquote>
     * <b>Key</b>   : a	SIL	d'	b <br>
     * <b>Value</b> : n/a	11	265	296	324	N <br> 
     * </blockquote>
     *  
     * @return     void
     *
     * @exception  IOException  If an I/O error occurs
     * 
     */
	
	public void loadMdef(String pathToMdef) throws IOException {
		
		hashMapMdef = new HashMap<String, String>();
		File fileMdef = new File(pathToMdef);
		BufferedReader brMdef = new BufferedReader(new FileReader(fileMdef));
		brMdef.readLine();
		
		// Line read from file 
		String templineMain;		
		// Line for temporary processing
		String tempLine;		
		// Extracted triPhone portion from line
		String triPhone;		
		// Extracted states portion from line
		String states;
		
		Boolean startProcessingLines = false;
		
		try {		
			while((templineMain = brMdef.readLine()) != null) {			
				if(startProcessingLines) {					
					tempLine = templineMain.substring(templineMain.indexOf("\t")+1);
					tempLine = tempLine.substring(tempLine.indexOf("\t")+1);
					tempLine = tempLine.substring(tempLine.indexOf("\t")+1);
					tempLine = tempLine.substring(tempLine.indexOf("\t")+1);
					
					triPhone = templineMain.substring(0,templineMain.indexOf(tempLine)-1);
					states = tempLine;
					
					hashMapMdef.put(triPhone, states);
					//System.out.println(templineMain.substring(0,templineMain.indexOf(tempLine)-1)+"\tSeperator\t"+tempLine);
				}				
													
				if(templineMain.startsWith("#base"))
					startProcessingLines = true;
			}
		} catch(Exception e) {
			System.out.println(e.toString());
		} finally {
			if(brMdef != null){
				System.out.println("Closing brMdef...");
				brMdef.close();
			}
				
		}
	
	}
	
	/**
	 * Reads a Dictionary file and stores it into HashMap. Word and phoneme sequence assumed to be separated by tab (\t).<br>
	 * 
	 * <b>Line format</b> : word	phonemes
	 * <b>eg</b>: ahamadanagara 	a h a m a d n a g a r
	 * 
	 * <b>Hash Entry example</b>
	 * <blockquote>
	 * <b>Key:</b> ahamadanagara<br>
	 * <b>Value:</b> a h a m a d n a g a r<br>
	 * </blockquote>
	 * 
	 * @param pathToDict
	 * @throws IOException
	 */
	
	public void loadDict(String pathToDict) throws IOException	{
		
		hashMapDict = new HashMap<String, String>();
		File fileDict = new File(pathToDict);
		BufferedReader brDict = new BufferedReader(new FileReader(fileDict));
			
		// Line read from file
		String tempLineMain = null;		
		// Extracted word from line
		String word = null;		
		// Extracted phonemes from line
		String phonems = null;
		
		try {
			while((tempLineMain = brDict.readLine()) != null) {			
				word = tempLineMain.substring(0,tempLineMain.indexOf("\t")).trim();
				phonems = tempLineMain.substring(tempLineMain.indexOf("\t")+1, tempLineMain.length());
				hashMapDict.put(word, phonems);
				//System.out.println(word+"\tSeperator\t"+phonems);
			}
			
		} catch(StringIndexOutOfBoundsException e){
			System.out.println("Dictionary content is in bad format!!");
		} catch(Exception e){			
			System.out.println("Something went wrong with loadind Dictionary : "+e.toString()+tempLineMain+word+phonems);
		} finally {			
			if(brDict != null) {				
				System.out.println("Closing brDict...");
				brDict.close();				
			}				
		}
	}
	
	//-------------------//
	
	/**
	 * This function takes the binary mean file given as an
	 * argument and stores it in a static 3-d array 'mean[][][]' for fast computation.
	 * 
	 * The format of the mean array is as follows:<br>
	 * <li>First dimension - senoneID </li>
	 * <li>Second dimension - gaussian </li>
	 * <li>Third dimension - feature number </li> <br>
	 * e.g. mean[500][4][38] refers to 500th senone, 4th gaussian and
	 * 38th feature.<br>
	 * SenoneID, gaussian and feature vectors starts from index <b>0</b> 
	 * and goes on till the number of senones, gaussians and 
	 * feature vectors <b> minus 1.</b>
	 * @param path path of the binary file
	 * @throws IOException
	 */
	public void mean_read(String path) throws IOException {
		
		FileInputStream is = null;
		DataInputStream dis = null;
		try{
			is = new FileInputStream(path);
			dis = new DataInputStream(new BufferedInputStream(is));
			senones = (int)dis.readFloat();
			gaussian = (int)dis.readFloat();
			mean = new float[senones][gaussian][39];
			for(int i = 0; i < senones; i++)
				for(int j = 0; j < gaussian; j++)
					for(int k = 0 ; k < 39; k++)
						mean[i][j][k] = dis.readFloat();
			
		}catch(Exception e){
			System.out.println(path + " file missing");
			e.printStackTrace();	
		}
		finally{
			if(is != null)is.close();
			if(dis != null)dis.close();
			}
	}
		
	/**
	 * * This function takes the binary variance file given as an
	 * argument and stores it in a static 3-d array 'var[][][]' for
	 *  fast computation.
	 * 
	 * The format of the variance array is as follows:<br>
	 * <li>First dimension - senoneID </li>
	 * <li>Second dimension - gaussian </li>
	 * <li>Third dimension - feature number </li> <br>
	 * e.g. var[500][4][38] refers to 500th state, 4th gaussian and
	 * 38th feature.<br>
	 * senoneID, gaussian and feature vectors starts from index <b>0</b> 
	 * and goes on till the number of senones, gaussians and 
	 * feature vectors <b> minus 1.</b>
	 * @param path path of the binary file
	 * @throws IOException
	 */
	public void var_read(String path) throws IOException {
		FileInputStream is = null;
		DataInputStream dis = null;
		try{
			is = new FileInputStream(path);
			dis = new DataInputStream(new BufferedInputStream(is));
			senones = (int)dis.readFloat();
			gaussian = (int)dis.readFloat();
			var = new float[senones][gaussian][39];
			for(int i = 0; i < senones; i++)
				for(int j = 0; j < gaussian; j++)
					for(int k = 0 ; k < 39; k++)
						var[i][j][k] = dis.readFloat();
			
		}catch(Exception e){
			System.out.println(path + " file missing");
			e.printStackTrace();
		}
		finally{
			if(is != null)is.close();
			if(dis != null)dis.close();
			}
	}

	/**
	 * This function takes the binary transition matrix file given 
	 * as an argument and stores it in a static 3-d array 'tmat[][][]' 
	 * for fast computation.
	 * 
	 * The format of the tmat array is as follows:<br>
	 * <li>First dimension - phone ID </li>
	 * <li>Second dimension - transition from </li>
	 * <li>Third dimension - transition to.</li> <br>
	 * e.g. tmat[60][1][2] refers to 60th CI phone with a transition
	 * from first to  second state.<br>
	 * The assumption is that the HMM cant skip state. <br>
	 * So tmat[60][1][3], tmat[60][0][2] will be zero and so on.<br>
	 * State, transition from and transition to starts from index <b>0</b> 
	 * and goes on till the number of CI phones and the number of 
	 * states per senone/CI phone.
	 * @param path path of the binary file
	 * @throws IOException
	 */
	public void tmat_read(String path) throws IOException {
		FileInputStream is = null;
		DataInputStream dis = null;
		try{
			is = new FileInputStream(path);
			dis = new DataInputStream(new BufferedInputStream(is));
			ci_states = (int)dis.readFloat();
			int states_per_triphone = (int)dis.readFloat();
			tmat = new float[ci_states][states_per_triphone][states_per_triphone];
			for(int i = 0; i < ci_states; i++)
				{
				for(int j = 0; j < states_per_triphone - 1 ; j++){
					for(int k = j ; k <= j+1; k++)
						tmat[i][j][k] = dis.readFloat();
					}
				}
		}catch(Exception e){
			System.out.println(path + " file missing");
			e.printStackTrace();
		}
		finally{
			if(is != null)is.close();
			if(dis != null)dis.close();
			}
	}
	
	/**
	 * This function takes the binary mixture weight file given 
	 * as an argument and stores it in a static 2-d array 'mixWt[][]' 
	 * for fast computation.
	 * 
	 * The format of the mixWt array is as follows:<br>
	 * <li>First dimension - senoneID;</li>
	 * <li>Second dimension - Gaussian mixture number; </li><br>
	 * e.g. mixWt[400][8] refers to 400th senone and 8th gaussian.<br>
	 * SenoneID and gaussian mix no. starts from index <b>0</b> 
	 * and goes on till the number of senones and gaussians
	 * minus 1.</b>
	 * @param path
	 * @throws IOException
	 */
	public void mixWt_read(String path) throws IOException {
		FileInputStream is = null;
		DataInputStream dis = null;
		try{
			is = new FileInputStream(path);
			dis = new DataInputStream(new BufferedInputStream(is));
			senones = (int)dis.readFloat();
			gaussian = (int)dis.readFloat();
			mixWt = new float[senones][gaussian];
			for(int i = 0; i < senones; i++)
				for(int j = 0; j < gaussian; j++)
						mixWt[i][j] = dis.readFloat();
			
		}catch(Exception e){
			System.out.println(path + " file missing");
			e.printStackTrace();
		}
		finally{
			if(is != null)is.close();
			if(dis != null)dis.close();
			}
	}
	
	/**
	 * This function read all the binary files (i.e mean,
	 * variance, transition mat and mixture weights) stored in the 
	 * directory 'dir' given as an argument. All these files have
	 * to be present in the directory.
	 * @param dir
	 * @throws IOException
	 */
	public void initialize(String dir) throws IOException {
		this.mean_read(dir + "/mean_bin");
		this.var_read(dir + "/variance_bin");
		this.tmat_read(dir + "/tmat_bin");
		this.mixWt_read(dir + "/mixWt_bin");
	}
	
	/**
	 * This function takes the path of the MFC file and
	 * stores the features in a 2-d static array feat[][].<br>
	 * Format of the array is as follows:
	 * <li> First dimension - time frame</li>
	 * <li> Second dimension - feature number</li> <br>
	 * Total number of features are 39.<br>
	 * e.g. feat[5][30] refers to the 6th time frame and 31st feature. <br>
	 * Remember the index for both the time frame and the feature number starts from 0.
	 * @param path
	 * @throws IOException
	 */
	public void readFeat(String path) throws IOException {
		
		FileInputStream is = null;
		DataInputStream dis = null;
		try{
			is = new FileInputStream(path);
			dis = new DataInputStream(new BufferedInputStream(is));
			int frames = (dis.readInt())/39;
			//System.out.println(frames);
			feat = new float[frames][39];
			for(int i = 0; i < frames; i++)
				for(int j = 0; j < 39; j++)
						feat[i][j] = dis.readFloat();
						
		}catch(Exception e){
			System.out.println(path + " file missing");
			e.printStackTrace();	
		}
		finally{
			if(is != null)is.close();
			if(dis != null)dis.close();
			}
	}

	
	//------------------//
	
	/**
	 *  
	 * @param String of triPhones where each phone is separated by tab ('\t') <br> eg : a	SIL	d'	b
	 * @return Integer array containing corresponding attrib, tmat and states of triPhone <br>
	 * eg: <br>
	 * states[0] = 1    // attrib[0: filler and 1: n/a] <br>
	 * states[1] = 11   // tmat <br>
	 * states[2] = 265  // state no of 'a' <br>
	 * states[3] = 296  // state no of 'SIL' <br>
	 * states[4] = 324  // state no of 'b' <br>
	 * 
	 */

	public int[] getStates(String triPhones) {
		
		int states[] = new int[5];
		String statesString[] = new String[6];
		//System.out.println(hashMapMdef.get(triPhones));
		
		try {
			statesString = hashMapMdef.get(triPhones).split("\t");
			
			states[0] = statesString[0].equals("filler") ? 0 : 1 ;
			states[1] = Integer.parseInt(statesString[1]);
			states[2] = Integer.parseInt(statesString[2]);
			states[3] = Integer.parseInt(statesString[3]);
			states[4] = Integer.parseInt(statesString[4]);
		}
		catch(NullPointerException e) {
			System.out.println("'" + triPhones.replace("\t", " ")+ "'" + " : TriPhone doesn't exist in 'mdef' file.");
		}
		
		
		return states;
	}
	
	/**
	 * 
	 * @param word <br> <b>eg:</b> ahamadanagara 
	 * @return <b>String</b> containing phoneme sequence of the word <br>
	 *         <b>eg:</b> a h a m a d n a g a r
	 */
	
	public static String getPhonemes(String word) {
		
		return hashMapDict.get(word);
	}

	/**
	 * 
	 * @param transcription <br> <b>eg:</b> laala deshii makaa
	 * @return <b>String</b> containing phoneme sequence of the transcription with initial and end SIL added <br>
	 *         <b>eg:</b> SIL l aa l d e sh ii m a k aa SIL
	 */
	public static String getPhonemsOfTrans(String transcription) {
		
		String words[] = transcription.split(" ");
		String phonems = "";
		
		System.out.println("\nActual transcription  : " + transcription);
		for(int Index = 0; Index < words.length; Index++) {
			phonems = phonems + " "+getPhonemes(words[Index])+" ";
			//System.out.println(getPhonemes(words[Index]));
		}
		
		return "SIL "+phonems.replaceAll("\\s+", " ").trim()+" SIL";
	}
	
	/**
	 * 
	 * @param transcription
	 * @return <b>Integer</b> array containing states of each triPhone of transcription
	 */
	
	public int[][] getStatesOfTrans(String transcription) {
		 
		String phonems = FilesLoader.getPhonemsOfTrans(transcription);
		int states[][];
		
		int indexFirst = 0;
		
		System.out.println("Phonem sequence       : "+phonems);
		
		String phonemsArray[]= phonems.split(" ");
		int noOfTriPhones = phonemsArray.length - 2;
		System.out.println("No of tri Phones      : "+noOfTriPhones);
		int noOfPhones = noOfTriPhones + 2;
		System.out.println("No of phones in Trans : " + noOfPhones);
		System.out.println();
		states = new int[3][3*noOfTriPhones];
		
		
		int statesTemp[] = new int[5];
		for(indexFirst = 1; indexFirst <= noOfTriPhones; indexFirst+=1) {
			
			if(indexFirst == 1){
				System.out.print("Tri Phone : "+phonemsArray[indexFirst]+"\t"+phonemsArray[indexFirst - 1]+"\t"+phonemsArray[indexFirst+1]+"\tb");
				statesTemp = getStates(phonemsArray[indexFirst]+"\t"+phonemsArray[indexFirst - 1]+"\t"+phonemsArray[indexFirst+1]+"\tb");
				System.out.println("\tStates : "+statesTemp[0]+" "+statesTemp[1]+" "+statesTemp[2]+" "+statesTemp[3]+" "+statesTemp[4]);
			}
			else if((indexFirst < noOfTriPhones) && (indexFirst > 1 )){
				System.out.print("Tri Phone : "+phonemsArray[indexFirst]+"\t"+phonemsArray[indexFirst - 1]+"\t"+phonemsArray[indexFirst+1]+"\ti");
				statesTemp = getStates(phonemsArray[indexFirst]+"\t"+phonemsArray[indexFirst - 1]+"\t"+phonemsArray[indexFirst+1]+"\ti");
				System.out.println("\tStates : "+statesTemp[0]+" "+statesTemp[1]+" "+statesTemp[2]+" "+statesTemp[3]+" "+statesTemp[4]);
			}
			else if(indexFirst == noOfTriPhones){
				System.out.print("Tri Phone : "+phonemsArray[indexFirst]+"\t"+phonemsArray[indexFirst - 1]+"\t"+phonemsArray[indexFirst+1]+"\te");
				statesTemp = getStates(phonemsArray[indexFirst]+"\t"+phonemsArray[indexFirst - 1]+"\t"+phonemsArray[indexFirst+1]+"\te");
				System.out.println("\tStates : "+statesTemp[0]+" "+statesTemp[1]+" "+statesTemp[2]+" "+statesTemp[3]+" "+statesTemp[4]);
			}
			
		
			states[0][((indexFirst -1)*3)] = statesTemp[2]; 
		    states[0][((indexFirst -1)*3)+1] = statesTemp[3];
		    states[0][((indexFirst -1)*3)+2] = statesTemp[4];
		    
		    states[1][((indexFirst -1)*3)] = statesTemp[1]; 
		    states[1][((indexFirst -1)*3)+1] = statesTemp[1];
		    states[1][((indexFirst -1)*3)+2] = statesTemp[1];
		    
		    states[2][((indexFirst -1)*3)] = statesTemp[0]; 
		    states[2][((indexFirst -1)*3)+1] = statesTemp[0];
		    states[2][((indexFirst -1)*3)+2] = statesTemp[0];
		}
	
		return states;
	}
}