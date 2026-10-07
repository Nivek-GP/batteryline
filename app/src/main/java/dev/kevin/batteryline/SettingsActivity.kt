package dev.kevin.batteryline

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.graphics.drawable.Icon
import android.os.BatteryManager
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import dev.kevin.batteryline.databinding.ActivitySettingsBinding
import dev.kevin.batteryline.databinding.ViewSliderBinding

/** Pantalla única de ajustes. Cada cambio se guarda al momento y el servicio lo aplica en vivo. */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var prefs: LinePrefs
    private lateinit var screen: Screen

    /** El usuario quiso encender la línea sin permiso: se enciende al volver de concederlo. */
    private var enableAfterPermission = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableDapEdgeToEdge()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.content.padForSystemBars()
        prefs = LinePrefs(this)

        binding.grantButton.setOnClickListener { requestOverlayPermission() }
        binding.enabledSwitch.setOnCheckedChangeListener { button, checked ->
            if (!button.isPressed) return@setOnCheckedChangeListener
            if (checked && !Settings.canDrawOverlays(this)) {
                button.isChecked = false
                enableAfterPermission = true
                requestOverlayPermission()
                return@setOnCheckedChangeListener
            }
            prefs.enabled = checked
            LineService.sync(this)
        }

        binding.thickness.bind(R.string.thickness_label, ::px) { value ->
            prefs.thickness = value
            // El grosor reduce el recorrido disponible para la posición.
            renderOffset()
        }
        binding.offset.bind(R.string.offset_label, ::px) { prefs.offset = it }
        binding.opacity.bind(R.string.opacity_label, ::percent) { prefs.opacity = it }
        binding.anchorGroup.addOnButtonCheckedListener { _, id, checked ->
            if (!checked) return@addOnButtonCheckedListener
            prefs.anchor = when (id) {
                R.id.anchorLeft -> Anchor.LEFT
                R.id.anchorCenter -> Anchor.CENTER
                else -> Anchor.RIGHT
            }
        }

        binding.fullAt.bind(R.string.full_at_label, ::percent) { value ->
            prefs.fullAt = value
            renderBatteryStatus()
        }
        binding.lowSwitch.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) prefs.lowEnabled = checked
            binding.lowThreshold.setEnabled(checked)
        }
        binding.lowThreshold.bind(R.string.low_threshold_label, ::percent) { prefs.lowThreshold = it }

        binding.marginLeft.bind(R.string.margin_left_label, ::px, steps = true) { prefs.setMarginLeft(screen.landscape, it) }
        binding.marginRight.bind(R.string.margin_right_label, ::px, steps = true) { prefs.setMarginRight(screen.landscape, it) }
        binding.autoButton.setOnClickListener { autoAdjustEdges() }

        binding.previewSwitch.setOnCheckedChangeListener { _, checked ->
            binding.previewLevel.setEnabled(checked)
            Preview.level = if (checked) binding.previewLevel.slider.value.toInt() else null
        }
        binding.previewLevel.bind(R.string.preview_label, ::percent) { Preview.level = it }

        binding.bootSwitch.setOnCheckedChangeListener { button, checked ->
            if (button.isPressed) prefs.startOnBoot = checked
        }
        binding.batteryOptRow.setOnClickListener { openBatteryOptimization() }
        binding.addTileButton.setOnClickListener { requestAddTile() }
    }

    override fun onResume() {
        super.onResume()
        if (enableAfterPermission && Settings.canDrawOverlays(this)) {
            prefs.enabled = true
            LineService.sync(this)
        }
        enableAfterPermission = false
        render()
    }

    override fun onPause() {
        // La vista previa solo dura mientras se ajusta: fuera de aquí vuelve el nivel real.
        binding.previewSwitch.isChecked = false
        Preview.level = null
        super.onPause()
    }

    private fun render() {
        screen = Screen(windowManager)
        val granted = Settings.canDrawOverlays(this)
        binding.permissionCard.isVisible = !granted
        binding.enabledSwitch.isChecked = granted && prefs.enabled
        renderBatteryStatus()

        binding.thickness.setRange(1, screen.statusBarHeight, prefs.thickness)
        renderOffset()
        binding.opacity.setRange(10, 100, prefs.opacity)
        binding.anchorGroup.check(
            when (prefs.anchor) {
                Anchor.LEFT -> R.id.anchorLeft
                Anchor.CENTER -> R.id.anchorCenter
                Anchor.RIGHT -> R.id.anchorRight
            },
        )

        binding.fullAt.setRange(50, 100, prefs.fullAt)
        binding.lowSwitch.isChecked = prefs.lowEnabled
        binding.lowThreshold.setRange(5, 50, prefs.lowThreshold)
        binding.lowThreshold.setEnabled(prefs.lowEnabled)

        binding.edgesTitle.setText(if (screen.landscape) R.string.edges_landscape else R.string.edges_portrait)
        renderMargins()

        binding.previewLevel.setRange(0, 100, Preview.level ?: batteryLevel() ?: 50)
        binding.previewLevel.setEnabled(binding.previewSwitch.isChecked)

        binding.bootSwitch.isChecked = prefs.startOnBoot
        binding.batteryOptStatus.setText(
            if (getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)) {
                R.string.battery_opt_off
            } else {
                R.string.battery_opt_on
            },
        )
    }

    private fun renderOffset() {
        val max = screen.statusBarHeight - prefs.thickness.coerceIn(1, screen.statusBarHeight)
        binding.offset.setRange(0, max, prefs.offset)
    }

    private fun renderMargins() {
        binding.marginLeft.setRange(0, screen.maxMargin, prefs.marginLeft(screen.landscape))
        binding.marginRight.setRange(0, screen.maxMargin, prefs.marginRight(screen.landscape))
    }

    private fun renderBatteryStatus() {
        val level = batteryLevel()
        binding.batteryStatus.text = if (level == null) {
            ""
        } else {
            getString(R.string.battery_status, level, (level * 100 / prefs.fullAt).coerceAtMost(100))
        }
    }

    private fun autoAdjustEdges() {
        val margins = screen.autoMargins(prefs.offset, prefs.thickness)
        if (margins == null) {
            Toast.makeText(this, R.string.edges_auto_unavailable, Toast.LENGTH_LONG).show()
            return
        }
        prefs.setMargins(screen.landscape, margins.first, margins.second)
        renderMargins()
    }

    /** Nivel actual leyendo el broadcast sticky, sin registrar ningún receiver. */
    private fun batteryLevel(): Int? {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        return if (level < 0 || scale <= 0) null else level * 100 / scale
    }

    private fun requestOverlayPermission() {
        val opened = runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri()))
        }.recoverCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
        }.isSuccess
        // Algunas ROMs de reproductores no traen esa pantalla: queda la vía adb del README.
        if (!opened) Toast.makeText(this, R.string.permission_unavailable, Toast.LENGTH_LONG).show()
    }

    @SuppressLint("BatteryLife")
    private fun openBatteryOptimization() {
        val ignoring = getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(packageName)
        val intent = if (ignoring) {
            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        } else {
            Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:$packageName".toUri())
        }
        runCatching { startActivity(intent) }
    }

    private fun requestAddTile() {
        getSystemService(StatusBarManager::class.java).requestAddTileService(
            ComponentName(this, LineTileService::class.java),
            getString(R.string.app_name),
            Icon.createWithResource(this, R.drawable.ic_tile),
            mainExecutor,
        ) { result ->
            if (result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED) {
                Toast.makeText(this, R.string.tile_already_added, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun px(value: Int) = getString(R.string.value_px, value)

    private fun percent(value: Int) = getString(R.string.value_percent, value)

    /**
     * Nombre, formato del valor y acción del slider; el texto del valor sigue al slider siempre.
     * Con [steps] aparecen botones −/+ que lo mueven de uno en uno, para el ajuste fino.
     */
    private fun ViewSliderBinding.bind(
        @StringRes labelRes: Int,
        format: (Int) -> String,
        steps: Boolean = false,
        action: (Int) -> Unit,
    ) {
        label.setText(labelRes)
        value.text = format(slider.value.toInt())
        slider.addOnChangeListener { _, current, fromUser ->
            value.text = format(current.toInt())
            if (fromUser) action(current.toInt())
        }
        if (!steps) return
        minus.isVisible = true
        plus.isVisible = true
        minus.setOnClickListener { step(-1, action) }
        plus.setOnClickListener { step(1, action) }
    }

    private fun ViewSliderBinding.step(delta: Int, action: (Int) -> Unit) {
        val next = (slider.value + delta).coerceIn(slider.valueFrom, slider.valueTo)
        if (next == slider.value) return
        slider.value = next
        action(next.toInt())
    }

    private fun ViewSliderBinding.setRange(from: Int, to: Int, current: Int) {
        // El slider exige valueFrom < valueTo; con un rango vacío se muestra bloqueado.
        slider.valueFrom = from.toFloat()
        slider.valueTo = maxOf(to, from + 1).toFloat()
        slider.value = current.coerceIn(from, maxOf(to, from)).toFloat()
        setEnabled(to > from)
    }

    private fun ViewSliderBinding.setEnabled(enabled: Boolean) {
        slider.isEnabled = enabled
        minus.isEnabled = enabled
        plus.isEnabled = enabled
        root.alpha = if (enabled) 1f else DISABLED_ALPHA
    }

    private companion object {
        const val DISABLED_ALPHA = 0.4f
    }
}
