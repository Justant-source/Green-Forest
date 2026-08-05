package com.vgc.dto.event.photoexhibition;
import java.util.List;
public class PhotoExhibitionPreviewResponse {
 private int validParticipantCount, uniqueVoterCount, selectionCount, participantRewardTotal, voterRewardTotal, rankRewardTotal, grandTotal;
 private List<Candidate> candidates;
 public PhotoExhibitionPreviewResponse(int p,int v,int s,int pt,int vt,int rt,List<Candidate> c){validParticipantCount=p;uniqueVoterCount=v;selectionCount=s;participantRewardTotal=pt;voterRewardTotal=vt;rankRewardTotal=rt;grandTotal=pt+vt+rt;candidates=c;}
 public int getValidParticipantCount(){return validParticipantCount;} public int getUniqueVoterCount(){return uniqueVoterCount;} public int getSelectionCount(){return selectionCount;} public int getParticipantRewardTotal(){return participantRewardTotal;} public int getVoterRewardTotal(){return voterRewardTotal;} public int getRankRewardTotal(){return rankRewardTotal;} public int getGrandTotal(){return grandTotal;} public List<Candidate> getCandidates(){return candidates;}
 public record Candidate(Long submissionId,String authorNickname,String title,int voteCount,String proposedTier,int reward){}
}
