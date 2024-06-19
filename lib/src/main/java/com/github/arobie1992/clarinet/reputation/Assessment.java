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
     * The possible status values for an assessment. Each has a priority conforming to how it should permit transitions.
     * The order of priorities is {@link Status#STRONG_PENALTY} > {@link Status#WEAK_PENALTY} >{@link Status#REWARD} >
     * {@link Status#NONE}. The exact values of the priorities are not part of the public API and should not be relied
     * upon, but this relative ordering is stable.
     */
    public enum Status {
        /**
         * No assessment has been made for the given message for the peer.
         * <p>
         * This is the default value and does not affect reputation.
         */
        NONE(0),
        /**
         * The message has been deemed to conform to Clarinet conventions.
         * <p>
         * This status denotes that the reputation for the peer should improve up to, but not past, the maximum value.
         */
        REWARD(1),
        /**
         * The message has been deemed to not conform to Clarinet conventions, but the source of aberration is unclear.
         * <p>
         * This status denotes that the reputation for the peer should suffer a small decrease, down two but not past
         * the minimum value. What defines small is entirely up to implementors, but must be less than
         * {@link Status#STRONG_PENALTY}. More formally:
         * <pre>
         *     given reputation(p1) == reputation(p2)
         *         and reputation(p1) > val(weakPen)   // i.e. the decrease will not reduce the reputation to 0
         *     if weakPen(p1)
         *         and strongPen(p2)
         *     then reputation(p1) > reputation(p2)
         * </pre>
         */
        WEAK_PENALTY(2),
        /**
         * The message has been deemed to not conform to Clarinet conventions, and the source of aberration can be
         * definitively determined.
         * <p>
         * This status denotes that the reputation for the peer should suffer a large decrease down to but not past
         * the minimum value. What defines large is entirely up to implementors, but must be greater than
         * {@link Status#WEAK_PENALTY}. More formally:
         * <pre>
         *     given reputation(p1) == reputation(p2)
         *         and reputation(p1) > val(weakPen)   // i.e. the decrease will not reduce the reputation to 0
         *     if weakPen(p1)
         *         and strongPen(p2)
         *     then reputation(p1) > reputation(p2)
         * </pre>
         */
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
