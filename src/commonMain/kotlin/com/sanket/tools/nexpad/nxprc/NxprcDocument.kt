package com.sanket.tools.nexpad.nxprc

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Unlimited Vector Canvas & Animation Document (.nxprc).
 * Stored as a binary package with magic header "NXRC" (0x4E, 0x58, 0x52, 0x43).
 *
 * Single Source of Truth for both NEXPAD (Android) and NEXPADDesktop.
 */
@Serializable
data class NxprcDocument(
    val version: Int = 2,
    val manifest: NxprcManifest,
    val canvas: NxprcCanvas = NxprcCanvas(),
    val animations: NxprcAnimations = NxprcAnimations()
) {
    companion object {
        val MAGIC = byteArrayOf(0x4E, 0x58, 0x52, 0x43) // "NXRC"
        fun encodeToBytes(doc: NxprcDocument): ByteArray = NxprcBinaryCodec.encode(doc)

        fun decodeFromBytes(bytes: ByteArray): Result<NxprcDocument> = NxprcBinaryCodec.decode(bytes)
    }
}

/** Severity level for a compiler diagnostic. */
enum class WarningSeverity { INFO, WARNING, DROPPED }

/**
# NEXPAD COMPONENT GENERATION PROTOCOL
 **Protocol Standard: NXPRC 10/10 Vector Engine Architecture**

## CORE RULES (QUICK SUMMARY FOR ALL MODELS):
1. Build ONE virtual controller component inside a single `<button>` element.
2. Follow the user's visual request first — user customization always wins within compiler boundaries.
3. Keep the component self-contained: one `<style>` block, system fonts, zero external assets.
4. Use only supported HTML/CSS/SVG primitives (no unsupported web page APIs).
5. Preserve the component's interaction meaning (category semantics), not a mandatory shape.
6. Make the design visually coherent with physically believable depth and lighting.
7. Use creativity when details are unspecified — never default to a generic circle unless requested or natural.
8. Design with restraint: avoid visual clutter; prefer the minimum number of layers required to achieve the requested aesthetic clearly; do not remove meaningful visual detail merely to reduce layer count.
9. Ensure the label/icon remains clearly readable with strong contrast in an unrotated DOM text node.
10. Return ONLY the complete, self-contained HTML/CSS inside one code block.
11. NEXPAD supports dual button labeling styles (Xbox: A, B, X, Y, LB, RB, LT, RT, LSB, RSB vs PlayStation: ✕, ○, □, △, L1, R1, L2, R2, L3, R3) and dynamically translates standard controller labels at runtime while preserving custom action text (e.g. ATTACK, DASH, JUMP).

You are an expert gamepad UI/UX designer and CSS shader artist creating a custom virtual controller Face Action Button for NEXPAD.

### TARGET COMPONENT IDENTITY:
- **Button Key [COMPONENT-REQUIRED]**: A (Standard Gamepad Face Button)
- **Category [GLOBAL-REQUIRED]**: BUTTON
- **Target Dimensions [GLOBAL-REQUIRED]**: width: 96px; height: 96px; (canvas bounding box)
- **Standard Color Profile [RECOMMENDED]**: Vibrant Emerald Green (Accent: #4ADE80, Glow: rgba(74, 222, 128, 0.6))
- **Standard Core [RECOMMENDED]**: linear-gradient(145deg, #10b981 0%, #059669 50%, #047857 100%)

### CATEGORY SEMANTICS & INTERACTION MEANING:
- **Interaction Meaning [COMPONENT-REQUIRED]**: Momentary discrete user actuation with tactile depression and instant spring release.
- **Visual Affordance [RECOMMENDED]**: Prominent elevation, tactile socket well, clear pressability, high-contrast center label.
- **Optional Visual Language [OPTIONAL]**: Multi-stop radial gradients, specular highlight arcs, metallic chamfer rings, neon edge halos.
- **Geometry [USER-OVERRIDE]**: `data-category` is metadata, not a shape instruction. The silhouette is completely yours: circle, hexagon, rounded rect, diamond, shield, or organic silhouette. Preserve the user's requested shape.

### DEFAULT VISUAL PROFILE / VISUAL TARGET — CONSOLE/XBOX INDUSTRIAL REALISM:
When no specific custom aesthetic or character theme is requested by the user, adopt an authentic console-grade hardware aesthetic:
1. **Matte Polycarbonate Body & Optical Depth**: Rich dual-cast molding — deep chassis base tones (`#14171e`, `#1c202a`, `#08090c`) with perimeter chamfer highlights, NOT flat monochrome or pure `#000`.
2. **Physical Contact Shadows & Recessed Socket**: Elevated dome seated inside subtle socket well (`box-shadow: 0 8px 24px rgba(0,0,0,0.65), inset 0 2px 4px rgba(255,255,255,0.4), inset 0 -6px 12px rgba(0,0,0,0.7)`).
3. **Restrained Detailing & Tactile Lighting**: Avoid unsolicited cyberpunk/neon glow clutter unless explicitly requested.
4. **Legible High-Contrast Letterform**: Prominent center glyph (A) with multi-stop 3D text shadow.

### SECTION 1 — INSTRUCTION PRIORITY & CONFLICT RESOLUTION
When instructions conflict, resolve them in this strict order of authority:
1. **Non-Negotiable Compiler Safety** [GLOBAL-REQUIRED] (Single button root, px bounds, DOM text, self-contained document, no external assets or scripts).
2. **User's Explicit Customization** [USER-OVERRIDE] (Highest design authority — user's artistic style, shape, palette, and theme always supersede defaults).
3. **Component Semantics** [COMPONENT-REQUIRED] (Preserve interaction meaning: tappable, directional, analog, etc.).
4. **Accessibility & Readability** [GLOBAL-REQUIRED] (High-contrast label legibility, touch target visibility).
5. **Design Quality Principles** [RECOMMENDED] (Physical coherence, balanced hierarchy, believable depth).
6. **Category Defaults** [RECOMMENDED] (Color palette suggestions, default glyphs used when user specifies none).
7. **Optional Inspiration** [OPTIONAL] (Theme suggestions, optional decorative flair).
8. **Starter-Template Examples [NON-BINDING SYNTAX REFERENCE]** (Syntax structure only — never copy its geometry, proportions, colors, materials, layer count, visual hierarchy, or silhouette unless those properties are independently required by the component contract or explicitly requested by the user).

> **The Golden Rule**: The user's visual and artistic instructions always win over defaults and recommendations, provided they remain compatible with the required compiler contract and component semantics.
> **Conflict Rule**: User instructions always take precedence over optional recommendations or category defaults.

### SECTION 2 — RULE CLASSIFICATION HIERARCHY
- **[GLOBAL-REQUIRED]**: Platform/engine constraints. Violation causes compiler rejection.
- **[COMPONENT-REQUIRED]**: Required for this component's interaction model (e.g. active feedback, control key).
- **[USER-OVERRIDE]**: User's explicit aesthetic requests. Highest design authority within compiler boundaries.
- **[RECOMMENDED]**: Proven design patterns for quality, depth, and touch affordance.
- **[OPTIONAL]**: Primitives and effects (SVG paths, conic gradients, filter nodes) to use when they enhance the requested aesthetic.
- **[NON-BINDING SYNTAX REFERENCE]**: Architectural syntax example only.

### SECTION 3 — USER CREATIVE AUTHORITY & FREE-HAND MODE
 **Strict on Code, Free on Design:**
```
Compiler Contract:    STRICT  (Single button, valid CSS/SVG primitives, explicit bounds)
User Visual Concept:  FREE    (Theme, character, style, geometry completely replace defaults)
AI Artistic Choice:   FREE    (Infer unspecified lighting, materials, palette, vector details)
Unsupported Details:  ADAPT   (Map impossible requests to nearest compilable representation)
Output Format:        STRICT  (Single ```html ... ``` block, zero markdown conversational text)
```
- **Free-Hand Rule**: Missing visual parameters are invitations for creative decisions, not missing information that must be filled using default style. Infer shape, palette, material, lighting, composition, texture and emblem treatment from user's concept. Do not ask for missing design parameters. Do not simplify meaningful artwork unless necessary.

### SECTION 4 — INTERPRETATION & FIDELITY MODES
- **FAITHFUL**: Preserve recognizable motifs and visual relationships.
- **INSPIRED**: Create an original design strongly influenced by them.
- **ABSTRACT**: Extract only the essential visual language.

### SECTION 5 — HARD COMPILER CONTRACT & STRICT BOUNDARIES
1. **Single compiled component [GLOBAL-REQUIRED]**: `<body>` must contain exactly one root `<button class="nexpad-btn" data-control="..." data-category="..." data-name="...">`. Keep every visual child inside it.
2. **Portable self-contained document [GLOBAL-REQUIRED]**: Include one `<style>` block, one root button, zero external assets, no `@import`, no `<link>`, no external fonts or scripts. System fonts only.
3. **Explicit geometry & positioning [GLOBAL-REQUIRED]**: Root component dimensions MUST use explicit `px` bounds (`position: relative`). Set `position: absolute`, `left`, `top`, `width`, and `height` on decorative children when deterministic layered artwork is desired. Flexbox (`display: flex`, `gap`, `justify-content`, `align-items`) MAY be used where flow/alignment is more appropriate. Dynamic `calc()` and `aspect-ratio` are supported on children.
4. **Arbitrary polygon shapes & free geometry [GLOBAL-REQUIRED]**: Use `border-radius` or `clip-path: polygon(...)` for circles, capsules, stars, diamonds, hexagons, octagons, and organic silhouettes. `data-category` is metadata, not a shape instruction. Preserve the user's requested shape.
5. **Physical 3D Layer Hierarchy & Recommended Layering [RECOMMENDED / ANTI-OCCLUSION REQUIRED]**:
- Recommended Layering Pattern:
- 0..2: Chassis housing, outer ring, ambient glow, recessed socket well
- 3..4: Main keycap face plate / center body surface (opaque base)
- 5..7: Tactile grips, ridges, secondary accents & optical halo glow
- 8..9: Embedded `<svg class="button-emblem">` vector emblem, insignia, SVG icons
- 10+: Center typography letterform & specular gloss reflections (::before/::after)
⚠️ The Hard Requirement: Opaque surface plates MUST sit underneath vector artwork and typography. In HTML DOM, declare face plate FIRST, embedded `<svg>` SECOND, and label `<span>` THIRD. Opaque surfaces must not unintentionally occlude required artwork or text.
6. **Text must be real DOM text without rotation [GLOBAL-REQUIRED]**: Labels and markings in unrotated `<span>` (`NO TEXT ROTATION`). When iconography is needed, use SVG/vector graphics; do not add text only because the component is a button.
7. **Stable CSS only [GLOBAL-REQUIRED]**: Do not use `@media`, `@supports`, `:hover`, or `:focus`. CSS transitions and layout animations are prohibited. For idle/ambient animation loops (pulsing, subtle rotation, shimmer), standard CSS `@keyframes` on transform/opacity properties are supported by the engine. Press feedback uses `.nexpad-btn:active` with spring micro-physics.
8. **Optical filters [GLOBAL-REQUIRED]**: GPU `filter: blur()`, `brightness()`, `contrast()`, `saturate()`, `hue-rotate()`. Do not use `backdrop-filter` or `mix-blend-mode`.
9. **Tactile active interaction [COMPONENT-REQUIRED]**: Always define `.nexpad-btn:active { transform: scale(...) translateY(...); }`.
10. **Tactile spring micro-physics [COMPONENT-REQUIRED]**: Component MUST declare spring variables in `:root`:
`--spring-damping: <number>;`, `--spring-stiffness: <number>;`, `--press-scale: <number>;`
Use user-specified tactile physics when provided; otherwise use category defaults (e.g. Bumpers: 0.75 / 520 / 0.96; Stick Buttons: 0.72 / 480 / 0.90; Face/Dpad/System: 0.68 / 440 / 0.92). If neither is specified, use global defaults: `--spring-damping: 0.68; --spring-stiffness: 440; --press-scale: 0.92;`.

### SECTION 6 — COMPILER CAPABILITIES — WHAT PRIMITIVES ARE BEST FOR:
#### ✅ FULLY SUPPORTED:
- `radial-gradient`: Spherical/concave shading, highlights, ambient glow.
- `linear-gradient`: Rake angles, light slopes, horizontal sheens, bevels.
- `conic-gradient`: Brushed metallic bezels, segmented rotary dials, sheen rings.
- `box-shadow`: Outset socket shadows, inset bevel rims, recessed well depths.
- `border-radius`: Circles, capsules, squircles, rounded rects.
- `clip-path: polygon(...)`: Stars, hexagons, diamonds, shields, custom silhouettes.
- `filter: blur(Npx)`, `filter: brightness(N)`, `filter: contrast(N)`: Single-function GPU filters.
- Embedded `<svg>` & Vector Nodes: `<path d="...">`, `<circle>`, `<polygon>`, `<g>`. Supports `<defs>` gradient paint servers (`linearGradient`, `radialGradient`).
- Vector Emblem Glow Rule: Vector layers compile into GPU Skia paths; do NOT rely on SVG `<filter>` graphs (`feGaussianBlur`, `feDropShadow`) on vector paths for glow. Place an underlying HTML/CSS `<span class="emblem-ambient">` with `filter: blur(4px)` or `box-shadow` underneath the `<svg>` to cast a luminous ambient aura!
- SVG Transforms & Group Matrices: Native support for `<g transform="translate(x, y) rotate(deg)">` and direct `<path transform="...">`.
- `calc()` and `aspect-ratio`: Dynamic child dimensions.
- Flexbox: Flow and alignment (`display: flex`, `gap`, `justify-content`, `align-items`).
- Ambient `@keyframes`: Supported for transform, opacity, scale, and rotate property animations.

#### ❌ NOT SUPPORTED:
- `mix-blend-mode`, `backdrop-filter`, `transition:`, `mask`, `display: grid`, layout-property keyframes.

### SECTION 7 — ARCHITECTURAL PATTERN: HTML/CSS BUTTON SHELL + EMBEDDED SVG VECTOR EMBLEM
When the user requests a character, hero, creature, vehicle, weapon, insignia, or intricate graphic:

⚠️ FORBIDDEN ANTI-PATTERN (NEVER DO THIS): Never build complex character faces, vehicle contours, or intricate emblems out of dozens of nested HTML <div> shapes or CSS clip-paths. They are brittle and hard to maintain.

✅ MANDATORY DUAL-ENGINE ARCHITECTURE (ALWAYS DO THIS):
1. The Outer HTML/CSS Button Shell (<button class="nexpad-btn" ...>):
- Handles the 3D physical tactile housing, surface material, perimeter bevel, specular curvature arc (`::after`), recessed socket shadows (`box-shadow`), and tactile active spring micro-physics (`--spring-damping: 0.68; --spring-stiffness: 440;` with `.nexpad-btn:active`).
2. The Embedded `<svg class="button-emblem" viewBox="0 0 100 100">` Vector Emblem:
- Embedded directly inside the `<button>`.
- Uses clean SVG vector paths (`<path d="...">`, `<polygon points="...">`, `<circle>`, `<ellipse>`, `<line>`) to draw the exact character, emblem, or insignia.
- Sized appropriately to sit centered or docked on the button face (e.g., `width: 50px; height: 50px; position: absolute;` or flexbox child).
- Supports `<defs>` gradient paint servers (`<linearGradient id="...">`, `<radialGradient id="...">` with `<stop>`) or solid vector fills and strokes.
- ⚠️ **VECTOR GLOW PATTERN**: Do NOT rely on SVG `<filter>` graphs (`<feGaussianBlur>`, `<feColorMatrix>`) on vector paths for glow. Instead, place an underlying HTML/CSS `<span class="emblem-ambient">` with `filter: blur(4px)` or `box-shadow: 0 0 16px var(--accent-glow)` underneath the `<svg>` to cast a luminous ambient aura!
- ✅ **SVG TRANSFORMS & ROTATIONS**: You can freely use `<g transform="translate(x,y) rotate(deg)">` or direct `<path transform="...">` to distribute or rotate shapes (such as radial petals, emblems, gear teeth, or insignia rays) around a center point, or bake coordinates directly. Both are fully compiled.
3. The High-Contrast Control Typography (<span class="btn-label">):
- Real DOM text for the gamepad key ensuring instantaneous legibility during high-speed gaming.
- ⚠️ **NO TEXT ROTATION**: Keep labels, sub-labels, and hardware text markings in straight, unrotated `<span>` elements (avoid `transform: rotate(...)` on text nodes to ensure 100% crisp subpixel font rasterization on mobile displays).

### SECTION 8 — DESIGN QUALITY CRITERIA & CONDITIONAL RESTRAINT
 **DESIGN QUALITY CRITERIA**:
1. *Recognizability*: Instantly identifiable key identity during gameplay.
2. *Legibility*: High contrast label readable at small handheld touch scales.
3. *Touch Affordance*: Visually communicates pressability, depth, and tactile actuation.
4. *Visual Hierarchy*: Primary glyph stands out above decorative bezels and ambient halos.
5. *Material Coherence*: Shading, highlights, and borders reflect a consistent material.
6. *Appropriate Depth*: Multi-tier inset/outset shadows creating realistic tactile socket recess.

 **DESIGN RESTRAINT & VISUAL BALANCE**:
- Use the minimum number of layers necessary to express the user's concept clearly; do not remove meaningful visual detail merely to reduce layer count.
- Avoid unnecessary glow or excessive shadows that visually compete with the button label.

### SECTION 9 — ADAPTATION RULES (HANDLING IMPOSSIBLE REQUESTS)
When a user request exceeds compiler limits or platform capabilities:
1. Preserve the user's primary visual, structural, and thematic intent.
2. Adapt unsupported or impractical details to the nearest supported CSS/SVG representation.
3. Do not abandon the concept.
4. Do not explain limitations or output conversational excuses.
5. Return the best compilable implementation.

### SECTION 10 — GEOMETRY & VISUAL QA CHECKLIST (SELF-CHECK BEFORE OUTPUT)
Self-check before output:
- [ ] Compiler Safety: Exactly one root `<button class="nexpad-btn"` with matching `data-control`, `data-category`, and `data-name`.
- [ ] Deterministic Bounds: Explicit px dimensions on root (`width: 96px; height: 96px;`) and layered children. Width and height must be > 0 with no NaN or infinite values.
- [ ] Boundary Containment: Children and nested plates stay within intended container bounds; padding must not clip usable content.
- [ ] Label Region Clearance: Real DOM text labels with strong contrast and readable font size in unrotated `<span>`. Labels must fit inside their intended region.
- [ ] SVG ViewBox Integrity: SVG artwork coordinates stay within the declared viewBox; no arbitrary clipping.
- [ ] Transform Origin Intent: Explicit `transform-origin` specified when rotations or scaling are applied to prevent unexpected drift.
- [ ] Visual Stacking & Occlusion: Foreground vector artwork and labels have higher z-index (or appear after) opaque background/surface plates.
- [ ] Tactile Physics: Valid active state `.nexpad-btn:active` with spring micro-physics (`--spring-damping`, `--spring-stiffness`) in `:root`.
- [ ] Clean Engine Profile: No forbidden properties (no `@media`, no external fonts, no external scripts, no `mix-blend-mode`, no `backdrop-filter`, no CSS Grid).
- [ ] Complex Graphics Architecture: If a character, emblem, or complex graphic is requested, uses an embedded `<svg class="button-emblem" viewBox="...">` vector element with clean `<path d="...">` rather than brittle CSS `<div>` hacks. For vector glow, use an underlying CSS `<span>` with `filter: blur()` or `box-shadow` (no SVG `<filter>` graphs).

### SECTION 11 — AUTHORITATIVE OUTPUT CONTRACT
To ensure reliable programmatic compilation, return ONLY the complete, self-contained HTML/CSS inside a single ```html ... ``` code block. Do NOT include any markdown conversation, explanations, or extraneous text outside it.

### USER CUSTOMIZATION SCHEMA:
The schema is a convenience, not a limitation. Users may describe any additional visual, structural, material, symbolic, or interaction concept in SPECIAL INSTRUCTIONS or free-form text. The AI follows explicit user customization above all defaults:
- **STYLE**: [e.g. Cyberpunk 2077 / Glassmorphism / Brushed Gunmetal / Retro Arcade / Minimal Flat / Anime Mecha / Custom]
- **COLOR / ACCENT**: [e.g. Neon cyan & dark obsidian / Crimson & carbon / Custom palette (Default: #4ADE80)]
- **SHAPE / SILHOUETTE**: [e.g. Faceted octagon / Smooth capsule / Organic shield / Asymmetric shard (Default: Circular)]
- **EMBLEM / GRAPHIC (OPTIONAL)**: [e.g. Embedded SVG vector emblem (`<svg viewBox="0 0 100 100"><path d="..."/></svg>`) for character art, hero logos, vehicle silhouettes, or intricate crests]
- **LABEL / GLYPH**: [e.g. "A" / Custom text / SVG icon emblem (Default: "A")]
- **MATERIAL / TEXTURE**: [e.g. Matte polycarbonate / Anodized aluminum / Smoked translucent glass / Stippled rubber]
- **LIGHTING & DEPTH**: [e.g. Top-left specular directional / Under-glow neon edge / Deep recessed socket]
- **TACTILE PHYSICS**: [e.g. Snappy micro-switch / Heavy spring depression / Soft fluid damping]
- **SPECIAL INSTRUCTIONS**: [Any specific visual elements, vector markings, or creative intent]

### USER DESIGN PARAMETERS & PREFERENCES:
- **CREATIVITY**: High (bold reinterpretation, unusual geometry, materials, and visual treatment while preserving the user's concept)
- **COMPLEXITY**: Auto (infer appropriate construction complexity from the concept) (Budget: 3..8 layers, up to 6 SVG nodes)
- **FIDELITY**: Inspired (create an original design strongly influenced by them)
- **VISUAL DENSITY**: Auto (infer from concept and target dimensions)




### USER DESIGN REQUEST:
<user_request>
Create an authentic, high-quality virtual controller component adhering to the specified design parameters.
</user_request>

Interpret this request creatively. The user request governs the visual design decisions (palette, geometry, materials, lighting, emblem), but may not override the HARD COMPILER CONTRACT.

### OUTPUT FORMAT CONTRACT:
Return ONLY the complete, self-contained HTML/CSS inside a single ```html ... ``` code block. Do NOT include any markdown conversation, explanations, or extraneous text. * A structured diagnostic emitted during HTML→NXPRC compilation.
 * Surfaces CSS properties that were silently ignored, lossy conversions, or fallback decisions,
 * along with machine-readable property names and suggested fixes for automated repair loops.
 */
