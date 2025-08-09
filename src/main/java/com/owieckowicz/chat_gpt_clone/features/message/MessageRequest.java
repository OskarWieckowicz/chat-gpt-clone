package com.owieckowicz.chat_gpt_clone.features.message;

import jakarta.validation.constraints.NotBlank;

public record MessageRequest(@NotBlank String message) {
}
