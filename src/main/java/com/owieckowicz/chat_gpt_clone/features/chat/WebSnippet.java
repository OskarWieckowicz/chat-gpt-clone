package com.owieckowicz.chat_gpt_clone.features.chat;

/**
 * Minimal search result used as context for the assistant.
 */
public record WebSnippet(String title, String url, String snippet) {}


