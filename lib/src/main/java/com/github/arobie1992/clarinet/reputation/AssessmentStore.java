package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;

public interface AssessmentStore {
    /**
     * Save the given assessment to the backing store per the {@link Assessment.Status} priorities and call
     * {@code reputationCallback} if this operation would return {@code true}.
     * <p>
     * If no assessment matching {@code assessment}'s {@code peerId} and {@code messageId} is present, persist
     * {@code assessment} regardless of status. If an assessment {@code p} matching {@code assessment}'s {@code peerId}
     * and {@code messageId} is present, only persist {@code assessment} if {@code assessment.comparePriority(p) >= 0}.
     * If {@code assessment.comparePriority(p) == 0}, implementations may choose whether to persist or not.
     * <p>
     * The persisted value prior to updates is used as the {@code existing} and the passed {@code assessment} is used as
     * the {@code updated} value in the call to {@link ReputationCallback#update(Assessment, Assessment)}.
     * <p>
     * Implementations must ensure that invocations of {@code reputationCallback} are thread safe with regard to
     * {@link Assessment.Status} priorities. While implementations cannot ensure that the passed
     * {@code reputationCallback} itself is thread safe, the supporting code that calls it must be. Additionally, series
     * of updates must be idempotent and reflect sequential atomic operations so that double counting does not occur.
     * For example, if two threads each attempt to save a new assessment, one with status
     * {@link Assessment.Status#REWARD} and one with {@link Assessment.Status#STRONG_PENALTY}, then two calls should be
     * made to {@code reputationCallback} reflecting either:
     * <ol>
     *     <li>{@code NONE} -> {@code REWARD}</li>
     *     <li>{@code REWARD} -> {@code STRONG_PENALTY}</li>
     * </ol>
     * or:
     * <ol>
     *     <li>{@code NONE} -> {@code STRONG_PENALTY}</li>
     *     <li>{@code STRONG_PENALTY} -> {@code REWARD}, i.e. no action taken as {@code STRONG_PENALTY} cannot be
     *     downgraded to {@code REWARD}.</li>
     * </ol>
     * For example, this ordering is not permitted:
     * <ol>
     *     <li>{@code NONE} -> {@code REWARD}</li>
     *     <li>{@code NONE} -> {@code STRONG_PENALTY}</li>
     * </ol>
     *
     * @param assessment the {@code Assessment} to attempt to save.
     * @param reputationCallback A callback that can be used to update a persisted reputation.
     * @return {@code true} if {@code assessment} was persisted; {@code false} otherwise.
     */
    boolean save(Assessment assessment, ReputationCallback reputationCallback);

    @FunctionalInterface
    interface ReputationCallback {
        void update(Assessment existing, Assessment updated);
    }

    /**
     * Find the {@link Assessment} uniquely identified by the combination of {@code peerId} and {@code messageId}.
     * <p>
     * If the backing store does not contain an {@code Assessment} for the parameters, return an {@code Assessment} with
     * the {@code peerId} and {@code messageId} populated and a {@code status} of {@link Assessment.Status#NONE}.
     * Implementations may decide to implicitly persist this returned default or not.
     * @param peerId the {@code PeerId} of the {@code Assessment}.
     * @param messageId the {@code MessageId} of the {@code Assessment}.
     * @return the persisted {@code Assessment} or a default per the specifications.
     */
    Assessment find(PeerId peerId, MessageId messageId);
}