data class CompileWarning(
    val severity: WarningSeverity,
    val code: String,
    val message: String,
    val source: String = "",
    val nodePath: String? = null,
    val property: String? = null,
    val originalValue: String? = null,
    val suggestedFix: String? = null
)

/** Alias for CompileWarning emphasizing structured diagnostic reporting. */
typealias CompileDiagnostic = CompileWarning

/**
 * Result of a full HTML→NXPRC compilation.
 * Always contains the compiled [document]. Any [warnings] surfaces CSS properties
 * that were silently dropped or approximated during compilation.
 * An empty [warnings] list means the HTML compiled with full fidelity.
 */
data class CompileResult(
    val document: NxprcDocument,
    val warnings: List<CompileWarning> = emptyList()
) {
    /** Structured diagnostics list equivalent to [warnings]. */
    val diagnostics: List<CompileDiagnostic> get() = warnings

    /** True if any DROPPED-severity warnings were emitted (CSS fully lost). */
    val hasLosses: Boolean get() = warnings.any { it.severity == WarningSeverity.DROPPED }

    /** Formatted summary for display in the UI or AI retry prompts. */
    fun warningsSummary(): String = if (warnings.isEmpty()) ""
        else warnings.joinToString("\n") {
            "[${it.severity}] ${it.code}: ${it.message}" +
            if (it.source.isNotEmpty()) " (source: ${it.source})" else ""
        }

    /** Structured diagnostic summary formatted specifically for AI compiler repair prompts. */
    fun structuredDiagnosticSummary(): String = if (warnings.isEmpty()) "No compiler diagnostics."
        else warnings.joinToString("\n") { w ->
            buildString {
                append("- [${w.severity}] ")
                if (w.property != null) append("Property '${w.property}': ")
                append(w.message)
                if (w.suggestedFix != null) append(" -> Suggestion: ${w.suggestedFix}")
            }
        }
}

