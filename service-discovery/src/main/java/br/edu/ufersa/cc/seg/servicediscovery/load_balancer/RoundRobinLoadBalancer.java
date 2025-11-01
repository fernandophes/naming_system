package br.edu.ufersa.cc.seg.servicediscovery.load_balancer;

import java.util.List;

public class RoundRobinLoadBalancer implements LoadBalancer<String> {

    private int index = 0;

    @Override
    public String choose(final List<String> options) {
        if (options.isEmpty()) {
            return null;
        }

        return options.get(getNextIndex(options.size()));
    }

    private int getNextIndex(final int size) {
        final var current = index;
        index = (index + 1) % size;
        return current;
    }

}
