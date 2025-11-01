package br.edu.ufersa.cc.seg.servicediscovery.load_balancer;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer implements LoadBalancer<String> {

    private final Random random = new Random();

    @Override
    public String choose(final List<String> options) {
        if (options.isEmpty()) {
            return null;
        }

        return options.get(random.nextInt(options.size()));
    }

}