@Serializable
data class SpringPhysicsDef(
    val dampingRatio: Float = 0.75f,
    val stiffness: Float = 400f,
    val pressedScale: Float = 0.92f,
    val enabled: Boolean = true
)

@Serializable
data class NxprcManifest(
    val id: String,                    // e.g. "rc.cyber_hex_a" - must start with "rc."
    val name: String,
    val author: String = "Designer",
    val version: String = "1.0.0",
    val category: String = "BUTTON",   // See NxprcCategory enum for valid values: BUTTON, DPAD, JOYSTICK, TRIGGER, BUMPER, HOME, SYSTEM, MACRO
    val defaultControl: String = "A",  // A, B, X, Y, LT, RT, LB, RB, LS, RS, UP, DOWN, etc.
    val widthDp: Int = 76,
    val heightDp: Int = 76,
    val description: String = "",
    val springPhysics: SpringPhysicsDef = SpringPhysicsDef()
)

@Serializable
enum class CompositingStrategy {
    AUTO,
    OFFSCREEN,
    MODULATE_ALPHA
}

@Serializable
data class LayerOutsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f
) {
    val hasOutsets: Boolean
        get() = left > 0f || top > 0f || right > 0f || bottom > 0f

    operator fun plus(other: LayerOutsets): LayerOutsets = LayerOutsets(
        left = maxOf(left, other.left),
        top = maxOf(top, other.top),
        right = maxOf(right, other.right),
        bottom = maxOf(bottom, other.bottom)
    )
}

