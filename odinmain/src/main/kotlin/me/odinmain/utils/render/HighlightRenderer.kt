package me.odinmain.utils.render

import me.odinmain.OdinMain.mc
import me.odinmain.ui.util.shader.GlowShader
import me.odinmain.ui.util.shader.OutlineShader
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.clock.Executor.Companion.register
import me.odinmain.utils.render.RenderUtils.renderBoundingBox
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL43.glDebugMessageCallback
import org.lwjgl.util.glu.GLU

object HighlightRenderer {
    enum class HighlightType {
        Outline, Overlay, Boxes, Box2d, Glow
    }
    data class HighlightEntity(val entity: Entity, val color: Color, val thickness: Float, val depth: Boolean, val glowIntensity: Float = 1f)
    const val highlightModeDefault = "Outline"
    val highlightModeList = arrayListOf("Outline", "Overlay", "Boxes", "Box 2D", "Glow")

    private val entityGetters: MutableList<Pair<() -> HighlightType, () -> Collection<HighlightEntity>>> = mutableListOf()
    private val entities = mapOf<HighlightType, MutableList<HighlightEntity>>(
        HighlightType.Outline to mutableListOf(),
        HighlightType.Overlay to mutableListOf(),
        HighlightType.Boxes to mutableListOf(),
        HighlightType.Box2d to mutableListOf(),
        HighlightType.Glow to mutableListOf()
    )

    fun addEntityGetter(type: () -> HighlightType, getter: () -> Collection<HighlightEntity>) {
        this.entityGetters.add(type to getter)
    }

    init {
        Executor(200) {
            entities.forEach { it.value.clear() }
            entityGetters.forEach {
                entities[it.first.invoke()]?.addAll(it.second.invoke())
            }
        }.register()
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        entities[HighlightType.Boxes]?.forEach {
            Renderer.drawBox(it.entity.renderBoundingBox, it.color, it.thickness, depth = it.depth, fillAlpha = 0)
        }
        entities[HighlightType.Box2d]?.forEach {
            Renderer.draw2DEntity(it.entity, it.thickness, it.color)
        }
    }

    @SubscribeEvent
    fun on2d(event: RenderGameOverlayEvent.Pre) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        RenderHelper.disableStandardItemLighting()
        mc.renderManager.setRenderOutlines(true)
        RenderUtils.enableOutlineMode()
        if (entities[HighlightType.Outline]?.isNotEmpty() == true) {
            OutlineShader.startDraw(event.partialTicks)
            entities[HighlightType.Outline]?.forEach {
                RenderUtils.outlineColor(it.color)
                mc.renderManager.renderEntityStatic(it.entity, event.partialTicks, true)
            }
            OutlineShader.stopDraw(Color.RED, entities[HighlightType.Outline]?.firstOrNull()?.thickness ?: 1f, 1f)
        }
        if (entities[HighlightType.Glow]?.isNotEmpty() == true) {
            GlowShader.startDraw(event.partialTicks)
            entities[HighlightType.Glow]?.forEach {
                RenderUtils.outlineColor(it.color)
                mc.renderManager.renderEntityStatic(it.entity, event.partialTicks, true)
            }
            GlowShader.endDraw(
                Color.WHITE,
                entities[HighlightType.Glow]?.firstOrNull()?.thickness ?: 1f,
                entities[HighlightType.Glow]?.firstOrNull()?.glowIntensity ?: 1f
            )
        }
        RenderUtils.disableOutlineMode()
        mc.renderManager.setRenderOutlines(false)
        RenderHelper.enableStandardItemLighting()
    }
}