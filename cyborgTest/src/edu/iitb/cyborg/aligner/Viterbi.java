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
 * @author  : Jigar Gada
 */

package edu.iitb.cyborg.aligner;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * This program takes the path of the folder where all the model files 
 * (i.e mean, variances, mixture weights and transition matrices) are stored and takes the
 * input file which is to be aligned. 
 * It computes the MFC features of the wav file and stores it in the same path as of audioInput file.
 * <p/>
 * Compulsary parameters
 * <li> -models folder where all the model files are stored</li>
 * <li> -i audio input file</li>
 *
 */

public class Viterbi {
	
	double oldCost;
	double newCost;
	int backptr[];
	int index;
	
	/**
	 * This function takes the state number and the feature vector and
	 * calculates the log likelihood value.
	 * @param s - State number
	 * @param x - Feature vector (39 dimensions)
	 */
	public static double b(int s,float x[]) {
		double Z[] = new double[FilesLoader.gaussian];
		for (int k = 0 ;k < FilesLoader.gaussian; k++){
			double det = 1.0;
			for (int q = 0; q < 39; q++)
				det = det * FilesLoader.var[s][k][q];
			Z[k] = FilesLoader.mixWt[s][k] /( Math.pow(2*Math.PI,39.0/2) * det);
		}
		double Prob = 0.0;
		double dist;
			for(int k = 0; k < FilesLoader.gaussian ;k++){
				dist = 0;
				for(int q = 0; q < 39; q++){
					dist = dist +  Math.pow((x[q] - FilesLoader.mean[s][k][q]),2)/FilesLoader.var[s][k][q];
				}
			Prob = Prob + Z[k] * Math.exp(-dist / 2);
		}
		double log_likelihood = Math.log(Prob)/Math.log(1.003);
		return log_likelihood;
	}
	
	/**
	 * This function returns the transition probability.  
	 * @param s - state number
	 * @param from - transition from
	 * @param to - transition to
	 * 
	 */
	public static double a(int s, int from, int to) {
		float temp;
		//This condition is if the transition is from one HMM 
		//to another HMM
		if((from != to) && (to % 3 == 0))
			temp = FilesLoader.tmat[s][2][3];
		else
		// this condition is for transition from one state to another
		// of same HMM
		temp = FilesLoader.tmat[s][from%3][to%3];
		
		double val = Math.log(temp)/Math.log(1.003);
		return val;
	}
	
	
	public static void main(String[] args) throws IOException {
		
		String models = null;
		String audioInput = null;
		String transcription = null;
		String dictionary = null;
		
		for(int i = 0; i < args.length ; i++){
			if(args[i].equals("-models")){
				models = args[++i];
			}
			if(args[i].equals("-i")){
			audioInput = args[++i];
			}
			if(args[i].equals("-t")){
				transcription = args[++i];
			}
			if(args[i].equals("-dict")){
				dictionary = args[++i];
			}
			
		}
		if(models == null || audioInput == null){
			System.out.println("Insuffient arguments\n Usage ---> \n" +
			"java -jar <> -models <folder path which has all model files> -i <input audio file> -t <transcription file> -dict <dictionary file>");
			System.exit(0);
		}
		//long time = System.currentTimeMillis();
		FilesLoader filesLoader = new FilesLoader();
		filesLoader.initialize(models);
		
		//--------------------//
		filesLoader.loadDict(dictionary);
		filesLoader.loadMdef(models+"\\mdef_tab");
		
		BufferedReader brTrans = new BufferedReader(new FileReader(transcription));
		String trans = brTrans.readLine();
		brTrans.close();
		//-------------------//
		
		//String audioInput = "D:\\docs\\java\\workspace\\M13MH02A0100I300_silRemoved.wav";
		String audioFeature = audioInput.replace(".wav", ".mfc");
		Runtime.getRuntime().exec("java -jar wav2feat.jar -c feature_extraction.xml -name cepstraFrontEnd" +
				" -i " + audioInput + " -o " + audioFeature);
//		System.out.println("java -jar wav2feat.jar -c feature_extraction.xml -name cepstraFrontEnd" +
//		" -i " + audioInput + " -o " + audioOutput);
		filesLoader.readFeat(audioFeature);
		//double x[] = {0.95,0.98,0.51,0.99,0.45,0.42,0.21,0.19,0.83,0.72,0.52,0.82,0.51,0.55,0.21,0.50,0.14,0.05,0.68,0.60,0.21,0.45,0.62,0.55,0.12,0.16,0.00,0.43,0.48,0.15,0.66,0.01,0.11,0.95,0.97,0.03,0.49,0.86,0.24};
		int totalTimeFrames = FilesLoader.feat.length;
		float x[][] = FilesLoader.feat;
		System.out.println(totalTimeFrames);
		
		//int states[][] = {{5,6,9,11},{13,13,13,5}};
		int states[][] = filesLoader.getStatesOfTrans(trans);
				
		for(int indexI = 0; indexI < states.length; indexI++){
			for(int indexJ = 0; indexJ < states[indexI].length; indexJ++){
				System.out.print(states[indexI][indexJ]+"\t");
			}
			System.out.println();
		}
		
		int N = states[0].length;
		System.out.println("N "+N);
		Viterbi[] path = new Viterbi[N];
		
		for (int i=0;i<N;i++)
		   path[i]= new Viterbi();  // create each actual Path
		
		for(int i = 0; i < N; i++)
			path[i].backptr = new int[N];
		
//-------- Viterbi algorithm ---------------------------------------------------------
		
		path[0].newCost = b(states[0][0],x[0]);
		path[0].backptr[0]++;
		path[0].oldCost = path[0].newCost;
		
		for(int t = 1; t < totalTimeFrames ; t++){
			path[0].newCost = b(states[0][0],x[t]) + a(states[1][0],0,0) + path[0].oldCost;
			path[0].index = 2;
			
			for(int s = 1; s <= N-1 ;s++){
				if((t < N && s < t) || (t >= N)){
					double a1 = path[s-1].oldCost + a(states[1][s-1],s-1,s) + b(states[0][s],x[t]);
					double a2 = path[s].oldCost + a(states[1][s],s,s) + b(states[0][s],x[t]);
					if(a1 > a2){
						path[s].newCost = a1;
						path[s].index = 1;
					}
					else {
						path[s].newCost = a1;
						path[s].index = 2;
					}
				}
				if(t< N && s==t){
					path[s].newCost = path[s-1].oldCost + a(states[1][s-1],s-1,s) + b(states[0][s],x[t]);
					path[s].index = 1;
					}
				}
			
			for(int s = N-1; s >= 0 ; s-- ){
				path[s].oldCost = path[s].newCost;
				if(path[s].index == 1)
					System.arraycopy(path[s-1].backptr, 0, path[s].backptr, 0, path[s-1].backptr.length);
				if(path[s].index != 0)
					path[s].backptr[s]++;
			}
		}
		
		double max = path[0].newCost;
		int pos = 0;
		for(int i = 1; i < N ;i++){
			if(path[i].newCost > max){
				max = path[i].newCost;
				pos = i;
			}
		}
		System.out.println("best cost = " + max);
		//System.out.println(pos);
		System.out.println("sequence of states -->");
		for(int i = 0; i < N; i++)
			System.out.println(states[0][i] + " " + path[pos].backptr[i]);
		
	}		
		

	
   }
