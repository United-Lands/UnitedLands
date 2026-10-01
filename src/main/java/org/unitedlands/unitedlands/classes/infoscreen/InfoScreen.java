package org.unitedlands.unitedlands.classes.infoscreen;

import java.util.LinkedList;

import org.bukkit.plugin.java.JavaPlugin;
import org.unitedlands.utils.United;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public abstract class InfoScreen {

    protected LinkedList<InfoScreenComponent> components = new LinkedList<>();

    public LinkedList<InfoScreenComponent> getComponents() {
        return components;
    }

    public void addComponent(String id, Component content) {
        components.add(new InfoScreenComponent(id, content));
    }

    public void addComponent(int index, String id, Component content) {
        components.add(index, new InfoScreenComponent(id, content));
    }

    public void addComponent(String id, String path, Object... values) {
        components.add(new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(path, values))));
    }

    public void addComponent(String id, String path, JavaPlugin plugin, Object... values) {
        components.add(new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(path, plugin, values))));
    }

    public void addComponent(int index, String id, String path, Object... values) {
        components.add(index, new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(path, values))));
    }

    public void addComponent(int index, String id, String path, JavaPlugin plugin, Object... values) {
        components.add(index, new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(path, plugin, values))));
    }

    public void addComponent(Audience sender, String id, String path, Object... values) {
        components.add(new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(sender, path, values))));
    }

    public void addComponent(Audience sender, String id, String path, JavaPlugin plugin, Object... values) {
        components.add(new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(sender, path, plugin, values))));
    }

    public void addComponent(Audience sender, int index, String id, String path, Object... values) {
        components.add(index, new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(sender, path, values))));
    }

    public void addComponent(Audience sender, int index, String id, String path, JavaPlugin plugin, Object... values) {
        components.add(index, new InfoScreenComponent(id, MiniMessage.miniMessage().deserialize(United.messenger().get(sender, path, plugin, values))));
    }

    public void addComponent(String afterKey, String id, Component content) {
        int index = 0;
        for (var c : components) {
            index++;
            if (c.getId().equals(afterKey))
                break;
        }
        components.add(index, new InfoScreenComponent(id, content));
    }

    public void removeComponent(String id) {
        InfoScreenComponent componentToRemove = null;
        for (var c : components) {
            if (c.getId().equals(id))
                componentToRemove = c;
        }
        if (componentToRemove != null)
            components.remove(componentToRemove);
    }

    public void removeComponent(int index) {
        components.remove(index);
    }

    public void send(Audience receiver) {
        var components = getComponents();
        if (components != null && !components.isEmpty()) {
            for (var component : components) {
                United.messenger().send(receiver, component.getContent());
            }
        }
    }

    public Component buildHeader(String name) {

        var header = "";

        var maxWidth = 55;

        var fillerStart = United.messenger().get("info-screens.header.filler-start");
        var filler = United.messenger().get("info-screens.header.filler");
        var fillerEnd = United.messenger().get("info-screens.header.filler-end");
        var fillerColor = United.messenger().get("info-screens.header.filler-color");
        var titleColor = United.messenger().get("info-screens.header.title-color");
        var titleStart = United.messenger().get("info-screens.header.title-start");
        var titleEnd = United.messenger().get("info-screens.header.title-end");

        var nameLength = name.length();
        var fillerStartLength = fillerStart.length();
        var fillerEndLength = fillerEnd.length();
        var titleStartLength = titleStart.length();
        var titleEndLength = titleEnd.length();

        var spaceToFill = Math.max(0,
                (maxWidth - nameLength - fillerStartLength - fillerEndLength - titleStartLength - titleEndLength));
        var halfSpace = spaceToFill / 2;
        var fillspaceFront = halfSpace;
        var fillspaceBack = halfSpace;
        if (spaceToFill % 2 == 0)
            fillspaceFront += 1;

        header += "<" + fillerColor + ">" + fillerStart;
        for (int i = 0; i < fillspaceFront; i++)
            header += filler;
        header += titleStart + "<" + titleColor + ">" + name + "</" + titleColor + ">" + titleEnd;
        for (int i = 0; i < fillspaceBack; i++)
            header += filler;
        header += fillerEnd + "</" + fillerColor + ">";

        return MiniMessage.miniMessage().deserialize(header);
    }

}
