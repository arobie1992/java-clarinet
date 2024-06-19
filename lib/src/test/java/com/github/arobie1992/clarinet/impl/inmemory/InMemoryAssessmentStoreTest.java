package com.github.arobie1992.clarinet.impl.inmemory;

import com.github.arobie1992.clarinet.core.ConnectionId;
import com.github.arobie1992.clarinet.message.MessageId;
import com.github.arobie1992.clarinet.peer.PeerId;
import com.github.arobie1992.clarinet.reputation.Assessment;
import com.github.arobie1992.clarinet.reputation.AssessmentStore;
import com.github.arobie1992.clarinet.testutils.AsyncAssert;
import com.github.arobie1992.clarinet.testutils.PeerUtils;
import com.github.arobie1992.clarinet.testutils.ThreadUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InMemoryAssessmentStoreTest {

    private final PeerId peerId = PeerUtils.senderId();
    private final MessageId messageId = new MessageId(ConnectionId.random(), 0);
    private InMemoryAssessmentStore store;
    private AssessmentStore.ReputationCallback callback;

    @BeforeEach
    void setup() {
        store = new InMemoryAssessmentStore();
        callback = mock(AssessmentStore.ReputationCallback.class);
    }

    @Test
    void testFindAndSave() {
        var assessment = store.find(peerId, messageId);
        assertEquals(new Assessment(peerId, messageId, Assessment.Status.NONE), assessment);
        var updated = assessment.updateStatus(Assessment.Status.REWARD);
        assertTrue(store.save(updated, callback));
        verify(callback).update(assessment, updated);
        assertEquals(updated, store.find(peerId, messageId));
    }

    @Test
    void testSaveNoPreexisting() {
        var assessment = new Assessment(peerId, messageId, Assessment.Status.REWARD);
        assertTrue(store.save(assessment, callback));
        verify(callback).update(null, assessment);
        assertEquals(assessment, store.find(peerId, messageId));
    }

    @Test
    void testSaveNotPersist() {
        var assessment = new Assessment(peerId, messageId, Assessment.Status.REWARD);
        assertTrue(store.save(assessment));
        assertFalse(store.save(new Assessment(peerId, messageId, Assessment.Status.NONE), callback));
        verify(callback, never()).update(any(), any());
        assertEquals(assessment, store.find(peerId, messageId));
    }

    @Test
    void testAtomicityOfCallbackInvocations() throws Throwable {
        var values = new ArrayList<Pair>();
        var latch = new CountDownLatch(1);
        callback = (e, u) -> {
            latch.countDown();
            values.add(new Pair(e == null ? null : e.status(), u.status()));
            ThreadUtils.sleepUnchecked(1000);
        };
        var t1 = AsyncAssert.started(() -> store.save(new Assessment(peerId, messageId, Assessment.Status.REWARD), callback));
        latch.await();
        store.save(new Assessment(peerId, messageId, Assessment.Status.WEAK_PENALTY), callback);
        t1.join();
        assertEquals(Arrays.asList(
                new Pair(null, Assessment.Status.REWARD),
                new Pair(Assessment.Status.REWARD, Assessment.Status.WEAK_PENALTY)
        ), values);
    }

    private record Pair(Assessment.Status existing, Assessment.Status updated) {}

    @Test
    void testFindAll() {
        var peerId2 = PeerUtils.receiverId();
        assertNotEquals(peerId, peerId2);
        var messageId2 = new MessageId(ConnectionId.random(), 1);
        var assessment1 = new Assessment(peerId, messageId, Assessment.Status.NONE);
        var assessment2 = new Assessment(peerId, messageId2, Assessment.Status.STRONG_PENALTY);
        var assessment3 = new Assessment(peerId2, messageId2, Assessment.Status.WEAK_PENALTY);
        assertTrue(store.save(assessment1));
        assertTrue(store.save(assessment2));
        assertTrue(store.save(assessment3));
        var found = store.findAll(peerId).toList();
        assertTrue(found.containsAll(List.of(assessment1, assessment2)));
        assertFalse(found.contains(assessment3));
    }

}