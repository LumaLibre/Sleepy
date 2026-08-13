package dev.lumas.sleepy.placeholder

import dev.lumas.core.annotation.Autowire
import dev.lumas.core.annotation.PlaceholderMeta
import dev.lumas.core.annotation.Register
import dev.lumas.core.model.placeholder.AbstractPlaceholderManager
import dev.lumas.sleepy.Sleepy

@Register(Autowire.PLACEHOLDER)
@PlaceholderMeta(
    identifier = "sleepy",
    author = "Jsinco",
    version = "1.0.0",
    persist = true,
)
class PlaceholderManager : AbstractPlaceholderManager<Sleepy, PlaceholderModule>(Sleepy.instance)
