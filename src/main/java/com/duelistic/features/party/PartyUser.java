package com.duelistic.features.party;

import java.util.UUID;

public class PartyUser {
    private UUID uniqueId;
    private PartyRank rank;

    public PartyUser(UUID uniqueId) {
        this(uniqueId, PartyRank.MEMBER);
    }

    public PartyUser(UUID uniqueId, PartyRank rank) {
        this.uniqueId = uniqueId;
        this.rank = rank;
    }

    public void setRank(PartyRank rank) {
        this.rank = rank;
    }

    public PartyRank getRank() {
        return rank;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }
}
