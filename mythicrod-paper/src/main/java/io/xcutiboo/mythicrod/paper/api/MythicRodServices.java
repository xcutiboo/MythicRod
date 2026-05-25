package io.xcutiboo.mythicrod.paper.api;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.jetbrains.annotations.NotNull;

import io.xcutiboo.mythicrod.api.MythicRodAPI;

/// Paper-bound convenience entry points for resolving MythicRod's public API.
///
/// External plugins compile against `MythicRodAPI`. This helper only resolves
/// the registered service at runtime.
///
/// Lookups are valid after MythicRod has enabled and before it disables. A
/// missing service means MythicRod is not installed, not enabled yet, or is
/// shutting down.
public final class MythicRodServices {

    private MythicRodServices() {
    }

    /// Looks up MythicRod's API using the global Bukkit server.
    ///
    /// @return optional service registration result
    @NotNull
    public static Optional<MythicRodAPI> find() {
        return find(Bukkit.getServer());
    }

    /// Looks up MythicRod's API from the supplied server.
    ///
    /// @param server Paper/Bukkit server instance
    /// @return optional service registration result
    @NotNull
    public static Optional<MythicRodAPI> find(@NotNull Server server) {
        return find(server.getServicesManager());
    }

    /// Looks up MythicRod's API from the supplied services manager.
    ///
    /// @param servicesManager services manager to query
    /// @return optional service registration result
    @NotNull
    public static Optional<MythicRodAPI> find(@NotNull ServicesManager servicesManager) {
        RegisteredServiceProvider<MythicRodAPI> registration = servicesManager.getRegistration(MythicRodAPI.class);
        if (registration == null) {
            return Optional.empty();
        }
        return Optional.of(registration.getProvider());
    }

    /// Resolves MythicRod's API using the global Bukkit server or throws.
    ///
    /// @return registered MythicRod API service
    /// @throws IllegalStateException when MythicRod has not registered its API
    @NotNull
    public static MythicRodAPI require() {
        return require(Bukkit.getServer());
    }

    /// Resolves MythicRod's API from the supplied server or throws.
    ///
    /// @param server Paper/Bukkit server instance
    /// @return registered MythicRod API service
    /// @throws IllegalStateException when MythicRod has not registered its API
    @NotNull
    public static MythicRodAPI require(@NotNull Server server) {
        return require(server.getServicesManager());
    }

    /// Resolves MythicRod's API from the supplied services manager or throws.
    ///
    /// @param servicesManager services manager to query
    /// @return registered MythicRod API service
    /// @throws IllegalStateException when MythicRod has not registered its API
    @NotNull
    public static MythicRodAPI require(@NotNull ServicesManager servicesManager) {
        return find(servicesManager).orElseThrow(
            () -> new IllegalStateException("MythicRodAPI is not currently registered")
        );
    }
}