@Serializable
data class RenderEffectDef(
    val blurRadiusX: Float = 0f,
    val blurRadiusY: Float = 0f,
    val tileMode: String = "CLAMP"
)

@Serializable
data class NxprcCanvas(
    val viewBoxWidth: Float = 100f,
    val viewBoxHeight: Float = 100f,
    val layers: List<CanvasLayer> = emptyList(),
    val clipToBounds: Boolean = false,
    val canvasOutsets: LayerOutsets = LayerOutsets(),
    val capLayerIndices: List<Int> = emptyList()
)

@Serializable
data class BoxShadowDef(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val blurRadius: Float = 0f,
    val spreadRadius: Float = 0f,
    val color: Long = NxprcDefaults.DEFAULT_SHADOW_COLOR,
    val isInset: Boolean = false
)

/** CSS visual filter subset preserved in the NXPRC render model. */
@Serializable
data class FilterDef(
    val blurRadius: Float = 0f,
    val brightness: Float = 1f,
    val saturation: Float = 1f,
    val hueRotateDegrees: Float = 0f,
    val renderEffect: RenderEffectDef = RenderEffectDef()
)

@Serializable
data class TextShadowDef(
    val offsetX: Float = 0f,
    val offsetY: Float = 2f,
    val blurRadius: Float = 0f,
    val color: Long = NxprcDefaults.DEFAULT_SHADOW_COLOR
)

