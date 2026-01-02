package com.duelistic.features.party;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PartyManager {
    private final Set<Party> parties;
    private final Set<PartyInvite> partyInvites;

    public PartyManager() {
        this.parties = new HashSet<>();
        this.partyInvites = new HashSet<>();
    }

    public Party createParty(UUID leader) {
        if (isInParty(leader))
            throw new IllegalStateException("user already in party");

        Party party = new Party(leader);
        parties.add(party);
        return party;
    }

    public void deleteParty(Party party) {
        if (party == null) return;
        parties.remove(party);
    }

    /**
     * Invites player into a party
     * Returns if the invite already exists
     * @throws IllegalArgumentException If inviter isn't in a party
     * @throws IllegalArgumentException If inviter isn't a party leader
     * @param inviter UUID of the inviter
     * @param invited UUID of the invited player
     */
    public void invite(UUID inviter, UUID invited) {
        if (getInvite(inviter, invited) != null) return;
        Party p = getParty(inviter);
        if (p != null && p.getPartyUser(inviter).getRank() == PartyRank.LEADER) {
            partyInvites.add(new PartyInvite(inviter, invited));
        } else {
            throw new IllegalArgumentException("inviter must be in a party as a leader");
        }
    }

    public void handleInvite(PartyInvite invite, boolean accept) {
        if (!accept) {
            partyInvites.remove(invite);
            return;
        }

        UUID inviter = invite.getInviter();
        UUID invited = invite.getInvited();

        Party party = getParty(inviter);
        if (party == null)
            throw new IllegalArgumentException("inviter must be in a party");

        if (party.getPartyUser(inviter).getRank() != PartyRank.LEADER)
            throw new IllegalArgumentException("inviter must be a party leader");

        Party oldParty = getParty(invited);
        if (oldParty != null) {
            removeFromParty(invited, oldParty);
        }

        addToParty(invited, party);
        partyInvites.remove(invite);
    }


    public PartyInvite getInvite(UUID inviter, UUID invited) {
        for (PartyInvite invite : partyInvites) {
            if (invite.getInvited().equals(invited)
                    && invite.getInviter().equals(inviter))
                return invite;
        }
        return null;
    }

    /**
     * Adds player to party
     * @throws IllegalArgumentException Party must not be null
     * @param userId The uuid of the player
     * @param party The object of the party
     */
    public void addToParty(UUID userId, Party party) {
        if (party == null)
            throw new IllegalArgumentException("party must be not null");
        party.getUsers().add(new PartyUser(userId));
    }

    /**
     * Removes player from party
     * @throws IllegalArgumentException Party must not be null
     * @param userId The uuid of the player
     * @param party The object of the party
     */
    public void removeFromParty(UUID userId, Party party) {
        if (party == null)
            throw new IllegalArgumentException("party must be not null");

        PartyUser user = party.getPartyUser(userId);
        if (user == null) return;

        boolean wasLeader = user.getRank() == PartyRank.LEADER;

        party.getUsers().remove(user);

        if (party.getUsers().isEmpty()) {
            parties.remove(party);
            return;
        }

        if (wasLeader) {
            PartyUser newLeader = party.getUsers().iterator().next();
            newLeader.setRank(PartyRank.LEADER);
        }
    }


    /**
     * Checks if user is in a party
     * @param userId Unique id of the user for which it should get checked
     * @return If user is in a party of the set "parties"
     */
    public boolean isInParty(UUID userId) {
        for (Party party : parties) {
            if (party.getPartyUser(userId) != null)
                return true;
        }
        return false;
    }

    public Party getParty(UUID userId) {
        for (Party party : parties) {
            if (party.getPartyUser(userId) != null)
                return party;
        }
        return null;
    }

    public Set<Party> getParties() {
        return parties;
    }

    public Set<PartyInvite> getPartyInvites() {
        return partyInvites;
    }
}
