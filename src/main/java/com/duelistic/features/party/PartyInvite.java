package com.duelistic.features.party;

import java.util.UUID;

public class PartyInvite {
    private final UUID inviter;
    private final UUID invited;

    public PartyInvite(UUID inviter, UUID invited) {
        this.inviter = inviter;
        this.invited = invited;
    }

    public UUID getInvited() {
        return invited;
    }

    public UUID getInviter() {
        return inviter;
    }
}
