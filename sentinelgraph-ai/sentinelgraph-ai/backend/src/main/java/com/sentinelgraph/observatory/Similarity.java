package com.sentinelgraph.observatory;
import java.text.Normalizer;
import java.util.*;
/** Same deterministic algorithm is used by manual tests and observation scans. */
public final class Similarity {
 private Similarity(){}
 public static String normalize(String input){
  String s=Normalizer.normalize(input,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
  String from="аесорхуіјѕοΙⅼ", to="aecopxyijsoIl";
  for(int i=0;i<from.length();i++)s=s.replace(from.charAt(i),Character.toLowerCase(to.charAt(i)));
  return s.replaceAll("[^\\p{L}\\p{N}]","");
 }
 public static Map<String,Object> compare(String brand,String candidate){
  String a=normalize(brand),b=normalize(candidate);
  int[][] d=new int[a.length()+1][b.length()+1];
  for(int i=0;i<=a.length();i++)d[i][0]=i;
  for(int j=0;j<=b.length();j++)d[0][j]=j;
  for(int i=1;i<=a.length();i++)for(int j=1;j<=b.length();j++)d[i][j]=Math.min(Math.min(d[i-1][j]+1,d[i][j-1]+1),d[i-1][j-1]+(a.charAt(i-1)==b.charAt(j-1)?0:1));
  double score=Math.round(10000.0*(1-(double)d[a.length()][b.length()]/Math.max(1,Math.max(a.length(),b.length()))))/100.0;
  List<Map<String,Object>> differences=new ArrayList<>();
  int[] ac=brand.codePoints().toArray(),bc=candidate.codePoints().toArray();
  for(int i=0;i<Math.max(ac.length,bc.length);i++)if(i>=ac.length||i>=bc.length||ac[i]!=bc[i])differences.add(Map.of("position",i,"official",i<ac.length?new String(Character.toChars(ac[i])):"∅","candidate",i<bc.length?new String(Character.toChars(bc[i])):"∅","codePoint",i<bc.length?String.format("U+%04X",bc[i]):"∅"));
  return Map.of("algorithm","NFKC + limited confusable mapping + punctuation/spacing removal + Levenshtein","normalizedBrand",a,"normalizedCandidate",b,"editDistance",d[a.length()][b.length()],"similarity",score,"rawDifferences",differences,"addedWords",b.startsWith(a)&&b.length()>a.length(),"normalizationChanged",!brand.equals(candidate)&&a.equals(b),"limitation","Limited confusable mapping; resemblance alone does not establish impersonation.");
 }
}
