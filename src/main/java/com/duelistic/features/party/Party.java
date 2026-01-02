package com.duelistic.features.party;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Party {
    private final Set<PartyUser> users;
    private final UUID partyId;

    public Party(UUID leaderId) {
        partyId = UUID.randomUUID();
        users = new HashSet<>();
        users.add(new PartyUser(leaderId, PartyRank.LEADER));
    }

    public Set<PartyUser> getUsers() {
        return users;
    }

    public UUID getPartyId() {
        return partyId;
    }

    public PartyUser getPartyUser(UUID userId) {
        for (PartyUser user : users) {
            if (user.getUniqueId().equals(userId))
                return user;
        }
        return null;
    }
}
