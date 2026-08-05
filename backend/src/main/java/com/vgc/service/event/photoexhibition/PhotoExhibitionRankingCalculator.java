package com.vgc.service.event.photoexhibition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Pure prize-band calculation shared by preview and finalization. */
public final class PhotoExhibitionRankingCalculator {
    private static final Band[] BANDS = {
            new Band("FIRST", 1, 500), new Band("SECOND", 2, 300), new Band("THIRD", 1, 0)
    };
    private PhotoExhibitionRankingCalculator() { }
    public static Result calculate(List<Candidate> candidates) {
        List<Candidate> ranked = candidates.stream().filter(c -> c.voteCount() > 0)
                .sorted(Comparator.comparingInt(Candidate::voteCount).reversed().thenComparingLong(Candidate::submissionId)).toList();
        List<Recipient> recipients = new ArrayList<>(); int offset = 0;
        for (Band band : BANDS) {
            if (offset >= ranked.size()) break;
            int cutoffIndex = Math.min(offset + band.baseSlots() - 1, ranked.size() - 1);
            int cutoff = ranked.get(cutoffIndex).voteCount(); int end = cutoffIndex + 1;
            while (end < ranked.size() && ranked.get(end).voteCount() == cutoff) end++;
            for (int i = offset; i < end; i++) recipients.add(new Recipient(ranked.get(i).submissionId(), ranked.get(i).voteCount(), band.tier(), band.drops()));
            offset = end;
        }
        return new Result(recipients, ranked.size());
    }
    public record Candidate(long submissionId, int voteCount) { }
    public record Recipient(long submissionId, int voteCount, String tier, int drops) { }
    public record Result(List<Recipient> recipients, int positiveCandidateCount) { public int rankDrops(){ return recipients.stream().mapToInt(Recipient::drops).sum(); } }
    private record Band(String tier, int baseSlots, int drops) { }
}
