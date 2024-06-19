package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;

import java.util.stream.Stream;

public interface AssessmentStore {
    /**
     * Save the given assessment to the backing store per the {@link Assessment.Status} priorities.
     * <p>
     * If no assessment matching {@code assessment}'s {@code peerId} and {@code messageId} is present, save
     * {@code assessment} regardless of status.
     * <p>
     * If an assessment {p} matching {@code assessment}'s {@code peerId} and {@code messageId} is present, only save
     * {@code assessment} if {@code assessment.comparePriority(p) >= 0}.
     * @param assessment the {@code Assessment} to attempt to save.
     * @return {@code true} if {@code assessment} was persisted; {@code false} otherwise.
     */
    default boolean save(Assessment assessment) {
        return save(assessment, (e, u) -> {});
    }

    /**
     * Save the given assessment to the backing store per the {@link Assessment.Status} priorities and call
     * {@code reputationCallback} if the save would return {@code true}.
     * <p>
     * If no assessment matching {@code assessment}'s {@code peerId} and {@code messageId} is present, save
     * {@code assessment} regardless of status. If an assessment {p} matching {@code assessment}'s {@code peerId} and
     * {@code messageId} is present, only save {@code assessment} if {@code assessment.comparePriority(p) >= 0}.
     * <p>
     * The persisted value prior to updates is used as the {@code existing} and the passed {@code assessment} is used as
     * the {@code updated} value in the call to {@link ReputationCallback#update(Assessment, Assessment)}.
     * <p>
     * Implementations must ensure that invocations of {@code reputationCallback} are thread safe with regard to
     * {@link Assessment.Status} priorities. While implementations cannot ensure that the passed
     * {@code reputationCallback} itself is thread safe, the supporting code that calls it must be. Additionally, series
     * of updates must be idempotent and reflect sequential operations so that double counting does not occur. For
     * example, if two threads each attempt to save a new assessment, one with status {@link Assessment.Status#REWARD}
     * and one with {@link Assessment.Status#STRONG_PENALTY}, then two calls should be made to
     * {@code reputationCallback} reflecting either:
     * <ol>
     *     <li>NONE -> REWARD</li>
     *     <li>REWARD -> STRONG_PENALTY</li>
     * </ol>
     * or:
     * <ol>
     *     <li>NONE -> STRONG_PENALTY</li>
     *     <li>STRONG_PENALTY -> REWARD, i.e. no action taken as STRONG_PENALTY cannot be downgraded to REWARD.</li>
     * </ol>
     * For example, this ordering is not permitted:
     * <ol>
     *     <li>NONE -> REWARD</li>
     *     <li>NONE -> STRONG_PENALTY</li>
     * </ol>
     * <p>
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

    /**
     * Retrieve the assessments for {@code peerId}, if any.
     * @param peerId the unique identifier for the peer these assessments correspond to.
     * @return a {@code Stream} of the assessments or an empty stream if there are none.
     */
    Stream<Assessment> findAll(PeerId peerId);
}