@Serializable
data class TransformDef(
    val rotationDegrees: Float = 0f,
    val offsetXRatio: Float = 0f,
    val offsetYRatio: Float = 0f,
    val scaleX: Float = 1.0f,
    val scaleY: Float = 1.0f,
    val skewX: Float = 0f,
    val skewY: Float = 0f,
    val originXRatio: Float = 0.5f,
    val originYRatio: Float = 0.5f,
    val isRotating: Boolean = false
) {
    val hasTransform: Boolean
        get() = rotationDegrees != 0f || offsetXRatio != 0f || offsetYRatio != 0f ||
                scaleX != 1.0f || scaleY != 1.0f || skewX != 0f || skewY != 0f || isRotating
}

@Serializable
data class EffectsDef(
    val opacity: Float = 1.0f,
    val filter: FilterDef = FilterDef(),
    val compositingStrategy: CompositingStrategy = CompositingStrategy.AUTO,
    val layerOutsets: LayerOutsets = LayerOutsets(),
    val drawCacheHint: Boolean = false
)

@Serializable
sealed class CanvasLayer {

    /**
     * Industry-grade generic CSS box layer.
     * Supports full CSS box model: rounded corners, multi-stop fills, borders,
     * outset box-shadows, inset box-shadows, opacity, transforms.
     */
    @Serializable
    @SerialName("BoxLayer")
    data class BoxLayer(
        val shapeType: String = "ROUNDED_RECT", // ROUNDED_RECT, OVAL, POLYGON, PATH
        val polygonSides: Int = 0,
        val pathData: String = "",
        val cornerRadiusTopLeft: Float = 14f,
        val cornerRadiusTopRight: Float = 14f,
        val cornerRadiusBottomRight: Float = 14f,
        val cornerRadiusBottomLeft: Float = 14f,
        val widthRatio: Float = 1.0f,
        val heightRatio: Float = 1.0f,
        val clipToBounds: Boolean = false,
        val fill: FillBrush = FillBrush.Solid(NxprcDefaults.DEFAULT_FILL_COLOR),
        val fills: List<FillBrush> = emptyList(),
        val stroke: StrokeStyle? = null,
        val boxShadows: List<BoxShadowDef> = emptyList(),
        // Flat legacy fields kept for JSON backward-compat (old files encoded them individually).
        // New code must NOT set these directly — write via [transform] and [effects] instead.
        // [effectiveTransform] always resolves the canonical authoritative TransformDef.
        val filter: FilterDef = FilterDef(),
        val opacity: Float = 1.0f,
        @Deprecated("Use transform.rotationDegrees", ReplaceWith("transform.rotationDegrees"))
        val rotationDegrees: Float = 0f,
        @Deprecated("Use transform.offsetXRatio", ReplaceWith("transform.offsetXRatio"))
        val offsetXRatio: Float = 0f,
        @Deprecated("Use transform.offsetYRatio", ReplaceWith("transform.offsetYRatio"))
        val offsetYRatio: Float = 0f,
        @Deprecated("Use transform.scaleX", ReplaceWith("transform.scaleX"))
        val scaleX: Float = 1.0f,
        @Deprecated("Use transform.scaleY", ReplaceWith("transform.scaleY"))
        val scaleY: Float = 1.0f,
        @Deprecated("Use transform.skewX", ReplaceWith("transform.skewX"))
        val skewX: Float = 0f,
        @Deprecated("Use transform.skewY", ReplaceWith("transform.skewY"))
        val skewY: Float = 0f,
        @Deprecated("Use transform.originXRatio", ReplaceWith("transform.originXRatio"))
        val originXRatio: Float = 0.5f,
        @Deprecated("Use transform.originYRatio", ReplaceWith("transform.originYRatio"))
        val originYRatio: Float = 0.5f,
        @Deprecated("Use transform.isRotating", ReplaceWith("transform.isRotating"))
        val isRotating: Boolean = false,
        /**
         * HIGH 3 FIX: [transform] is the single authoritative source of transform data.
         * The flat fields above exist solely for backward-compat JSON deserialization.
         * Always read transforms via [effectiveTransform].
         */
        val transform: TransformDef = TransformDef(),
        val effects: EffectsDef = EffectsDef(
            opacity = opacity,
            filter = filter
        )
    ) : CanvasLayer() {
        /**
         * Returns the canonical transform for this layer.
         * Priority order (HIGH 3 FIX):
         *   1. [transform] if it has any non-default values (newly encoded documents).
         *   2. Flat legacy fields (old documents encoded before the nested TransformDef existed).
         * This ensures both old and new .nxprc files decode correctly.
         */
        @Suppress("DEPRECATION")
        val effectiveTransform: TransformDef
            get() = if (transform.hasTransform) transform
                    else TransformDef(
                        rotationDegrees = rotationDegrees,
                        offsetXRatio = offsetXRatio,
                        offsetYRatio = offsetYRatio,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        skewX = skewX,
                        skewY = skewY,
                        originXRatio = originXRatio,
                        originYRatio = originYRatio,
                        isRotating = isRotating
                    )

        val effectiveEffects: EffectsDef
            get() = if (effects != EffectsDef()) effects else EffectsDef(
                opacity = opacity,
                filter = filter
            )
    }

