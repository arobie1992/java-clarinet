package com.github.arobie1992.clarinet.impl.reputation;

import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.reputation.ReputationService;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static com.github.arobie1992.clarinet.reputation.Assessment.Status.*;

public class ProportionalReputationService implements ReputationService {
    private final Map<PeerId, Rep> reps = new ConcurrentHashMap<>();

    private static final Map<Assessment.Status, Delta> deltas = Collections.unmodifiableMap(new EnumMap<>(Map.of(
            NONE          , new Delta(0, 0),
            REWARD        , new Delta(1, 1),
            WEAK_PENALTY  , new Delta(0, 1),
            STRONG_PENALTY, new Delta(0, 3)
    )));

    @Override
    public void update(Assessment existing, Assessment updated) {
        Objects.requireNonNull(updated);
        if(existing != null) {
            if(!updated.peerId().equals(existing.peerId())) {
                throw new IllegalArgumentException("existing and updated refer to different peers");
            }
            if(!updated.messageId().equals(existing.messageId())) {
                throw new IllegalArgumentException("existing and updated refer to different messages");
            }
        }
        reps.compute(updated.peerId(), (k, e) -> {
            if(e == null) {
                e = new Rep();
            }
            if(existing == null) {
                return e.add(deltas.get(updated.status()));
            }
            return updated.status().comparePriority(existing.status()) > 0
                ? e.sub(deltas.get(existing.status())).add(deltas.get(updated.status()))
                : e;
        });
    }

    @Override
    public double get(PeerId peerId) {
        return reps.computeIfAbsent(peerId, k -> new Rep()).value();
    }

    private record Delta(double good, double total) {}

    private record Rep(double good, double total) {
        private Rep {
            if(good < 1 || total < 1) {
                throw new IllegalArgumentException("Reputation cannot be updated with given existing and updated values");
            }
        }
        private Rep() {
            this(1,1);
        }
        private Rep add(Delta delta) {
            return new Rep(good + delta.good, total + delta.total);
        }
        private Rep sub(Delta delta) {
            return new Rep(good - delta.good, total - delta.total);
        }
        private double value() {
            return good/total;
        }
    }
}
