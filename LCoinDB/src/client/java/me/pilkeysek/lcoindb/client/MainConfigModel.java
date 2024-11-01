package me.pilkeysek.lcoindb.client;

import io.wispforest.owo.config.annotation.Config;
import io.wispforest.owo.config.annotation.ExcludeFromScreen;
import io.wispforest.owo.config.annotation.Modmenu;

import java.util.List;

@Modmenu(modId = "lcoindb")
@Config(name = "lcoindb-config", wrapperName = "MainConfig")
public class MainConfigModel {
    public boolean authenticationEnabled = true;
    public List<String> trustedAuthServers = List.of("localhost","localhost:25565","lcauth.skye.host");
    public String apiUrl = "http://lcapi.skye.host";
    @ExcludeFromScreen
    public String secret = "";
}
