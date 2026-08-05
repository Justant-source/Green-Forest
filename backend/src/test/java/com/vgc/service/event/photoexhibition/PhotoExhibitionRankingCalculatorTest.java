package com.vgc.service.event.photoexhibition;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static com.vgc.service.event.photoexhibition.PhotoExhibitionRankingCalculator.Candidate;

class PhotoExhibitionRankingCalculatorTest {
 @Test void normalBands_areOneTwoOne(){ var r=calc(9,8,7,6,5); assertThat(r.recipients()).extracting(x->x.tier()).containsExactly("FIRST","SECOND","SECOND","THIRD"); }
 @Test void firstCutoffTie_expands(){ var r=calc(9,9,8,8,7); assertThat(r.recipients()).extracting(x->x.tier()).containsExactly("FIRST","FIRST","SECOND","SECOND","THIRD"); }
 @Test void secondCutoffTie_expands(){ var r=calc(10,9,8,8,7); assertThat(r.recipients()).extracting(x->x.tier()).containsExactly("FIRST","SECOND","SECOND","SECOND","THIRD"); }
 @Test void thirdCutoffTie_expands(){ var r=calc(10,9,8,7,7); assertThat(r.recipients()).extracting(x->x.tier()).containsExactly("FIRST","SECOND","SECOND","THIRD","THIRD"); }
 @Test void allTied_getFirstBandOnly(){ var r=calc(3,3,3,3); assertThat(r.recipients()).hasSize(4).allSatisfy(x->assertThat(x.tier()).isEqualTo("FIRST")); }
 @Test void insufficientPositiveWorks_areRankedWithoutZeros(){ var r=calc(4,0,-1); assertThat(r.positiveCandidateCount()).isEqualTo(1); assertThat(r.recipients()).extracting(x->x.tier()).containsExactly("FIRST"); }
 private static PhotoExhibitionRankingCalculator.Result calc(int... votes){ return PhotoExhibitionRankingCalculator.calculate(java.util.stream.IntStream.range(0,votes.length).mapToObj(i->new Candidate(i+1,votes[i])).toList()); }
}
