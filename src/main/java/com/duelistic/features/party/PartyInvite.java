package com.duelistic.features.party;

import java.util.UUID;

public class PartyInvite {
    private UUID inviter;
    private UUID invited;

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
