package com.github.arobie1992.clarinet.impl.reputation;

import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static com.github.arobie1992.clarinet.reputation.Assessment.Status.*;
import static com.github.arobie1992.clarinet.reputation.Assessment.Status.STRONG_PENALTY;
import static org.junit.jupiter.api.Assertions.*;

class ProportionalReputationServiceTest {

    private final PeerId peerId = PeerUtils.senderId();
    private final ConnectionId connectionId = ConnectionId.random();
    private int seqNo;
    private ProportionalReputationService service;

    @BeforeEach
    void setup() {
        service = new ProportionalReputationService();
        seqNo = 0;
    }

    @Test
    void testUpdateStatusValues() {
        assertEquals(1.0, service.get(peerId));
        service.update(null, nextOf(Assessment.Status.WEAK_PENALTY));
        assertEquals(0.5, service.get(peerId));
        service.update(null, nextOf(Assessment.Status.STRONG_PENALTY));
        assertEquals(0.2, service.get(peerId));
        service.update(null, nextOf(Assessment.Status.REWARD));
        assertEquals(2.0/6, service.get(peerId));
        service.update(null, nextOf(Assessment.Status.NONE));
    }

    @Test
    void testUpdateMismatchPeerId() {
        var existing = nextOf(Assessment.Status.NONE);
        var peerId = PeerUtils.witnessId();
        assertNotEquals(existing.peerId(), peerId);
        var updated = new Assessment(peerId, existing.messageId(), Assessment.Status.REWARD);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.update(existing, updated));
        assertEquals("existing and updated refer to different peers", ex.getMessage());
    }

    @Test
    void testUpdateMismatchMessageId() {
        var existing = nextOf(Assessment.Status.NONE);
        var messageId = new MessageId(ConnectionId.random(), 0);
        assertNotEquals(existing.messageId(), messageId);
        var updated = new Assessment(existing.peerId(), messageId, Assessment.Status.REWARD);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.update(existing, updated));
        assertEquals("existing and updated refer to different messages", ex.getMessage());
    }

    @ParameterizedTest
    @MethodSource("statusCompareArgs")
    void testUpdateRespectsPriorities(Assessment.Status initial, double initialRep, Assessment.Status updated, double updatedRep) {
        var a = nextOf(initial);
        service.update(null, a);
        assertEquals(initialRep, service.get(a.peerId()));
        var b = new Assessment(a.peerId(), a.messageId(), updated);
        service.update(a, b);
        assertEquals(updatedRep, service.get(a.peerId()));
    }

    private static Stream<Arguments> statusCompareArgs() {
        return Stream.of(
                Arguments.of(NONE          , 1   , NONE          , 1),
                Arguments.of(NONE          , 1   , REWARD        , 1),
                Arguments.of(NONE          , 1   , WEAK_PENALTY  , 0.5),
                Arguments.of(NONE          , 1   , STRONG_PENALTY, 0.25),
                Arguments.of(REWARD        , 1   , NONE          , 1),
                Arguments.of(REWARD        , 1   , REWARD        , 1),
                Arguments.of(REWARD        , 1   , WEAK_PENALTY  , 0.5),
                Arguments.of(REWARD        , 1   , STRONG_PENALTY, 0.25),
                Arguments.of(WEAK_PENALTY  , 0.5 , NONE          , 0.5),
                Arguments.of(WEAK_PENALTY  , 0.5 , REWARD        , 0.5),
                Arguments.of(WEAK_PENALTY  , 0.5 , WEAK_PENALTY  , 0.5),
                Arguments.of(WEAK_PENALTY  , 0.5 , STRONG_PENALTY, 0.25),
                Arguments.of(STRONG_PENALTY, 0.25, NONE          , 0.25),
                Arguments.of(STRONG_PENALTY, 0.25, REWARD        , 0.25),
                Arguments.of(STRONG_PENALTY, 0.25, WEAK_PENALTY  , 0.25),
                Arguments.of(STRONG_PENALTY, 0.25, STRONG_PENALTY, 0.25)
        );
    }

    @Test
    void testGoodBelow1() {
        var a = nextOf(REWARD);
        var b = a.updateStatus(WEAK_PENALTY);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.update(a, b));
        assertEquals("Reputation cannot be updated with given existing and updated values", ex.getMessage());
    }

    @Test
    void testTotalBelow1() {
        var a = nextOf(WEAK_PENALTY);
        var b = a.updateStatus(STRONG_PENALTY);
        var ex = assertThrows(IllegalArgumentException.class, () -> service.update(a, b));
        assertEquals("Reputation cannot be updated with given existing and updated values", ex.getMessage());
    }

    @Test
    void testUpdateNullUpdated() {
        assertThrows(NullPointerException.class, () -> service.update(null, null));
    }

    private Assessment nextOf(Assessment.Status status) {
        return new Assessment(peerId, new MessageId(connectionId, seqNo++), status);
    }

}