    @Serializable
    @SerialName("VectorPath")
    data class VectorPath(
        val pathData: String,
        val fill: FillBrush = FillBrush.Solid(NxprcDefaults.DEFAULT_FILL_COLOR),
        val stroke: StrokeStyle? = StrokeStyle(NxprcDefaults.DEFAULT_ACCENT_COLOR, 2.5f),
        val rotationDegrees: Float = 0f,
        val isRotating: Boolean = false,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f,
        val scale: Float = 1.0f
    ) : CanvasLayer()

    @Serializable
    @SerialName("GradientShape")
    data class GradientShape(
        val shapeType: String = "ROUNDED_RECT", // ROUNDED_RECT, OVAL, HEXAGON, OCTAGON
        val cornerRadius: Float = 14f,
        val fill: FillBrush = FillBrush.LinearGradient(listOf(NxprcDefaults.DEFAULT_FILL_COLOR, 0xFF003366L), 45f),
        val stroke: StrokeStyle? = StrokeStyle(NxprcDefaults.DEFAULT_ACCENT_COLOR, 2f),
        val filter: FilterDef = FilterDef(),
        val opacity: Float = 1.0f,
        // Flat legacy fields — kept for backward-compat JSON deserialization only.
        @Deprecated("Use transform.rotationDegrees", ReplaceWith("transform.rotationDegrees"))
        val rotationDegrees: Float = 0f,
        @Deprecated("Use transform.offsetXRatio", ReplaceWith("transform.offsetXRatio"))
        val offsetXRatio: Float = 0f,
        @Deprecated("Use transform.offsetYRatio", ReplaceWith("transform.offsetYRatio"))
        val offsetYRatio: Float = 0f,
        val widthRatio: Float = 1.0f,
        val heightRatio: Float = 1.0f,
        @Deprecated("Use transform.scaleX", ReplaceWith("transform.scaleX"))
        val scaleX: Float = 1.0f,
        @Deprecated("Use transform.scaleY", ReplaceWith("transform.scaleY"))
        val scaleY: Float = 1.0f,
        @Deprecated("Use transform.originXRatio", ReplaceWith("transform.originXRatio"))
        val originXRatio: Float = 0.5f,
        @Deprecated("Use transform.originYRatio", ReplaceWith("transform.originYRatio"))
        val originYRatio: Float = 0.5f,
        /** Single authoritative source of transform data. Read via [effectiveTransform]. */
        val transform: TransformDef = TransformDef(),
        val effects: EffectsDef = EffectsDef(
            opacity = opacity,
            filter = filter
        )
    ) : CanvasLayer() {
        /** LOW 3 FIX: use hasTransform (not structural equality) — same fix as BoxLayer. */
        @Suppress("DEPRECATION")
        val effectiveTransform: TransformDef
            get() = if (transform.hasTransform) transform
                    else TransformDef(
                        rotationDegrees = rotationDegrees,
                        offsetXRatio = offsetXRatio,
                        offsetYRatio = offsetYRatio,
                        scaleX = scaleX,
                        scaleY = scaleY,
                        originXRatio = originXRatio,
                        originYRatio = originYRatio
                    )

        val effectiveEffects: EffectsDef
            get() = if (effects != EffectsDef()) effects else EffectsDef(
                opacity = opacity,
                filter = filter
            )
    }

