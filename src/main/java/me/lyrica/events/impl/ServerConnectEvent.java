package me.lyrica.events.impl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.lyrica.events.Event;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

@AllArgsConstructor @Getter
public class ServerConnectEvent extends Event {
    private final ServerAddress address;
    private final ServerInfo info;
}
