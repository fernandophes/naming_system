package br.edu.ufersa.cc.seg.servicediscovery.load_balancer;

import java.util.List;

public interface LoadBalancer<T> {

    T choose(final List<String> options);

}
