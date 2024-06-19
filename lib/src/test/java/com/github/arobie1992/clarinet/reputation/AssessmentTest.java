package com.github.arobie1992.clarinet.reputation;

import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import static com.github.arobie1992.clarinet.reputation.Assessment.Status.*;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AssessmentTest {

    private final PeerId peerId = PeerUtils.senderId();
    private final MessageId messageId = new MessageId(ConnectionId.random(), 0);

    @Test
    void testNullPeerId() {
        assertThrows(NullPointerException.class, () -> new Assessment(null, messageId, Assessment.Status.NONE));
    }

    @Test
    void testNullMessageId() {
        assertThrows(NullPointerException.class, () -> new Assessment(peerId, null, Assessment.Status.NONE));
    }

    @Test
    void testNullStatus() {
        assertThrows(NullPointerException.class, () -> new Assessment(peerId, messageId, null));
    }

    @ParameterizedTest
    @MethodSource("updateStatusArgs")
    void testUpdateStatus(Assessment.Status initial, Assessment.Status update, boolean shouldUpdate) {
        var a = new Assessment(peerId, messageId, initial);
        var b = a.updateStatus(update);
        if(shouldUpdate) {
            assertEquals(initial, a.status());
            assertEquals(update, b.status());
        } else {
            assertSame(a, b);
            assertEquals(initial, b.status());
        }
    }

    static Stream<Arguments> updateStatusArgs() {
        return Stream.of(
                Arguments.of(NONE          , NONE          , false),
                Arguments.of(NONE          , REWARD        , true),
                Arguments.of(NONE          , WEAK_PENALTY  , true),
                Arguments.of(NONE          , STRONG_PENALTY, true),
                Arguments.of(REWARD        , NONE          , false),
                Arguments.of(REWARD        , REWARD        , false),
                Arguments.of(REWARD        , WEAK_PENALTY  , true),
                Arguments.of(REWARD        , STRONG_PENALTY, true),
                Arguments.of(WEAK_PENALTY  , NONE          , false),
                Arguments.of(WEAK_PENALTY  , REWARD        , false),
                Arguments.of(WEAK_PENALTY  , WEAK_PENALTY  , false),
                Arguments.of(WEAK_PENALTY  , STRONG_PENALTY, true),
                Arguments.of(STRONG_PENALTY, NONE          , false),
                Arguments.of(STRONG_PENALTY, REWARD        , false),
                Arguments.of(STRONG_PENALTY, WEAK_PENALTY  , false),
                Arguments.of(STRONG_PENALTY, STRONG_PENALTY, false)
        );
    }

    @ParameterizedTest
    @MethodSource("statusCompareArgs")
    void testStatusComparePriority(Assessment.Status s1, Assessment.Status s2, int expected) {
        assertEquals(expected, s1.comparePriority(s2));
    }

    private static Stream<Arguments> statusCompareArgs() {
        return Stream.of(
                Arguments.of(NONE          , NONE          , 0),
                Arguments.of(NONE          , REWARD        , -1),
                Arguments.of(NONE          , WEAK_PENALTY  , -1),
                Arguments.of(NONE          , STRONG_PENALTY, -1),
                Arguments.of(REWARD        , NONE          , 1),
                Arguments.of(REWARD        , REWARD        , 0),
                Arguments.of(REWARD        , WEAK_PENALTY  , -1),
                Arguments.of(REWARD        , STRONG_PENALTY, -1),
                Arguments.of(WEAK_PENALTY  , NONE          , 1),
                Arguments.of(WEAK_PENALTY  , REWARD        , 1),
                Arguments.of(WEAK_PENALTY  , WEAK_PENALTY  , 0),
                Arguments.of(WEAK_PENALTY  , STRONG_PENALTY, -1),
                Arguments.of(STRONG_PENALTY, NONE          , 1),
                Arguments.of(STRONG_PENALTY, REWARD        , 1),
                Arguments.of(STRONG_PENALTY, WEAK_PENALTY  , 1),
                Arguments.of(STRONG_PENALTY, STRONG_PENALTY, 0)
        );
    }
}