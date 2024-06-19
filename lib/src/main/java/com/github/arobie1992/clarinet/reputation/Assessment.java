package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.Objects;

public record Assessment(PeerId peerId, MessageId messageId, Status status) {
    public Assessment {
        Objects.requireNonNull(peerId);
        Objects.requireNonNull(messageId);
        Objects.requireNonNull(status);
    }

    /**
     * Attempt to create a new {@code Assessment} with the provided {@code status} per the {@link Status} priorities.
     * @param status the {@link Status} to attempt to set.
     * @return a new {@code Assessment} with a status of {@code status} if
     *         {@code this.status.comparePriority(status) > 0}; {@code this} otherwise.
     */
    public Assessment updateStatus(Status status) {
        return status.value <= this.status.value
                ? this
                : new Assessment(peerId, messageId, status);
    }

    /**
     * The possible status values for an assessment. Each has a priority conforming to
     */
    public enum Status {
        NONE(0),
        REWARD(1),
        WEAK_PENALTY(2),
        STRONG_PENALTY(3);

        private final int value;

        Status(int value) {
            this.value = value;
        }

        public int comparePriority(Status other) {
            return Integer.compare(this.value, other.value);
        }
    }
}