    @Serializable
    @SerialName("GlowRing")
    data class GlowRing(
        val glowColor: Long = NxprcDefaults.DEFAULT_ACCENT_COLOR,
        val blurRadius: Float = 14f,
        val pulseEnabled: Boolean = true
    ) : CanvasLayer()

    @Serializable
    @SerialName("BezelSocket")
    data class BezelSocket(
        val outerBezelColor: Long = 0xFF1C1D24L,
        val outerBevelStroke: Long = 0xFF292A30L,
        val shadowColor: Long = NxprcDefaults.DEFAULT_SHADOW_COLOR,
        val insetRatio: Float = 0.04f
    ) : CanvasLayer()

    @Serializable
    @SerialName("InnerShadow")
    data class InnerShadow(
        val shadowColor: Long = 0x73000000L,
        val highlightColor: Long = 0x35FFFFFFL,
        val strokeWidth: Float = 3.5f
    ) : CanvasLayer()

    @Serializable
    @SerialName("GlossReflection")
    data class GlossReflection(
        val offsetXRatio: Float = 0.14f,
        val offsetYRatio: Float = 0.07f,
        val widthRatio: Float = 0.55f,
        val heightRatio: Float = 0.32f,
        val rotationDegrees: Float = -18f,
        val alpha: Float = 0.75f,
        val blurRadius: Float = 0f
    ) : CanvasLayer()

