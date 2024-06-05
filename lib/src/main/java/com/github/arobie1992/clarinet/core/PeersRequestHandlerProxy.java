package com.github.arobie1992.clarinet.core;

import com.github.arobie1992.clarinet.adt.Some;
import com.github.arobie1992.clarinet.peer.Peer;
import com.github.arobie1992.clarinet.transport.ExchangeHandler;
import com.github.arobie1992.clarinet.transport.RemoteInformation;

import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

class PeersRequestHandlerProxy implements ExchangeHandler<PeersRequest, PeersResponse> {

    private final SecureRandom secureRandom = new SecureRandom();
    private final ExchangeHandler<PeersRequest, PeersResponse> userHandler;
    private final Node node;

    PeersRequestHandlerProxy(ExchangeHandler<PeersRequest, PeersResponse> userHandler, Node node) {
        this.userHandler = userHandler;
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public Some<PeersResponse> handle(RemoteInformation remoteInformation, PeersRequest message) {
        if(userHandler != null) {
            return Objects.requireNonNull(userHandler.handle(remoteInformation, message), "userHandler returned null");
        }

        var requested = message.requested().stream().map(node.peerStore()::find).filter(Optional::isPresent).map(Optional::get).toList();
        var chosen = new ArrayList<>(requested);
        if(message.requested().contains(node.id())) {
            chosen.add(new Peer(node.id(), new HashSet<>(node.transport().addresses())));
        }

        if(message.additionalRequested() > 0) {
            var remaining = message.additionalRequested();
            var candidates = node.peerStore().all()
                    .filter(id -> !message.requested().contains(id))
                    .collect(Collectors.toCollection(ArrayList::new));

            while(!candidates.isEmpty() && remaining > 0) {
                var index = secureRandom.nextInt(candidates.size());
                var peerId = candidates.remove(index);
                var opt = node.peerStore().find(peerId);
                if(opt.isPresent()) {
                    chosen.add(opt.get());
                    remaining--;
                }
            }
        }

        return new Some<>(new PeersResponse(chosen));
    }

    @Override
    public Class<PeersRequest> inputType() {
        return PeersRequest.class;
    }
}
