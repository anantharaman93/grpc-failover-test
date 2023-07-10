package com.ananth.grpc;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.Status;

import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;

public class AutoSwitchNameResolverProvider extends NameResolverProvider
{
	@FunctionalInterface
	public interface HostSwitcher
	{
		SocketAddress switchHost(SocketAddress current);
	}
	
	private final HostSwitcher currentSocketAddress;
	
	public AutoSwitchNameResolverProvider(HostSwitcher currentSocketAddress)
	{
		this.currentSocketAddress = currentSocketAddress;
	}
	
	@Override
	protected boolean isAvailable()
	{
		return true;
	}
	
	@Override
	protected int priority()
	{
		return 5;
	}
	
	@Override
	public NameResolver newNameResolver(URI targetUri, NameResolver.Args args)
	{
		return new AutoSwitchNameResolver();
	}
	
	@Override
	public String getDefaultScheme()
	{
		return "multi";
	}
	
	private class AutoSwitchNameResolver extends NameResolver
	{
		private SocketAddress current;
		private Listener2 listener;
		
		@Override
		public String getServiceAuthority()
		{
			return "dummy";
		}
		
		@Override
		public void start(Listener2 listener)
		{
			this.listener = listener;
			reload();
		}
		
		@Override
		public void refresh()
		{
			reload();
		}
		
		private void reload()
		{
			try
			{
//				List<EquivalentAddressGroup> equivalentAddressGroups = addresses.stream().map(EquivalentAddressGroup::new).collect(Collectors.toList());
//				listener.onResult(ResolutionResult.newBuilder().setAddresses(equivalentAddressGroups).build());
				
				current = currentSocketAddress.switchHost(current);
				EquivalentAddressGroup addressGroup = new EquivalentAddressGroup(current);
				listener.onResult(ResolutionResult.newBuilder().setAddresses(Collections.singletonList(addressGroup)).build());
			}
			catch (Exception e)
			{
				listener.onError(Status.UNAVAILABLE.withCause(e));
			}
		}
		
		@Override
		public void shutdown()
		{
		
		}
	}
}