    @Serializable
    @SerialName("CenterGlyph")
    data class CenterGlyph(
        val text: String? = "A",
        val fontSizeSp: Float = 22f,
        val textColor: Long = NxprcDefaults.DEFAULT_ACCENT_COLOR,
        val iconSvgPath: String? = null,
        val iconColor: Long = NxprcDefaults.DEFAULT_ACCENT_COLOR,
        val shadowColor: Long? = null,
        val shadowOffsetY: Float = 2f,
        val highlightColor: Long? = null,
        val textShadows: List<TextShadowDef> = emptyList(),
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f
    ) : CanvasLayer()

    @Serializable
    @SerialName("TextLayer")
    data class TextLayer(
        val text: String,
        val fontSizeSp: Float = 22f,
        val fontWeight: Int = 700, // 400 = normal, 700 = bold, 900 = black
        val textColor: Long = 0xFFF5F5F5L,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f,
        val textShadows: List<TextShadowDef> = emptyList(),
        val maxLines: Int = 1,
        val lineHeightSp: Float = 0f,
        val textAlign: String = "CENTER"
    ) : CanvasLayer()
}

@Serializable
sealed class FillBrush {
    @Serializable
    @SerialName("Solid")
    data class Solid(val color: Long) : FillBrush()

    @Serializable
    @SerialName("LinearGradient")
    data class LinearGradient(
        val colors: List<Long>,
        val angleDegrees: Float = 0f,
        val stops: List<Float> = emptyList()
    ) : FillBrush()

    @Serializable
    @SerialName("RadialGradient")
    data class RadialGradient(
        val colors: List<Long>,
        val radiusRatio: Float = 0.5f,
        val centerXRatio: Float = 0.5f,
        val centerYRatio: Float = 0.5f,
        val stops: List<Float> = emptyList(),
        val aspectRatio: Float = 1.0f
    ) : FillBrush()

    @Serializable
    @SerialName("SweepGradient")
    data class SweepGradient(
        val colors: List<Long>,
        val centerXRatio: Float = 0.5f,
        val centerYRatio: Float = 0.5f,
        val stops: List<Float> = emptyList(),
        val startAngleDegrees: Float = 0f
    ) : FillBrush()
}

@Serializable
data class StrokeStyle(
    val color: Long,
    val width: Float = 2f,
    val isDashed: Boolean = false,
    val dashWidth: Float = 0f,
    val dashGap: Float = 0f,
    val isTopOnly: Boolean = false
)

/** Supported animatable property types in the universal timeline track engine. */
@Serializable
enum class AnimatedProperty {
    SCALE,
    SCALE_X,
    SCALE_Y,
    ROTATION,
    OPACITY,
    TRANSLATE_X,
    TRANSLATE_Y,
    HUE_ROTATE
}

/** Normalized keyframe point on an animation timeline track. */
@Serializable
data class AnimationKeyframePoint(
    val fraction: Float, // 0.0f to 1.0f (representing 0% to 100%)
    val value: Float     // scalar value at this keyframe point
)

/** Universal timeline track driving a single animatable property over time. */
@Serializable
data class AnimationTrack(
    val property: AnimatedProperty,
    val keyframes: List<AnimationKeyframePoint>,
    val durationMs: Int = 2000,
    val isInfinite: Boolean = true,
    val easing: String = "LINEAR" // LINEAR, EASE, EASE_IN_OUT, FAST_OUT_SLOW_IN
)

@Serializable
data class NxprcAnimations(
    val idleType: String = "PULSE",       // PULSE, ROTATE, SHIMMER, RGB_CYCLE, CUSTOM, NONE
    val idleDurationMs: Int = 2000,
    val pressFeedback: String = "SPRING", // SPRING, SHOCKWAVE, FLASH
    val springStiffness: Float = 600f,
    val springDamping: Float = 0.65f,
    val pressScale: Float = 0.88f,
    val pressOffsetY: Float = 0f,
    val enableGameRumble: Boolean = true,
    val rumbleIntensity: Float = 1.0f,
    val joystickSpringTension: Float = 750f,
    val triggerMaxPullDepth: Float = 12f,
    val tracks: List<AnimationTrack> = emptyList()
)
