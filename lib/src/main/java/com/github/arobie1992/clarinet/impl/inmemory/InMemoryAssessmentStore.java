package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.reputation.AssessmentStore;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class InMemoryAssessmentStore implements AssessmentStore {

    private record ID(PeerId peerId, MessageId messageId) {
        private ID {
            Objects.requireNonNull(peerId);
            Objects.requireNonNull(messageId);
        }
    }

    private final Map<ID, Assessment> assessments = new ConcurrentHashMap<>();

    @Override
    public boolean save(Assessment assessment, ReputationCallback reputationCallback) {
        final AtomicBoolean persisted = new AtomicBoolean(false);
        assessments.compute(new ID(assessment.peerId(), assessment.messageId()), (id, existing) -> {
            if(existing == null) {
                reputationCallback.update(null, assessment);
                persisted.set(true);
                return assessment;
            }
            boolean persist = assessment.status().comparePriority(existing.status()) >= 0;
            if(persist) {
                reputationCallback.update(existing, assessment);
                persisted.set(true);
                return assessment;
            }
            return existing;
        });
        return persisted.get();
    }

    @Override
    public Assessment find(PeerId peerId, MessageId messageId) {
        return assessments.computeIfAbsent(new ID(peerId, messageId), k -> new Assessment(peerId, messageId, Assessment.Status.NONE));
    }

}
