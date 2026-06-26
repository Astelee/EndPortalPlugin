package com.newscraft.endportal;

import org.bukkit.plugin.java.JavaPlugin;

/**
 * Classe principal do plugin EndPortalPlugin.
 * É carregada automaticamente pelo servidor Paper/Spigot.
 */
public class EndPortalPlugin extends JavaPlugin {

    // Instância estática para acesso fácil em outras classes
    private static EndPortalPlugin instance;

    @Override
    public void onEnable() {
        // Salva a instância do plugin
        instance = this;

        // Registra o Listener que vai monitorar os eventos do jogo
        getServer().getPluginManager().registerEvents(
            new EndPortalListener(this), this
        );

        // Mensagem no console quando o plugin é ativado
        getLogger().info("=================================");
        getLogger().info(" EndPortalPlugin ativado!");
        getLogger().info(" Servidor: NewsCraft");
        getLogger().info(" Versão: " + getDescription().getVersion());
        getLogger().info("=================================");
    }

    @Override
    public void onDisable() {
        // Mensagem no console quando o plugin é desativado
        getLogger().info("EndPortalPlugin desativado. Até logo!");
    }

    /**
     * Retorna a instância do plugin.
     * Útil para acessar o plugin de outras classes.
     */
    public static EndPortalPlugin getInstance() {
        return instance;
    }
}
