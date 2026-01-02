package com.duelistic.features.party;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages all parties and party invites.
 * Responsible for creating, deleting and modifying parties,
 * as well as handling invitations.
 */
public class PartyManager {

    /** All active parties */
    private final Set<Party> parties;

    /** All currently active party invites */
    private final Set<PartyInvite> partyInvites;

    /**
     * Creates a new PartyManager with empty party and invite sets.
     */
    public PartyManager() {
        this.parties = new HashSet<>();
        this.partyInvites = new HashSet<>();
    }

    /**
     * Creates a new party with the given user as leader.
     *
     * @param leader UUID of the party leader
     * @return the newly created party
     * @throws IllegalStateException if the user is already in a party
     */
    public Party createParty(UUID leader) {
        if (isInParty(leader))
            throw new IllegalStateException("user already in party");

        Party party = new Party(leader);
        parties.add(party);
        return party;
    }

    /**
     * Deletes a party and removes it from the manager.
     *
     * @param party the party to delete (ignored if null)
     */
    public void deleteParty(Party party) {
        if (party == null) return;
        parties.remove(party);
    }

    /**
     * Invites a player into the inviter's party.
     * <p>
     * If an invite already exists, this method does nothing.
     *
     * @param inviter UUID of the party leader sending the invite
     * @param invited UUID of the player being invited
     * @throws IllegalArgumentException if the inviter is not in a party
     *                                  or is not the party leader
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

    /**
     * Handles a party invite by either accepting or declining it.
     * <p>
     * If accepted, the invited player will be added to the inviter's party.
     * If the invited player is already in a party, they will be removed first.
     *
     * @param invite the invite to handle
     * @param accept whether the invite should be accepted
     * @throws IllegalArgumentException if the inviter is not in a party
     *                                  or is not the party leader
     */
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

    /**
     * Retrieves an existing party invite between two users.
     *
     * @param inviter UUID of the inviter
     * @param invited UUID of the invited user
     * @return the PartyInvite if found, otherwise null
     */
    public PartyInvite getInvite(UUID inviter, UUID invited) {
        for (PartyInvite invite : partyInvites) {
            if (invite.getInvited().equals(invited)
                    && invite.getInviter().equals(inviter))
                return invite;
        }
        return null;
    }

    /**
     * Adds a user to the given party.
     *
     * @param userId UUID of the user to add
     * @param party the party the user should join
     * @throws IllegalArgumentException if the party is null
     */
    public void addToParty(UUID userId, Party party) {
        if (party == null)
            throw new IllegalArgumentException("party must be not null");

        party.getUsers().add(new PartyUser(userId));
    }

    /**
     * Removes a user from a party.
     * <p>
     * If the removed user was the leader, leadership will be transferred
     * to another party member.
     * If the party becomes empty, it will be deleted.
     *
     * @param userId UUID of the user to remove
     * @param party the party the user should be removed from
     * @throws IllegalArgumentException if the party is null
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
     * Checks whether a user is currently in any party.
     *
     * @param userId UUID of the user
     * @return true if the user is in a party, otherwise false
     */
    public boolean isInParty(UUID userId) {
        for (Party party : parties) {
            if (party.getPartyUser(userId) != null)
                return true;
        }
        return false;
    }

    /**
     * Gets the party a user is currently in.
     *
     * @param userId UUID of the user
     * @return the party the user is in, or null if none exists
     */
    public Party getParty(UUID userId) {
        for (Party party : parties) {
            if (party.getPartyUser(userId) != null)
                return party;
        }
        return null;
    }

    /**
     * @return all currently active parties
     */
    public Set<Party> getParties() {
        return parties;
    }

    /**
     * @return all currently active party invites
     */
    public Set<PartyInvite> getPartyInvites() {
        return partyInvites;
    }
}
