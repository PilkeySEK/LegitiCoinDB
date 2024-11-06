package me.pilkeysek.lcoindb.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.Modmenu;

import java.util.List;

@Modmenu(modId = "lcoindb")
@Config(name = "lcoindb-config", wrapperName = "MainConfig")
public class MainConfigModel {
    public boolean authenticationEnabled = true;
    public List<String> trustedAuthServers = List.of("localhost","localhost:25565","lcauth.skye.host","lcauth.minceraft.host");
    public String apiUrl = "https://lcapi.minceraft.host";
    @ExcludeFromScreen
    public String secret = "";
}
