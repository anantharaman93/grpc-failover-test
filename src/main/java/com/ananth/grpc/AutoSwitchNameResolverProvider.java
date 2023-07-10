package com.ananth.grpc;

import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.Status;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AutoSwitchNameResolverProvider extends NameResolverProvider
{
	@FunctionalInterface
	public interface HostSwitcher
	{
		SocketAddress switchHost(SocketAddress current, List<InetSocketAddress> configured);
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
		String authority = targetUri.getAuthority();
		String[] hostsAndPorts = StringUtils.split(authority, ',');
		
		List<InetSocketAddress> addresses = new ArrayList<>(hostsAndPorts.length);
		for (String hostAndPort : hostsAndPorts)
		{
			String host = StringUtils.substringBefore(hostAndPort, ':');
			int port = Integer.parseInt(StringUtils.substringAfter(hostAndPort, ':'));
			
			InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
			addresses.add(inetSocketAddress);
		}
		
		return new AutoSwitchNameResolver(authority, addresses);
	}
	
	@Override
	public String getDefaultScheme()
	{
		return "multi";
	}
	
	private class AutoSwitchNameResolver extends NameResolver
	{
		private final String authority;
		private final List<InetSocketAddress> addresses;
		
		public AutoSwitchNameResolver(String authority, List<InetSocketAddress> addresses)
		{
			this.authority = authority;
			this.addresses = addresses;
		}
		
		private SocketAddress current;
		private Listener2 listener;
		
		@Override
		public String getServiceAuthority()
		{
			return authority;
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
				
				current = currentSocketAddress.switchHost(current, addresses);
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
