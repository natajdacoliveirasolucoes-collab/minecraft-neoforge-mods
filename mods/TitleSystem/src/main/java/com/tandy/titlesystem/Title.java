package com.tandy.titlesystem;

import net.minecraft.network.chat.Component;

public record Title(String id, Component display, int priority, int killsRequired, boolean collector) {
}
