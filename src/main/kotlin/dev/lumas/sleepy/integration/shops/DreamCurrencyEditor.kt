package dev.lumas.sleepy.integration.shops

import dev.lumas.shops.api.currency.CurrencyEditor
import dev.lumas.shops.api.currency.CurrencySelection
import dev.lumas.shops.components.data.KeyConsumer
import dev.lumas.shops.components.data.KeyConsumerRegistry
import dev.lumas.sleepy.util.Messages
import io.papermc.paper.dialog.Dialog
import io.papermc.paper.registry.data.dialog.ActionButton
import io.papermc.paper.registry.data.dialog.DialogBase
import io.papermc.paper.registry.data.dialog.action.DialogAction
import io.papermc.paper.registry.data.dialog.input.DialogInput
import io.papermc.paper.registry.data.dialog.type.DialogType
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import java.util.Locale

@Suppress("UnstableApiUsage")
class DreamCurrencyEditor : CurrencyEditor {
    override fun open(player: Player, selection: CurrencySelection, onComplete: Runnable, onCancel: Runnable) {
        val current = selection.editingCurrency(DreamCurrency::class.java)
        val submit = KeyConsumer.of(this, SUBMIT) { _, viewer, view ->
            val amount = view.getText(INPUT_AMOUNT)?.toLongOrNull()?.takeIf { it > 0 }
            if (amount == null) {
                Messages.send(viewer, "sleepy.currency.editor.invalid")
                onCancel.run()
                return@of
            }
            selection.currency(DreamCurrency(amount))
            onComplete.run()
        }
        val cancel = KeyConsumer.of(this, CANCEL) { _, _, _ -> onCancel.run() }

        KeyConsumerRegistry.INSTANCE.register(player, submit, cancel)
        player.showDialog(build(player.locale(), current))
    }

    private fun build(locale: Locale, current: DreamCurrency?): Dialog {
        val amount = DialogInput.text(INPUT_AMOUNT, translate("sleepy.currency.editor.amount", locale))
            .maxLength(18)
            .initial(current?.amount?.toString() ?: "1")
            .width(200)
            .build()
        val submit = ActionButton.builder(translate("sleepy.currency.editor.submit", locale))
            .action(DialogAction.customClick(SUBMIT, null))
            .build()
        val cancel = ActionButton.builder(translate("sleepy.currency.editor.cancel", locale))
            .action(DialogAction.customClick(CANCEL, null))
            .build()
        val base = DialogBase.builder(translate("sleepy.currency.editor.title", locale))
            .canCloseWithEscape(false)
            .inputs(listOf(amount))
            .build()

        return Dialog.create { builder ->
            builder.empty().base(base).type(DialogType.confirmation(submit, cancel))
        }
    }

    private fun translate(key: String, locale: Locale): Component =
        GlobalTranslator.render(Component.translatable(key), locale)

    companion object {
        private val SUBMIT = Key.key("sleepy", "shops/price/submit")
        private val CANCEL = Key.key("sleepy", "shops/price/cancel")
        private const val INPUT_AMOUNT = "amount"
    }
}
