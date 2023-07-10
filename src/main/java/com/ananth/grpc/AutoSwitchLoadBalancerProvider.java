package com.ananth.grpc;

import io.grpc.LoadBalancer;
import io.grpc.LoadBalancerProvider;
import io.grpc.Status;
import io.grpc.internal.PickFirstLoadBalancerProvider;
import io.grpc.util.ForwardingLoadBalancer;

public class AutoSwitchLoadBalancerProvider extends LoadBalancerProvider
{
	@Override
	public boolean isAvailable()
	{
		return true;
	}
	
	@Override
	public int getPriority()
	{
		return 5;
	}
	
	@Override
	public String getPolicyName()
	{
		return AutoSwitchLoadBalancerProvider.class.getName();
	}
	
	@Override
	public LoadBalancer newLoadBalancer(LoadBalancer.Helper helper)
	{
		LoadBalancer pickFirst = new PickFirstLoadBalancerProvider().newLoadBalancer(helper);
		
		return new ForwardingLoadBalancer()
		{
			@Override
			public void handleResolvedAddresses(ResolvedAddresses resolvedAddresses)
			{
				delegate().handleResolvedAddresses(resolvedAddresses);
			}
			
			@Override
			public boolean acceptResolvedAddresses(ResolvedAddresses resolvedAddresses)
			{
				boolean accepted = delegate().acceptResolvedAddresses(resolvedAddresses);
				return accepted;
			}
			
			@Override
			public void handleNameResolutionError(Status error)
			{
				delegate().handleNameResolutionError(error);
			}
			
			@Override
			protected LoadBalancer delegate()
			{
				return pickFirst;
			}
		};
	}
}
