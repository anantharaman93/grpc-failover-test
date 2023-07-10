package com.ananth.grpc;

import io.grpc.*;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthCheckResponse;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.protobuf.services.HealthStatusManager;

import java.io.IOException;

public class Test
{
	public static void main(String[] args) throws IOException
	{
		int serverPort1 = 9101;
		int serverPort2 = 9102;
		
		Server server1 = Grpc.newServerBuilderForPort(serverPort1, InsecureServerCredentials.create())
				.addService(new HealthStatusManager().getHealthService())
				.build();
		server1.start();
		
		Server server2 = Grpc.newServerBuilderForPort(serverPort2, InsecureServerCredentials.create())
				.addService(new HealthStatusManager().getHealthService())
				.build();
		server2.start();
		
		AutoSwitchNameResolverProvider.HostSwitcher hostSwitcher = (current, others) -> current == null ? others.get(0) : current;
		
		NameResolverRegistry.getDefaultRegistry().register(new AutoSwitchNameResolverProvider(hostSwitcher));
		AutoSwitchLoadBalancerProvider loadBalancerProvider = new AutoSwitchLoadBalancerProvider();
		LoadBalancerRegistry.getDefaultRegistry().register(loadBalancerProvider);
		
		// ManagedChannel managedChannel = ManagedChannelBuilder.forAddress("localhost", serverPort1)
		ManagedChannel managedChannel = ManagedChannelBuilder.forTarget("multi://localhost:" + serverPort1 + ",localhost:" + serverPort2)
				.defaultLoadBalancingPolicy(loadBalancerProvider.getPolicyName())
				.usePlaintext()
				.build();
		
		HealthGrpc.HealthBlockingStub healthStub = HealthGrpc.newBlockingStub(managedChannel);
		
		HealthCheckResponse result = healthStub.check(HealthCheckRequest.getDefaultInstance());
		System.out.println(result.getStatus());
		
		server1.shutdownNow();
		
		result = healthStub.check(HealthCheckRequest.getDefaultInstance());
		System.out.println(result.getStatus());
	}
}
